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
package maltcmsEval.db

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.transform.Canonical
import maltcmsEval.pipes.JavaFilePipe
import java.sql.*
import org.h2.jdbcx.JdbcConnectionPool


/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
@Canonical
class SqlDatabase {

    private final Sql db
    private final String location
    private final Set<String> tableNames
	final JdbcConnectionPool connectionPool

    public SqlDatabase(String dblocation) {
		connectionPool = JdbcConnectionPool.create(
            "jdbc:h2:${dblocation};CACHE_SIZE=1048576;FILE_LOCK=FS","","");
		connectionPool.setMaxConnections(200)
		connectionPool.setLoginTimeout(240)
	    db = new Sql(connectionPool.getConnection())
				//Sql.newInstance("jdbc:h2:nioMapped:${dblocation};CACHE_SIZE=1048576;MVCC=TRUE;AUTOCOMMIT=OFF;WRITE_DELAY=600;LOCK_MODE=0;FILE_LOCK=SOCKET", "org.h2.Driver")
		db.setCacheStatements(true)
		db.setCacheNamedQueries(true)
		db.setEnableNamedQueries(true)
        this.location = dblocation
        primaryKeyMap.put(JavaFilePipe.class, ["uid"])
        SqlUtils.setLogSizeMb(db,1024)
		def resultSet = db.connection.metaData.getTables (null, null, null, null)
		tableNames = [] as Set
        while(resultSet.next()) {
			String name = resultSet.getString ("TABLE_NAME")
			tableNames << name
        }
    }

	public SqlDatabase(SqlDatabase database) {
		db = new Sql(database.connectionPool.getConnection())
//		db = Sql.newInstance("jdbc:h2:${dblocation};CACHE_SIZE=1048576;FILE_LOCK=FS;MVCC=TRUE", "org.h2.Driver")
//		db = Sql.newInstance("jdbc:hsqldb:file:${database.location};shutdown=false;hsqldb.default_table_type=cached", "org.hsqldb.jdbc.JDBCDriver")
		//db.setCacheStatements(true)
		//db.setCacheNamedQueries(true)
		//db.setEnableNamedQueries(true)
		this.location = database.location
        this.tableNames = database.tableNames
	}

	public SqlDatabase child() {
		return this//new SqlDatabase(this)
	}

    public void create(Object obj) {
		tableNames << SqlUtils.getTableName(obj)
        SqlUtils.createOrUpdate(db, obj, primaryKeyMap[obj.class])
    }

    public void update(Object obj) {
        SqlUtils.createOrUpdate(db, obj, primaryKeyMap[obj.class])
    }

    public void delete(Object obj) {
        SqlUtils.remove(db, obj, primaryKeyMap[obj.class])
    }

    public void execute(String string) {
        db.execute(string)
    }

    public List<GroovyRowResult> query(String query) {
        try{
            return db.rows(query)
        }catch(Exception e) {
            return null
        }
    }

    public GroovyRowResult queryByPrimaryKey(Object obj) {
		String tableName = SqlUtils.getTableName(obj)
		if(!tableNames.contains(tableName)) {
			return null
		}
        try {
            List<GroovyRowResult> result = db.rows("select * from " + SqlUtils.getTableName(obj) + " where " + primaryKeyMap.get(obj.class)[0] + "='" +
				obj.properties[primaryKeyMap.get(obj.class)[0]] + "'").get(0)
            if (result.isEmpty()) {
                return null
            }
            return result.get(0)
        } catch (Exception e) {
            return null
        }
    }

    public GroovyRowResult queryByPrimaryKey(Class objClazz, Object primaryKeyValue) {
		String tableName = SqlUtils.getTableName(objClazz)
		if(!tableNames.contains(tableName)) {
			return null
		}
        try {
            List<GroovyRowResult> result = db.rows("select * from " + SqlUtils.getTableName(objClazz) + " where " + primaryKeyMap
				.get
				(objClazz)[0] + "='" +
				primaryKeyValue.toString() + "'")
            if (result.isEmpty()) {
                return null
            }
            return result.get(0)
        } catch (Exception e) {
            return null
        }
    }

    public void close() {
		try {
			this.db.commit()
			this.db.close()
		}catch(Exception e){

		}
//        if (dbs.containsKey(location)) {
//            dbs.remove(location)
//        }
    }

    public static Map<Class<?>, List<String>> primaryKeyMap = new HashMap<Class<?>, List<String>>()

    private transient static Map<String, SqlDatabase> dbs = new HashMap<String, SqlDatabase>()

    public static SqlDatabase open(String dblocation, boolean clearFile) {
        File dbFile = new File("${dblocation}.db")
        if (dbFile.exists() && clearFile) {
            println "Deleting database file at ${dbFile}"
            dbFile.delete()
        }
        if (!dbs.containsKey(dblocation)) {
            dbs.put(dblocation, new SqlDatabase(dblocation))
        }
        return dbs.get(dblocation)
    }
}
