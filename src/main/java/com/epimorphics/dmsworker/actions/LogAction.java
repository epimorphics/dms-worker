/******************************************************************
 * File:        LogAction.java
 * Created by:  Dave Reynolds
 * Created on:  2 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsworker.actions;

import static com.epimorphics.json.JsonUtil.getStringValue;

import org.apache.jena.atlas.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epimorphics.appbase.tasks.impl.BaseAction;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMonitorReporter;

public class LogAction extends BaseAction {
    static Logger log = LoggerFactory.getLogger( LogAction.class );
    
    @Override
    protected JsonObject doRun(JsonObject parameters,
            ProgressMonitorReporter monitor) {
        log.info( getStringValue(parameters, "message", "No message body") );
        monitor.succeeded();
        return JsonUtil.emptyObject();
    }

}
