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

/**
 *
 * @author Nils Hoffmann
 */
class AlignmentFilters {
	public static File locateWorkflowFile(File outputDirectory) {
		File workflowFile = new File(outputDirectory, "workflow.xml")
		if (workflowFile.exists()) {
			return workflowFile
		}
		throw new IOException("Could not locate workflow.xml file in firectory ${outputDirectory}")
	}

	public static File locateAlignmentFile(File outputDirectory, String alignmentFilename) {
		File workflowFile = new File(outputDirectory, "workflow.xml")
		File alignmentFile
		if (workflowFile.exists()) {
			//println "Found workflow.xml in ${outputDirectory}"
			def workflow = new XmlSlurper().parse(workflowFile)
			def alignmentResults = workflow.workflowElementResult.find {
				it['@slot'] == 'ALIGNMENT' && it['@file']
				.text().endsWith(alignmentFilename)
			}
			if (alignmentResults.size() == 0) {
				throw new RuntimeException("Could not locate any alignment results!")
			}
			if (alignmentResults.size() > 1) {
				println "Warning: found multiple alignment results: ${alignmentResults}"
			}
			alignmentFile = new File(alignmentResults.@file.text())
			//println "Using ${alignmentFile} for pipe ${outputDirectory.name}"
		} else {
			def alignmentFiles = []
			outputDirectory.eachFileRecurse { fit ->
				//println "Checking file ${fit.absolutePath}"
				if (fit.name.endsWith(alignmentFilename)) {
					alignmentFiles << fit
				}
			}
			if (alignmentFiles.size() > 1) {
				println "Warning: found multiple alignment results: ${alignmentFiles}"
			}else if(alignmentFiles.size() == 0) {
				println "Warning: could not locate ${alignmentFilename} in output location!"
			}else{
				alignmentFile = alignmentFiles[0]
			}
			//println "Using ${alignmentFile} for output ${outputDirectory.name}"
		}
		return alignmentFile
	}
}

