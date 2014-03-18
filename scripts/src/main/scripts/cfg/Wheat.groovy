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

/*
*  base settings and paths
*/
//name of this evaluation instance
evalName = "Wheat_Evaluation"
//output location of processing and evaluation results
outputLocation = new File(baseDir,evalName)
//location of logging output
logLocation = new File(outputLocation,"logs")
//configuration files for maltcms
cfgDir = new File(baseDir,"../cfg")
//location of maltcms
maltcmsDir = new File(baseDir,"maltcms")
//basedir of data for evaluation
evalBaseDir = new File(baseDir,"../Hohenheim")

//location of raw data
dataDir = new File(evalBaseDir,"data")
//location of peak data
featureDir = new File(evalBaseDir,"groundTruth/xcmsPeaks/peakData")
featureFileSuffix = "*.txt"

db = SqlDatabase.open(new File(outputLocation, "${evalName}.maltcmsEval.db").path,false)

useQSub = false
idleQueue = false
//queue to use
queue = "all.q@@qics"
//per job resource pre-allocation
//virtualMemoryLowerBound = 16
//virtualMemoryUpperBound = 24
//cpusPerJob = 4
virtualMemoryLowerBound = 4
virtualMemoryUpperBound = 6
cpusPerJob = 1
parallelEnvironment = ["-pe","multislot","${cpusPerJob}"] 
queueResources = ["s_vmem=${virtualMemoryLowerBound}G","h_vmem=${virtualMemoryUpperBound}G"]

//configuration option for JavaFilePipe
maxHeap = "-Xmx${virtualMemoryUpperBound}G"

/*
 *  evaluation settings
 */
//name of the multiple alignment file to use
multipleAlignmentFile = "multiple-alignment.csv"
//location of the ground truth file to compare against
groundTruthFile = new File(evalBaseDir,"groundTruth/xcmsPeaks/xcms-meltdb.csv")
/*
 *  execution settings
 */
//limit of parallel threads to use
//leaves one spare processor on your system
maxThreads = Runtime.getRuntime().availableProcessors()-1
//only recommended for large cluster systems with 'useQSub=true'
//maxThreads = 50
//location of configuration for jdk logging
javaLogCfg = new File(cfgDir,"logging.properties").toURI().toURL().toString()
//location of configuration for log4j logging
maltcmsLogCfg = new File(cfgDir,"log4j.properties").toURI().toURL().toString()
/*
 *  feature import specific
 */
//correct for 0 or 1 based indexing scheme
scanIndexOffset = 0
/*
 *  bipace specific
 */
//retention time tolerance
rtTolerance = [0.1,1.0,5.0,10.0,30.0,40.0]
//retention time threshold lower bound
//(below this value, no calculation of arraySimilarity is performed)
rtThreshold = [0.0,0.25,0.5,0.75,0.9,0.95]
//hard threshold for rt difference, should be larger than rtTolerance
maxRTDifference = [60.0]
minCliqueSize = [2,5,10,20,30,40]
/*
 *  cemappDtw specific
 */
//retention time tolerance
rtToleranceDtw = [1.0,5.0,10.0,30.0,40.0]
//whether dtw should run on the total ion current data (TIC) or on mass spectra (MZI)
dtwFactory = ["mziDtwWorkerFactory"]
//global gap penalty for dtw
globalGapPenalty = [0.0] //,0.25,0.5,0.75,1.0]
//weight for boosting or penalizing diagonal moves within dtw recurrence
matchWeight = [2.0,2.1,2.25]
//radius around anchors to use (if anchors are available)
anchorRadius = [0,1]
//percentage of the longer of the chromatograms as constraint on band constraint width
bandWidthPercentage = [0.1]
/*
 *  bipace and cemappDtw
 */
//similarity functions to use for comparison of mass spectra
arraySimilarity = ["dotSimilarity","cosineSimilarity","linCorrSimilarity","rankCorrSimilarity","lpSimilarity"]

otherToolsPipes = [

]


