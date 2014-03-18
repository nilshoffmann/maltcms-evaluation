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
import maltcmsEval.db.SqlDatabase
import maltcmsEval.pipes.PipeDescriptor
import maltcmsEval.pipes.VirtualFileProcessPipe
import maltcmsEval.pipes.evaluation.PerformanceMetrics
import maltcmsEval.pipes.filter.MaltcmsAlignmentResultOutputFilter
import maltcmsEval.pipes.filter.MaltcmsPipeInputFilter
import maltcmsEval.pipes.provider.FileInputProvider

import static maltcmsEval.Utils.createParametersTable2
import static maltcmsEval.Utils.createPerformanceTable
import static maltcmsEval.Utils.createExecutionMetricsTable
import maltcmsEval.pipes.evaluation.ExecutionMetrics

def cli = new CliBuilder(usage:'CreateTablesMspaDatasetI.groovy')
cli.c(args:1, argName:'cfg', 'the configuration file to use')
cli.b(args:1, argName: 'baseDir','the base directory')
def options = cli.parse(args)
if(options==null) {
    return 1
}

def slurper = new ConfigSlurper()
slurper.setBinding([baseDir: new File(options.b)])
def cfg = slurper.parse(new File(options.c).toURI().toURL())
//register primary key field names for objects
SqlDatabase.primaryKeyMap.put(PipeDescriptor.class, "uid")
SqlDatabase.primaryKeyMap.put(PerformanceMetrics.class,"toolName")
SqlDatabase.primaryKeyMap.put(ExecutionMetrics.class, "uid")
def evalOutDir = new File(cfg.outputLocation,"evaluation")
evalOutDir.mkdirs()

def toolNames = ["bipacePlain","bipaceRt","bipace2D",
    "mspa-pad","mspa-pas","mspa-pam","mspa-swpad", "mspa-dwpas",
    "swpa-swrm","swpa-swre", "swpa-swrme", "swpa-swrme2"]
def paramKeys = [
    "uid",
    "name",
    "similarityFunction",
    "arraySimilarity",
    "maxRTDifference",
    "rtTolerance",
    "rtThreshold",
    "maxRTDifferenceRt1",
    "rt1Tolerance",
    "rt1Threshold",
    "maxRTDifferenceRt2",
    "rt2Tolerance",
    "rt2Threshold",
    "minCliqueSize",
    "variant",
    "w",
    "k",
    "rho",
    "distance"
]
def substitutionMap = [
    "rtTolerance":"D",
    "rtThreshold":"T",
    "rt1Tolerance":"D1",
    "rt2Tolerance":"D2",
    "rt1Threshold":"T1",
    "rt2Threshold":"T2",
    "minCliqueSize":"MCS",
    "variant":"Variant",
]

//create csv tables
createPerformanceTable(cfg.db, evalOutDir)
createParametersTable2(cfg.db, evalOutDir,toolNames,paramKeys,substitutionMap)
createExecutionMetricsTable(cfg.db, evalOutDir)
cfg.db.close()
