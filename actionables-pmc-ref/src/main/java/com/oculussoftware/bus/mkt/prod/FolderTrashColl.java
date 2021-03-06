package com.oculussoftware.bus.mkt.prod;

import com.oculussoftware.api.repi.*;
import com.oculussoftware.api.sysi.*;

import java.util.*;

/**
* Filename:    FolderTrashColl.java
* Date:        4-20-00
* Description: 
*
* Copyright 1-31-2000 Oculus Software.  All Rights Reserved.
*
* @author Egan Royal
* @version 1.2
*/
public class FolderTrashColl extends FolderColl
{
  protected String COL_NAME = "NAME";  
  /*
  * Change Activity
  *
  * Issue number    Programmer      Date        Description
    
    BUG00419       apota                        Compass trash fix effort.
                                                Currently there is no trash for
                                                compass.
      
  */
  //----------------------------- Public Constructor -------------------------
  /** Default constructor just initializes the product list */
  public FolderTrashColl() throws OculusException
  {
    super();
  }

  //----------------------------- Protected Constructor -------------------------
  /** Default constructor just initializes the product list */
  protected FolderTrashColl(Comparator sortCrit) throws OculusException
  {
    super(sortCrit);
  }


  //------------------------ Protected Methods --------------------------------
  protected String getLoadQuery()
    throws ORIOException
  {
    return " SELECT * "+
           " FROM "+TABLE+      
           " WHERE "+COL_DELETESTATE+"="+DeleteState.DELETED.getIntValue()+
           " ORDER BY "+COL_NAME;
  }  
  
//------------------- IBusinessObjectList Methods --------------------------
  public IRCollection setSort(Comparator sortCrit)
    throws OculusException
  {
    FolderTrashColl sortedList = new FolderTrashColl(sortCrit);
    sortedList._items.addAll(this._items);
    return sortedList;
  }
  
  
//----------------- IPoolable Methods ------------------------------------
  /** Returns a duplicate IProductColl object, but without the ObjectContext */
  public Object clone()
  {
    FolderTrashColl prodList = null;
    try
    {
      prodList = new FolderTrashColl();
      prodList.setIID(_iid);
      prodList._items.addAll(this._items);
      prodList.reset();
    }
    catch (OculusException orioExp) {}
    return prodList;
  }
}