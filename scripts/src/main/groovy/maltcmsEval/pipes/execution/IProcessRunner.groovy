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

import java.util.List
import java.util.UUID
import java.util.concurrent.Future

/**
 * Interface for process execution handlers.
 *
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
public interface IProcessRunner extends Thread.UncaughtExceptionHandler{
	/**
	 *	Run a process using the given execution string.
	 *	
	 *	@param execution the command followed by its arguments.
	 *	@return the exit code of the process
	 */
    int runProcess(List<String> execution)
	/**
	 *	Run a process using the given execution string and custom process id. 
	 *	
	 *	@param execution the command followed by its arguments.
	 *	@param customId the custom id to use for logging and identification of the process.
	 *	@return the exit code of the process
	 */
    int runProcess(List<String> execution, UUID customId)
	/**
	 *	Run a process using the given execution string.
	 *	
	 *	@return the unique id of this process
	 */
    UUID getProcessID()
	/**
	 *  Returns the wall clock time for running this process. Available after
	 *  execution has finished.
	 *  
	 *	@return the wall clock time elapsed between process start and termination
	 */
    long getExecutionTime()
}
