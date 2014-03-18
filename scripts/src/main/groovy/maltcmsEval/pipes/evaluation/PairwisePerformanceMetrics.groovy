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
package maltcmsEval.pipes.evaluation

import groovy.sql.GroovyRowResult
import groovy.transform.Canonical
import net.sf.maltcms.evaluation.spi.classification.PairwisePerformanceMetrics

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
@Canonical
public class PairwisePerformanceMetrics {

    int tp, fp, tn, fn, f1
    String toolName
	String pairName
    String groundTruthName

    public PairwisePerformanceMetrics(String groundTruthName, net.sf.maltcms.evaluation.spi.classification.PairwisePerformanceMetrics pm) {
        this(pm.toolName, groundTruthName, pm.instanceName, pm.tp,pm.fp,pm.tn,pm.fn)
    }

    public PairwisePerformanceMetrics(String toolName, String groundTruthName, String pairName, int tp, int fp,
        int tn, int fn) {
        this.toolName = toolName;
		this.pairName = pairName;
        this.tp = tp;
        this.fp = fp;//+unmatchedTool;
        this.tn = tn;
		this.fn = fn;
        this.groundTruthName = groundTruthName;
    }

	public double getF1() {
		return 2.0d * ((getPrecision() * getRecall()) / (getPrecision() + getRecall()));
	}

    public double getSensitivity() {
		double denom = tp+fn
        return (denom==0.0d)?0.0d:tp/denom;
    }

    public double getSpecificity() {
		double denom = tn + fp
        return (denom==0.0d)?0.0d:tn/denom;
    }

    public double getFPR() {
        return 1-getSpecificity();
    }

    public double getFNR() {
        return 1-getSensitivity();
    }

    public double getAccuracy() {
        double tpv = tp;
        double tnv = tn;
        double fpv = fp;
        double fnv = fn;
		double denom = tpv+tnv+fpv+fnv;
		return (denom==0.0d)?0.0d:(tpv+tnv)/denom
    }

    public double getGain() {
        //System.out.println("tp+fn=" + (tp + fn));
        //System.out.println("tp+tn+fp+fn=" + (tp + tn + fp + fn));
		double denom = tp + tn + fp + fn
        double r = (denom==0.0d)?0.0:((double) (tp + fn)) / ((double) (denom));
        //System.out.println("R=" + r);
        //System.out.println("Precisions=" + getPrecision());
        double gain = (r==0.0d)?0.0d:getPrecision() / r;
        return gain;
    }

    public double getPrecision() {
		double denom = tp+fp
        return (denom==0.0d)?0.0d:tp/denom;
    }

    public double getRecall() {
        return getSensitivity();
    }

    private double getMCC() {
        double a = (getTp() * getTn()) - (getFp() * getFn());
        double b = Math.sqrt((getTp() + getFp()) * (getTp() + getFn()) * (getTn() + getFp()) * (getTn() + getFn()));
        return (b==0.0d)?0.0d:a / b;
    }

    public static PairwisePerformanceMetrics fromSql(GroovyRowResult row) {
        return new PairwisePerformanceMetrics(row.TOOLNAME, row.GROUNDTRUTHNAME, row.PAIRNAME, row.TP, row.FP, row.TN, row.FN)
    }
}
