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

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
@Canonical
public class PerformanceMetrics {

    int tp, fp, tn, fn, realfn, groundTruthEntities, toolEntities, unmatchedTool, unmatchedGroundTruth, groups
    double dist, f1
    String toolName
    String groundTruthName

    public PerformanceMetrics(String groundTruthName, net.sf.maltcms.evaluation.spi.classification.PerformanceMetrics pm) {
        this(pm.toolName,groundTruthName, pm.tp,pm.fp,pm.tn,pm.fn,pm.groundTruthEntities,pm.toolEntities,pm.K,pm.dist,pm.unmatchedToolEntities,
			pm.unmatchedGroundTruthEntities)
    }

    public PerformanceMetrics(String toolName, String groundTruthName, int tp, int fp,
        int tn, int fn, int N, int M, int groups,double dist,int unmatchedTool, int unmatchedGroundTruth) {
        this.toolName = toolName;
        this.tp = tp;
        this.fp = fp;//+unmatchedTool;
        this.tn = tn;
        this.realfn = fn;
        this.groundTruthName = groundTruthName;
        this.groundTruthEntities = N;
        this.groups = groups;
        this.toolEntities = M;
        this.dist = dist;
        this.unmatchedTool = unmatchedTool;
        this.unmatchedGroundTruth = unmatchedGroundTruth;
        this.fn = this.realfn;// + (this.unmatchedGroundTruth);
        this.f1 = 2.0d * ((getPrecision() * getRecall()) / (getPrecision() + getRecall()));
    }

    public double getSensitivity() {
        double tpv = tp;
        double fnv = fn;
		double denom = tpv+fnv
		return (denom==0.0d)?0.0d:tpv/denom
    }

    public double getSpecificity() {
        double tnv = tn;
        double fpv = fp;
		double denom = tnv+fpv
		return (denom==0.0d)?0.0d:tnv/denom
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
		double denom = (tpv + tnv + fpv + fnv)
		return (denom==0.0d)?0.0d:(tpv+tnv)/denom
    }

    public double getGain() {
        //System.out.println("tp+fn=" + (tp + fn));
        //System.out.println("tp+tn+fp+fn=" + (tp + tn + fp + fn));
        double r = ((double) (tp + fn)) / ((double) (tp + tn + fp + fn));
        //System.out.println("R=" + r);
        //System.out.println("Precisions=" + getPrecision());
		return (r==0.0d)?0.0d:getPrecision()/0.0d
    }

    public double getPrecision() {
        double tpv = tp;
        double fpv = fp;
		double denom = (tpv + fpv);
		return (denom==0.0)?0.0:tpv/denom;
    }

    public double getRecall() {
        return getSensitivity();
    }

    private double getMCC() {
        double a = (getTp() * getTn()) - (getFp() * getFn());
        double b = Math.sqrt((getTp() + getFp()) * (getTp() + getFn()) * (getTn() + getFp()) * (getTn() + getFn()));
        return (b==0?0:a / b);
    }

    public static PerformanceMetrics fromSql(GroovyRowResult row) {
        return new PerformanceMetrics(row.TOOLNAME, row.GROUNDTRUTHNAME, row.TP, row.FP, row.TN, row.FN, row.GROUNDTRUTHENTITIES,
			row.TOOLENTITIES, row.GROUPS, row.DIST, row.UNMATCHEDTOOL, row.UNMATCHEDGROUNDTRUTH)
    }
}
