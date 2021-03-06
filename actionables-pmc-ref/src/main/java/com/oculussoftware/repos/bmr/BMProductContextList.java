package com.oculussoftware.repos.bmr;

import com.oculussoftware.api.sysi.*;
import com.oculussoftware.api.repi.*;
import com.oculussoftware.rdb.*;
import com.oculussoftware.repos.util.*;
import com.oculussoftware.system.*;
import com.oculussoftware.util.*;

import java.util.*;
import java.sql.*;

public class BMProductContextList extends BMModelElementList 
{
  
 public BMProductContextList() throws OculusException
  {
    super();
  }
  
  //-------------------------- IRModelElement -------------------------
  public String getClassName() { return "ProductContext";}
  public String getLoadQuery()
    throws OculusException
  {
    return "SELECT * FROM PRODCONTEXTLIST WHERE ISACTIVE=1 ";
  }// 
    
  public Object dolly() throws OculusException 
  { 
    BMProductContextList gColl = new BMProductContextList();
    gColl.setIID(_iid);
    gColl._coll.addAll(this._coll);
    gColl.reset();
    return gColl;      
  }

}