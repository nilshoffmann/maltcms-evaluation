/* 
 * Maltcms, modular application toolkit for chromatography-mass spectrometry. 
 * Copyright (C) 2008-2013, The authors of Maltcms. All rights reserved.
 *
 * Project website: http://maltcms.sf.net
 *
 * Maltcms may be used under the terms of either the
 *
 * GNU Lesser General Public License (LGPL)
 * http://www.gnu.org/licenses/lgpl.html
 *
 * or the
 *
 * Eclipse Public License (EPL)
 * http://www.eclipse.org/org/documents/epl-v10.php
 *
 * As a user/recipient of Maltcms, you may choose which license to receive the code 
 * under. Certain files or entire directories may not be covered by this 
 * dual license, but are subject to licenses compatible to both LGPL and EPL.
 * License exceptions are explicitly declared in all relevant files or in a 
 * LICENSE file in the relevant directories.
 *
 * Maltcms is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. Please consult the relevant license documentation
 * for details.
 */
package maltcmsEval.pipes.provider

import net.sf.mpaxs.spi.concurrent.CompletionServiceFactory

import maltcmsEval.pipes.JoinProvider
import maltcmsEval.pipes.Pipe
import maltcmsEval.pipes.PipeIterator

/**
 * Calls dataprovider to provide the input files. Sets up 
 * a ICompletionService and executes the pipes provided 
 * by the pipeProvider on all input files, thereby collecting
 * all created output files. 
 * 
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class FileJoinProvider extends JoinProvider<File, File> {
    public List<File> call() {
        List<File> input = dataProvider.call()
        List<File> output = []
        CompletionServiceFactory<File> poolFactory = new CompletionServiceFactory<File>()
		poolFactory.blockingWait = true
        poolFactory.maxThreads = maxThreads
        def pool = poolFactory.newLocalCompletionService()
        PipeIterator<Pipe<File, File>> pipeIter = pipeProvider.call()
        long n = input.size()*pipeIter.numberOfCombinations
        print "Running ${n} ${name} instances (${pipeIter.numberOfCombinations}"+
        " parameter combinations on ${input.size()} input location(s)) "
        for(File inputFile:input) {
            //println "Executing ${pipeIter.numberOfCombinations} combinations for input file ${inputFile.path}"
            while(pipeIter.hasNext()) {
                Pipe<File,File> pipe = pipeIter.next()
                pipe.dataProvider = new FileInputProvider(input:inputFile)
                //println "Submitting pipe!"
                pool.submit(pipe)
            }
            //reset PipeIterator (pipes should not be reused)
            pipeIter = pipeProvider.call()
        }
        //wait for pool tasks to finish
        output = pool.call()
        //remove all null values from output
        print " done!\n"
        return output - null
    }

}
