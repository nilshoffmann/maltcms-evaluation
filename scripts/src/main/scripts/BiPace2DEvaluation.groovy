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
import maltcmsEval.db.SqlUtils
import maltcmsEval.pipes.PostProcessing
import maltcmsEval.pipes.ForkJoinPipe
import maltcmsEval.pipes.JavaFilePipe
import maltcmsEval.pipes.filter.MaltcmsAlignmentResultOutputFilter
import maltcmsEval.pipes.filter.MspaPairwiseAlignmentResultOutputFilter
import maltcmsEval.pipes.filter.MspaAlignmentResultOutputFilter
import maltcmsEval.pipes.filter.PairwiseAlignmentResultOutputFilter
import maltcmsEval.pipes.filter.MaltcmsPipeInputFilter
import maltcmsEval.pipes.provider.LazyListDataProvider
import maltcmsEval.pipes.provider.FileInputProvider
import maltcmsEval.pipes.provider.FileJoinProvider
import maltcmsEval.pipes.provider.FixedListDataProvider
import maltcmsEval.pipes.provider.MaltcmsParameterizedPipeProvider
import maltcmsEval.pipes.provider.ParameterizedFilePipeProvider
import net.sf.mpaxs.spi.concurrent.CompletionServiceFactory
import static maltcmsEval.Utils.*
import groovy.io.FileType
import maltcmsEval.pipes.execution.LocalProcessRunner
import maltcmsEval.pipes.PipeDescriptor
import maltcmsEval.pipes.evaluation.PerformanceMetrics
import maltcmsEval.pipes.evaluation.PairwisePerformanceMetrics
import maltcmsEval.pipes.execution.QSubProcessRunner
import maltcmsEval.pipes.filter.MaltcmsWorkflowMetricsOutputFilter
import maltcmsEval.pipes.filter.WorkflowMetricsOutputFilter
import maltcmsEval.pipes.evaluation.ExecutionMetrics

def cli = new CliBuilder(usage:'BiPace2DEvaluation')
cli.c(args:1, argName: 'cfg', 'the configuration file to use')
cli.b(args:1, argName: 'baseDir','the base directory')
cli.o(args:1, argName: 'outputBaseDir', 'the output base directory')
cli.e(args:1, argName: 'environment', 'the environment to use, either local, cluster, or cluster-cebitec')
def options = cli.parse(args)
if(options==null || !options.e) {
    System.exit(1)
}
def environment = options.e
println "Using environment ${options.e}"
println "Parsing configuration (${options.c}) with baseDir ${new File(options.b).absolutePath}"
def slurper = new ConfigSlurper(environment)
slurper.setBinding([baseDir: new File(options.b),outputBaseDir: new File(options.o)])
def configFile = new File(options.c)
def cfg = slurper.parse(configFile.toURI().toURL())
def defaultCfg = slurper.parse(new File(configFile.getParent(),"Defaults.groovy").toURI().toURL())
cfg = defaultCfg.merge(cfg)
cfg.logLocation.mkdirs()
cfg.outputLocation.mkdir()
//register primary key field names for objects
SqlDatabase.primaryKeyMap.put(PipeDescriptor.class, ["uid"])
SqlDatabase.primaryKeyMap.put(PerformanceMetrics.class,["toolName","groundTruthName"])
SqlDatabase.primaryKeyMap.put(PairwisePerformanceMetrics.class,["toolName","groundTruthName","pairName"])
SqlDatabase.primaryKeyMap.put(ExecutionMetrics.class, ["uid"])
def db = cfg.db
//closure for addition of two elements
def add = {a, b -> a + b}
println "Setting up maltcmsEval.pipes..."
LocalProcessRunner.consumeProcessOutput = cfg.execution.consumeProcessOutput
LocalProcessRunner.useQSub = cfg.qsub.useQSub
if(cfg.qsub.useQSub) {
    println "Using qsub for job submission!"
	QSubProcessRunner.createSession()
}
QSubProcessRunner.idle = cfg.qsub.idleQueue
QSubProcessRunner.qsub = cfg.qsub.qsub
QSubProcessRunner.bashString = cfg.qsub.bashString
QSubProcessRunner.queue = cfg.qsub.queue
QSubProcessRunner.parallelEnvironment = cfg.qsub.parallelEnvironment
QSubProcessRunner.queueResources = cfg.qsub.queueResources
QSubProcessRunner.environmentVariables = cfg.qsub.environmentVariables
QSubProcessRunner.maxRetries = cfg.qsub.maxRetries

PostProcessing pp = new PostProcessing(db)

if(!cfg.cfgDir.exists()) {
    println "Could not locate cfg directory in expected location ${cfg.cfgDir}"
    System.exit(1)
}
println "Using config location ${cfg.cfgDir}"
def maltcmsJVMARGS = ["-d64","-Xmx100m","-cp","${cfg.maltcms.classpath}","net.sf.maltcms.apps.Maltcms"]
def guineuJVMARGS = ["-d64","-Xmx200m","-cp","${cfg.guineu.classpath}","net.sf.maltcms.guineu.GuineuEvaluation"]
/*
 * Maltcms related pipes
 */
def preprocessingPipe = [
    name: "preprocessing",
    database: db,
    dataProvider: new FileInputProvider(input: cfg.cdfDataDir),
    output: new File(cfg.outputLocation, "preprocessing"),
    logLocation: cfg.logLocation,
    inputFilters: [new MaltcmsPipeInputFilter()],
    PARAMS: ["-c", "${cfg.cfgDir}/preprocessing.properties", "-i", "<IN>", "-f", "*.cdf", "-o", "<OUT>"],
    JAVACOMMAND: cfg.java.binary,
    JAVAARGS: ["-Djava.util.logging.config.file=${cfg.javaLogCfg}", "-Dlog4j.configuration=${cfg.maltcmsLogCfg}", "-Dmaltcms.home=${cfg.maltcmsDir.absolutePath}"],
    JVMARGS: maltcmsJVMARGS
] as JavaFilePipe

/*
 * BiPACE time penalized similarity
 * parameterized pipe provider, which creates one pipe for each unique parameter
 * combination
 */
def bipaceRt = [
    name: ["bipaceRt"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "bipaceRt")
    ],
    logLocation: [
            cfg.logLocation
    ],
    inputFilters: [
            [new MaltcmsPipeInputFilter()]
    ],
    outputFilters: [
            [new MaltcmsAlignmentResultOutputFilter(
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
                    delta: 0.0d,
                    alignmentFilename: cfg.multipleAlignmentFile,
                    keepBestKperCategory: 1
            ),
            new MaltcmsAlignmentResultOutputFilter(
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
                    delta: 0.0d,
                    alignmentFilename: cfg.multipleAlignmentFile,
                    keepBestKperCategory: 1
            ),
            new PairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new PairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile,
            ),
			new MaltcmsWorkflowMetricsOutputFilter()
            ]
    ],
    PARAMS: [
            ["-c", "${cfg.cfgDir}/bipaceRt.properties", "-i", "<IN>",
                    "-f",
                    "*.cdf", "-o", "<OUT>"]
    ],
    JAVACOMMAND: [cfg.java.binary],
    JAVAARGS: mapZip(add,cfg.bipaceRt.args),
    JVMARGS: [
       maltcmsJVMARGS
    ]
] as MaltcmsParameterizedPipeProvider

/*
 * BiPACE plain
 * parameterized pipe provider, which creates one pipe for each unique parameter
 * combination
 */
def bipacePlain = [
    name: ["bipacePlain"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "bipacePlain")
    ],
    logLocation: [
            cfg.logLocation
    ],
    inputFilters: [
            [new MaltcmsPipeInputFilter()]
    ],
    outputFilters: [
            [new MaltcmsAlignmentResultOutputFilter(
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
                    delta: 0.0d,
                    alignmentFilename: cfg.multipleAlignmentFile,
                    keepBestKperCategory: 1
            ),
            new MaltcmsAlignmentResultOutputFilter(
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
                    delta: 0.0d,
                    alignmentFilename: cfg.multipleAlignmentFile,
                    keepBestKperCategory: 1
            ),
            new PairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new PairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
			new MaltcmsWorkflowMetricsOutputFilter()
            ]
    ],
    PARAMS: [
            ["-c", "${cfg.cfgDir}/bipace.properties", "-i", "<IN>", "-f",
                    "*.cdf", "-o", "<OUT>"]
    ],
    JAVACOMMAND: [cfg.java.binary],
    JAVAARGS: mapZip(add,cfg.bipacePlain.args),
    JVMARGS: [
       maltcmsJVMARGS
    ]
] as MaltcmsParameterizedPipeProvider

/*
 * BiPACE2D
 * parameterized pipe provider, which creates one pipe for each unique parameter
 * combination
 */

def bipace2D = [
    name: ["bipace2D"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "bipace2D")
    ],
    logLocation: [
            cfg.logLocation
    ],
    inputFilters: [
            [new MaltcmsPipeInputFilter()]
    ],
    outputFilters: [
            [new MaltcmsAlignmentResultOutputFilter(
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
                    delta: 0.0d,
                    alignmentFilename: cfg.multipleAlignmentFile,
                    keepBestKperCategory: 1
            ),
            new MaltcmsAlignmentResultOutputFilter(
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
                    delta: 0.0d,
                    alignmentFilename: cfg.multipleAlignmentFile,
                    keepBestKperCategory: 1
            ),
            new PairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new PairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
			new MaltcmsWorkflowMetricsOutputFilter()
            ]
    ],
    PARAMS: [
            ["-c", "${cfg.cfgDir}/bipace2D.properties", "-i", "<IN>",
                    "-f",
                    "*.cdf", "-o", "<OUT>"]
    ],
    JAVACOMMAND: [cfg.java.binary],
    JAVAARGS: mapZip(add,cfg.bipace2D.args),
    JVMARGS: [
       maltcmsJVMARGS
    ]
] as MaltcmsParameterizedPipeProvider

/*
 * Guineu with score alignment
 * parameterized pipe provider, which creates one pipe for each unique parameter
 * combination
 *
 * Example command line:
 * java -cp Guineu.jar net.sf.maltcms.guineu.GuineuEvaluation -i <IN> -o <OUT> --arraySimilarity "weightedCos" --similarityFunction "scoreSimilarity" --maxRTDifferenceRt1 60.0 --maxRTDifferenceRt2 1.0d --rt1Penalty 1.0 --rt2Penalty 1.0 --minSpectrumMatch 0.75 --minSimilarity 600
 */

def guineu = [
    name: ["guineu"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "guineu")
    ],
    logLocation: [
            cfg.logLocation
    ],
    inputFilters: [
            [new MaltcmsPipeInputFilter()]
    ],
    outputFilters: [
            [new MaltcmsAlignmentResultOutputFilter(
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
                    delta: 0.0d,
                    alignmentFilename: cfg.multipleAlignmentFile,
                    keepBestKperCategory: 1
            ),
            new MaltcmsAlignmentResultOutputFilter(
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
                    delta: 0.0d,
                    alignmentFilename: cfg.multipleAlignmentFile,
                    keepBestKperCategory: 1
            ),
            new PairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new PairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
                new MaltcmsWorkflowMetricsOutputFilter()
            ]
    ],
    PARAMS:
            mapZip(add,cfg.guineu.args)
    ,
    JAVACOMMAND: [cfg.java.binary],
    JAVAARGS: mapZip(add,cfg.guineu.java.args),
    JVMARGS: [
       guineuJVMARGS
    ]
] as MaltcmsParameterizedPipeProvider

def mspaPad = [
    name: ["mspa-pad"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "mspa")
    ],
    logLocation: [
            cfg.logLocation
    ],
    outputFilters: [
            [new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
			new WorkflowMetricsOutputFilter()
            ]
    ],
    COMMAND:[cfg.mspa.binary],
    PARAMS: mapZip(add,cfg.mspa.pad.args)
] as ParameterizedFilePipeProvider

def mspaPas = [
    name: ["mspa-pas"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "mspa")
    ],
    logLocation: [
            cfg.logLocation
    ],
    outputFilters: [
            [new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new WorkflowMetricsOutputFilter()
            ]
    ],
    COMMAND:[cfg.mspa.binary],
    PARAMS: mapZip(add,cfg.mspa.pas.args)
] as ParameterizedFilePipeProvider

def mspaDwpas = [
    name: ["mspa-dwpas"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "mspa")
    ],
    logLocation: [
            cfg.logLocation
    ],
    outputFilters: [
            [new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
			new WorkflowMetricsOutputFilter()
            ]
    ],
    COMMAND:[cfg.mspa.binary],
    PARAMS: mapZip(add,cfg.mspa.dwpas.args)
] as ParameterizedFilePipeProvider

def mspaSwpad = [
    name: ["mspa-swpad"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "mspa")
    ],
    logLocation: [
            cfg.logLocation
    ],
    outputFilters: [
            [new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new WorkflowMetricsOutputFilter()
            ]
    ],
    COMMAND:[cfg.mspa.binary],
    PARAMS: mapZip(add,cfg.mspa.swpad.args)
] as ParameterizedFilePipeProvider

def mspaPam = [
    name: ["mspa-pam"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "mspa")
    ],
    logLocation: [
            cfg.logLocation
    ],
    outputFilters: [
            [new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new WorkflowMetricsOutputFilter()
            ]
    ],
    COMMAND:[cfg.mspa.binary],
    PARAMS: mapZip(add,cfg.mspa.pam.args)
] as ParameterizedFilePipeProvider

def swpaSwrm = [
    name: ["swpa-swrm"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "swpa")
    ],
    logLocation: [
            cfg.logLocation
    ],
    outputFilters: [
            [new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new WorkflowMetricsOutputFilter()
            ]
    ],
    COMMAND:[cfg.swpa.binary],
    PARAMS: mapZip(add,cfg.swpa.swrm.args)
] as ParameterizedFilePipeProvider

def swpaSwre = [
    name: ["swpa-swre"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "swpa")
    ],
    logLocation: [
            cfg.logLocation
    ],
    outputFilters: [
            [new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
	        new WorkflowMetricsOutputFilter()
            ]
    ],
    COMMAND:[cfg.swpa.binary],
    PARAMS: mapZip(add,cfg.swpa.swre.args)
] as ParameterizedFilePipeProvider

def swpaSwrme = [
    name: ["swpa-swrme"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "swpa")
    ],
    logLocation: [
            cfg.logLocation
    ],
    outputFilters: [
            [new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
			new WorkflowMetricsOutputFilter()
            ]
    ],
    COMMAND:[cfg.swpa.binary],
    PARAMS: mapZip(add,cfg.swpa.swrme.args)
] as ParameterizedFilePipeProvider

def swpaSwrme2 = [
    name: ["swpa-swrme2"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "swpa")
    ],
    logLocation: [
            cfg.logLocation
    ],
    outputFilters: [
            [new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mSPA",
                    groundTruthFile: cfg.groundTruthFile,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new MspaPairwiseAlignmentResultOutputFilter(
					peakListDir: cfg.dataDir,
                    groundTruthName: "mgma",
                    groundTruthFile: cfg.groundTruthFileMgma,
					alignmentFilename: cfg.multipleAlignmentFile
            ),
            new WorkflowMetricsOutputFilter()
            ]
    ],
    COMMAND:[cfg.swpa.binary],
    PARAMS: mapZip(add,cfg.swpa.swrme2.args)
] as ParameterizedFilePipeProvider

def peakListFiles = []
cfg.dataDir.eachFileMatch(~/.*\.csv/){ f->
    if (f.isFile()) peakListFiles += f
}

if(peakListFiles.isEmpty()) {
    println "Warning: no peak lists found in ${cfg.dataDir.absolutePath}"
    System.exit(1)
}

println "Using peak list files in: ${cfg.dataDir}"

def peakListsOutput = new FixedListDataProvider<File>(input: [cfg.dataDir])

print "Running preprocessing pipe "
def preprocessingOutput = new LazyListDataProvider<File>(input: [preprocessingPipe])
//fork/join pipe for parallel execution
println " done!"

//  Bipace pipes
def forkJoinPipes = []
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: preprocessingOutput,
    pipeProvider: bipaceRt,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "BiPACE w/ RT")
)

forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: preprocessingOutput,
    pipeProvider: bipacePlain,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "BiPACE")
)

forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: preprocessingOutput,
    pipeProvider: bipace2D,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "BiPACE 2D")
)

//  mSPA pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: mspaPad,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "mSPA PAD")
)

//  mSPA pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: mspaPas,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "mSPA PAS")
)

//  mSPA pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: mspaDwpas,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "mSPA DWPAS")
)

//  mSPA pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: mspaSwpad,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "mSPA SWPAD")
)


//  mSPA pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: mspaPam,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "mSPA PAM")
)

//  SWPA pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: swpaSwrm,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads,name: "SWPA SWRM")
)

//  SWPA pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: swpaSwre,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "SWPA SWRE")
)

//  SWPA pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: swpaSwrme,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "SWPA SWRME")
)

//  SWPA pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: swpaSwrme2,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "SWPA SWRME2")
)

forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: peakListsOutput,
    pipeProvider: guineu,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "Guineu")
)

CompletionServiceFactory<File> poolFactory = new CompletionServiceFactory<File>()
//pool should execute tasks sequentially, tasks can each execute with multiple parallel threads
poolFactory.maxThreads = 1
poolFactory.timeOut = 5
poolFactory.timeUnit = java.util.concurrent.TimeUnit.MINUTES
def pool = poolFactory.newLocalCompletionService()
forkJoinPipes.each{ job -> pool.submit(job)}
//wait until all tasks have finished
def bipaceCemappResults = pool.call()
pp.shutdown()
db.close()

println "Done!"
