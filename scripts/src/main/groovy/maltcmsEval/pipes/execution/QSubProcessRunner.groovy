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

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.SessionFactory;
import java.util.concurrent.Future
/**
 * Extension of {@link LocalProcessRunner} to execute jobs on 
 * a grid execution environment using the drmaa api. 
 * 
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
public class QSubProcessRunner extends LocalProcessRunner {

    public static String bashString = "/vol/gnu/bin/bash"
    public static String qsub = "/vol/codine-6.2/bin/sol-amd64/qsub"
    public static String queue = "all.q@@qics"
    public static List parallelEnvironment = []
    public static List environmentVariables = []
    public static List queueResources = []
    public static boolean idle = true
	public static Session session = null
    long executionTime = -1
	
	/**
	 * Static method to start a global drmaa session.
	 * Also initializes the {@link QSubProcessReaper} and registers 
	 * it as a shutdown hook to be executed when the VM receives a 
	 * SIGTERM for an orderly shutdown of running tasks.
	 */
	public static void createSession() {
		if(QSubProcessRunner.session==null) {
			println "Creating session"
			SessionFactory factory = SessionFactory.getFactory()
			QSubProcessRunner.session = factory.getSession()
			QSubProcessRunner.session.init("")
			QSubJobReaper qsjr = new QSubJobReaper()
			qsjr.session = QSubProcessRunner.session
			Runtime.getRuntime().addShutdownHook(qsjr)
		}
	}

	public final static class QSubJobReaper extends Thread {
		Session session

		@Override
		public void run() {
			println "Running QSubJobReaper"
			if(session!=null) {
				println "Terminating pending jobs!"
				try {
					session.control(Session.JOB_IDS_SESSION_ALL,Session.TERMINATE)
				}catch(DrmaaException ex) {
					println("Error while terminating job: " + ex.getMessage());
				}
				try {
					session.exit()
				}catch(DrmaaException ex) {
					println("Error while exiting session: " + ex.getMessage());
				}
			}else{
				throw new NullPointerException("Session is null!")
			}
		}
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
        monitorDir.mkdirs()
        if(customId==null) {
            processID = getIdentifier(execution)
        }else{
            processID = customId
        }
        return runQSubProcess(execution)
    }

	private def runQSubProcess(List<String> execution) {
		String command = execution[0]
		String[] arguments = execution.tail() as String[]
		File script = new File(monitorDir,"${processID}.sh")
        def str = ["#!${bashString}",execution.join(" "),"exit \$?"]
        script.setText(str.join("\n"))
        File out = new File(monitorDir,"${processID}.eo")
        script.setExecutable(true)

		if(idle) {
			queueResources << "idle=1"
		}
		int exitValue = -1
		
		try {
			JobTemplate jt = session.createJobTemplate();
			jt.setRemoteCommand(command);
			jt.setArgs(Arrays.asList(arguments))
			jt.setJobEnvironment(System.getProperties())
			StringBuilder nativeSpec = new StringBuilder()
			if(queue.length()>0) {
				nativeSpec.append "-q ${queue} "
				//			nativeSpec.append "-l wc_qdomain=${queue} "
			}
			if(!queueResources.isEmpty()) {
				queueResources.each{
					it -> nativeSpec.append "-l ${it} "
				}
			}		 
			if(!parallelEnvironment.isEmpty()) {
				nativeSpec.append "${parallelEnvironment.join(" ")} "
			}  
			if(!environmentVariables.isEmpty()) {
				environmentVariables.each{
					it -> nativeSpec.append "-v ${it} "
				}
			}
		 
//			println "NativeSpec: \"${nativeSpec.toString().trim()}\""
			jt.setNativeSpecification(nativeSpec.toString().trim());
			jt.setWorkingDirectory(System.getProperty("user.dir"))
			jt.setJobName("maltcms-${processID}")
			jt.setOutputPath(":${out.absolutePath}")
			jt.setJoinFiles(true)
		
			String id = session.runJob(jt);
			//System.out.println("Your job has been submitted with id " + id);
			session.deleteJobTemplate(jt);
			JobInfo info = session.wait(id, Session.TIMEOUT_WAIT_FOREVER);

			if (info.wasAborted()) {
				System.out.println("Job " + info.getJobId() + " never ran");
			} else if (info.hasExited()) {
				exitValue = info.getExitStatus()
			} else if (info.hasSignaled()) {
				/*System.out.println("Job " + info.getJobId() +
				" finished due to signal " +
				info.getTerminatingSignal());*/
			} else {
				//System.out.println("Job " + info.getJobId() +
				//				  " finished with unclear conditions");
			}
		 
		} catch (DrmaaException e) {
			System.out.println("Error: " + e.getMessage());
		}
		//remove logging information if job succeeded and no error occurred
        if(exitValue==returnSuccessValue) {
            out.delete()
            script.delete()
        }
		return exitValue
	}
}
