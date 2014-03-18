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
import groovy.sql.GroovyRowResult
import groovy.transform.Canonical
import maltcmsEval.db.SqlDatabase
import maltcmsEval.db.SqlUtils
import maltcmsEval.pipes.PipeDescriptor
import maltcmsEval.pipes.evaluation.PerformanceMetrics
import maltcmsEval.pipes.evaluation.ExecutionMetrics

SqlDatabase testDb = SqlDatabase.open(new File(".", "h2testdb").canonicalPath, true)
SqlDatabase.primaryKeyMap.put(PipeDescriptor.class, "uid")
SqlDatabase.primaryKeyMap.put(PerformanceMetrics.class,"toolName")
SqlDatabase.primaryKeyMap.put(ExecutionMetrics.class, "uid")
File inDir = new File("testIn")
inDir.mkdirs()
File outDir = new File("testOut")
outDir.mkdirs()
def testObjects = []
def testPerformanceMetrics = []
for (int i = 0; i < 10; i++) {
    PipeDescriptor to = new PipeDescriptor("to" + i, UUID.randomUUID(), "ls -al",
            "maltcmsEval.pipes.JavaFilePipe", [p1: "blabsd",
            p2: i*8961923,
            p3: 123.23325+i],
            inDir, outDir,
            125897968L+8961*i)
    testDb.create(to)
    //String toolName, int tp, int fp, int tn, int fn, int N, int M, int groups,double dist,
    //int unmatchedTool, int unmatchedGroundTruth
    PerformanceMetrics pm = new PerformanceMetrics(to.uid.toString(),1130,21,26,12,1200,1156,6,12.214,12,24)
    testDb.create(pm)
    testPerformanceMetrics << pm
    testObjects << to
}

testDb.query("select * from "+SqlUtils.getTableName(PipeDescriptor.class)+" order by name ASC").each {
    it ->
    PipeDescriptor dbobj = (fromSql(it))
    def results = testObjects.find {tobj -> tobj.name == dbobj.name}
    println "Original: ${results}"
    println "Database: ${dbobj}"
    assert results.equals(dbobj)
}

PipeDescriptor testPipeDescriptor = new PipeDescriptor("to"+25,UUID.randomUUID(), "ls -al","maltcmsEval.pipes.JavaFilePipe", [p1: "blabsd",
        p2: 8961923*1.241,
        p3: 123.23325],
        inDir, outDir,
        125897968L+8961*34)

List<GroovyRowResult> results = testDb.query("select * from "+SqlUtils.getTableName(PipeDescriptor.class)+" where uid='"+testPipeDescriptor.uid+"'")
assert results.isEmpty() == true

List<GroovyRowResult> results2 = testDb.query("select * from "+SqlUtils.getTableName(PipeDescriptor.class)+" where "+
"uid='"+testObjects[5].uid+"'")
assert results2.size() == 1
List<GroovyRowResult> pmresult = testDb.query("select * from "+SqlUtils.getTableName(PerformanceMetrics.class)+
        " where toolName='"+results2.get(0).uid+"'")
assert pmresult.isEmpty() == false
println pmresult.get(0)
println PerformanceMetrics.fromSql(pmresult.get(0))

//testing updates
testObjects[0].name = "update succeeded"
testDb.update(testObjects[0])
List<GroovyRowResult> updateResults = testDb.query("select * from "+SqlUtils.getTableName(PipeDescriptor.class)+
        " where uid='"+testObjects[0].uid+"'")
GroovyRowResult updateResult = testDb.queryByPrimaryKey(testObjects[0])
assert updateResult != null
assert PipeDescriptor.fromSql(updateResult) == testObjects[0]
assert results2.size() == 1
assert PipeDescriptor.fromSql(updateResults.get(0)) == testObjects[0]
List<GroovyRowResult> allPipeDescriptors = testDb.query("select * from "+SqlUtils.getTableName(PipeDescriptor.class))
assert allPipeDescriptors.size() == testObjects.size()

@Canonical
class TestObject {
    int counter
    String name
    UUID uid
    Map<String, String> parameters
    long runtime

    public static Map fromSql(GroovyRowResult row) {
        return [counter: row.COUNTER, name: row.NAME,
                uid: UUID.fromString(row.UID), parameters: SqlUtils.toMap(row.PARAMETERS),
                runtime: row.RUNTIME]
    }
}

public static PipeDescriptor fromSql(GroovyRowResult row) {
    return new PipeDescriptor(row.NAME.toString(), UUID.fromString(row.UID), row.COMMAND.toString(), row.PIPELINECLASS.toString(),
            SqlUtils.toMap(row.PARAMETERS), new File(row.INPUT.toString()),
            new File(row.OUTPUT.toString()), row.RUNTIME)
}



