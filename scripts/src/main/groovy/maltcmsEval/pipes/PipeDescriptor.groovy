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

import groovy.io.FileType
import groovy.sql.GroovyRowResult
import groovy.transform.Canonical
import java.security.MessageDigest
import maltcmsEval.db.SqlUtils

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
@Canonical
class PipeDescriptor {
    String name
    UUID uid
    String command
    String pipelineClass
    Map<String, String> parameters
    File input
    File output
    String hash

    public PipeDescriptor(String name, UUID uid, String command, String pipelineClass, Map parameters, File input,
		File output, String hash) {
        this.name = name
        this.uid = uid
        this.command = command
        this.pipelineClass = pipelineClass
        this.parameters = new LinkedHashMap<String, String>()
        parameters.each {
            key, value -> this.parameters[key] = value.toString()
        }
        this.input = input
        this.output = output
        this.hash = hash
    }

    public PipeDescriptor(String name, UUID uid, String command, String pipelineClass, Map parameters, File input,
		File output) {
        this(name,uid,command,pipelineClass,parameters,input,output,createHash(input, output))
    }

    public static String createHash(File inputDir, File outputDir) {
        def files = []
        inputDir.eachFileRecurse(FileType.FILES) {
            file ->
            files << file
        }
        outputDir.eachFileRecurse(FileType.FILES) {
            file ->
            files << file
        }
        files.sort()
        return digest(files)
    }

	public static Map isUpToDate(File input, File output, String hash) {
        if (input.exists() && input.isDirectory() && output.exists() && output.isDirectory()) {
			String chash = createHash(input, output)
            return ["upToDate":chash.equals(hash),"hash":chash]
        }
        return ["upToDate":false,"hash":null]
    }

    public boolean isUpToDate(File input, File output) {
        if (input.exists() && input.isDirectory() && output.exists() && output.isDirectory()) {
            return createHash(input, output).equals(hash)
        }
        return false
    }

    public static String digest(List<File> files) {
        MessageDigest digest = MessageDigest.getInstance("SHA-1")
        for (File file: files) {
            file.withInputStream() {is ->
                byte[] buffer = new byte[8192]
                int read = 0
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        byte[] sha1 = digest.digest()
        BigInteger bigInt = new BigInteger(1, sha1)
        return bigInt.toString(16).padLeft(40, '0')
    }

    public static fromSql(GroovyRowResult row) {
        return new PipeDescriptor(row.NAME, UUID.fromString(row.UID), row.COMMAND, row.PIPELINECLASS,
			SqlUtils.toMap(row.PARAMETERS), new File(row.INPUT),
			new File(row.OUTPUT), row.hash) {
        }
    }
}
