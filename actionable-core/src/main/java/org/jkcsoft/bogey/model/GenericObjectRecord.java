/*
 * Copyright (c) Jim Coles (jameskcoles@gmail.com) 2016. through present.
 *
 * Licensed under the following license agreement:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Also see the LICENSE file in the repository root directory.
 */

package org.jkcsoft.bogey.model;

import org.jkcsoft.bogey.metamodel.Identifier;

/**
 *
 */

public class GenericObjectRecord {

    public GenericObjectRecord getParentObject() {
        return null;
    }

    public void set(String attrCodeName, Object value) {
    }

    /**
     * Returns all IProperties as a map keyed by Oid's
     */
    public Object get(String attrCodeName) {
        return null;
    }

    public Identifier getAssocRef(String assocCodeName) {
        return null;
    }

    public void setParentObject(GenericObjectRecord parent) {

    }

}