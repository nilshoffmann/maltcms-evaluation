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
class RPipe extends FileProcessPipe {
    //on unix, this is simply R CMD BATCH --vanilla
    //on windows, use something like this (adjust the path to R.exe as needed)
    //"C:\Program Files\R\R-2.13.1\bin\R.exe" CMD BATCH --vanilla
    def RCOMMAND = "R CMD BATCH"
    def RARGS = ["--vanilla", "--no-restore", "--no-save"]

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
        if (!RARGS.isEmpty()) {
            RARGS.each {
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
        def command = [RCOMMAND] + RARGS + params
        //println "Command ${command}"
		//        paramString = paramString.replaceAll("<OUT>",output.path)
		//        def command = "${RCOMMAND} ${RARGS.join(" ")} ${JVMARGS} ${JAVAJAR} ${paramString}"
        return command
    }

    public void storePipeDescriptor(List<String> command) {
        //println "Persisting PipeDescriptor"
        def params = [:]
        RARGS.each {
            arg ->
            String param = arg.replaceAll("-", "")
            String[] keyVal = param.split("=")
            params[keyVal[0].trim()] = keyVal[1].trim()
        }
        params["name"] = name
        println "Adding params: ${params}"
        PipeDescriptor pd = new PipeDescriptor(name, uid, command.join(" "), getClass().getName(), params, input,
			output)
        //creates or updates the given PipeDescriptor
		PostProcessing.objects.add(pd)
		//child.create(pd)
        //print "."
    }

    public String toString() {
        StringBuilder sb = new StringBuilder()
        sb.append("Pipe ${getClass().getSimpleName()}, id=${uid.toString()}, params=${PARAMS}, rargs=${RARGS}")
        return sb.toString()
    }
}

