package com.oculussoftware.bus.mkt.prod;

import com.oculussoftware.system.*;
import com.oculussoftware.api.repi.*;
import com.oculussoftware.api.sysi.*;
import com.oculussoftware.api.busi.mkt.prod.*;
import com.oculussoftware.api.busi.*;
import com.oculussoftware.repos.util.SequentialIID;
import com.oculussoftware.rdb.*;
import com.oculussoftware.bus.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
* Filename:    ProductVersionColl.java
* Date:        
* Description: Handles a list of IProductVersion objects
*
* Copyright 1-31-2000 Oculus Software.  All Rights Reserved.
*
* @author Saleem Shafi
* @version 1.2
*/

/*
* Change Activity
*
* Issue number  	Programmer    	Date      	Description
*                   isyed          7/27/00    fixed SQL error 
*/

public class FeatureBaselineAddedColl extends FeatureColl implements IFeatureColl, IPersistable
{
  protected String TABLE = "FEATURE";
  protected String COL_ORDERNUM = "ORDERNUM";
  protected String COL_DELETESTATE = "DELETESTATE";

  private String _firstID;
  private String _secondID;
	//----------------------------- Public Constructor -------------------------
	/** Default constructor just initializes the product list */
	public FeatureBaselineAddedColl() throws OculusException
	{
    super();
	}

  protected FeatureBaselineAddedColl(Comparator sortCrit) throws OculusException
  {
    super(sortCrit);
  }

  //------------------------ Protected Methods --------------------------------
  protected String getLoadQuery()
    throws ORIOException
  {
    return " SELECT feat.OBJECTID "+
           " FROM ((FEATURE feat LEFT OUTER JOIN FEATUREREVISION rev ON feat.OBJECTID=rev.FEATUREID) "+
           "   LEFT OUTER JOIN BCATFEATURELINK bcatfeatlink ON rev.OBJECTID=bcatfeatlink.REVISIONID) "+
           "   LEFT OUTER JOIN BCATEGORY bcat ON bcatfeatlink.CATEGORYID=bcat.OBJECTID "+
           " WHERE bcat.BASELINEID="+_firstID+" AND feat.OBJECTID NOT IN "+
           "   (SELECT feat2.OBJECTID  "+
           "   FROM ((FEATURE feat2 LEFT OUTER JOIN FEATUREREVISION rev2 ON feat2.OBJECTID=rev2.FEATUREID) "+
           "     LEFT OUTER JOIN BCATFEATURELINK bcatfeatlink2 ON rev2.OBJECTID=bcatfeatlink2.REVISIONID) "+
           "     LEFT OUTER JOIN BCATEGORY bcat2 ON bcatfeatlink2.CATEGORYID=bcat2.OBJECTID "+
           "   WHERE bcat2.BASELINEID="+_secondID+
           "    AND bcatfeatlink2.STATEID <> "+IDCONST.COMPASS.getIIDValue()+" "+
           "    AND bcatfeatlink2.STATEID <> "+IDCONST.DEFERRED.getIIDValue()+" "+
           "    AND bcatfeatlink2.STATEID <> "+IDCONST.MYCONCEPTS.getIIDValue()+" "+
           "    AND bcatfeatlink2.STATEID <> "+IDCONST.DEFINPROGRESSREVIEW.getIIDValue()+" "+
           "   ) "+
           "   AND bcatfeatlink.STATEID <> "+IDCONST.COMPASS.getIIDValue()+" "+
           "   AND bcatfeatlink.STATEID <> "+IDCONST.DEFERRED.getIIDValue()+" "+
           "   AND bcatfeatlink.STATEID <> "+IDCONST.MYCONCEPTS.getIIDValue()+" "+
           "   AND bcatfeatlink.STATEID <> "+IDCONST.DEFINPROGRESSREVIEW.getIIDValue()+" ";
           
  }
  
  
//------------------- IBusinessObjectList Methods --------------------------
  public IRCollection setSort(Comparator sortCrit)
    throws OculusException
  {
    FeatureBaselineAddedColl sortedList = new FeatureBaselineAddedColl(sortCrit);
    sortedList._items.addAll(this._items);
    return sortedList;
  }
  
	
//----------------- IPoolable Methods ------------------------------------
	/** Returns a duplicate IProductList object, but without the ObjectContext */
	public Object dolly() throws OculusException
	{
		FeatureBaselineAddedColl verList = null;
			verList = new FeatureBaselineAddedColl();
			verList.setIID(_iid);
			verList._items = this._items;
			verList.reset();
		return verList;
	}

  /** Pseudo-constructor that expects the IIID of the object and the ObjectContext as args */
  public IPoolable construct(IObjectContext context, IDataSet args)
    throws OculusException
  {
    super.construct(context,args);
    
    _firstID = ((IIID)args.get("FirstID")).getLongValue()+"";
    _secondID = ((IIID)args.get("SecondID")).getLongValue()+"";

    return this;
  }


}