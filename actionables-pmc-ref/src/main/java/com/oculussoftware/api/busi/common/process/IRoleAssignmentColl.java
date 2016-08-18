package com.oculussoftware.api.busi.common.process;

import com.oculussoftware.api.sysi.*;
import com.oculussoftware.api.repi.*;

/*
* $Workfile: IRoleAssignmentColl.java $
* Description: Defines the methods needed for an unordered collection
* of IRoleAssignments.
*
* Copyright 1-31-2000 productmarketing.com.  All Rights Reserved.
*
* Change Activity
* Issue number  	Programmer    	Date      	Description
* ------------    ----------      ----        -----------
* 
*/

/** 
* An extension of IRCollection, defines the convenience methods 
* necessary for iterating through an unordered collection of
* IRoleAssignments.
*
* @author Egan Royal
*/
public interface IRoleAssignmentColl extends IRCollection
{
  /**
  * Calls the next() method of the superclass, casts 
  * it to an IProcessRole, and returns it.
  * @return The next IProcessRole in the collection.
  * @exception com.oculussoftware.api.sysi.OculusException  
  */
  public IRoleAssignment nextRoleAssignment() throws OculusException;
  
  /**
  * Delegates to the superclass hasNext() method.
  * @return true - iff there is a next element.  
  */
  public boolean hasMoreRoleAssignments();
}