/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package maltcmsEval.pipes

import maltcmsEval.pipes.PostProcessing

/**
 *
 * @author Nils Hoffmann
 */
class TestDataGenerator extends Pipe<File,File>{
	def filesToGenerate = 500
	def fileExtension = "csv"
	def fileMatchExpression = "/.*\\.csv/"
	def createOutputDirectory = true
    File logLocation
    DataProvider<File> dataProvider
    @Deprecated
    boolean deleteLogOnSuccess = true
    public static boolean skipFileHashing = false

    public boolean isUpToDate(File input, File output) {
        def queryResult = database.queryByPrimaryKey(PipeDescriptor.class, uid)

        if (queryResult != null) {
            PipeDescriptor pd = PipeDescriptor.fromSql(queryResult)
            if(FileProcessPipe.skipFileHashing) {
                println "Skipping output and input file hash comparison, reenable with maltcmsEval.pipes.FileProcessPipe.skipFileHashing=false"
                println "Record for UUID ${uid} found in database!"
                return true
            }
            if (pd.isUpToDate(input, output)) {
                //println "Pipe input and output are up to date for pipe ${name} with uid ${uid}, skipping execution!"
                //print "."
                return true
            }
        }
        return false
    }

    UUID createUUID() {
        def parameters = []
		parameters << filesToGenerate
		parameters << fileMatchExpression
		parameters << fileExtension
        return UUID.nameUUIDFromBytes(parameters.join(" ").getBytes())
    }

    public File call() {
		originalDatabase = database
		database = openConnection()
		input = dataProvider.call()
		input = filterInput(input)
		uid = createUUID()
		output = new File(output, uid.toString()).canonicalFile
		//println "Running command ${command}"
		if (!isUpToDate(input, output)) {
			if (output.exists()) {
				//println "Deleting output directory of ${uid.toString()} from previous incomplete run"
				def ant = new AntBuilder()
				//ant.project.getBuildListeners().each{ it.setOutputPrintStream(new PrintStream('/dev/null')) }
				ant.delete(dir: output.absolutePath)
			}

			if (createOutputDirectory) {
				output.mkdirs()
			}
			try {
				def peakListFiles = []
				println "Copying $filesToGenerate files in $input matching $fileMatchExpression."
				//println "Input dir: $input"
				input.eachFileMatch(~fileMatchExpression){ f->
					//println "File: ${f}"
					if (f.isFile()) peakListFiles << f
				}
				if(peakListFiles.isEmpty()) {
					throw new RuntimeException("Warning: no peak lists found in ${input}")
				}
				int digits = Math.log10(filesToGenerate)+1
				def fileNameRange = 0..filesToGenerate-1
				def fileNames = new ArrayList(fileNameRange)
				//Collections.shuffle(peakListFiles)
				File smokeTestDataDir = output.absoluteFile
				smokeTestDataDir.mkdirs()
				for(idx in 0..filesToGenerate-1) {
					Integer fileIdx = idx % peakListFiles.size
					//println "$idx : $fileIdx: name: ${fileNames[idx]}"
					File source = peakListFiles[fileIdx]
					def filename = "$idx".padLeft(digits, '0' )+"."+fileExtension
					//println filename
					File destination = new File(smokeTestDataDir,filename)
					destination.getParentFile().mkdirs()
					destination.withDataOutputStream { os->
						source.withDataInputStream { is->
							os << is
						}
					}
				}
				//println "Process terminated normally with return value: ${returnVal}"
			} catch (Throwable e) {
				e.printStackTrace()
				throw new RuntimeException("Process for ${name} with ${uid} terminated with exception ${e.getLocalizedMessage()}")
			}
			while(!output.exists() && !output.isDirectory()) {
				try {
					Thread.sleep(2000);
				}catch(InterruptedException e) {

				}
			}
			storePipeDescriptor()
		}
		process(input, output)
		File out = filterOutput(output)
		if (out == null) {
			println "Output filter returned null, removing output!"
			new AntBuilder().delete(dir: output.absolutePath)
		}
		database = originalDatabase
		return out
    }

    public void storePipeDescriptor() {
        //println "Persisting PipeDescriptor"
        def params = [:]
		params["filesToGenerate"] = filesToGenerate
		params["fileMatchExpression"] = fileMatchExpression
		params["fileExtension"] = fileExtension
        params["name"] = name
        //println "Adding params: ${params}"
        PipeDescriptor pd = new PipeDescriptor(name, uid, name, getClass().getName(), params, input,
			output)
        //creates or updates the given PipeDescriptor
		PostProcessing.objects.add(pd)
        //database.create(pd)
        //print "."
    }

    public String toString() {
        StringBuilder sb = new StringBuilder()
        sb.append("Pipe ${getClass().getSimpleName()}, id=${uid.toString()}, params=${["filesToGenerate":filesToGenerate,"fileMatchExpression":fileMatchExpression,"fileExtension":fileExtension]}")
        return sb.toString()
    }
}

