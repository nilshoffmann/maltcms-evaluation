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
import java.lang.ref.SoftReference
/**
 * Provides lazily created result type, depending on the generic type.
 * Uses the supplied {@link DataProvider} instances to create
 * the element on demand. The result is referred to via a SoftReference.
 * Thus, the DataProvider must allow multiple invocations of it should the 
 * SoftReference be cleared by garbage collection. 
 * 
 * @author Nils Hoffmann
 */
class LazyDataProvider<OUT> extends DataProvider<OUT> {
    DataProvider<OUT> input
    private SoftReference<OUT> out = null

    /**
     * Calls the @see DataProvider instance,
     * thereby creating the result if it does not yet exist.
     *
     * @return the populated typed result list.
	 * @throws NullPointerException if the object returned from the data provider is null.
     */
    public OUT call() {
		OUT result = null
        if (out==null) {
			result = input.call()
			if(result==null) {
				throw new NullPointerException("Returned value must not be null!")
			}
			out = new SoftReference<OUT>(result)
			return out.get()
        }else{
			result = out.get();
			if(result==null) {
				result = input.call()
				out = new SoftReference<OUT>(result)
			}
		}
        return result
    }
}

