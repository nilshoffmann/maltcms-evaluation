/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package maltcmsEval.pipes.provider

import maltcmsEval.pipes.Pipe
import maltcmsEval.pipes.PipeIterator
import maltcmsEval.pipes.PipeProvider
/**
 *
 * @author Nils Hoffmann
 */
abstract class GenericParameterizedFilePipeProvider extends PipeProvider<File, File> {
    List parameterNames = []
    List parameters = []

    /**
     * Expects a {@link Map} of {@link String} parameter names mapped to
     * a corresponing list of valid {@link String} values for each parameter.
     */
    public GenericParameterizedFilePipeProvider(Map m) {
        parameterNames = m.keySet() as List
        parameters = m.values() as List
    }

}

