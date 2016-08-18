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

import com.jkc.roml.lang.runtime.Querier;

import java.util.*;

/**
 * Translates our internal Query model to SQL using ANSI sql.  This is an abstract class that
 * supplies the algorithm and default ANSI sql implementations.  This class
 * should be extended for any db vendor we want to support.  The class is designed
 * to enable extension by overriding private methods where a db vendor's sql
 * differs from ANSI standard.  The primary override is for db vendors (Oracle)
 * that don't support the "JOIN" clauses in the FROM clause.
 * <p>
 * Various algorithms and special rules:
 * <p>
 * 1 - The Basics
 * The primary algorithm is as follows:
 * 1) Evaluate the IQuery select, filter and sort nodes to determing all needed joins.
 * This is possible because we use IQAttrRefs which specify association chains
 * with respect to the target class of the IQuery.  Each IXAssoc in a
 * chain corresponds to a required join.
 * 2) Build the select, from, where, and order by clauses of the sql using the
 * OO-RDB mapping info embedded in the xmeta IXClass, IXAssoc, IXClassAttrs.
 * <p>
 * 2 - Enum Literal handling
 * When an IQuery select, sort or filter references an enumerated data type, (i.e.,
 * IXClassAttr.getPrimType.isEnum() == TRUE) some special rules are applied.
 * For a select, dereference the EnumLiteral.OBJECTID to the EnumLiteral.NAME.
 * For an inequality filter (i.e., ">", "<", ">=", etc) dereference to the
 * EnumLiteral.ORDERNUM.
 * For a sort by, dereference to the ENUMLITERAL.ORDERNUM.
 * <p>
 * 3 - Multi-valued Enums
 * A multi-valued Enum data type (MULTIENUM, MULTICHECK, etc) is really a value
 * that is a set as opposed to a sinlge (scalar) value.  Comparison operators are
 * are applying to the set.
 * <p>
 * 4 - Dates
 * The dates stored in the database are accurate to the millisecond, however
 * when users specifies filter criteria they are interested in the 'day'.
 * Therefore the query engine must apply the following interpretations to achieve the
 * proper comparison operator sematics:
 * <p>
 * NOTE: C1 is the column / object state variable.  D2 is user-specified filter criteria.
 * <p>
 * Comparison                 ==>  Interpretation
 * ----------------------------------------------
 * C1 = D2                  => C1 >= D2 and C1 < (D2 + 1 day)
 * C1 != D2                 => C1 <  D2 or  C1 >= (D2 + 1 day)
 * C1 'on or before'  D2    => C1 < (D2 + 1 day)
 * C1 'on or after' D2      => C1 >= D2
 * <p>
 * 5 - Roles
 * Roles are treated in a manner analogous to enumerated attributes.  The following
 * analogous between tables hold:
 * <p>
 * Roles               Enum Attrs
 * ---------------------------------
 * ProcessRole       = Atribute
 * ObjectRoleAssign  = EnumSelection
 * User              = EnumLiteral
 * <p>
 * For a select, dereference the User.OBJECTID to the User.NAME (first last)
 * For an inequality filter (i.e., ">", "<", ">=", etc) (not applicable)
 * For a sort by, dereference to the User.LASTNAME.
 */
public class RdbQuerier extends Querier {
    public static SqlQuerier getSQLWriter(DBVendor vendor, IXMR xmr)
            throws Exception {
        SqlQuerier retObj = null;

        if (vendor == DBVendor.ORACLE) {
            retObj = new OracleSqlQuerier();
        } else if (vendor == DBVendor.SQL_SERVER) {
            retObj = new SqlServerSqlQuerier();
        }
        retObj.setXMR(xmr);

        return retObj;
    }

    //-------------------------------------------------------------
    // Static methods
    //-------------------------------------------------------------

    /**
     * These are the default mappings of IOperator to SQL operator.
     * Method buildOperClause( ) might override these depending on
     * the argument types.
     */
    public static String mapOperToSQL(IOperator oper) {
        String retVal = null;
        if (oper == CompOper.COMP_LIKE) retVal = "LIKE";
        else if (oper == CompOper.COMP_NOTLIKE) retVal = "NOT LIKE";
        else if (oper == CompOper.COMP_ELEMENTOF) retVal = "IN";
        else if (oper == CompOper.COMP_NOTELEMENTOF) retVal = "NOT IN";
        else if (oper == CompOper.COMP_EQ) retVal = "=";
        else if (oper == CompOper.COMP_NOTEQ) retVal = "<>";
        else if (oper == CompOper.COMP_GT) retVal = ">";
        else if (oper == CompOper.COMP_GTEQ) retVal = ">=";
        else if (oper == CompOper.COMP_LT) retVal = "<";
        else if (oper == CompOper.COMP_LTEQ) retVal = "<=";
        else if (oper == CompOper.COMP_ONORAFTER) retVal = ">=";
        else if (oper == CompOper.COMP_ONORBEFORE) retVal = "<=";
        else if (oper == CompOper.COMP_ISNULL) retVal = "IS";
        else if (oper == CompOper.COMP_ISNOTNULL) retVal = "IS NOT";
        else if (oper == BoolOper.BOOL_AND) retVal = "AND";
        else if (oper == BoolOper.BOOL_OR) retVal = "OR";

        return retVal;
    }

    public static String mapDirToSQL(SortDir dir) {
        String retVal = null;
        if (dir == SortDir.ASC) retVal = "ASC";
        else if (dir == SortDir.DESC) retVal = "DESC";
        return retVal;
    }

    //-------------------------------------------------------------
    // Private instance vars
    //-------------------------------------------------------------

    private IXMR xmr = null;
    private List allAssocs = new Vector();
    private List allExtAttrs = new Vector();
    private List allEnumLitAttrs = new Vector();
    private List allRoleAttrs = new Vector();
    private boolean extKeywordSearch = false;

    //-------------------------------------------------------------
    // Constructor(s)
    //-------------------------------------------------------------
    public RdbQuerier() {
    }

    //-------------------------------------------------------------
    // Public instance methods
    //-------------------------------------------------------------
    public void setXMR(IXMR xmr) {
        xmr = xmr;
    }

    public List getAssocs() {
        return allAssocs;
    }

    public List getExtAttrs() {
        return allExtAttrs;
    }

    public List getEnumLitAttrs() {
        return allEnumLitAttrs;
    }

    public boolean getIsExtKeywordSearch() {
        return extKeywordSearch;
    }

    /**
     * Creates a union of all queries.  Consuming object must ensure
     * that queries are compatible for UNION.
     *
     * @ param sortdirs Must contain one SortDir for each item in the
     * select since all sorting is done by number.
     */
    public String translate(List queries, List sortdirs)
            throws Exception {
        StringBuffer sbSql = new StringBuffer(1000);

        Iterator iter = null;
        if (queries != null && (iter = queries.iterator()) != null) {
            while (iter.hasNext()) {
                IQuery query = (IQuery) iter.next();
                translate(sbSql, query);
                if (queries.indexOf(query) < (queries.size() - 1))
                    sbSql.append(" UNION ");
            }
        }
        if (sortdirs != null && (iter = sortdirs.iterator()) != null) {
            sbSql.append(" ORDER BY ");
            int num = 0;
            while (iter.hasNext()) {
                num++;
                SortDir dir = (SortDir) iter.next();
                sbSql.append(num + mapDirToSQL(dir));
                if (num < sortdirs.size())
                    sbSql.append(", ");
            }
        }
        // This is baaaadddddd....
        String retVal = sbSql.toString();
        if (retVal.length() < 10)
            retVal = null;
        return retVal;
    }

    public String translate(IQuery query)
            throws Exception {
        String retSql = "";
        StringBuffer sbSql = new StringBuffer(1000);
        translate(sbSql, query);
        return sbSql.toString();
    }

    //-------------------------------------------------------------
    // Private instance methods
    //-------------------------------------------------------------

    private void translate(StringBuffer sbSql, IQuery query)
            throws Exception {
        allAssocs = new Vector();
        allExtAttrs = new Vector();
        allEnumLitAttrs = new Vector();
        allRoleAttrs = new Vector();
        extKeywordSearch = false;

        // pre-evaluate IQuery to determine needed associations / joins...
        preEvalQuery(query);

        doSelect(sbSql, query);
        doFrom(sbSql, query);
        doWhere(sbSql, query);
        doOrderBy(sbSql, query);
    }

    /**
     * Traverse attrRefs to get superset of associations needed.
     */
    private void preEvalQuery(IQuery query) {
        // eval select
        buildListsFromSelect(query.getSelect().getAttrs());

        // eval filter
        buildListsFromFilter(query.getFilter().getExpr());

        // eval sort
        buildListsFromSort(query.getSort().getSortItems());

    }

    private void buildListsFromSelect(List attrs) {
        List alist = getAssocs();

        Iterator iattrs = attrs.iterator();
        while (iattrs.hasNext()) {
            IQSelectItem selitem = (IQSelectItem) iattrs.next();
            if (selitem.getAttr() instanceof IQAttrRef) {
                IQAttrRef attrref = (IQAttrRef) selitem.getAttr();
                addChainToList(attrref.getAssocs(), alist);
                addAttrToLists(attrref);
            }
        }
    }

    /**
     * Adds the IQAttrRef to the proper pre-eval list(s).
     * NOTE: An IXClassAttr is a role (IXClassAttr.getIsRole() == TRUE) has a
     * Primitive type of Primitive.ENUM or MULTIENUM, but is not a true
     * enum literal; thus, the special handling below.
     */
    private void addAttrToLists(IQAttrRef attrref) {
        List extAttrs = getExtAttrs();
        List enumLitAttrs = getEnumLitAttrs();
        if (attrref.getAttr().getIsExtended() && !extAttrs.contains(attrref)) {
            extAttrs.add(attrref);
        }
        if (attrref.getAttr().getPrimType().isEnum() && !enumLitAttrs.contains(attrref)) {
            enumLitAttrs.add(attrref);
        }
    }

    private void buildListsFromSort(List sorts) {
        List alist = getAssocs();
        Iterator isorts = sorts.iterator();
        while (isorts.hasNext()) {
            IQSortItem sortitem = (IQSortItem) isorts.next();
            addChainToList(sortitem.getAttrRef().getAssocs(), alist);
            IQAttrRef attrref = sortitem.getAttrRef();
            addAttrToLists(attrref);
        }
    }

    private void buildListsFromFilter(Object arg) {
        List alist = getAssocs();
        if (arg instanceof IQFilterExpr) {
            IQFilterExpr fex = (IQFilterExpr) arg;

            // Track whether query is searching extended keywords attritbutes
            if (((IQFilterExpr) arg).getOper() == CompOper.COMP_EXT_KEYWORD_LIKE)
                extKeywordSearch = true;

            buildListsFromFilter(fex.getLeft());
            buildListsFromFilter(fex.getRight());
        } else if (arg instanceof IQAttrRef) {
            IQAttrRef attrref = (IQAttrRef) arg;
            addChainToList(attrref.getAssocs(), alist);
            addAttrToLists(attrref);
        }
    }


    private void addChainToList(IXAssocChain chain, List alist) {
        Iterator ichain = chain.iterator();
        while (ichain.hasNext()) {
            IXAssoc ass = (IXAssoc) ichain.next();
            if (!alist.contains(ass)) {
                alist.add(ass);
            }
        }
    }

    private void doSelect(StringBuffer sbSql, IQuery query)
            throws AppException {
        List extAttrs = getExtAttrs();

        Vector sqlitems = new Vector();
        IQSelect select = query.getSelect();
        if (select != null) {
            sbSql.append("SELECT ");
            if (select.getUseDistinct())
                sbSql.append(" DISTINCT ");
            List items = select.getAttrs();
            if (items != null) {
                Iterator iter = items.iterator();
                while (iter.hasNext()) {
                    IQSelectItem selitem = (IQSelectItem) iter.next();
                    String strItem = "";
                    if (selitem.getAttr() == null)
                        throw new AppException("null valued select item");

                    if (selitem.getAttr() instanceof IQAttrRef) {
                        IQAttrRef ar = (IQAttrRef) selitem.getAttr();
                        if (ar.getAttr().getIsRole()) {
                            strItem = getEnumTableSyn(query.getTargetClass(), ar, extAttrs) + ".LASTNAME";
                        } else if (ar.getAttr().getPrimType().isEnum()) {
                            strItem = getEnumTableSyn(query.getTargetClass(), ar, extAttrs) + ".NAME";
                        } else {
                            strItem = getAnyColRef(query.getTargetClass(), ar, extAttrs);
                        }
                    } else
//          if (selitem.getAttr() instanceof String)
                    {
                        strItem = selitem.getAttr().toString();
                    }
                    // add the alias
                    if (selitem.getAlias() != null) {
                        strItem += " AS " + selitem.getAlias() + " ";
                    }
                    sqlitems.add(strItem);
                }
                sbSql.append(StringUtil.buildCommaDelList(sqlitems));
            } else {
                sbSql.append(" * ");
            }
        }
    }

    private static final char C_SPC = ' ';

    /**
     * Builds the from clause.
     */
    private void doFrom(StringBuffer sbSql, IQuery query)
            throws AppException {
        List assocs = getAssocs();
        List extAttrs = getExtAttrs();
        List enumLitAttrs = getEnumLitAttrs();

        sbSql.append(" FROM ");
        IXClass target = query.getTargetClass();

        // central table of the join
        sbSql.append(target.getTableName());
        sbSql.append(C_SPC);
        sbSql.append(_getTableSyn(target, null));
        sbSql.append(C_SPC);

        IXAssocChain chain = new AssocChain();
        List visits = new Vector();
        // join in all tables corresponding to referenced classes
        doFromRec(sbSql, target, chain, target, assocs, visits);

        // join in extended attr tables as needed
        Iterator iAttrs = extAttrs.iterator();
        while (iAttrs.hasNext()) {
            IQAttrRef attrref = (IQAttrRef) iAttrs.next();
            addExtAttrJoin(sbSql, target, attrref, extAttrs);
        }

        // join in enumliteral table as needed
        iAttrs = enumLitAttrs.iterator();
        while (iAttrs.hasNext()) {
            IQAttrRef attrref = (IQAttrRef) iAttrs.next();
            addEnumLitAttrJoin(sbSql, target, attrref, extAttrs);
        }

        // add in ext attr tables (CHAR, and LONG_CHAR) as needed
        if (getIsExtKeywordSearch()) {
            String parsyn = getTableSyn(target, null);
            sbSql.append(" LEFT OUTER JOIN ");
            sbSql.append(_xmr.mapPrimToTable(Primitive.CHAR));
            sbSql.append(C_SPC);
            sbSql.append(SYN_EXCHAR);
            sbSql.append(C_SPC);
            sbSql.append(" ON " + SYN_EXCHAR + ".PAROBJECTID = " + parsyn + ".OBJECTID");
            sbSql.append(" LEFT OUTER JOIN ");
            sbSql.append(_xmr.mapPrimToTable(Primitive.LONG_CHAR));
            sbSql.append(C_SPC);
            sbSql.append(SYN_EXLCHAR);
            sbSql.append(C_SPC);
            sbSql.append(" ON " + SYN_EXLCHAR + ".PAROBJECTID = " + parsyn + ".OBJECTID");
        }
    }

    /**
     * Does depth-first build of SQL joins.
     */
    private void doFromRec(StringBuffer sbSql, IXClass start, IXAssocChain chain, IXClass left, List assocs, List visits)
            throws AppException {
        List classAssocs = null;
        try {
            classAssocs = xmr.getClassAssocs(left);
        } catch (Exception ex) {
            throw new AppException(ex);
        }

        Iterator icas = classAssocs.iterator();
        while (icas.hasNext()) {
            IXAssoc xas = (IXAssoc) icas.next();
            if (_isNewAndNeeded(xas, assocs, visits)) {
                visits.add(xas);
                IXClass right = getOtherClass(xas, left);
                buildFrom(sbSql, start, chain, xas, left, right);
                chain.add(xas);
                doFromRec(sbSql, start, chain, right, assocs, visits);
                chain.removeLast();
            }
        }
    }

    private IXClass getOtherClass(IXAssoc ass, IXClass end)
            throws AppException {
        IXClass retObj = null;
        try {
            retObj = getXMR().getOtherEnd(ass, end);
        } catch (Exception ex) {
            throw new AppException(ex);
        }
        return retObj;
    }

    IXMR getXMR() {
        return xmr;
    }

    /**
     * Builds the SQL join need to traverse the specified association (xas).
     *
     * @param left      The left class side of the association.  This has already been added
     *                  to the join.
     * @param right     The  side of the association.  This is the class we are join into the
     *                  overall join clause.
     * @param leftchain The chain of associations leading up to the left class.  Does not include xas.
     */
    private void buildFrom(StringBuffer sbSql, IXClass start, IXAssocChain leftchain, IXAssoc xas, IXClass left, IXClass right)
            throws AppException {
        String childSyn = null;
        String parentSyn = null;
        String leftCol = null;
        String rightCol = null;
        IXAssocChain rightchain = new AssocChain(leftchain);
        rightchain.add(xas);
        String rightSyn = getTableSyn(start, rightchain);
        String leftSyn = getTableSyn(start, leftchain);
        // treat m2m and rec assocs kind of the same.
        if (xas.isM2M() || xas.isRec()) {
            String assocTableName = null;
            String assocTableSyn = null;
            if (xas.isM2M()) {
                // leftTable LEFT OUTER JOIN assocTable LEFT OUTER JOIN rightTable
                assocTableName = xas.getAssocClass().getTableName();
                assocTableSyn = leftSyn + xas.getSyn() + xas.getAssocClass().getSyn();
                if (left == xas.getFromClass()) {
                    childSyn = getTableSyn(start, leftchain);
                    parentSyn = getTableSyn(start, rightchain);
                    leftCol = xas.getFromColName();
                    rightCol = xas.getToColName();
                } else {
                    childSyn = getTableSyn(start, rightchain);
                    parentSyn = getTableSyn(start, leftchain);
                    leftCol = xas.getToColName();
                    rightCol = xas.getFromColName();
                }
            } else if (xas.isRec()) {
                // theTable LEFT OUTER JOIN indexTable LEFT OUTER JOIN theTable
                // NOTE: for recursive assoc, getFromClass( ) == getToClass( )
                if (xas.getFromClass() != xas.getToClass())
                    throw new AppException("A recursive association [" + xas.getSyn() + "," + xas.getIID().getLongValue() + "] was found in which the from class does not equal the to class.");

                assocTableName = xas.getFromClass().getIndexTable();
                assocTableSyn = leftSyn + xas.getSyn();
                childSyn = getTableSyn(start, leftchain);
                parentSyn = getTableSyn(start, rightchain);
                leftCol = xas.getFromClass().getIndexChildColName();
                rightCol = xas.getFromClass().getIndexParentColName();
            }
            sbSql.append(" LEFT OUTER JOIN ");
            sbSql.append(assocTableName);
            sbSql.append(C_SPC);
            sbSql.append(assocTableSyn);
            sbSql.append(" ON ");
            sbSql.append(assocTableSyn + "." + leftCol + "=" + childSyn + ".OBJECTID");
            sbSql.append(" LEFT OUTER JOIN ");
            sbSql.append(right.getTableName());
            sbSql.append(C_SPC);
            sbSql.append(rightSyn);
            sbSql.append(" ON ");
            sbSql.append(assocTableSyn + "." + rightCol + "=" + parentSyn + ".OBJECTID");
        } else {
            if (left == xas.getFromClass()) {
                // left table is the child
                childSyn = getTableSyn(start, leftchain);
                parentSyn = getTableSyn(start, rightchain);
            } else {
                // right table is the child ???
                // Is this valid?  --> yes.
                childSyn = getTableSyn(start, rightchain);
                parentSyn = getTableSyn(start, leftchain);
            }
            sbSql.append(" LEFT OUTER JOIN ");
            sbSql.append(right.getTableName());
            sbSql.append(C_SPC);
            sbSql.append(rightSyn);
            sbSql.append(" ON ");
            sbSql.append(childSyn + "." + xas.getFromColName());
            sbSql.append(" = ");
            sbSql.append(parentSyn + "." + "OBJECTID ");
        }
    }

    private void addExtAttrJoin(StringBuffer sbSql, IXClass start, IQAttrRef attrref, List extAttrs)
            throws AppException {
        String parsyn = getTableSyn(start, attrref.getAssocs());
        String xsyn = getXTableSyn(start, attrref, extAttrs);
        //
        if (!attrref.getAttr().getIsRole()) {
            sbSql.append(" LEFT OUTER JOIN ");
            sbSql.append(_xmr.mapPrimToTable(attrref.getAttr().getPrimType()));
            sbSql.append(C_SPC);
            sbSql.append(xsyn);
            sbSql.append(C_SPC);
            sbSql.append(" ON (" + xsyn + ".PAROBJECTID = " + parsyn + ".OBJECTID");
            sbSql.append(" AND " + xsyn + ".ATTRIBUTEID = " + attrref.getAttr().getIID().getLongValue() + ") ");
        } else {
            sbSql.append(" LEFT OUTER JOIN OBJECTROLEASSIGN ");
            sbSql.append(C_SPC);
            sbSql.append(xsyn);
            sbSql.append(C_SPC);
            sbSql.append(" ON (" + xsyn + ".PAROBJECTID = " + parsyn + ".OBJECTID");
            sbSql.append(" AND " + xsyn + ".ROLEID = " + attrref.getAttr().getIID().getLongValue() + ") ");
        }
    }

    /**
     * Adds join to ENUMLITERAL table to get order num or other columns.
     */
    private void addEnumLitAttrJoin(StringBuffer sbSql, IXClass start, IQAttrRef attrref, List extAttrs)
            throws AppException {
        String enumref = getAnyColRef(start, attrref, extAttrs);
        String enumtabsyn = getEnumTableSyn(start, attrref, extAttrs);
        //
        String table = "ENUMLITERAL";
        if (attrref.getAttr().getIsRole())
            table = "APPUSER";

        sbSql.append(" LEFT OUTER JOIN " + table);
        sbSql.append(C_SPC);
        sbSql.append(enumtabsyn);
        sbSql.append(C_SPC);
        sbSql.append(" ON ( " + enumref + "=" + enumtabsyn + ".OBJECTID)");
    }

    private String getAnyColRef(IXClass start, IQAttrRef attrref, List extAttrs)
            throws AppException {
        String retVal = null;
        IXClassAttr attr = attrref.getAttr();
        if (attr.getIsExtended()) {
            retVal = getXColRef(start, attrref, extAttrs);
        } else {
            retVal = getColRef(start, attrref);
        }
        return retVal;
    }

    /**
     * Gets a column ref for an extended attribute.
     */
    private String getXColRef(IXClass start, IQAttrRef attrref, List extAttrs)
            throws AppException {
        // add normal table ref
        String retVal = getXTableSyn(start, attrref, extAttrs);
        // add index of ext attr based on provided list
        retVal += ".";
        if (attrref.getAttr().getIsRole()) {
            retVal += "USERID";
        } else if (attrref.getAttr().getPrimType().isMultiValued()) {
            retVal += "ENUMLITERALID";
        } else {
            retVal += "VALUE";
        }
        return retVal;
    }

    private String getColRef(IXClass start, IQAttrRef attrref)
            throws AppException {
        // add normal table ref
        String retVal = getColRef(start, attrref.getAssocs(), attrref.getAttr());
        return retVal;
    }

    /**
     * Builds an unambiguous SQL column reference for an attr/column reference.
     */
    private String getColRef(IXClass start, IXAssocChain asses, IXClassAttr attr)
            throws AppException {
        String retVal = getTableSyn(start, asses);
        retVal += "." + attr.getColName();
        return retVal;
    }

    private String getEnumTableSyn(IXClass start, IQAttrRef attrref, List extAttrs)
            throws AppException {
        return getAttrTableSyn(start, attrref, extAttrs)
                + (attrref.getAttr().getIsRole() ? "_usr" : "_enlit");
    }

    private String getAttrTableSyn(IXClass start, IQAttrRef attrref, List attrs)
            throws AppException {
        if (attrref.getAttr().getIsRole())
            return getXTableSyn(start, attrref, attrs);
        else if (attrref.getAttr().getIsExtended())
            return getXTableSyn(start, attrref, attrs);
        else
            return getTableSyn(start, attrref.getAssocs());
    }

    /**
     * Builds an unambiguous synonym for a class / table reference.
     */
    private String getTableSyn(IXClass start, IXAssocChain asses)
            throws AppException {
        String retVal = start.getSyn();
        IXClass current = start;
        Iterator iAsses = null;
        if (asses != null && (iAsses = asses.iterator()) != null) {
            while (iAsses.hasNext()) {
                IXAssoc ass = (IXAssoc) iAsses.next();
                IXClass next = getOtherClass(ass, current);
                retVal += "_" + ass.getSyn();
                retVal += "_" + next.getSyn();
                current = next;
            }
        }
        return retVal;
    }

    private String getXTableSyn(IXClass start, IQAttrRef attrref, List extAttrs)
            throws AppException {
        String retVal = getTableSyn(start, attrref.getAssocs());
        retVal += "_" + extAttrs.indexOf(attrref);
        return retVal;
    }

    private boolean isNewAndNeeded(IXAssoc xas, List assocs, List visits) {
        boolean retVal = false;
        if (assocs.contains(xas) && !visits.contains(xas)) {
            retVal = true;
        }
        return retVal;
    }


    private void doWhere(StringBuffer sbSql, IQuery query)
            throws Exception, AppException {
        IQFilter filter = query.getFilter();
        if (filter.getExpr() != null) {
            sbSql.append(" WHERE ");
            buildWhere(sbSql, null, query.getTargetClass(), query.getFilter().getExpr());
        }
    }

    private void buildWhere(StringBuffer sbSql, IQFilterExpr parExpr, IXClass start, Object arg)
            throws Exception, AppException {
        boolean bIsParentEnumInequality = isEnumInequality(parExpr);

        if (arg instanceof IQFilterExpr) {
            // check for special cases first.
            IQFilterExpr fex = (IQFilterExpr) arg;
            boolean bIsDateExpr = isDateExpr(fex);
            IOperator oper = fex.getOper();
            sbSql.append("(");
            if (oper == CompOper.COMP_CONTAINS) {
                doOper_ContainsAny(sbSql, fex, start);
            } else if (oper == CompOper.COMP_NOTCONTAINS) {
                doOper_NotContainsAny(sbSql, fex, start);
            } else if (oper == CompOper.COMP_CONTAINSALL) {
                doOper_ContainsAll(sbSql, fex, start);
            } else if (bIsDateExpr) {
                doOper_Date(sbSql, fex, start);
            } else if (oper == CompOper.COMP_TRUE || oper == CompOper.COMP_FALSE) {
                doOper_Boolean(sbSql, fex, start);
            } else if (oper == CompOper.COMP_EXT_KEYWORD_LIKE) {
                doOper_ExtKeywordLike(sbSql, fex, start);
            } else {
                // do the 'simple' operators that map directly to sql
                buildWhere(sbSql, fex, start, fex.getLeft());
                buildOperClause(sbSql, fex);
                buildWhere(sbSql, fex, start, fex.getRight());
            }
            sbSql.append(")");
        }
        // handle a class attribute reference
        else if (arg instanceof IQAttrRef || arg instanceof IXClassAttr) {
            doAttrSQL(arg, sbSql, start, bIsParentEnumInequality);
        }
        // handle user entered literals
        else {
            if (bIsParentEnumInequality) {
                sbSql.append(_doEnumLitSubSelect(parExpr, arg));
            } else {
                // ASSERTION: parent operator must be a CompOper
                sbSql.append(_doUserArg(parExpr, arg));
            }
        }
    }

    private String doUserArg(IQFilterExpr parExpr, Object arg)
            throws AppException {
        String retVal = null;
        if (arg != null) {
            CompOper cop = (CompOper) parExpr.getOper();
            if (arg instanceof String) {
                if (cop == CompOper.COMP_LIKE || cop == CompOper.COMP_NOTLIKE || cop == CompOper.COMP_EXT_KEYWORD_LIKE) {
                    retVal = "'%" + SQLUtil.primer(arg.toString()) + "%'";
                } else {
                    retVal = "'" + SQLUtil.primer(arg.toString()) + "'";
                }
            } else if (arg instanceof java.sql.Timestamp) {
                retVal = "'" + DateUtil.truncToDayStart((java.sql.Timestamp) arg) + "'";
            } else if (arg instanceof Boolean) {
                retVal = getBooleanLiteral(arg);
            } else if (arg instanceof Object[]) {
                retVal = '(' + StringUtil.buildCommaDelList(Arrays.asList((Object[]) arg)) + ')';
            } else if (arg instanceof long[]) {
                retVal = '(' + StringUtil.buildCommaDelList((long[]) arg) + ')';
            } else {
                retVal = SQLUtil.primer(arg.toString());
            }
        }
        return retVal;
    }

    private String getBooleanLiteral(Object arg) {
        String retVal = "";
        if (arg instanceof Boolean) {
            Boolean barg = (Boolean) arg;
            if (barg == Boolean.TRUE)
                retVal = "1";
            else
                retVal = "0";
        }
        return retVal;
    }

    /**
     * Build SQL for and attr/column reference.
     */
    private void doAttrSQL(Object arg, StringBuffer sbSql, IXClass start, boolean bIsParentEnumInequality)
            throws Exception {
        List extAttrs = getExtAttrs();
        IQAttrRef attrref = null;
        if (arg instanceof IQAttrRef) {
            attrref = (IQAttrRef) arg;
        } else if (arg instanceof IXClassAttr) {
            attrref = XMR.getInstance().getAttrRef(null, start, null, (IXClassAttr) arg);
        }
        // if refering to an enum literal as part of an inequality, dereference to the
        // ENUMLITERAL.ORDERNUM
        if (bIsParentEnumInequality) {
            sbSql.append(_getEnumTableSyn(start, attrref, extAttrs) + ".ORDERNUM");
        } else {
            sbSql.append(_getAnyColRef(start, attrref, extAttrs));
        }
    }

    public static final String SYN_EXCHAR = "xk_char";
    public static final String SYN_EXLCHAR = "xk_lchar";

    private void doOper_ExtKeywordLike(StringBuffer sbSql, IQFilterExpr fex, IXClass start)
            throws Exception {
        String right = ".VALUE LIKE " + doUserArg(fex, fex.getRight());
        sbSql.append(SYN_EXCHAR);
        sbSql.append(right);
        sbSql.append(" OR ");
        sbSql.append(SYN_EXLCHAR);
        sbSql.append(right);
    }

    private void doOper_Boolean(StringBuffer sbSql, IQFilterExpr fex, IXClass start)
            throws Exception {
        // do the 'simple' operators that map directly to sql
        buildWhere(sbSql, fex, start, fex.getLeft());
        sbSql.append(" = ");
        Boolean bval = ((fex.getOper() == CompOper.COMP_TRUE) ? Boolean.TRUE : Boolean.FALSE);
        sbSql.append(_doUserArg(fex, bval));
    }

    /**
     * Comparison                 ==>  Interpretation
     * ----------------------------------------------
     * C1 = D2                  => C1 >= D2 and C1 < (D2 + 1 day)
     * C1 != D2                 => C1 < D2 or  C1 >= (D2 + 1 day)
     * C1 'on or before'  D2    => C1 < (D2 + 1 day)
     * C1 'on or after' D2      => C1 >= D2
     */
    private void doOper_Date(StringBuffer sbSql, IQFilterExpr fex, IXClass start)
            throws Exception {
        List extAttrs = getExtAttrs();
        CompOper cop = (CompOper) fex.getOper();
        java.sql.Timestamp arg = null;
        if (fex.getRight() instanceof java.sql.Timestamp) {
            arg = (java.sql.Timestamp) fex.getRight();
        } else {
            arg = DateFormatter.getDateTimestamp(fex.getRight().toString());
        }

        if (cop == CompOper.COMP_EQ) {
            doAttrSQL(fex.getLeft(), sbSql, start, false);
            sbSql.append(" >= ");
            sbSql.append("'" + arg + "'");
            sbSql.append(" AND ");
            doAttrSQL(fex.getLeft(), sbSql, start, false);
            sbSql.append(" < ");
            sbSql.append("'" + DateUtil.add(arg, Calendar.DATE, 1) + "'");
        } else if (cop == CompOper.COMP_NOTEQ) {
            doAttrSQL(fex.getLeft(), sbSql, start, false);
            sbSql.append(" < ");
            sbSql.append("'" + arg + "'");
            sbSql.append(" OR ");
            doAttrSQL(fex.getLeft(), sbSql, start, false);
            sbSql.append(" >= ");
            sbSql.append("'" + DateUtil.add(arg, Calendar.DATE, 1) + "'");
        } else if (cop == CompOper.COMP_ONORBEFORE) {
            doAttrSQL(fex.getLeft(), sbSql, start, false);
            sbSql.append(" < ");
            sbSql.append("'" + DateUtil.add(arg, Calendar.DATE, 1) + "'");
        } else if (cop == CompOper.COMP_ONORAFTER) {
            doAttrSQL(fex.getLeft(), sbSql, start, false);
            sbSql.append(" >= ");
            sbSql.append("'" + arg + "'");
        } else {
        }
    }

    /**
     * Implement rules for mapping IOperator's to SQL operators.
     */
    private void buildOperClause(StringBuffer sbSql, IQFilterExpr fex)
            throws Exception {
        IOperator oper = fex.getOper();
        // use default mapping
        sbSql.append(C_SPC);
        sbSql.append(mapOperToSQL(oper));
        sbSql.append(C_SPC);
    }

    private boolean isEnumInequality(IQFilterExpr fex) {
        boolean retVal = false;
        if (fex != null
                && fex.getOper() instanceof CompOper
                && fex.getLeft() instanceof IQAttrRef) {
            CompOper oper = (CompOper) fex.getOper();
            IQAttrRef attrref = (IQAttrRef) fex.getLeft();
            retVal = (oper.isInequality() && attrref.getAttr().getPrimType().isEnum());
        }
        return retVal;
    }

    private boolean isDateExpr(IQFilterExpr fex) {
        boolean retVal = false;
        if (fex != null
                && fex.getOper() instanceof CompOper
                && fex.getLeft() instanceof IQAttrRef) {
            CompOper oper = (CompOper) fex.getOper();
            IQAttrRef attrref = (IQAttrRef) fex.getLeft();
            PrimitiveDataType primtype = attrref.getAttr().getPrimType();
            retVal = (primtype == Primitive.TIME);
        }
        return retVal;
    }

    private void doOper_ContainsAny(StringBuffer sbSql, IQFilterExpr fex, IXClass start)
            throws Exception {
        doOper_ContainsXXX(sbSql, fex, BoolOper.BOOL_OR, start);
    }

    private void doOper_ContainsXXX(StringBuffer sbSql, IQFilterExpr fex, BoolOper bop, IXClass start)
            throws Exception {
        try {
            IQAttrRef aref = (IQAttrRef) fex.getLeft();
            String subselect = " in " + doEnumSubSelect(aref, start);
            long ids[] = (long[]) fex.getRight();
            for (int idx = 0; idx < ids.length; idx++) {
                sbSql.append("(" + ids[idx] + subselect + ")");
                if (idx < (ids.length - 1))
                    sbSql.append(mapOperToSQL(bop));
            }
        } catch (Exception ex) {
            throw new Exception(ex);
        }
    }

    private String doEnumLitSubSelect(IQFilterExpr parExpr, Object arg)
            throws AppException {
        return "(SELECT ORDERNUM FROM ENUMLITERAL EL WHERE OBJECTID = " + doUserArg(parExpr, arg) + ")";
    }

    private String doEnumSubSelect(IQAttrRef aref, IXClass start)
            throws AppException {
        String retVal = null;
        if (aref.getAttr().getIsRole()) {
            retVal = "(SELECT USERID FROM OBJECTROLEASSIGN ORA WHERE PAROBJECTID = "
                    + getTableSyn(start, aref.getAssocs()) + ".OBJECTID "
                    + " AND ROLEID = " + aref.getAttr().getIID().getLongValue() + ")";
        } else {
            retVal = "(SELECT ENUMLITERALID FROM ENUMSELECTION ES WHERE PAROBJECTID = "
                    + getTableSyn(start, aref.getAssocs()) + ".OBJECTID "
                    + " AND ATTRIBUTEID = " + aref.getAttr().getIID().getLongValue() + ")";
        }
        return retVal;
    }

    private void doOper_NotContainsAny(StringBuffer sbSql, IQFilterExpr fex, IXClass start)
            throws Exception {
        sbSql.append("NOT (");
        doOper_ContainsAny(sbSql, fex, start);
        sbSql.append(")");
    }

    private void doOper_ContainsAll(StringBuffer sbSql, IQFilterExpr fex, IXClass start)
            throws Exception {
        doOper_ContainsXXX(sbSql, fex, BoolOper.BOOL_AND, start);
    }

    private void doOper_NotContainsAll(StringBuffer sbSql, IQFilterExpr fex, IXClass start)
            throws Exception {
        sbSql.append("NOT (");
        doOper_ContainsAll(sbSql, fex, start);
        sbSql.append(")");
    }

    private boolean hasPrimArg(IQFilterExpr fex, PrimitiveDataType prim) {
        boolean retVal = false;
        retVal = (_isPrimType(fex.getLeft(), prim) || isPrimType(fex.getRight(), prim));
        return retVal;
    }

    private boolean isPrimType(Object arg, PrimitiveDataType prim) {
        boolean retVal = false;
        if (arg instanceof IQAttrRef) {
            IQAttrRef attrref = (IQAttrRef) arg;
            retVal = (attrref.getAttr().getPrimType() == prim);
        }
        return retVal;
    }

    private PrimitiveDataType getExprPrimType(IQFilterExpr fex) {
        PrimitiveDataType retVal = null;
        if ((retVal = getPrimType(fex.getLeft())) == null) {
            retVal = getPrimType(fex.getRight());
        }
        return retVal;
    }

    private PrimitiveDataType getPrimType(Object arg) {
        PrimitiveDataType retVal = null;
        if (arg instanceof IQAttrRef) {
            IQAttrRef attrref = (IQAttrRef) arg;
            retVal = attrref.getAttr().getPrimType();
        }
        return retVal;
    }

    private void doOrderBy(StringBuffer sbSql, IQuery query)
            throws Exception {
        List extAttrs = getExtAttrs();

        Vector sqlitems = new Vector();
        IQSort sort = query.getSort();
        if (sort != null) {
            List sorts = sort.getSortItems();
            if (sorts != null && sorts.size() > 0) {
                sbSql.append(" ORDER BY ");
                Iterator isort = sorts.iterator();
                while (isort.hasNext()) {
                    IQSortItem si = (IQSortItem) isort.next();
                    String colref = null;
                    if (si.getAttrRef().getAttr().getPrimType().isEnum()) {
                        colref = getEnumTableSyn(query.getTargetClass(), si.getAttrRef(), extAttrs) + ".ORDERNUM";
                    } else {
                        colref = getAnyColRef(query.getTargetClass(), si.getAttrRef(), extAttrs);
                    }
                    sqlitems.add(colref + " " + mapDirToSQL(si.getDir()));
                }
                sbSql.append(StringUtil.buildCommaDelList(sqlitems));
            }
        }
    }
}