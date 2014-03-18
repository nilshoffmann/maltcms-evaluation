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
import maltcmsEval.pipes.evaluation.PairwisePerformanceMetrics
import maltcms.datastructures.array.IFeatureVector
import net.sf.maltcms.evaluation.api.classification.Category
import net.sf.maltcms.evaluation.api.classification.Entity
import net.sf.maltcms.evaluation.api.classification.EntityGroup
import net.sf.maltcms.evaluation.api.classification.EntityGroupList
import net.sf.maltcms.evaluation.api.classification.IFeatureVectorComparator
import net.sf.maltcms.evaluation.api.classification.INamedPeakFeatureVector
import net.sf.maltcms.evaluation.api.classification.IPerformanceMetrics
import net.sf.maltcms.evaluation.api.classification.PeakNameFeatureVectorComparator
import net.sf.maltcms.evaluation.api.classification.PeakRowIndexFeatureVectorComparator
import net.sf.maltcms.evaluation.spi.EntityGroupBuilder
import net.sf.maltcms.evaluation.spi.classification.ChromaTOFPeakListEntityTable
import net.sf.maltcms.evaluation.spi.classification.PairwiseClassificationPerformanceTest
import org.apache.commons.io.FileUtils
import static maltcmsEval.pipes.filter.AlignmentFilters.*

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class MspaPairwiseAlignmentResultOutputFilter extends OutputFilter<File, File> {

	File peakListDir
    File groundTruthFile
    String groundTruthName
	String alignmentFilename = "multiple-alignment.csv"

	boolean isUpToDate(SqlDatabase database, Pipe<File,File> pipe, String groundTruthName) {
		return false
		/*
		List<GroovyRowResult> rows = database.query("SELECT * FROM "+SqlUtils.getTableName(PairwisePerformanceMetrics.class)+" WHERE TOOLNAME='"+pipe.uid.toString()+"' AND GROUNDTRUTHNAME='"+groundTruthName+"' LIMIT 1")
		if(rows==null || rows.isEmpty()) {
			return false
		}
		return true
		*/
	}

    @Override
    File filterOutput(Pipe<File, File> pipe, File input) {
        //println "Running alignment result output filter for ${groundTruthName}"
		File alignmentFile = locateAlignmentFile(input, alignmentFilename)
        if (alignmentFile == null) {
            throw new IOException("Could not locate alignment file ${alignmentFilename}")
        }
		if(groundTruthName == null) {
			groundTruthName = groundTruthFile.canonicalPath
		}
		if(!isUpToDate(pipe.database, pipe, groundTruthName)) {
			if(alignmentFile.readLines().size==0 || alignmentFile.readLines().size==1) {
				//println "Alignment file for ${pipe.uid.toString()} is empty!"
			} else {
				//perform the alignment classification
				EntityGroupBuilder egb = new EntityGroupBuilder();
				//load peak data
				File[] files = FileUtils.listFiles(peakListDir, ["csv"] as String[], false).toArray(new File[0]);
				ChromaTOFPeakListEntityTable<INamedPeakFeatureVector> t = new ChromaTOFPeakListEntityTable<INamedPeakFeatureVector>(files);
				//load ground truth alignment
				List<EntityGroup<INamedPeakFeatureVector>> ref = egb.buildCSVPeak2DAssociationGroups(groundTruthFile, t);
				EntityGroupList referenceGroups = new EntityGroupList(ref.get(0).getCategories().toArray(new Category[0]));
				referenceGroups.addAll(ref);

				List<EntityGroup<INamedPeakFeatureVector>> tool = egb.buildMSPAPeak2DAssociationGroups(alignmentFile.parentFile, t)
				if(tool.isEmpty()) {
					println "Could not build entity groups for ${pipe.uid.toString()}!"
				} else {
					EntityGroupList toolGroups = new EntityGroupList(tool.get(0).getCategories().toArray(new Category[0]));
					toolGroups.addAll(tool);
					try {
						PairwiseClassificationPerformanceTest<INamedPeakFeatureVector> cpt = new PairwiseClassificationPerformanceTest<INamedPeakFeatureVector>(t, referenceGroups, new PeakNameFeatureVectorComparator());
						List<PairwisePerformanceMetrics> pm = cpt.performTest(pipe.uid.toString(), toolGroups);
						//        println "Storing performance metrics."
						for(net.sf.maltcms.evaluation.spi.classification.PairwisePerformanceMetrics metric:pm) {
							PairwisePerformanceMetrics pmet = new PairwisePerformanceMetrics(groundTruthName, metric)
							//        println "TP: ${pm.tp} FP: ${pm.fp} TN: ${pm.tn} FN: ${pm.fn} UNMATCHED_GT:" +
							//                "${pm.unmatchedGroundTruth} UNMATCHED_TOOL: ${pm.unmatchedTool}"
							PostProcessing.objects.add(pmet)
							//pipe.database.create(pmet)
						}
					}catch(java.lang.IllegalArgumentException e) {
//							println "Could not perform alignment classification performance test."
					}
				}
			}
		}else{
			println "${this.getClass().getName()} with input ${input} is up to date!"
		}
        return input
    }
}
