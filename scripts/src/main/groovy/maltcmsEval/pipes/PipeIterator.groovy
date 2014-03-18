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

import groovy.transform.Canonical

/**
 * @author Nils.Hoffmann@CeBiTec.Uni-Bielefeld.DE
 */
@Canonical
abstract class PipeIterator<PIPE> implements Iterator<PIPE>{
    int numberOfCombinations
	private List parameters = []
    private List parameterNames = []
    private int index = 0
    private List combinations = []

    public PipeIterator(List parameters, List parameterNames) {
        this.parameters = parameters
        this.parameterNames = parameterNames
        this.combinations = parameters.combinations()
        this.numberOfCombinations = this.combinations.size()
        //println "Combinations: ${this.numberOfCombinations}"
    }

    public boolean  hasNext() {
        return index<combinations.size()
    }

    public Pipe<File,File> next() {
        if(!hasNext()) {
            throw new NoSuchElementException()
        }
        List combination = combinations[index]
        def pipeParameters = [:
        ]
        //print "Combination: "
        for(int i = 0;i<combination.size();i++) {
            //println "Partition ${i+1}/${combination.size()}"
            //print "${parameterNames[i]}=${combination[i]} "
            pipeParameters[parameterNames[i]]=combination[i]
            //print "\n"
        }
        index++
        //println "Pipe parameters: ${pipeParameters}"
        return create(pipeParameters)
    }

    public void remove() {
        throw new UnsupportedOperationException()
    }
	
	public abstract PIPE create(pipeParameters)
}
