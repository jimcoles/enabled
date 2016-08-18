/*
 * Copyright (c) Jim Coles (jameskcoles@gmail.com) 2016. through present.
 *
 * Licensed under the following license agreement:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Also see the LICENSE file in the repository root directory.
 */
package org.jkcsoft.bogey.rdb;

import com.jkc.data.dms.DataSystem;
import com.jkc.data.dms.ModelExtractor;
import com.jkc.data.metamodels.ssc.DataModel;

/**
 * @author J. Coles
 * @version 1.0
 */
public class RdbModelExtractor extends ModelExtractor {
    //----------------------------------------------------------------------------
    // Private instance vars
    //----------------------------------------------------------------------------

    //----------------------------------------------------------------------------
    // Constructor(s) (private, package, protected, public)
    //----------------------------------------------------------------------------

    public RdbModelExtractor() {
    }

    //----------------------------------------------------------------------------
    // Instance methods
    //----------------------------------------------------------------------------
    public DataModel buildDataModel(DataSystem ds) {
        return null;
    }

    DataModel startExtraction() throws Exception {
        return null;
    }

    //---- <Accessors and Mutators> ----------------------------------------------

    //---- </Accessors and Mutators> ----------------------------------------------

    //----------------------------------------------------------------------------
    // Private methods
    //----------------------------------------------------------------------------

}
