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
package maltcmsEval.pipes

import maltcmsEval.pipes.execution.LocalProcessRunner

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
abstract class ProcessPipe<IN,OUT> extends Pipe<IN,OUT> {
    int returnSuccessValue = 0
    int returnValue = 0
    File logLocation
    DataProvider<IN> dataProvider
    @Deprecated
    boolean deleteLogOnSuccess = true
    LocalProcessRunner runner = new LocalProcessRunner()
    abstract List<String> buildCommand()

    public OUT call() {
		input = dataProvider.call()
        input = filterInput(input)
		//buildCommand also creates the pipe's uid based on input
        List<String> command = buildCommand()
        uid = UUID.nameUUIDFromBytes(command.getBytes())
        //print "Running '${name}' with uid ${uid}..."
        runner.monitorDir = logLocation
        try{
            returnValue = runner.runProcess(command,uid)
        }catch(Throwable e) {
            throw new RuntimeException("Process for ${name} with command ${command} with uid ${uid} terminated with"+
                    " exception ${e.getLocalizedMessage()}")
        }
        if(returnValue!=returnSuccessValue) {
            throw new RuntimeException("Process for ${name} with command ${command} with uid ${uid} terminated with ${returnValue}!")
        }
		process(input, output)
        return filterOutput(output)
    }
}
