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
evalName = "mSPA_Dataset_I_short"
//output location of processing and evaluation results
outputLocation = new File(outputBaseDir,"${evalName}").canonicalFile
//location of logging output
logLocation = new File(outputLocation,"logs").canonicalFile
//configuration files for maltcms
cfgDir = new File(baseDir,"../maltcms/cfg").canonicalFile
//location of maltcms
maltcmsDir = new File(baseDir,"maltcms").canonicalFile
//basedir of data for evaluation
evalBaseDir = new File(baseDir,"../mSPA").canonicalFile
//location of peak reports
dataDir = new File(evalBaseDir,"data/mSPA_Dataset_I").canonicalFile
//location of cdf files created from peak reports
cdfDataDir = new File(evalBaseDir,"data/mSPA_Dataset_I/cdf").canonicalFile

db = SqlDatabase.open(new File(outputLocation, "${evalName}.maltcmsEval.db").path,false)

/*
 *  evaluation settings
 */
//name of the multiple alignment file to use
multipleAlignmentFile = "multiple-alignment.csv"
//location of the ground truth file to compare against
groundTruthFile = new File(evalBaseDir,"groundTruth/mSPA_Dataset_I/reference-alignment.txt").canonicalFile
groundTruthFileMgma = new File(evalBaseDir,"groundTruth/mSPA_Dataset_I/reference-alignment-mgma.txt").canonicalFile
//location of configuration for jdk logging
javaLogCfg = new File(cfgDir,"logging.properties").canonicalFile.toURI().toURL().toString()
//location of configuration for log4j logging
maltcmsLogCfg = new File(cfgDir,"log4j.properties").canonicalFile.toURI().toURL().toString()
//maltcms pipeline defaults
maltcmsPipelineDefaults = new File(cfgDir,"xml/workflowDefaults.xml").canonicalFile.toURI().toURL().toString()

maltcms {
    classpath = new File(maltcmsDir,"maltcms.jar").canonicalPath
}

/*
 * Define specific overrides of default values
 */
environments {
    local {
        java {
            virtualMemoryUnit = "m"
            virtualMemoryLowerBound = 16
            virtualMemoryUpperBound = 256
        }
        execution {
            //set to true to print process output to stdout/stderr (for debugging only!)
            consumeProcessOutput = true
        }
    }
    cluster {
        java {
            minHeap = "-Xms256m"
            maxHeap = "${minHeap}"
        }
        //cfgDir = new File(baseDir,"../maltcms/cfg").canonicalFile
    }
	clusterCebitec {
        java {
            minHeap = "-Xms256m"
            maxHeap = "${minHeap}"
        }
        //cfgDir = new File(baseDir,"../maltcms/cfg").canonicalFile
    }
}
/*
 * Parameterization
 */
/*
 *  bipace specific
 */
//retention time tolerance
//rtTolerance = [0.5,1.0,2.0,2.5,3.0,5.0,10.0,15.0,20.0,25.0,30.0]
rtTolerance = [20.0]
//retention time threshold lower bound
//(below this value, no calculation of arraySimilarity is performed)
//rtThreshold = [0.5]
rtThreshold = [0.0,0.25,0.5,0.75,0.9,0.95]
//hard threshold for rt difference, should be larger than rtTolerance
maxRTDifference = [60.0]

//BiPace2D specific retention time tolerance for first column
//rt1Tolerance = [2.5,5.0]
//rt1Threshold = [0.2,0.25,0.3]
rt1Tolerance = [40.0]//[10.0,20.0,30.0,40.0]
rt1Threshold = [0.1]//[0.1,0.25,0.5,0.75]
//rt1Threshold = [0.0]
//BiPace2D specific retention time tolerance for second column
//rt2Tolerance = [1.0]
//rt2Tolerance = [0.05,0.1,0.15]
rt2Tolerance = [0.1]//[0.1,0.5,1,1.5]
//rt2Threshold = [0.4,0.5,0.6]
rt2Threshold = [0.8]//[0.8,0.9,0.95,0.99]

maxRTDifferenceRt1 = [100.0]
maxRTDifferenceRt2 = [2.0]

//minimum number of peaks in a clique to be reported
minCliqueSize = [2]//,7,8,9,10]

arraySimilarity = ["dotSimilarity","weightedCos","cosineSimilarity","linCorrSimilarity"]
//arraySimilarity = ["dotSimilarity","linCorrSimilarity"]

bipacePlain.args = [
    "-Djava.util.logging.config.file=": [javaLogCfg],
    "-Dmaltcms.home=": [maltcmsDir.absolutePath],
    "-Dlog4j.configuration=": [maltcmsLogCfg],
    "-DsimilarityFunction=": ["plainSimilarity"],
    "-DarraySimilarity=": arraySimilarity,
    "-DmaxRTDifference=": maxRTDifference,
    "-DminCliqueSize=": minCliqueSize
]

bipaceRt.args = [
    "-Djava.util.logging.config.file=": [javaLogCfg],
    "-Dlog4j.configuration=": [maltcmsLogCfg],
    "-Dmaltcms.home=": [maltcmsDir.absolutePath],
    "-DrtTolerance=": rtTolerance,
    "-DrtThreshold=": rtThreshold,
    "-DsimilarityFunction=": ["timePenalizedSimilarity"],
    "-DarraySimilarity=": arraySimilarity,
    "-DmaxRTDifference=": maxRTDifference,
    "-DminCliqueSize=": minCliqueSize
]

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
    "-DminCliqueSize=": minCliqueSize
]

//location of guineuDir
guineu {
	dir = new File(baseDir,"../guineu").canonicalFile
	classpath = new File(guineu.dir,"Guineu.jar").canonicalPath
	args = [
		"--input ":["<IN>"],
		"--output ":["<OUT>"],
		"--rt1Penalty ": [0.0,0.5,1.0],
		"--rt2Penalty ": [0.0,0.5,1.0],
		"--rtiPenalty ": [0.0],
		"--similarityFunction ": ["scoreSimilarity"],
		"--arraySimilarity ": ["weightedCos"],
		"--minSimilarity ": [400,500,600],
		"--minSpectrumMatch ": [0.25,0.5,0.75,0.9],
		"--maxRTDifferenceRt1 ": [60.0],
		"--maxRTDifferenceRt2 ": [2.0],
		"--threads ": [1]
	]
	java.args = [
		"-Djava.util.logging.config.file=": [javaLogCfg],
		"-Dguineu.home=": [guineu.dir.absolutePath]
	]
}

/*
 * mspa configuration
 */
mspa.binary = new File(baseDir,"../mSPA/mspa/mspa-evaluation.R").canonicalFile.absolutePath
mspa.pad.args = [
    "--filePrefix=": ["Standard_"],
    "--dataPath=": [dataDir.absolutePath],
    "--variant=": ["PAD"],
    "--distance=": ["euclidean","maximum","manhattan","canberra"],
    "--arraySimilarity=": ["dot","linCorr"],
    "--directory=": ["<OUT>"]
]
mspa.pas.args = [
    "--filePrefix=": ["Standard_"],
    "--dataPath=": [dataDir.absolutePath],
    "--variant=": ["PAS"],
    "--distance=": ["euclidean","maximum","manhattan","canberra"],
    "--arraySimilarity=": ["dot","linCorr"],
    "--directory=": ["<OUT>"]
]
mspa.dwpas.args = [
    "--filePrefix=": ["Standard_"],
    "--dataPath=": [dataDir.absolutePath],
    "--variant=": ["DW-PAS"],
    "--k=": [3,5,10,15,20],
    "--distance=": ["euclidean","maximum","manhattan","canberra"],
    "--arraySimilarity=": ["dot","linCorr"],
    "--directory=": ["<OUT>"]
]
mspa.swpad.args = [
    "--filePrefix=": ["Standard_"],
    "--dataPath=": [dataDir.absolutePath],
    "--variant=": ["SW-PAD"],
    //"--rho=": [0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,0.93,0.95,0.97,0.99],
    "--rho=": [0.9,0.93,0.95],
    "--distance=": ["euclidean","maximum","manhattan","canberra"],
    "--arraySimilarity=": ["dot","linCorr"],
    "--directory=": ["<OUT>"]
]
mspa.pam.args = [
    "--filePrefix=": ["Standard_"],
    "--dataPath=": [dataDir.absolutePath],
    "--variant=": ["PAM"],
    //"--w=": [0.01,0.05,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,0.95,0.99],
    //"--w=": [0.4,0.5,0.6],
	"--w=": [0.6],
    //"--distance=": ["euclidean","maximum","manhattan","canberra"],
	"--distance=": ["canberra"],
    //"--arraySimilarity=": ["dot","linCorr"],
	"--arraySimilarity=": ["linCorr"],
    "--directory=": ["<OUT>"]
]
/*
 * swpa configuration
 */
swpa.binary = new File(baseDir,"../SWPA/swpa/swpa-evaluation.R").canonicalFile.absolutePath
swpa.swrm.args = [
    "--filePrefix=": ["Standard_"],
    "--dataPath=": [dataDir.absolutePath],
    "--variant=":["SWRM"],
    "--distance=": ["euclidean","maximum","manhattan","canberra"],
    "--arraySimilarity=":["dot","linCorr"],
    "--rho=":[0.8,0.9,0.93], //"--rho=":[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,0.93,0.95,0.97,0.99],
    "--directory=": ["<OUT>"]
]
swpa.swre.args = [
    "--filePrefix=": ["Standard_"],
    "--dataPath=": [dataDir.absolutePath],
    "--variant=":["SWRE"],
    //"--distance=": ["euclidean","maximum","manhattan","canberra"],
	"--distance=": ["euclidean"],
 //   "--arraySimilarity=":["dot","linCorr"],
	"--arraySimilarity=":["linCorr"],
    //"--rho=":[0.8,0.9,0.93], //"--rho=":[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,0.93,0.95,0.97,0.99],
	"--rho=":[0.93],
    "--directory=": ["<OUT>"]
]
swpa.swrme.args = [
    "--filePrefix=": ["Standard_"],
    "--dataPath=": [dataDir.absolutePath],
    "--variant=":["SWRME"],
    "--distance=": ["euclidean","maximum","manhattan","canberra"],
    "--arraySimilarity=":["dot","linCorr"],
    "--rho=":[0.8,0.9,0.93], //"--rho=":[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,0.93,0.95,0.97,0.99],
    "--directory=": ["<OUT>"]
]
swpa.swrme2.args = [
    "--filePrefix=": ["Standard_"],
    "--dataPath=": [dataDir.absolutePath],
    "--variant=":["SWRME2"],
    "--distance=": ["euclidean","maximum","manhattan","canberra"],
    "--arraySimilarity=":["dot","linCorr"],
    "--rho=":[0.8,0.9,0.93], //"--rho=":[0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9,0.93,0.95,0.97,0.99],
    "--directory=": ["<OUT>"]
]
