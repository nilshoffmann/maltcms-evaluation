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
package maltcmsEval

import groovy.sql.GroovyRowResult
import maltcmsEval.db.SqlDatabase
import maltcmsEval.db.SqlUtils
import maltcmsEval.pipes.PipeDescriptor
import maltcmsEval.pipes.evaluation.PerformanceMetrics
import maltcmsEval.pipes.evaluation.PairwisePerformanceMetrics
import maltcmsEval.pipes.evaluation.ExecutionMetrics
import maltcmsEval.pipes.evaluation.InputFileCountMetrics

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class Utils {
    /*
     * Helper functions and classes
     */

    static List zip(f, a, b) {
        def result = []
        def al = a as List
        def bl = b as List
        assert al.size() == bl.size()
        0.upto(al.size() - 1) { index ->
            result << f(al[index], bl[index])
        }
        result
    }

    static List zipPairs(f, a, b) {
        def result = []
        def al = a as List
        def bl = b as List
        //println "${al} vs. ${bl}"
        0.upto(a.size() - 1) { indexA ->
            result[indexA] = []
            0.upto(b.size() - 1) { indexB ->
                result[indexA] << f(al[indexA], bl[indexB])
            }
        }
        result
    }

    static List mapZip(f, Map map) {
        def valueCombinations = map.values().combinations()
        def combinations = []
        //create concatenated param names
        def paramNames = map.keySet()//.collect{ti -> "-D"+ti+"="}
        //loop over all combinations and create concatenated command line parameters
        //as in -Dname=value
        valueCombinations.each { it ->
            combinations << zip(f, paramNames, it) //.join(" ")
        }
        return combinations
    }

    /**
     * Query classification performance and create csv table from it
     */
    public static File createPerformanceTable(SqlDatabase db, File evalOutDir) {
        List<GroovyRowResult> results = db.query("select * from " +
			SqlUtils.getTableName(PerformanceMetrics.class))
		if(results!=null && !results.isEmpty()) {
		    def performanceFile = new File(evalOutDir, "performance.csv")
		    performanceFile.setText("uid\tReferenceName\tTP\tFP\tFN\tTN\tGtsize\tToolsize\t" +
		            "Gtunmatched\tToolunmatched\tF1\tPrecision\tRecall\tFNR\tFPR\tSpecificity\n")
		    performanceFile.withWriterAppend {
		        writer ->
		        for (GroovyRowResult result: results) {
		            PerformanceMetrics bar = PerformanceMetrics.fromSql(result)
		            writer.write("${bar.toolName}\t${bar.groundTruthName}\t${bar.tp}\t${bar.fp}\t${bar.fn}" +
		                    "\t${bar.tn}\t${bar.groundTruthEntities}\t${bar.toolEntities}" +
		                    "\t${bar.unmatchedGroundTruth}\t${bar.unmatchedTool}" +
		                    "\t${bar.f1}\t${bar.precision}\t${bar.recall}\t${bar.FNR}\t${bar.FPR}\t${bar.specificity}\n")
		        }
		    }
		    return performanceFile
		}
		println "Could not create performance table. No data present in database!"
		return null
    }

	/**
     * Query classification pairwise performance and create csv table from it
     */
    public static File createPairwisePerformanceTable(SqlDatabase db, File evalOutDir) {
        List<GroovyRowResult> results = db.query("select * from " +
			SqlUtils.getTableName(PairwisePerformanceMetrics.class))
		if(results!=null && !results.isEmpty()) {
		    def performanceFile = new File(evalOutDir, "pairwise-performance.csv")
		    performanceFile.setText("uid\tReferenceName\tPairName\tTP\tFP\tFN\tTN\tF1\tPrecision\tRecall\tFNR\tFPR\tSpecificity\n")
		    performanceFile.withWriterAppend {
		        writer ->
		        for (GroovyRowResult result: results) {
		            PairwisePerformanceMetrics bar = PairwisePerformanceMetrics.fromSql(result)
		            writer.write("${bar.toolName}\t${bar.groundTruthName}\t${bar.pairName}\t${bar.tp}\t${bar.fp}\t${bar.fn}" +
		                    "\t${bar.tn}\t${bar.f1}\t${bar.precision}\t${bar.recall}\t${bar.FNR}\t${bar.FPR}\t${bar.specificity}\n")
		        }
		    }
		    return performanceFile
		}
		println "Could not create pairwise performance table. No data present in database!"
		return null
    }

    /**
     * Query classification performance and create csv table from it
     */
    public static File createExecutionMetricsTable(SqlDatabase db, File evalOutDir) {
        List<GroovyRowResult> results = db.query("select * from " +
			SqlUtils.getTableName(ExecutionMetrics.class))
		if(results!=null && !results.isEmpty()) {
		    def performanceFile = new File(evalOutDir, "executionMetrics.csv")
		    performanceFile.setText("uid\truntime\tmemory\n")
		    performanceFile.withWriterAppend {
		        writer ->
		        for (GroovyRowResult result: results) {
		            ExecutionMetrics bar = ExecutionMetrics.fromSql(result)
		            writer.write("${bar.uid}\t${bar.cpuTime/1000000f}\t${bar.maxMemory/1024f*1024f}\n")
		        }
		    }
		    return performanceFile
		}
		println "Could not create execution metrics table. No data present in database!"
		return null
    }

    /**
     * Query bipace pipeline parameters and create csv table from it
     */
    public static File createParametersTable(SqlDatabase db, File evalOutDir) {
        List<GroovyRowResult> results = db.query("select * from " + SqlUtils.getTableName(PipeDescriptor.class)
			+ " WHERE NAME IN (" +
			["'bipaceRt'", "'bipacePlain'", "'cemappDtwRt'", "'cemappDtwPlain'", "'robinsonAuto'"].join(
                 ",") + ")")
		if(!results.isEmpty()) {
		    def parametersFile = new File(evalOutDir, "parameters.csv")
		    def paramKeys = ["uid", "name", "similarityFunction", "arraySimilarity", "maxRTDifference", "rtThreshold",
		            "rtTolerance",
		            "dtwFactory",
		            "globalGapPenalty",
		            "matchWeight",
		            "anchorRadius",
		            "useAnchors",
		            "globalBand",
		            "bandWidthPercentage",
		            "minCliqueSize"
		    ]
		    def substitutionMap = ["bandWidthPercentage":"BC","rtTolerance":"D","rtThreshold":"T","minCliqueSize":"MCS",
		            "matchWeight":"W","anchorRadius":"R","useAnchors":"DTW",
		            "globalBand":"BandConstraint"]
		    def substitutedHeader = []
		    paramKeys.each{
		        key->
				if(substitutionMap.containsKey(key)) {
					substitutedHeader << substitutionMap[key]
				}else{
					substitutedHeader << key
				}
		    }
		    parametersFile.setText(substitutedHeader.join("\t") + "\n")
		    def writer = parametersFile.withWriterAppend {
		        writer ->

		        results.each {
		            result ->
		            PipeDescriptor pd = PipeDescriptor.fromSql(result)
		            def values = []
		            paramKeys.each {
		                key ->
		                //println "key: ${key}"
		                if (pd.properties[key] == null) {

		                    if (pd.parameters[key] == null) {
		                        //println "null"
		                        if (key == "globalBand"){
		                            values << "NA"
		                        }else if (key == "useAnchors") {
		                            values << "Unpartitioned"
		                        } else {
		                            values << "NA"
		                        }
		                    } else {
		                        if (key == "globalBand"){
		                            if(pd.parameters[key].equals("false")) {
		                                values << "Local"
		                            }else{
		                                values << "Global"
		                            }
		                        }else if (key == "useAnchors") {
		                            if(pd.parameters[key].equals("false")) {
		                                values << "Unpartitioned"
		                            }else{
		                                values << "Partitioned"
		                            }
		                            //values << "DTW"
		                        }else if (pd.parameters[key].equals("false") || pd.parameters[key].equals("true")) {
		                            //println "boolean"
		                            values << pd.parameters[key].toString().toUpperCase()
		                        } else if (pd.parameters[key].toString().equals("NaN")) {
		                            //println "NaN"
		                            values << "NA"
		                        } else {
		                            //println "default"
		                            values << pd.parameters[key]
		                        }
		                    }
		                } else {
		                    if(!key.equals("uid")) {
		                        values << pd.properties[key]
		                    }
		                }
		            }
		            String line = pd.uid.toString() + "\t" + values.join("\t") + "\n"
		            line.replaceAll("NaN", "NA")
		            writer.write(line)
		        }
		    }
		    return parametersFile
		}
		println "Could not create parameters table. No data present in database!"
		return null
    }
	
	public static File createFileNumberTable(SqlDatabase db, File evalOutDir) {
		List<GroovyRowResult> results = db.query("select * from " + SqlUtils.getTableName(InputFileCountMetrics.class))
		if(!results.isEmpty()) {
		    def parametersFile = new File(evalOutDir, "inputFileNumberCount.csv")
		    parametersFile.setText("uid\tnfiles\tnpeaks\n")
		    def writer = parametersFile.withWriterAppend {
		        writer ->

		        results.each {
		            result ->
					InputFileCountMetrics metrics = InputFileCountMetrics.fromSql(result)
		            String line = metrics.uid.toString() + "\t" + metrics.nfiles + "\t" + metrics.npeaks + "\n"
		            line.replaceAll("NaN", "NA")
		            writer.write(line)
		        }
		    }
		    return parametersFile
		}
		println "Could not create inputFileNumberCount table. No data present in database!"
		return null
	}

    /**
     * Query bipace pipeline parameters and create csv table from it
     */
    public static File createParametersTable2(SqlDatabase db, File evalOutDir, List toolNames, List paramKeys, Map substitutionMap) {
        List<GroovyRowResult> results = db.query("select * from " + SqlUtils.getTableName(PipeDescriptor.class)
			+ " WHERE NAME IN (" +toolNames.collect{it -> "'${it}'"}.join(
                 ",") + ")")
		if(!results.isEmpty()) {
		    def parametersFile = new File(evalOutDir, "parameters.csv")
		    def substitutedHeader = []
		    paramKeys.each{
		        key->
				if(substitutionMap.containsKey(key)) {
					substitutedHeader << substitutionMap[key]
				}else{
					substitutedHeader << key
				}
		    }
		    parametersFile.setText(substitutedHeader.join("\t") + "\n")
		    def writer = parametersFile.withWriterAppend {
		        writer ->

		        results.each {
		            result ->
		            PipeDescriptor pd = PipeDescriptor.fromSql(result)
		            def values = []
		            paramKeys.each {
		                key ->
		                //println "key: ${key}"
		                if (pd.properties[key] == null) {

		                    if (pd.parameters[key] == null) {
		                        //println "null"
		                        values << "NA"
		                    } else {
		                        if (pd.parameters[key].equals("false") || pd.parameters[key].equals("true")) {
		                            //println "boolean"
		                            values << pd.parameters[key].toString().toUpperCase()
		                        } else if (pd.parameters[key].toString().equals("NaN")) {
		                            //println "NaN"
		                            values << "NA"
		                        } else {
		                            //println "default"
		                            values << pd.parameters[key]
		                        }
		                    }
		                } else {
		                    if(!key.equals("uid")) {
		                        values << pd.properties[key]
		                    }
		                }
		            }
		            String line = pd.uid.toString() + "\t" + values.join("\t") + "\n"
		            line.replaceAll("NaN", "NA")
		            writer.write(line)
		        }
		    }
		    return parametersFile
		}
		println "Could not create parameters table. No data present in database!"
		return null
    }

    public static getBestF1Result(SqlDatabase db, List<String> name) {
        String query = "SELECT * FROM " + SqlUtils.getTableName(PipeDescriptor.class) +
                " JOIN " + SqlUtils.getTableName(PerformanceMetrics.class) + " ON " +
		SqlUtils.getTableName(PerformanceMetrics.class) + ".TOOLNAME=" +
		SqlUtils.getTableName(PipeDescriptor.class) + ".UID " +
                "ORDER BY F1 DESC LIMIT 1"
        //println "Query: ${query}"
        List<GroovyRowResult> result = db.query(query)
        if (result == null || result.isEmpty()) {
            println "Failed to find any Performance metrics!"
            System.exit(1)
        }
        return new File(result.get(0).OUTPUT)
    }

    public static getBestTpFpResult(SqlDatabase db, List<String> name) {
        String query = "SELECT * FROM "+SqlUtils.getTableName(PerformanceMetrics.class) + " JOIN " +
		SqlUtils.getTableName(PipeDescriptor.class) + " ON " +
		SqlUtils.getTableName(PerformanceMetrics.class) + ".TOOLNAME=" +
		SqlUtils.getTableName(PipeDescriptor.class) + ".UID ORDER BY TP DESC, FP ASC, RUNTIME ASC"
        List<GroovyRowResult> result = db.query(query)
        if (result == null || result.isEmpty()) {
            println "Failed to find any Performance metrics!"
            System.exit(1)
        }
        return new File(result.get(0).OUTPUT)
    }

    public static getBestPrecisionResult(SqlDatabase db, List<String> name) {
        String query = "SELECT * FROM " + SqlUtils.getTableName(PerformanceMetrics.class) + " JOIN " +
		SqlUtils.getTableName(PipeDescriptor.class) + " ON " +
		SqlUtils.getTableName(PerformanceMetrics.class) + ".TOOLNAME=" +
		SqlUtils.getTableName(PipeDescriptor.class) + ".UID ORDER BY PRECISION DESC, RECALL DESC, RUNTIME ASC"
        List<GroovyRowResult> result = db.query(query)
        if (result == null || result.isEmpty()) {
            println "Failed to find any Performance metrics!"
            System.exit(1)
        }
        return new File(result.get(0).OUTPUT)
    }
}
