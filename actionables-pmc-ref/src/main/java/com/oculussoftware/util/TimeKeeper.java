package com.oculussoftware.util;

import java.text.*;
import java.util.*;
import java.io.*;

import com.oculussoftware.*;
import com.oculussoftware.ui.*;


/**
* Filename:    TimeKeeper.java
* Date:        11-3-1999
* Description: Designed to track events () and then allow client object to
* report on time deltas for the various events.
*
* Copyright 1-31-2000 Oculus Software.  All Rights Reserved.
*
* @author Jim Coles
* @version 1.1
*/

/*
* Change Activity
*
* Issue number  	Programmer    	Date      	Description
*
*/
public class TimeKeeper
{
  //---------------------------------
  // Private Class variables
  //---------------------------------
  private static final SimpleDateFormat _deltaFmt
     = new SimpleDateFormat ("mm:ss.SSS");

  private static final int EnvironmentgetDebugLevel = DebugSrvc.DBG_HIGH;
     
  //---------------------------------
  // Constructors
  //---------------------------------
  public TimeKeeper ()
  {
  }

  //---------------------------------
  // Private Instance variables
  //---------------------------------
  private List _eventLog = new Vector();

  
  //---------------------------------
  // Public Instance methods
  //---------------------------------
  public void resetLog()
  {
    _eventLog.clear();
  }
  
  public long logEvent(String strEvent)
  {
    long eid = System.currentTimeMillis(); //UIDGen.genFastUID();
//    Long lid = new Long(eid);
    _eventLog.add(new LogEvent(strEvent, eid));
//    _keyOrder.addElement(lid);
    return eid;
  }
  
  public String getTimeDelta(long t1, long t2)
  {
    java.util.Date delta = new java.util.Date(t2 - t1);
    return _deltaFmt.format(delta);
  }
  
  public void printTable(PrintWriter out)
  {
    long tx = 0, t0 = 0, txm1 =0;
    int idx = -1;
    Object key = null;
    out.println("<TABLE>");
    out.println("<TR BGCOLOR='SILVER'>");
    if (EnvironmentgetDebugLevel >= DebugSrvc.DBG_HIGH) {
      out.println("<TH>Event ID</TH>");
    }
    out.println("<TH>Event</TH> <TH>Delta Time</TH> <TH>Cummulative Time</TH>  </TR>");
    Iterator ievents = _eventLog.iterator();
    if (ievents.hasNext()) {
      do {
        idx++;
        LogEvent le = (LogEvent) ievents.next();
        tx = le.getTime();
        out.println("<TR>");
        if( EnvironmentgetDebugLevel >= DebugSrvc.DBG_HIGH ) {
          out.println("<TD> "+ le.getTime() + "</TD>");
        }
        out.println("<TD> "+ _formatLabel(le.getName()) + "</TD>");
        if (idx == 0) {
          t0 = tx;
          out.println("<TD> - </TD>");
        }
        else {
          out.println("<TD> "+getTimeDelta(txm1, tx)+" </TD>");
        }
        out.println("<TD> "+getTimeDelta(t0, tx)+" </TD>");
        out.println("</TR>");
        txm1 = tx;
      }
      while (ievents.hasNext());
    }
    out.println("</TABLE>");
  }

  public void printTextTable(PrintStream out)
  {
    long tx = 0, t0 = 0, txm1 =0;
    int idx = -1;
    Object key = null;
    out.println("****************************************");
    out.println("Event\t\tDelta Time\tCummulative Time");
    Iterator ievents = _eventLog.iterator();
    if (ievents.hasNext()) {
      do {
        idx++;
        LogEvent le = (LogEvent) ievents.next();
        tx = le.getTime();
        out.println("");
        out.print(le.getName() + "\t");
        if (idx == 0) {
          t0 = tx;
          out.print(" - \t");
        }
        else {
          out.print(""+getTimeDelta(txm1, tx)+"\t");
        }
        out.print(""+getTimeDelta(t0, tx)+"");
        out.println("");
        txm1 = tx;
      }
      while (ievents.hasNext());
    }
    out.println("****************************************");
  }
  
  //---------------------------------
  // Private Instance methods
  //---------------------------------
  
  private String _formatLabel(String label)
  {
    return "<FONT SIZE=2 FACE='ARIAL'> <B>"+ label + "</B></FONT>";    
  }
  
  //---------------------------------
  // Private classes
  //---------------------------------
  /** */
  private static class LogEvent
  {
    private String _name = null;
    private long   _time = 0;
    
    public LogEvent(String name, long time)
    {
      _name = name;
      _time = time;
    }
    
    public String getName() { return _name;}
    public long getTime() { return _time;}
  }
}