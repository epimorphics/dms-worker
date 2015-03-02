/******************************************************************
 * File:        Worker.java
 * Created by:  Dave Reynolds
 * Created on:  1 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsworker;

import com.epimorphics.appbase.core.AppConfig;

public class Worker {
    public static final String CONFIGS="/opt/dms-worker/conf/app.conf,/etc/dms-worker/app.conf,src/test/conf";
    
    public static void main(String[] args) {
        AppConfig.startApp("DMSWorker", CONFIGS);
    }
}
