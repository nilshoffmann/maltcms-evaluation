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

import groovy.sql.GroovyRowResult
import maltcmsEval.db.SqlDatabase
import groovy.sql.Sql
import groovy.transform.Canonical
import maltcmsEval.pipes.JavaFilePipe
import java.sql.*
import org.h2.jdbcx.JdbcConnectionPool
import java.util.concurrent.*


/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class PostProcessing {

    private SqlDatabase database

	public static LinkedBlockingQueue objects = new java.util.concurrent.LinkedBlockingQueue()

	def executor

	class CustomThreadFactory implements ThreadFactory {
		int number
		String name
		int priority
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "${name}-${number}");
			t.setPriority(priority)
			number++
			return t
		}
 	}

	public PostProcessing(SqlDatabase database){
		this.database = database
		executor = Executors.newSingleThreadScheduledExecutor(new CustomThreadFactory(name: "H2DBWriterPool",priority:Thread.MAX_PRIORITY))
		executor.scheduleAtFixedRate(new Runnable(){
			public void run(){
				def objs = [] as List
				objects.drainTo(objs)
				def db = database.child()
				println "Writing ${objs.size()} objects to database"
				objs.each { obj -> db.create(obj)}
			}
		}, 60, 60, TimeUnit.SECONDS)

	}

	public void shutdown() {
		executor.shutdown()
		executor.awaitTermination(30, TimeUnit.MINUTES)
		if(!objects.isEmpty()) {
			println "Adding remaining ${objects.size()} objects!"
			def objs = [] as List
			objects.drainTo(objs)
			def db = database.child()
			objs.each { obj -> db.create(obj)}
		}
	}

}
