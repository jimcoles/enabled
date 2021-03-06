package com.oculussoftware.bus.mkt.comm;

import com.oculussoftware.system.*;
import com.oculussoftware.api.repi.*;
import com.oculussoftware.api.sysi.*;
import com.oculussoftware.repos.util.SequentialIID;
import com.oculussoftware.rdb.*;
import com.oculussoftware.bus.*;
import com.oculussoftware.api.busi.mkt.comm.*;
import com.oculussoftware.api.busi.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
* Filename:    AlertConfigColl.java
* Date:        2/14/00
* Description: 
*
* Copyright 1-31-2000 Oculus Software.  All Rights Reserved.
*
* @author Egan Royal
* @version 1.2
*/
public class AlertConfigColl extends BusinessObjectColl implements IAlertConfigColl, IPersistable
{
  /*
  * Change Activity
  *
  * Issue number    Programmer      Date        Description
  */

  protected String TABLE = "ALERTCONFIG";
  protected String COL_PAROBJECTID = "PAROBJECTID";

	//----------------------------- Public Constructor -------------------------
	/** Default constructor just initializes the product list */
	public AlertConfigColl() throws OculusException
	{
    super();
	}//

  protected AlertConfigColl(Comparator sortCrit) throws OculusException
  {
    super (sortCrit);
  }//
  
  //------------------------ Protected Methods --------------------------------
  protected String getLoadQuery() throws ORIOException
  {
    return " SELECT * FROM "+TABLE+
           " WHERE "+COL_PAROBJECTID+"="+getIID().getLongValue()+" ";
  }//

  protected String getClassName () { return "AlertConfig"; }
	//----------------- IHyperLinkList Methods ------------------------------------
	/**
	*
 	*/
  public IAlertConfig nextAlertConfig() throws OculusException
	{
		return (IAlertConfig)next();
	}//
	
	/**
	*	
	*/
  public boolean hasMoreAlertConfigs()
	{
		return hasNext();
	}//
  
//------------------- IBusinessObjectList Methods --------------------------
  public IRCollection setSort(Comparator sortCrit) throws OculusException
  {
    AlertConfigColl sortedList = new AlertConfigColl(sortCrit);
    sortedList._items.addAll(this._items);
    return sortedList;
  }//
  
	
//----------------- IPoolable Methods ------------------------------------
	public Object dolly() throws OculusException
	{
	  AlertConfigColl alertList = null;
			alertList = new AlertConfigColl();
			alertList.setIID(_iid);
			alertList._items = this._items;
			alertList.reset();
		return alertList;
	}//
}//end class