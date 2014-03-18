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
import maltcmsEval.pipes.provider.FileInputProvider
import maltcmsEval.pipes.filter.MaltcmsPipeInputFilter
import maltcmsEval.pipes.filter.MaltcmsAlignmentResultOutputFilter
import maltcmsEval.pipes.VirtualFileProcessPipe

/*
 *  base settings and paths
 */
//name of this evaluation instance
evalName = "chlamy_Dataset_I-smokeTest"
//output location of processing and evaluation results
outputLocation = new File(outputBaseDir,"${evalName}").canonicalFile
//location of logging output
logLocation = new File(outputLocation,"logs").canonicalFile
//configuration files for maltcms
cfgDir = new File(baseDir,"../maltcms/cfg").canonicalFile
//location of maltcms
maltcmsDir = new File(baseDir,"maltcms").canonicalFile
//basedir of data for evaluation
evalBaseDir = new File(baseDir,"../chlamy").canonicalFile
//location of peak reports
dataDir = new File(evalBaseDir,"data/chlamy_Dataset_I/").canonicalFile
//location of cdf files created from peak reports
cdfDataDir = new File(evalBaseDir,"data/chlamy_Dataset_I/cdf").canonicalFile
csvDataDir = dataDir

db = SqlDatabase.open(new File(outputLocation, "${evalName}.maltcmsEval.db").path,false)

/*
 *  evaluation settings
 */
//name of the multiple alignment file to use
multipleAlignmentFile = "multiple-alignment.csv"
//location of the ground truth file to compare against
groundTruthFile = new File(evalBaseDir,"groundTruth/chlamy_Dataset_I/reference-alignment.txt").canonicalFile
groundTruthFileMgma = new File(evalBaseDir,"groundTruth/chlamy_Dataset_I/reference-alignment-mgma.txt").canonicalFile
groundTruthFileManual = new File(evalBaseDir,"groundTruth/manualReference/multiple-alignment-manual.txt").canonicalFile
//location of configuration for jdk logging
javaLogCfg = new File(cfgDir,"logging.properties").canonicalFile.toURI().toURL().toString()
//location of configuration for log4j logging
maltcmsLogCfg = new File(cfgDir,"log4j.properties").canonicalFile.toURI().toURL().toString()
//maltcms pipeline defaults
maltcmsPipelineDefaults = new File(cfgDir,"xml/workflowDefaults.xml").canonicalFile.toURI().toURL().toString()

maltcms {
    classpath = new File(maltcmsDir,"maltcms.jar").canonicalPath
}

smokeTest{
    numFiles = [20,40,60,120,240,500]
    //numFiles = [500]
    cdfDir = new File(outputLocation,"smokeTestDataCdf").canonicalFile
	csvDir = new File(outputLocation,"smokeTestDataCsv").canonicalFile
	threads = [4,8,16,32]
}

/*
 * Define specific overrides of default values
 */
environments {
    local {
        java {
            virtualMemoryUnit = "m"
            virtualMemoryLowerBound = 16
            virtualMemoryUpperBound = 16384
	    	minHeap = "-Xms${virtualMemoryLowerBound}${virtualMemoryUnit}"
	   		maxHeap = "-Xmx${virtualMemoryUpperBound}${virtualMemoryUnit}"
        }
        execution {
            maxThreads = 1
            //maxThreads = Runtime.getRuntime().availableProcessors()
            //print process output to stdout/stderr (for debugging only!)
            consumeProcessOutput = true
        }
    }
    cluster {
        java {
            virtualMemoryUnit = "m"
            virtualMemoryLowerBound = 1024
            virtualMemoryUpperBound = 6192
            minHeap = "-Xms${virtualMemoryLowerBound}${virtualMemoryUnit}"
            maxHeap = "-Xmx${virtualmemoryUpperBound}${virtualMemoryUnit}"
        }
        //cfgDir = new File(baseDir,"../maltcms/cfg").canonicalFile
        execution {
            maxThreads = 1
            maxRetries = 10
        }
    }
	clusterCebitec {
        java {
            virtualMemoryUnit = "m"
            virtualMemoryLowerBound = 1024
            virtualMemoryUpperBound = 16384
            minHeap = "-Xms${virtualMemoryLowerBound}${virtualMemoryUnit}"
            maxHeap = "-Xmx${virtualmemoryUpperBound}${virtualMemoryUnit}"
        }
		qsub {
			//queue to use
			queue = "all.q@@sucslin"
			//the parallel environment to allocate
			parallelEnvironment = ["-pe","multislot","48"]
			//per job resource pre-allocation
			queueResources = ["arch=lx24-amd64"]//,"mem_free=${java.virtualMemoryUpperBound}${java.virtualMemoryUnit}"]
		}
        //cfgDir = new File(baseDir,"../maltcms/cfg").canonicalFile
        execution {
            maxThreads = 25
            maxRetries = 10
        }
    }
}
/*
 * Parameterization
 */

//BiPace2D specific retention time tolerance for first column
rt1Tolerance = [40.0]
rt1Threshold = [0.5]
//BiPace2D specific retention time tolerance for second column
rt2Tolerance = [0.5]
rt2Threshold = [0.9]
//BiPace2D specific maximum retention time difference
maxRTDifferenceRt1 = [120.0]
maxRTDifferenceRt2 = [2.0]

//minimum number of peaks in a clique to be reported
//since mSPA and SWPA only report alignments to one reference,
//this is the only fair setting
minCliqueSize = [2]
arraySimilarity = ["weightedCos"]//dotSimilarity"]
bipace2D.args = [
    "-Djava.util.logging.config.file=": [javaLogCfg],
    "-Dlog4j.configuration=": [maltcmsLogCfg],
    "-Dmaltcms.home=": [maltcmsDir.absolutePath],
    "-Drt1Tolerance=": rt1Tolerance,
    "-Drt1Threshold=": rt1Threshold,
    "-Drt2Tolerance=": rt2Tolerance,
    "-Drt2Threshold=": rt2Threshold,
    "-DsimilarityFunction=": ["timePenalizedSimilarity"],
    "-DarraySimilarity=": arraySimilarity,
    "-DmaxRTDifferenceRt1=": maxRTDifferenceRt1,
    "-DmaxRTDifferenceRt2=": maxRTDifferenceRt2,
    "-DminCliqueSize=": minCliqueSize,
	"-Dthreads=": smokeTest.threads
]

//location of guineuDir
guineu {
	dir = new File(baseDir,"../guineu").canonicalFile
	classpath = new File(guineu.dir,"Guineu.jar").canonicalPath
	args = [
		"--input ":["<IN>"],
		"--output ":["<OUT>"],
		"--rt1Penalty ": [0.1],
		"--rt2Penalty ": [0.1],
		"--rtiPenalty ": [0.0],
		"--similarityFunction ": ["scoreSimilarity"],
		"--arraySimilarity ": ["weightedCos"],
		"--minSimilarity ": [600],
		"--minSpectrumMatch ": [0.5],
		"--maxRTDifferenceRt1 ": [120.0],
		"--maxRTDifferenceRt2 ": [2.0],
		"--threads ": smokeTest.threads
	]
	java.args = [
		"-Djava.util.logging.config.file=": [new File(guineu.dir,"conf/logging.properties").canonicalPath],
		"-Dguineu.home=": [guineu.dir.absolutePath],
	]
}

filePattern = "*\\.csv"

/*
 * mspa configuration
*/
mspa.binary = new File(baseDir,"../mSPA/mspa/mspa-evaluation.R").canonicalFile.absolutePath

mspa.pam.args = [
    "--filePrefix=": [filePattern],
    "--dataPath=": ["<IN>"],
    "--variant=": ["PAM"],
    "--w=": [0.95],
    "--distance=": ["canberra"],
    "--arraySimilarity=": ["linCorr"],
    "--directory=": ["<OUT>"]
]
/*
 * swpa configuration
 */
swpa.binary = new File(baseDir,"../SWPA/swpa/swpa-evaluation.R").canonicalFile.absolutePath

swpa.swre.args = [
    "--filePrefix=": [filePattern],
    "--dataPath=": ["<IN>"],
    "--variant=":["SWRE"],
    "--distance=": ["canberra"],
    "--arraySimilarity=":["linCorr"],
    "--rho=":[0.9],
    "--directory=": ["<OUT>"]
]
