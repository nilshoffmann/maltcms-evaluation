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
package maltcmsEval.pipes

import maltcmsEval.db.SqlDatabase
import maltcmsEval.pipes.PostProcessing
/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class FileProcessPipe extends ProcessPipe<File, File> {
    def COMMAND = ""
    def PARAMS = []
    def createOutputDirectory = false
    public static boolean skipFileHashing = false

    public boolean isUpToDate(File input, File output) {
		if(!output.exists()) {
			return false
		}
		File hashDir = new File(output.getParentFile(),".hashes")
		if(!hashDir.exists()) {
			hashDir.mkdirs()
		}
		File hashFile = new File(hashDir,uid.toString())
		String hash = null
		if(hashFile.exists()) {
			hash = hashFile.getText()
			String refHash = PipeDescriptor.createHash(input,output)
			//println "Hash file: '${hash}' calculated: '${refHash}'"
			if(hash.equals(refHash)) {
				//println "Pipe ${uid} is up to date"
				return true
			}
			hashFile.withWriter{w -> w << ""}
			return false
		}else{
			hashFile.withWriter{w -> w << ""}
			return false
		}
    }

    UUID createUUID() {
        def parameters = []
        if (!PARAMS.isEmpty()) {
            PARAMS.each {
                it ->
                String tmp = it
                if (it.contains("<IN>")) {
                    tmp = it.replaceAll("<IN>", input.path)
                }
                parameters << tmp
            }
        }
        return UUID.nameUUIDFromBytes(parameters.join(" ").getBytes())
    }

    List<String> buildCommand() {
        def params = []
        if (!PARAMS.isEmpty()) {
            PARAMS.each {
                it ->
                String tmp = it
                if (it.contains("<IN>")) {
                    tmp = it.replaceAll("<IN>", input.path)
                }
                params << tmp
            }
        }
        uid = createUUID()
        output = new File(output, uid.toString()).canonicalFile
        params = params.collect {
            it ->
            String tmp = it
            if (it.contains("<OUT>")) {
                tmp = it.replaceAll("<OUT>", output.absolutePath)
            }
            tmp
        }
        def command = [COMMAND] + params
        //println "Command ${command}"
		//        paramString = paramString.replaceAll("<OUT>",output.path)
		//        def command = "${RCOMMAND} ${RARGS.join(" ")} ${JVMARGS} ${JAVAJAR} ${paramString}"
        return command
    }

    public File call() {
		originalDatabase = database
		database = openConnection()
		input = dataProvider.call()
		//buildCommand also creates the pipe's uid based on input
		input = filterInput(input)
		List<String> command = buildCommand()
		File out = output
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
			//print "Running '${name}' with uid ${uid}..."
			runner.monitorDir = logLocation

			int returnVal = -1
			try {
				returnVal = runner.runProcess(command, uid)
				//println "Process terminated normally with return value: ${returnVal}"
			} catch (Throwable e) {
				e.printStackTrace()
				throw new RuntimeException("Process for ${name} with ${uid} terminated with exception ${e.getLocalizedMessage()}")
			}
			if (returnVal != returnSuccessValue) {
				throw new RuntimeException(
                        """
Process for ${name} with uid ${uid} terminated with code ${returnVal}!
Command Line:
${command.join(" ")}
""")
			}
			while(!output.exists() && !output.isDirectory()) {
				try {
					Thread.sleep(2000);
				}catch(InterruptedException e) {

				}
			}
		}
		storePipeDescriptor(command)
		process(input, output)
		out = filterOutput(output)
		if (out == null) {
			println "Output filter returned null, removing output!"
			new AntBuilder().delete(dir: output.absolutePath)
		}
		database = originalDatabase
		return out
    }

    public void storePipeDescriptor(List<String> command) {
        def params = [:]
        PARAMS.each {
            arg ->
            String param = arg.replaceAll("-", "")
            String[] keyVal = param.split("=")
            params[keyVal[0].trim()] = keyVal[1].trim()
        }
        params["name"] = name
        PipeDescriptor pd = new PipeDescriptor(name, uid, command.join(" "), getClass().getName(), params, input,
			output)
        //creates or updates the given PipeDescriptor
		PostProcessing.objects.add(pd)
		//database.create(pd)

		File hashDir = new File(output.getParentFile(),".hashes")
		if(!hashDir.exists()) {
			hashDir.mkdirs()
		}
		File hashFile = new File(hashDir,uid.toString())
		hashFile.withWriter{w -> w << pd.hash}
    }

    public String toString() {
        StringBuilder sb = new StringBuilder()
        sb.append("Pipe ${getClass().getSimpleName()}, id=${uid.toString()}, params=${PARAMS}")
        return sb.toString()
    }
}
