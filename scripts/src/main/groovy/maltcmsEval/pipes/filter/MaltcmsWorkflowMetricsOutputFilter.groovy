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
import maltcmsEval.pipes.Pipe
import maltcmsEval.pipes.PostProcessing
import maltcmsEval.db.SqlDatabase
import maltcmsEval.pipes.evaluation.ExecutionMetrics

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class MaltcmsWorkflowMetricsOutputFilter extends OutputFilter<File, File>  {

    @Override
    File filterOutput(Pipe<File, File> pipe, File input) {
        File workflowProperties = new File(new File(input,"Factory"),"workflowStats.properties")
        if(workflowProperties.exists()) {
            ConfigSlurper slurper = new ConfigSlurper()
            def cfg = slurper.parse(workflowProperties.toURI().toURL())
            ExecutionMetrics em = new ExecutionMetrics(uid:pipe.uid,cpuTime: cfg.cputime_nanoseconds,
				maxMemory:cfg.maxUsedMemory_bytes)
			PostProcessing.objects.add(em)
			//pipe.database.create(em)
        }else{
            println "Could not locate ${workflowProperties.path} for pipe ${pipe.uid}!"
        }
        return input
    }

}
