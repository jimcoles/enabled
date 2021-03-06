/*
 * Copyright (c) Jim Coles (jameskcoles@gmail.com) 2016. through present.
 *
 * Licensed under the following license agreement:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Also see the LICENSE file in the repository root directory.
 */

package org.jkcsoft.bogey.metamodel;


import java.util.List;
import java.util.Vector;

public class QuerySort {
    private List items = new Vector();

    public QuerySortItem addSortItem(QueryAttrRef attr, SortDir dir) {
        QuerySortItem item = new QuerySortItem(attr, dir);
        items.add(item);
        return item;
    }

    public List getSortItems() {
        return items;
    }
}
