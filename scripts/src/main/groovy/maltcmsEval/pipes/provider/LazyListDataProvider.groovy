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

import maltcmsEval.pipes.DataProvider
import maltcmsEval.pipes.ListDataProvider

/**
 * Provides a lazily created list of elements, depending on the generic type.
 * Uses the supplied list of typed {@link DataProvider} instances to create
 * each element on demand.
 *
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 *
 */
class LazyListDataProvider<OUT> extends ListDataProvider<OUT> {
    List<DataProvider<OUT>> input
    private List<OUT> out = []

    /**
     * Iterates the supplied list of generic {@link DataProvider} instances,
     * thereby populating the result list.
     *
     * @return the populated typed result list.
     */
    public List<OUT> call() {
        if (out.isEmpty()) {
            input.each {
                dp -> out << dp.call()
            }
        }
        return out
    }
}
