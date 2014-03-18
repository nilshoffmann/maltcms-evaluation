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
package maltcmsEval.pipes.provider

import maltcmsEval.pipes.PipeProvider
import maltcmsEval.pipes.Pipe
import maltcmsEval.pipes.PipeIterator
import maltcmsEval.pipes.MaltcmsPipeIterator

/**
 * Creates a {@link MaltcmsPipeIterator} from a {@link Map} of
 * parameters and value lists for each parameter.
 * 
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
class MaltcmsParameterizedPipeProvider extends PipeProvider<File, File> {
    List parameterNames = []
    List parameters = []

    public MaltcmsParameterizedPipeProvider(Map m) {
        parameterNames = m.keySet() as List
        parameters = m.values() as List
    }

    public PipeIterator<Pipe<File, File>> call() {
        return new MaltcmsPipeIterator(parameters,parameterNames)
    }
}
