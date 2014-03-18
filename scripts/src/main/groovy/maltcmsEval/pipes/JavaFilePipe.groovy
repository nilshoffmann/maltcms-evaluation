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

import maltcmsEval.pipes.PostProcessing
import maltcmsEval.db.SqlDatabase
/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class JavaFilePipe extends FileProcessPipe {
    def JAVACOMMAND = "java"
    def JAVAARGS = []
    def JVMARGS = []

    UUID createUUID() {
        def parameters = []
        if (!PARAMS.isEmpty()) {
            PARAMS.each {
                it ->
                String tmp = it
                if (it.contains("<IN>")) {
                    tmp = it.replaceAll("<IN>", input.canonicalFile.absolutePath)
                }
                parameters << tmp
            }
        }
        if (!JAVAARGS.isEmpty()) {
            JAVAARGS.each {
                it -> parameters << it
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
				if (it.contains(" ")) {
					String[] tokens = it.split(" ")
					tokens.each { token ->
						params << token
					}
				}else{
                	params << tmp
				}
            }
        }
		params = params.collect { it ->
			String tmp = it
			if (it.contains("<IN>")) {
            	tmp = it.replaceAll("<IN>", input.canonicalFile.absolutePath)
            }
			tmp
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
        def command = [JAVACOMMAND] + JAVAARGS + JVMARGS + params
        return command
    }

    public void storePipeDescriptor(List<String> command) {
        //println "Persisting PipeDescriptor"
        def params = [:]
        JAVAARGS.each {
            arg ->
            String[] keyVal = arg.replaceAll("-D", "").split("=")
            params[keyVal[0].trim()] = keyVal[1].trim()
        }
		def spacedParams = []
		PARAMS.each {
			arg ->
			if(arg.startsWith("--")) {
				if(arg.contains("=")) {
					// process --param=value arguments
					String param = arg.replaceAll("-", "")
				    String[] keyVal = param.split("=")
			        params[keyVal[0].trim()] = keyVal[1].trim()
				}else{
					//push back --param value arguments
					String param = arg.replaceAll("-","")
					String[] keyVal = param.split(" ")
					spacedParams << keyVal[0].trim()
					spacedParams << keyVal[1].trim()
				}
			}
		}
		if(spacedParams.size()>0) {
			def collated = spacedParams.collate(2)
			collated.each {
				list ->
				if(list.size==2){
					String key = list[0]
					String value = list[1]
					params[key] = value
				}else{
					println "Sublist had ${list.size} elements!"
				}
			}
		}
        params["name"] = name
        //println "Adding params: ${params}"
        PipeDescriptor pd = new PipeDescriptor(name,uid, command.join(" "), getClass().getName(), params, input,
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
        sb.append("Pipe ${getClass().getSimpleName()}, id=${uid.toString()}, params=${PARAMS}, javaargs=${JAVAARGS}")
        return sb.toString()
    }
}
