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
package maltcmsEval.pipes.filter

import maltcmsEval.pipes.Processor
import maltcmsEval.pipes.Pipe
import maltcmsEval.pipes.PostProcessing
import maltcmsEval.db.SqlDatabase
import maltcmsEval.pipes.evaluation.InputFileCountMetrics
import cross.datastructures.fragments.FileFragment
import cross.datastructures.fragments.IVariableFragment
import ucar.ma2.Array

/**
 *
 * @author Nils Hoffmann
 */
class InputFileCountProcessor extends Processor<File, File>  {

	def fileExtension = "csv"
	def fileMatchExpression = "/.*\\.csv/"

    @Override
    void process(Pipe<File, File> pipe, File input, File output) {
		int nfiles = 0
		int npeaks = 0
		println "Matching files with expression: $fileMatchExpression"
		input.eachFileMatch(~fileMatchExpression){ f->
			println "File: ${f}"
			if (f.isFile()){
				nfiles++
				if(fileExtension.equals("cdf")) {
					FileFragment fragment = new FileFragment(f)
					IVariableFragment si = fragment.getChild("scan_index")
					Array a = si.getArray()
					npeaks+=a.getShape()[0]
				}else if(fileExtension.equals("csv")) {
					int num = 0;
					f.eachLine { line -> if(!line.isEmpty()) {num++} }
					//subtract one for the header line
					npeaks+=(num-1)
				}
			}
		}
		println "Found $nfiles files!"
		InputFileCountMetrics em = new InputFileCountMetrics(uid:pipe.uid, nfiles: nfiles, npeaks: npeaks)
		//pipe.database.create(em)
		PostProcessing.objects.add(em)
    }

}

