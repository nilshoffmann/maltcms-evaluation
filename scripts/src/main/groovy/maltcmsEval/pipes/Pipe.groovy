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

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
abstract class Pipe<IN,OUT> extends DataProvider<OUT>{
    String name = getClass().getSimpleName()
    UUID uid
    OUT output
    IN input
    SqlDatabase database
	SqlDatabase originalDatabase
    long runningTime = 0
    List<InputFilter<IN,OUT>> inputFilters = []
    List<OutputFilter<IN,OUT>> outputFilters = []
	List<Processor<IN,OUT>> processors = []

	public process(IN input, OUT output) {
        processors.each{
            processor ->
			print "Running processor: "
			processor.process(this, input, output)
			println "processed ${input} and ${output}"
        }
	}
	
    public IN filterInput(IN input) {
        def IN tmpIn = input
        inputFilters.each{
            filter ->
			//print "Running input filter: "
			tmpIn = filter.filterInput(this, tmpIn)
			//println "filtered ${input} to ${tmpIn}"
        }
        return tmpIn
    }

    public OUT filterOutput(OUT output) {
        def OUT tmpOut = output
        outputFilters.each{
            filter ->
			def OUT filterOut = filter.filterOutput(this,tmpOut)
			if (filterOut == null) {
				return null
			}
			tmpOut = filterOut
        }
        return tmpOut
    }
	
	public SqlDatabase openConnection() {
		SqlDatabase connection = null
		while(connection==null) {
			try{
				connection = database.child()
			}catch(java.sql.SQLException ex) {
				println "SQlException: ${ex.getLocalizedMessage()}"
			}
		}
		return connection
	}
}
