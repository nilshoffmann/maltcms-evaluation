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

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class SqlUtils {

    public static MAX_STRINGLENGTH = 4096

    /**
     *  Given a String str and a String val, tries to infer the best suitable
     *  type (as an sql expression) for val.
     *
     */
    public static String inferColumnType(String str, String val) {
        try {
            Integer.parseInt(val)
            return (str + " int")
        } catch (Exception e1) {
            try {
                Long.parseLong(val)
                return (str + " long")
            } catch (Exception e) {
                try {
                    Double.parseDouble(val)
                    return (str + " double")
                } catch (Exception e2) {
                    return (str + " varchar(" + MAX_STRINGLENGTH + ")")
                }
            }
        }
        return ""
    }

    public static String inferColumnTypeFromField(String key, Object value) {
        if(value instanceof Integer) {
            return (key + " int")
        } else if(value instanceof Long) {
            return (key + " bigint")
        } else if(value instanceof Boolean) {
            return (key + " boolean")
        } else if(value instanceof Short) {
            return (key + " smallint")
        } else if(value instanceof Byte) {
            return (key + " tinyint")
        } else if(value instanceof Float) {
            return (key + " real")
        } else if(value instanceof Double) {
            return (key + " double")
        } else if(value instanceof BigDecimal) {
            return (key + " decimal")
        } else if(value instanceof String) {
            if(value.length()>MAX_STRINGLENGTH) {
                throw new RuntimeException("Length of "+key+""" (${value.length()}) exceeds predefined SqlUtils
.MAX_STRINGLENGTH="""+MAX_STRINGLENGTH+"! Try setting this to a higher value, e.g. SqlUtils.MAX_STRINGLENGTH=8192!")
            }
            return (key + " varchar(" + MAX_STRINGLENGTH + ")")
        } else {
            String valueString = value.toString()
            //println "Infering column type for ${key} from field's toString() value: ${valueString}!"
            return inferColumnTypeFromField(key,valueString)
        }
        //throw new RuntimeException("Could not find a datatype mapping for ${key} of type ${value.getClass().getName
        //    ()}!")
    }

    public static List<String> buildTableHeader(Map hm, List<String> primaryKeyColumn) {
        def cols = []
		def keyColumns = primaryKeyColumn as Set
        for (e in hm) {
            if (primaryKeyColumn != null && !primaryKeyColumn.isEmpty() && primaryKeyColumn.size()==1 &&primaryKeyColumn[0].equals(e.key)) {
                cols << inferColumnTypeFromField(e.key.toString().toUpperCase(), e.value) + " NOT NULL PRIMARY KEY "
            } else {
                cols << inferColumnTypeFromField(e.key.toString().toUpperCase(), e.value)
            }
			//            if (primaryKeyColumn != null && !primaryKeyColumn.isEmpty() && primaryKeyColumn.equals(e.key)) {
			//                cols << inferColumnType(e.key.toString().toUpperCase(), e.value.toString()) + " PRIMARY KEY "
			//            } else {
			//                cols << inferColumnType(e.key.toString().toUpperCase(), e.value.toString())
			//            }
        }
        if (cols.size() == 0) {
            return ""
        }
        return cols
    }

    public static void writeToCSV(Sql db, String expression, File csvfile) {
        println "Writing to ${csvfile}"
        csvfile.getParentFile().mkdirs()
        csvfile.setText("")
        def keyset = null; //store the column names
        db.withTransaction {
            db.eachRow(expression) { row ->
                //If the keyset is null create one and add the column names
                if (keyset == null) {
                    //We want all the column names except the one named 'id
                    keyset = row.toRowResult().keySet().findAll { it != 'id' }

                    //create a nice header for our output
                    csvfile.append(keyset.join("\t") + "\n")
                }

                //select all values from columns in keyset for currently active row
                //join by "\t" and terminate line with "\n"
                csvfile.append(keyset.collect { k ->
						isNumeric(row.getProperty(k).toString()) ? row.getProperty(k) : "\"" + row.getProperty(k) + "\""
					}.join("\t") + "\n")
            }
        }
    }

    public static Map copyMap(Map m, List keysToFilter) {
        Map params = [:]
        Set reduced = m.keySet() - keysToFilter as SortedSet
        for (key in reduced) {
            params.put(key, m.get(key))
        }
        return params
    }

    public static String getColumnName(String property) {
        return property.toUpperCase()
    }

	public static Object escape(value) {
		if(value instanceof Number) {
			return value
		}else{
			return "'"+value.toString()+"'"
		}
	}

    public static createOrUpdate(Sql db, Object obj, List<String> primaryKeyMember) {
        String tablename = getTableName(obj)

        def paramMap = copyMap(obj.properties, ["class", "metaClass"])
        List<String> cols = buildTableHeader(paramMap, primaryKeyMember)
        def vals = []
        for (key in paramMap.keySet()) {
            //println "key: ${key}, value: ${paramMap.get(key)}"
            //            colnames.add(key)
			def value = paramMap.get(key)
			if(value instanceof Number || value instanceof String) {
				vals.add(value)
			}else{
				vals.add(value.toString())
			}
		}
		String columns = "(" + cols.join(",") + ")"
		//		println "$columns"
		//        String values = "VALUES (" + vals.collect {it -> "\'${it}\'"}.join(",") + ")"
		String values = "VALUES (" + vals.collect {it -> "?"}.join(",") + ")"
		//println "Columns: ${columns}"
		//println "Values: ${values}"

		def elems = cols.collect{it -> it.split(" ")[0]+" = ?"}
		elems = elems.join(",")
		db.withTransaction {
			db.execute("CREATE TABLE IF NOT EXISTS " + tablename + " " + columns)
			def colSelection = []
			def validColumns = new HashSet<String>()
			//db.eachRow("SELECT COLUMN_NAME FROM Information_Schema.Columns WHERE TABLE_NAME=?", [tablename]) { row ->
			//	validColumns << row.COLUMN_NAME
			//}
			//println "Valid columns for table are: ${validColumns}"
			for (String column in cols) {
				String colname = column.split(" ")[0].trim().toUpperCase()
				//println "Inspecting column ${colname}"
/*
				if (!validColumns.contains(colname)) {
					try {
						db.execute("ALTER TABLE " + tablename + " ADD " + column)
						colSelection << colname
					} catch (Exception re) {
						println "Exception while trying to add column ${column} to table ${tablename}:"
						re.printStackTrace()
					}
				} else {
						*/
					colSelection << colname
				//}
			}

			if(primaryKeyMember.size()>1) {
				//println "Creating compound index on columns ${primaryKeyMember}"
				db.execute("CREATE INDEX IF NOT EXISTS "+tablename+"_IDX ON "+tablename+" ("+primaryKeyMember.join(",")+")")
			}

			String insertExpr = ""
            insertExpr = "MERGE INTO " + tablename + " (" + colSelection.join(",") + ") KEY("+primaryKeyMember.join(",")+") " + values
            //println "Insert Expression: ${insertExpr}"

            db.execute(insertExpr, vals)
		}
	}

	public static String multiKeyQuery(List<String> keys) {
		def query = []
		keys.each {
			key ->
			query << "${key} = ?"
		}
		return query.join(" AND ")
	}

	public static List multiKeyQueryValues(List<String> keys, Map parameters) {
		def values = []
		keys.each {
			key ->
			def value = parameters[key]
			if(value instanceof Number || value instanceof String) {
				values.add(value)
			}else{
				values.add(value.toString())
			}
		}
		return values
	}

	public static setLogSizeMb(Sql db, Integer logSize) {
		//println "Setting database transaction log size to ${logSize} MBytes"
		db.withTransaction {
			//db.execute("SET MAX_LOG_SIZE "+logSize)
		}
	}

	public static remove(Sql db, Object obj, String primaryKeyMember) {
		String tablename = getTableName(obj)
		db.withTransaction {
			db.execute("DELETE FROM " + tablename + " WHERE "+primaryKeyMember.toUpperCase()+"='" + obj
				.properties[primaryKeyMember]
				+ "'")
		}
	}

	public static String getTableName(Class<?> clazz) {
		return clazz.getName().toUpperCase().replaceAll("\\.", "_")
	}

	public static String getTableName(Object obj) {
		return obj.class.getName().toUpperCase().replaceAll("\\.", "_")
	}

	public static boolean isNumeric(String s) {
		try {
			Double.parseDouble(s)
			return true
		} catch (NumberFormatException nfe) {
			return false
		}
	}

	public static Map toMap(String mapString) {
		LinkedHashMap map = [:]
		if (mapString == "[:]") {
			return map
		}
		//remove [ and ]
		mapString = mapString[1..mapString.size() - 2]
		//split at ,
		mapString.split(",").each {param ->
			//split at :
			def nameAndValue = param.split(":")
			map[nameAndValue[0].trim()] = nameAndValue[1].trim()
		}
		return map
	}

}
