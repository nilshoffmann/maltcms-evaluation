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
import maltcmsEval.pipes.ForkJoinPipe
import maltcmsEval.pipes.JavaFilePipe
import maltcmsEval.pipes.PostProcessing
import maltcmsEval.pipes.filter.MaltcmsAlignmentResultOutputFilter
import maltcmsEval.pipes.filter.MaltcmsPipeInputFilter
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
import maltcmsEval.pipes.evaluation.InputFileCountMetrics
import maltcmsEval.pipes.TestDataGenerator
import maltcmsEval.pipes.provider.LazyDataProvider
import maltcmsEval.pipes.provider.LazyListDataProvider
import maltcmsEval.pipes.filter.InputFileCountProcessor
import maltcmsEval.pipes.provider.GenericParameterizedFilePipeProvider
import maltcmsEval.pipes.Pipe
import maltcmsEval.pipes.PipeIterator

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
//register primary key / secondary key field names for objects
SqlDatabase.primaryKeyMap.put(PipeDescriptor.class, ["uid"])
SqlDatabase.primaryKeyMap.put(InputFileCountMetrics.class, ["uid"])
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
def maltcmsJVMARGS = ["-d64","-Xmx16G","-Dcom.sun.management.jmxremote","-Dcom.sun.management.jmxremote.port=17401","-Dcom.sun.management.jmxremote.authenticate=false","-Dcom.sun.management.jmxremote.ssl=false","-cp","${cfg.maltcms.classpath}","net.sf.maltcms.apps.Maltcms"]
def guineuJVMARGS = ["-d64","-Xmx16G","-cp","${cfg.guineu.classpath}","net.sf.maltcms.guineu.GuineuEvaluation"]

//create smoke test data

def cdfFileGeneratorPipe = [
	name: ["cdfFileGenerator"],
	database: [db],
	output: [cfg.smokeTest.cdfDir],
	logLocation: [cfg.logLocation],
	filesToGenerate: cfg.smokeTest.numFiles,
	fileExtension: ["cdf"],
	fileMatchExpression: [".*\\.cdf"]
]
GenericParameterizedFilePipeProvider cdfFilePipeProvider = new GenericParameterizedFilePipeProvider(cdfFileGeneratorPipe) {
	public PipeIterator<Pipe<File, File>> call() {
        return new PipeIterator(parameters,parameterNames) {
			public Pipe<File,File> create(pipeParameters) {
				return new TestDataGenerator(pipeParameters)
			}
		}
    }
}

def csvFileGeneratorPipe = [
	name: ["csvFileGenerator"],
	database: [db],
	output: [cfg.smokeTest.csvDir],
	logLocation: [cfg.logLocation],
	filesToGenerate: cfg.smokeTest.numFiles,
	fileExtension: ["csv"],
	fileMatchExpression: [".*\\.csv"]
]

GenericParameterizedFilePipeProvider csvFilePipeProvider = new GenericParameterizedFilePipeProvider(csvFileGeneratorPipe) {
	public PipeIterator<Pipe<File, File>> call() {
        return new PipeIterator(parameters,parameterNames) {
			public Pipe<File,File> create(pipeParameters) {
				return new TestDataGenerator(pipeParameters)
			}
		}
    }
}

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
	processors: [
			[new InputFileCountProcessor(
				 fileExtension: "cdf",
				 fileMatchExpression: ".*\\.cdf"
			 )]
	],
    outputFilters: [
			[new MaltcmsWorkflowMetricsOutputFilter()]
    ],
    PARAMS: [
            ["-c", "${cfg.cfgDir}/bipace2DSmokeTest.properties", "-i", "<IN>",
                    "-f",
                    "*.cdf", "-o", "<OUT>"]
    ],
    JAVACOMMAND: [cfg.java.binary],
    JAVAARGS: mapZip(add,cfg.bipace2D.args),
    JVMARGS: [
       maltcmsJVMARGS
    ]
] as MaltcmsParameterizedPipeProvider

def guineu = [
    name: ["guineu"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "guineu")
    ],
    logLocation: [
            cfg.logLocation
    ],
	processors: [
			[new InputFileCountProcessor(
				 fileExtension: "csv",
				 fileMatchExpression: ".*\\.csv"
			 )]
	],
    outputFilters: [
			[new MaltcmsWorkflowMetricsOutputFilter()]
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

def mspaPAM = [
    name: ["mspa-pam"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "mspa")
    ],
    logLocation: [
            cfg.logLocation
    ],
	processors: [
			[new InputFileCountProcessor(
				 fileExtension: "csv",
				 fileMatchExpression: ".*\\.csv"
			 )]
	],
    outputFilters: [
			[new WorkflowMetricsOutputFilter()]
    ],
    COMMAND:[cfg.mspa.binary],
    PARAMS: mapZip(add,cfg.mspa.pam.args)
] as ParameterizedFilePipeProvider

def swpaSWRE = [
    name: ["swpa-swre"],
    database: [db],
    output: [
            new File(cfg.outputLocation, "swpa")
    ],
    logLocation: [
            cfg.logLocation
    ],
	processors: [
			[new InputFileCountProcessor(
				 fileExtension: "csv",
				 fileMatchExpression: ".*\\.csv"
			 )]
	],
    outputFilters: [
			[new WorkflowMetricsOutputFilter()]
    ],
    COMMAND:[cfg.swpa.binary],
    PARAMS: mapZip(add,cfg.swpa.swre.args)
] as ParameterizedFilePipeProvider

def cdfFileGenerationPipes = new ForkJoinPipe<File,File>(
	dataProvider: new FixedListDataProvider<File>(input: [cfg.cdfDataDir]),
	pipeProvider: cdfFilePipeProvider,
	joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "CDF File Generation")
)

def csvFileGenerationPipes = new ForkJoinPipe<File,File>(
	dataProvider: new FixedListDataProvider<File>(input: [cfg.csvDataDir]),
	pipeProvider: csvFilePipeProvider,
	joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "CSV File Generation")
)

/*
 * Maltcms related pipes
 */
def preprocessingPipe = [
    name: ["preprocessing"],
    database: [db],
    output: [new File(cfg.outputLocation, "preprocessing")],
    logLocation: [cfg.logLocation],
    PARAMS: [["-c", "${cfg.cfgDir}/preprocessing.properties", "-i", "<IN>", "-f", "*.cdf", "-o", "<OUT>"]],
    JAVACOMMAND: [cfg.java.binary],
    JAVAARGS: [["-Djava.util.logging.config.file=${cfg.javaLogCfg}", "-Dlog4j.configuration=${cfg.maltcmsLogCfg}", "-Dmaltcms.home=${cfg.maltcmsDir.absolutePath}"]],
    JVMARGS: [maltcmsJVMARGS]
] as MaltcmsParameterizedPipeProvider

def preprocessingPipes = new ForkJoinPipe<File,File>(
	dataProvider: cdfFileGenerationPipes,
	pipeProvider: preprocessingPipe,
	joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "CDF File Preprocessing")
)

//  Bipace pipes
forkJoinPipes = []
forkJoinPipes << new ForkJoinPipe<File,File>(
    dataProvider: preprocessingPipes,
    pipeProvider: bipace2D,
    joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "BiPACE 2D")
)

// Guineu pipes
forkJoinPipes << new ForkJoinPipe<File,File>(
	dataProvider: csvFileGenerationPipes,
	pipeProvider: guineu,
	joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "Guineu")
)

forkJoinPipes << new ForkJoinPipe<File,File>(
	dataProvider: csvFileGenerationPipes,
	pipeProvider: mspaPAM,
	joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "mSPA PAM")
)

forkJoinPipes << new ForkJoinPipe<File,File>(
	dataProvider: csvFileGenerationPipes,
	pipeProvider: swpaSWRE,
	joinProvider: new FileJoinProvider(maxThreads: cfg.execution.maxThreads, name: "SWPA SWRE")
)

CompletionServiceFactory<File> poolFactory = new CompletionServiceFactory<File>()
//pool should execute tasks sequentially, tasks can each execute with multiple parallel threads
poolFactory.maxThreads = 4
poolFactory.timeOut = 5
poolFactory.timeUnit = java.util.concurrent.TimeUnit.MINUTES
def pool = poolFactory.newLocalCompletionService()
forkJoinPipes.each{ job -> pool.submit(job)}
//wait until all tasks have finished
def bipaceCemappResults = pool.call()
pp.shutdown()
db.close()
println "Done!"
