package com.oculussoftware.api.busi.common.reports; 

import com.oculussoftware.util.IntEnum;
import com.oculussoftware.api.sysi.OculusException;

/*
* $Workfile: FooterKind.java $
* Description: Integer Enumeration of FooterKinds for Custom MRD
* Reports.
*                                                      
* Copyright 1-31-2000 productmarketing.com.  All Rights Reserved.
*
* Change Activity
* Issue number  	Programmer    	Date      	Description
* ------------    ----------      ----        -----------
* 
*/

/** 
* Integer Enumeration of FooterKinds for Custom MRD Reports.
*
* @author Egan Royal
*/
public final class FooterKind extends IntEnum
{
	/**
  * int value 0. 
  */
  public final static FooterKind DATE  = new FooterKind(0);
  
  /**
  * int value 1. 
  */ 
  public final static FooterKind PAGENUMBER = new FooterKind(1);
  
  /**
  * int value 2. 
  */
  public final static FooterKind TEXT = new FooterKind(2);
  
  /**
  * int value 3. 
  */ 
  public final static FooterKind NONE = new FooterKind(3);

  /**
  * Takes an int and returns a FooterKind iff the int is valid.
  *
  * @param val the int value
  * @return The FooterKind that corresponds to the int value.
  * @exception com.oculussoftware.api.sysi.OculusException 
  * If the int value does not correspond to a valid FooterKind
  */
  public static FooterKind getInstance(int val) throws OculusException
  {
    if(val == 0)
      return DATE;
    else if(val == 1)
      return PAGENUMBER;
    else if(val == 2)
      return TEXT;
    else if(val == 3)
      return NONE;  
    else
      throw new OculusException("Invalid FooterKind.");
  }//end getInstance
  
  /** Private constructor */
  private FooterKind(int s) { super(s); }
}