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

import groovy.sql.GroovyRowResult
import maltcmsEval.pipes.PostProcessing
import maltcmsEval.db.SqlDatabase
import maltcmsEval.db.SqlUtils
import maltcmsEval.pipes.OutputFilter
import maltcmsEval.pipes.Pipe
import maltcmsEval.pipes.evaluation.PerformanceMetrics
import net.sf.maltcms.evaluation.api.classification.EntityGroup
import net.sf.maltcms.evaluation.api.classification.PeakRTFeatureVectorComparator
import net.sf.maltcms.evaluation.spi.EntityGroupBuilder
import net.sf.maltcms.evaluation.spi.classification.ClassificationPerformanceTest
import net.sf.maltcms.evaluation.spi.classification.PeakRTFeatureVector
import static maltcmsEval.pipes.filter.AlignmentFilters.*

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class MaltcmsAlignmentResultOutputFilter extends OutputFilter<File, File> {

    File groundTruthFile
    String groundTruthName
    double delta = 0.0d
    String alignmentFilename = "multiple-alignmentRT.csv"
    int keepBestKperCategory = 2

	boolean isUpToDate(SqlDatabase database, Pipe<File,File> pipe, String groundTruthName) {
		return false
		/*
		List<GroovyRowResult> rows = database.query("SELECT * FROM "+SqlUtils.getTableName(PerformanceMetrics.class)+" WHERE TOOLNAME='"+pipe.uid.toString()+"' AND GROUNDTRUTHNAME='"+groundTruthName+"' LIMIT 1")
		if(rows==null || rows.isEmpty()) {
			return false
		}
		return true
		*/
	}

    @Override
    File filterOutput(Pipe<File, File> pipe, File input) {
		File alignmentFile = locateAlignmentFile(input, alignmentFilename)
		if (alignmentFile == null) {
			throw new IOException("Could not locate alignment file ${alignmentFilename}")
		}
        //println "Running alignment result output filter for ${groundTruthName}"
        //perform the alignment classification
        if(groundTruthName == null) {
            groundTruthName = groundTruthFile.canonicalPath
        }
		if(!isUpToDate(pipe.database,pipe,groundTruthName)) {
			EntityGroupBuilder egb = new EntityGroupBuilder();
			List<EntityGroup> gt = egb.buildCSVPeakAssociationGroups(groundTruthFile)
			List<EntityGroup> toolGroups = egb.buildCSVPeakAssociationGroups(alignmentFile)
			if(alignmentFile.readLines().size==0 || alignmentFile.readLines().size==1) {
				//println "Alignment file for ${pipe.uid.toString()} is empty!"
			} else {
				try {
					ClassificationPerformanceTest<PeakRTFeatureVector> cpt = new ClassificationPerformanceTest<PeakRTFeatureVector>(
						gt, new PeakRTFeatureVectorComparator(delta))
					//name is the textual representation of the pipe uid
					net.sf.maltcms.evaluation.spi.classification.PerformanceMetrics pm = cpt.performTest(pipe.uid.toString(), toolGroups)
					PerformanceMetrics pmet = new PerformanceMetrics(groundTruthName, pm)
					PostProcessing.objects.add(pmet)
					//pipe.database.create(pmet)
				}catch(java.lang.IllegalArgumentException e) {
					println "Could not perform alignment classification performance test."
				}
			}
		}else{
			//				println "${this.getClass().getName()} with input ${input} is up to date!"
		}

        return input
    }
}
