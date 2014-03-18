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
package maltcmsEval.pipes.execution

import groovy.transform.Canonical
import java.text.SimpleDateFormat
import java.util.UUID
import java.util.concurrent.Future

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
@Canonical
public class LocalProcessRunner implements IProcessRunner{

    public static boolean consumeProcessOutput = false
    public static boolean useQSub = false
    public static int maxRetries = 10
    File monitorDir = new File("monitor")
    double initialSleep = 5.0d
    UUID processID = null
    File baseDirectory = new File(".").getCanonicalFile()
    long executionTime = -1
    int returnSuccessValue = 0
    QSubProcessRunner qpr = null

    UUID getProcessID() {
        return processID
    }

	/**
	 * {@InheritDoc}
	 */
    @Override
    public int runProcess(List<String> execution) {
        return runProcess(execution,null)
    }

	/**
	 * {@InheritDoc}
	 */
    @Override
    public int runProcess(List<String> execution, UUID customId) {
        if(customId==null) {
            //println "Creating new identifier uid"
            processID = getIdentifier(execution)
        }else{
            //println "Using customId: ${customId}"
            processID = customId
        }
        //println "creating monitor directory ${monitorDir}"
		int returnCode = -1
        monitorDir.mkdirs()
        if(LocalProcessRunner.useQSub) {
            //println "Using qsub"
            if(qpr == null) {
                //println "Creating qsub process runner"
                qpr = new QSubProcessRunner()
                qpr.monitorDir = monitorDir
                qpr.baseDirectory = baseDirectory
                QSubProcessRunner.consumeProcessOutput = LocalProcessRunner.consumeProcessOutput
            }
            //println "Running process via qsub"
            int exitCode = qpr.runProcess(execution, customId)
            executionTime = qpr.getExecutionTime()
            returnCode = exitCode
        }else{
            returnCode = runLocalProcess(execution)
        }
		return returnCode
    }

    public void uncaughtException(Thread t, Throwable e) {
        println "Caught exception: ${e.getLocalizedMessage()} from ${t}"
    }

    private def runLocalProcess(List<String> execution) {
        //println "Running process locally"
        File callFile = new File(monitorDir,"${processID}.cmd")
        callFile.setText(execution.join(" "))
        //println "Building process for ${execution.join(" ")}"
        def stringExec = execution.collect{item -> item.toString()}
        ProcessBuilder builder = new ProcessBuilder(stringExec)
        //def Process proc = execution.join(" ").execute()
        //def Process proc = builder.directory(baseDirectory).redirectErrorStream(true).start()
        def Process proc = builder.redirectErrorStream(true).start()
        long startTime = System.currentTimeMillis()
        if(consumeProcessOutput) {
            //println "Redirecting process output to console"
            proc.consumeProcessOutput(System.out, System.out)
        }else{
            //println "Redirecting process output to file"
            File out = new File(monitorDir,"${processID}.eo")

            BufferedOutputStream ostream = out.newOutputStream()
            proc.consumeProcessOutput(ostream,ostream)
        }
        proc.waitFor()
        executionTime = System.currentTimeMillis()-startTime
        int exitValue = proc.exitValue()
        //remove logging information if job succeeded
        if(exitValue==returnSuccessValue) {
            File out = new File(monitorDir,"${processID}.eo")
            out.delete()
            callFile.delete()
        }
        return exitValue
    }

    protected def getIdentifier(List<String> execution) {
        def ident = UUID.nameUUIDFromBytes((execution.join("-")).getBytes())
        return ident
    }

}

