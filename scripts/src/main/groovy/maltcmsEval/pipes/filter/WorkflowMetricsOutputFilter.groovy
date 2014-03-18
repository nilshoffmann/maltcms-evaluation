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
package maltcmsEval.pipes.filter

import maltcmsEval.pipes.OutputFilter
import maltcmsEval.pipes.PostProcessing
import maltcmsEval.pipes.Pipe
import maltcmsEval.db.SqlDatabase
import maltcmsEval.pipes.evaluation.ExecutionMetrics

/**
 * OutputFilter that parses the file 'runtime.txt' in a pipe's output
 * directory. The file is a simple config file (key = value) with keys
 * 'cputime_nanoseconds' and 'maxUsedMemory_bytes'.
 *
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class WorkflowMetricsOutputFilter extends OutputFilter<File, File>  {

    @Override
    File filterOutput(Pipe<File, File> pipe, File input) {
        def timingFiles = []
        input.eachFileRecurse { fit ->
            //println "Checking file ${fit.absolutePath}"
            if (fit.name.equals("runtime.txt")) {
                timingFiles << fit
            }
        }
        if (timingFiles.size() > 1) {
            println "Warning: found multiple runtime results: ${timingFiles}"
            println "Using first: ${timingFiles[0]}"
        }
        if(timingFiles.size()!=0) {
            def timingFile = timingFiles[0]
            //println "Using timing file ${timingFile}"
            ConfigSlurper slurper = new ConfigSlurper()
            def cfg = slurper.parse(timingFile.toURI().toURL())
            def nanotime = cfg.cputime_nanoseconds
            def bytes = cfg.maxUsedMemory_bytes
            //println "Runtime was ${nanotime} ns, max memory was ${bytes} bytes.)"
            ExecutionMetrics em = new ExecutionMetrics(uid:pipe.uid,cpuTime:nanotime,
				maxMemory:bytes)
			PostProcessing.objects.add(em)
			//pipe.database.create(em)
            //timingFile.delete()
        } else {
            println "Could not locate runtime.txt for pipe ${pipe.uid}!"
        }

        return input
    }

}
