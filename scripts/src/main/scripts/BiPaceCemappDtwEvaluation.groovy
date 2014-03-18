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
import maltcmsEval.pipes.PostProcessing
import maltcmsEval.pipes.ForkJoinPipe
import maltcmsEval.pipes.JavaFilePipe
import maltcmsEval.pipes.filter.MaltcmsAlignmentResultOutputFilter
import maltcmsEval.pipes.filter.MaltcmsPipeInputFilter
import maltcmsEval.pipes.provider.FileInputProvider
import maltcmsEval.pipes.provider.FileJoinProvider
import maltcmsEval.pipes.provider.FixedListDataProvider
import maltcmsEval.pipes.provider.MaltcmsParameterizedPipeProvider
import net.sf.mpaxs.spi.concurrent.CompletionServiceFactory
import static maltcmsEval.Utils.*
import maltcmsEval.pipes.execution.LocalProcessRunner
import maltcmsEval.pipes.PipeDescriptor
import maltcmsEval.pipes.evaluation.PerformanceMetrics
import maltcmsEval.pipes.execution.QSubProcessRunner
import maltcmsEval.pipes.filter.MaltcmsWorkflowMetricsOutputFilter
import maltcmsEval.pipes.evaluation.ExecutionMetrics

def cli = new CliBuilder(usage:'Evaluation.groovy')
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
QSubProcessRunner.environmentVariables = cfg.qsub.environmentVariables
QSubProcessRunner.queueResources = cfg.qsub.queueResources

PostProcessing pp = new PostProcessing(db)
/*
cli.c(args:1, argName:'cfg', 'the configuration file to use')
cli.b(args:1, argName: 'baseDir','the base directory')
def options = cli.parse(args)
if(options==null) {
    return 1
}
*/
int mb = 1024*1024
//Getting the runtime reference from system
Runtime runtime = Runtime.getRuntime()
println("Used Memory:"
	+ String.format("%.2f",(runtime.totalMemory() - runtime.freeMemory()) / mb) +"MB")
//Print free memory
println("Free Memory:"
	+ String.format("%.2f",runtime.freeMemory() / mb)+"MB")
//Print total available memory
println("Total Memory:" + String.format("%.2f",runtime.totalMemory() / mb)+"MB")
//Print Maximum available memory
println("Max Memory:" + String.format("%.2f",runtime.maxMemory() / mb)+"MB")
/*
println "Parsing configuration (${options.c}) with baseDir ${options.b}"
def slurper = new ConfigSlurper()
slurper.setBinding([baseDir: new File(options.b)])
def cfg = slurper.parse(new File(options.c).toURI().toURL())
cfg.logLocation.mkdirs()
cfg.outputLocation.mkdir()
*/
//register primary key field names for objects
/*
SqlDatabase.primaryKeyMap.put(PipeDescriptor.class, "uid")
SqlDatabase.primaryKeyMap.put(PerformanceMetrics.class,"toolName")
SqlDatabase.primaryKeyMap.put(ExecutionMetrics.class, "uid")
def db = cfg.db
*/
//closure for addition of two elements
/*
def add = {a, b -> a + b}
println "Setting up maltcmsEval.pipes..."
LocalProcessRunner.consumeProcessOutput = false
LocalProcessRunner.useQSub = cfg.qsub.useQSub
QSubProcessRunner.idle = cfg.qsub.idleQueue
QSubProcessRunner.qsub = cfg.qsub.qsub
QSubProcessRunner.queue = cfg.qsub.queue
QSubProcessRunner.parallelEnvironment = cfg.parallelEnvironment
QSubProcessRunner.queueResources = cfg.queueResources
*/
if(!cfg.cfgDir.exists()) {
    println "Could not locate cfg directory in expected location ${cfg.cfgDir}"
    exit 1
}
println "Using config location ${cfg.cfgDir}"
/*
 * Feature Import
 */
def featureFiles = "${cfg.featureDir.path}/${cfg.featureFileSuffix}"
println "Using peak input files: ${featureFiles}"

def featureImportPipe = [
        name: "featureImport",
        database: db,
        dataProvider: new FileInputProvider(input: cfg.dataDir),
        output: new File(cfg.outputLocation, "featureImport"),
        logLocation: cfg.logLocation,
        PARAMS: ["-jar", "${cfg.maltcmsDir}/maltcms.jar", "-c", "${cfg.cfgDir}/featureImport.properties", "-i", "<IN>",
                "-f", "*.cdf", "-o", "<OUT>"],
        JAVAARGS: ["-Djava.util.logging.config.file=${cfg.javaLogCfg}",
                "-Dlog4j.configuration=${cfg.maltcmsLogCfg}",
                "-DfilesToRead=${featureFiles}",
                "-DscanIndexOffset=${cfg.scanIndexOffset}"],
        JVMARGS: ["-d64","${cfg.maxHeap}"]
] as JavaFilePipe

def preprocessingPipe = [
        name: "preprocessing",
        database: db,
        dataProvider: featureImportPipe,
        output: new File(cfg.outputLocation, "preprocessing"),
        logLocation: cfg.logLocation,
        inputFilters: [new MaltcmsPipeInputFilter()],
        PARAMS: ["-jar", "${cfg.maltcmsDir}/maltcms.jar", "-c", "${cfg.cfgDir}/preprocessing.properties", "-i", "<IN>", "-f", "*.cdf", "-o", "<OUT>"],
        JAVAARGS: ["-Djava.util.logging.config.file=${cfg.javaLogCfg}", "-Dlog4j.configuration=${cfg.maltcmsLogCfg}"],
        JVMARGS: ["-d64","${cfg.maxHeap}"]
] as JavaFilePipe
//preprocessingPipe.call()
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
                        groundTruthFile: cfg.groundTruthFile,
                        delta: 0.0d,
                        alignmentFilename: cfg.multipleAlignmentFile,
                        keepBestKperCategory: 1
                ),
                    new MaltcmsWorkflowMetricsOutputFilter()
                ]
        ],
        PARAMS: [
                ["-jar", "${cfg.maltcmsDir}/maltcms.jar", "-c", "${cfg.cfgDir}/bipaceRt.properties", "-i", "<IN>",
                        "-f",
                        "*.cdf", "-o", "<OUT>"]
        ],
        JAVAARGS: mapZip(add,
                [
                        "-Djava.util.logging.config.file=": [cfg.javaLogCfg],
                        "-Dlog4j.configuration=": [cfg.maltcmsLogCfg],
                        "-DrtTolerance=": cfg.rtTolerance,
                        "-DrtThreshold=": cfg.rtThreshold,
                        "-DsimilarityFunction=": ["timePenalizedSimilarity"],
                        "-DarraySimilarity=": cfg.arraySimilarity,
                        "-DmaxRTDifference=": cfg.maxRTDifference,
			            "-DminCliqueSize=": cfg.minCliqueSize
                ]
        ),
        JVMARGS: [
        	["-d64","${cfg.maxHeap}"]
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
                        groundTruthFile: cfg.groundTruthFile,
                        delta: 0.0d,
                        alignmentFilename: cfg.multipleAlignmentFile,
                        keepBestKperCategory: 1
                ),
                        new MaltcmsWorkflowMetricsOutputFilter()
                ]
        ],
        PARAMS: [
                ["-jar", "${cfg.maltcmsDir}/maltcms.jar", "-c", "${cfg.cfgDir}/bipace.properties", "-i", "<IN>", "-f",
                        "*.cdf", "-o", "<OUT>"]
        ],
        JAVAARGS: mapZip(add,
                [
                        "-Djava.util.logging.config.file=": [cfg.javaLogCfg],
                        "-Dlog4j.configuration=": [cfg.maltcmsLogCfg],
                        "-DsimilarityFunction=": ["plainSimilarity"],
                        "-DarraySimilarity=": cfg.arraySimilarity,
                        "-DmaxRTDifference=": cfg.maxRTDifference,
			            "-DminCliqueSize=": cfg.minCliqueSize
                ]
        ),
        JVMARGS: [
        	["-d64","${cfg.maxHeap}"]
        ]
] as MaltcmsParameterizedPipeProvider

/*
 * Cemapp DTW plain
 * parameterized pipe provider, which creates one pipe for each unique parameter
 * combination
 */
def cemappDtwPlain = [
        name: ["cemappDtwPlain"],
        database: [db],
        output: [
                new File(cfg.outputLocation, "cemappDtwPlain")
        ],
        logLocation: [
                cfg.logLocation
        ],
        inputFilters: [
                [new MaltcmsPipeInputFilter()]
        ],
        outputFilters: [
                [new MaltcmsAlignmentResultOutputFilter(
                        groundTruthFile: cfg.groundTruthFile,
                        delta: 0.0d,
                        alignmentFilename: cfg.multipleAlignmentFile,
                        keepBestKperCategory: 1
                ),
                        new MaltcmsWorkflowMetricsOutputFilter()
                ]
        ],
        PARAMS: [
                ["-jar", "${cfg.maltcmsDir}/maltcms.jar", "-c", "${cfg.cfgDir}/cemappDtw.properties", "-i", "<IN>",
                        "-f",
                        "*.cdf", "-o", "<OUT>"]
        ],
        JAVAARGS: mapZip(add,
                [
                        "-Djava.util.logging.config.file=": [cfg.javaLogCfg],
                        "-Dlog4j.configuration=": [cfg.maltcmsLogCfg],
                        "-DrtTolerance=":[Double.NaN],
                        "-DdtwFactory=":cfg.dtwFactory,
                        "-DglobalGapPenalty=":cfg.globalGapPenalty,
                        "-DmatchWeight=":cfg.matchWeight,
                        "-DanchorRadius=":cfg.anchorRadius,
                        "-DuseAnchors=":[false],
                        "-DglobalBand=":[true],
                        "-DbandWidthPercentage=":cfg.bandWidthPercentage,
                        "-DsimilarityFunction=": ["plainPairwiseSimilarity"],
                        "-DarraySimilarity=": cfg.arraySimilarity
                ]
        ),
        JVMARGS: [
        	["-d64","${cfg.maxHeap}"]
        ]
] as MaltcmsParameterizedPipeProvider

/*
 * Cemapp DTW time penalized
 * parameterized pipe provider, which creates one pipe for each unique parameter
 * combination
 */
def cemappDtwRt = [
        name: ["cemappDtwRt"],
        database: [db],
        output: [
                new File(cfg.outputLocation, "cemappDtwRt")
        ],
        logLocation: [
                cfg.logLocation
        ],
        inputFilters: [
                [new MaltcmsPipeInputFilter()]
        ],
        outputFilters: [
                [new MaltcmsAlignmentResultOutputFilter(
                        groundTruthFile: cfg.groundTruthFile,
                        delta: 0.0d,
                        alignmentFilename: cfg.multipleAlignmentFile,
                        keepBestKperCategory: 1
                ),
                        new MaltcmsWorkflowMetricsOutputFilter()
                ]
        ],
        PARAMS: [
                ["-jar", "${cfg.maltcmsDir}/maltcms.jar", "-c", "${cfg.cfgDir}/cemappDtwRt.properties", "-i", "<IN>",
                        "-f",
                        "*.cdf", "-o", "<OUT>"]
        ],
        JAVAARGS: mapZip(add,
                [
                        "-Djava.util.logging.config.file=": [cfg.javaLogCfg],
                        "-Dlog4j.configuration=": [cfg.maltcmsLogCfg],
                        "-DrtTolerance=":cfg.rtToleranceDtw,
                        "-DdtwFactory=":cfg.dtwFactory,
                        "-DglobalGapPenalty=":cfg.globalGapPenalty,
                        "-DmatchWeight=":cfg.matchWeight,
                        "-DanchorRadius=":cfg.anchorRadius,
                        "-DuseAnchors=":[false],
                        "-DglobalBand=":[true],
                        "-DbandWidthPercentage=":cfg.bandWidthPercentage,
                        "-DsimilarityFunction=": ["timePenalizedPairwiseSimilarity"],
                        "-DarraySimilarity=": cfg.arraySimilarity
                ]
        ),
        JVMARGS: [
        	["-d64","${cfg.maxHeap}"]
        ]
] as MaltcmsParameterizedPipeProvider

/*
 * Cemapp DTW plain with anchors
 * parameterized pipe provider, which creates one pipe for each unique parameter
 * combination
 */
def cemappDtwPlainAnchors = [
        name: ["cemappDtwPlain"],
        database: [db],
        output: [
                new File(cfg.outputLocation, "cemappDtwPlain")
        ],
        logLocation: [
                cfg.logLocation
        ],
        inputFilters: [
                [new MaltcmsPipeInputFilter()]
        ],
        outputFilters: [
                [new MaltcmsAlignmentResultOutputFilter(
                        groundTruthFile: cfg.groundTruthFile,
                        delta: 0.0d,
                        alignmentFilename: cfg.multipleAlignmentFile,
                        keepBestKperCategory: 1
                ),
                        new MaltcmsWorkflowMetricsOutputFilter()
                ]
        ],
        PARAMS: [
                ["-jar", "${cfg.maltcmsDir}/maltcms.jar", "-c", "${cfg.cfgDir}/cemappDtw.properties", "-i", "<IN>",
                        "-f",
                        "*.cdf", "-o", "<OUT>"]
        ],
        JAVAARGS: mapZip(add,
                [
                        "-Djava.util.logging.config.file=": [cfg.javaLogCfg],
                        "-Dlog4j.configuration=": [cfg.maltcmsLogCfg],
                        "-DrtTolerance=":[Double.NaN],
                        "-DdtwFactory=":cfg.dtwFactory,
                        "-DglobalGapPenalty=":cfg.globalGapPenalty,
                        "-DmatchWeight=":cfg.matchWeight,
                        "-DanchorRadius=":cfg.anchorRadius,
                        "-DuseAnchors=":[true],
                        "-DglobalBand=":[false],
                        "-DbandWidthPercentage=":cfg.bandWidthPercentage,
                        "-DsimilarityFunction=": ["plainPairwiseSimilarity"],
                        "-DarraySimilarity=": cfg.arraySimilarity
                ]
        ),
        JVMARGS: [
        	["-d64","${cfg.maxHeap}"]
        ]
] as MaltcmsParameterizedPipeProvider

/*
 * Cemapp DTW time penalized with anchors
 * parameterized pipe provider, which creates one pipe for each unique parameter
 * combination
 */
def cemappDtwRtAnchors = [
        name: ["cemappDtwRt"],
        database: [db],
        output: [
                new File(cfg.outputLocation, "cemappDtwRt")
        ],
        logLocation: [
                cfg.logLocation
        ],
        inputFilters: [
                [new MaltcmsPipeInputFilter()]
        ],
        outputFilters: [
                [new MaltcmsAlignmentResultOutputFilter(
                        groundTruthFile: cfg.groundTruthFile,
                        delta: 0.0d,
                        alignmentFilename: cfg.multipleAlignmentFile,
                        keepBestKperCategory: 1
                ),
                        new MaltcmsWorkflowMetricsOutputFilter()
                ]
        ],
        PARAMS: [
                ["-jar", "${cfg.maltcmsDir}/maltcms.jar", "-c", "${cfg.cfgDir}/cemappDtwRt.properties", "-i", "<IN>",
                        "-f",
                        "*.cdf", "-o", "<OUT>"]
        ],
        JAVAARGS: mapZip(add,
                [
                        "-Djava.util.logging.config.file=": [cfg.javaLogCfg],
                        "-Dlog4j.configuration=": [cfg.maltcmsLogCfg],
                        "-DrtTolerance=":cfg.rtToleranceDtw,
                        "-DdtwFactory=":cfg.dtwFactory,
                        "-DglobalGapPenalty=":cfg.globalGapPenalty,
                        "-DmatchWeight=":cfg.matchWeight,
                        "-DanchorRadius=":cfg.anchorRadius,
                        "-DuseAnchors=":[true],
                        "-DglobalBand=":[false],
                        "-DbandWidthPercentage=":cfg.bandWidthPercentage,
                        "-DsimilarityFunction=": ["timePenalizedPairwiseSimilarity"],
                        "-DarraySimilarity=": cfg.arraySimilarity
                ]
        ),
        JVMARGS: [
        	["-d64","${cfg.maxHeap}"]
        ]
] as MaltcmsParameterizedPipeProvider

def preprocessingOutput = new FixedListDataProvider<File>(input: [preprocessingPipe.call()])
//fork/join pipe for parallel execution
/*
    Bipace pipes
 */
ForkJoinPipe<File, File> bipaceRtPipe = [
        dataProvider: preprocessingOutput,
        pipeProvider: bipaceRt,
        joinProvider: new FileJoinProvider(maxThreads: cfg.maxThreads)
] as ForkJoinPipe<File, File>

ForkJoinPipe<File, File> bipacePlainPipe = [
        dataProvider: preprocessingOutput,
        pipeProvider: bipacePlain,
        joinProvider: new FileJoinProvider(maxThreads: cfg.maxThreads)
] as ForkJoinPipe<File, File>

/*
    Cemapp DTW pipes with default input and no anchors
 */
//fork pipe for parallel execution
ForkJoinPipe<File, File> cemappDtwRtPipe = [
        dataProvider: preprocessingOutput,
        pipeProvider: cemappDtwRt,
        joinProvider: new FileJoinProvider(maxThreads: cfg.maxThreads)
] as ForkJoinPipe<File, File>

ForkJoinPipe<File, File> cemappDtwPlainPipe = [
        dataProvider: preprocessingOutput,
        pipeProvider: cemappDtwPlain,
        joinProvider: new FileJoinProvider(maxThreads: cfg.maxThreads)
] as ForkJoinPipe<File, File>

CompletionServiceFactory<File> poolFactory = new CompletionServiceFactory<File>()
//pool should execute tasks sequentially, tasks can execute with multiple parallel threads
poolFactory.maxThreads = 1

def pool = poolFactory.newLocalCompletionService()
pool.submit(bipacePlainPipe)
pool.submit(bipaceRtPipe)
pool.submit(cemappDtwPlainPipe)
pool.submit(cemappDtwRtPipe)
//wait until all tasks have finished
def bipaceCemappResults = pool.call()

/*
   Query database for best bipace results
*/

File bipaceBestF1Output = getBestF1Result(db, ["bipacePlain", "bipaceRt"])
println "Best result: ${bipaceBestF1Output}"

//select best bipace TP result and best FP result and feed into cemappDTW runs
//fork pipe for parallel execution
ForkJoinPipe<File, File> bipaceBestTpCemappDtwRtPipe = [
        dataProvider: new FixedListDataProvider<File>(input: [bipaceBestF1Output]),
        pipeProvider: cemappDtwRtAnchors,
        joinProvider: new FileJoinProvider(maxThreads: cfg.maxThreads)
] as ForkJoinPipe<File, File>

ForkJoinPipe<File, File> bipaceBestTpCemappDtwPlainPipe = [
        dataProvider: new FixedListDataProvider<File>(input: [bipaceBestF1Output]),
        pipeProvider: cemappDtwPlainAnchors,
        joinProvider: new FileJoinProvider(maxThreads: cfg.maxThreads)
] as ForkJoinPipe<File, File>

pool = poolFactory.newLocalCompletionService()
pool.submit(bipaceBestTpCemappDtwRtPipe)
pool.submit(bipaceBestTpCemappDtwPlainPipe)

//wait for cemappDtwWithAnchors to finish
def cemappDtwWithAnchorsResults = pool.call()
pp.shutdown()
db.close()
println "Done!"


