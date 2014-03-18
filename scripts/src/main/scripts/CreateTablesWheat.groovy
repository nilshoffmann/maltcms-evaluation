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
import maltcmsEval.pipes.evaluation.PerformanceMetrics

import static maltcmsEval.Utils.createParametersTable
import static maltcmsEval.Utils.createPerformanceTable
import static maltcmsEval.Utils.createExecutionMetricsTable
import maltcmsEval.pipes.evaluation.ExecutionMetrics

def cli = new CliBuilder(usage:'CreateTablesWheat.groovy')
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

//create csv tables
createPerformanceTable(cfg.db, evalOutDir)
createParametersTable(cfg.db, evalOutDir)
createExecutionMetricsTable(cfg.db, evalOutDir)
cfg.db.close()
