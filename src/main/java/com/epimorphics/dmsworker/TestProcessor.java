/******************************************************************
 * File:        TestProcessor.java
 * Created by:  Dave Reynolds
 * Created on:  2 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsworker;

import com.amazonaws.services.sqs.model.Message;

public class TestProcessor implements TaskProcessor {

    @Override
    public boolean canProcess(String action, String target) {
        return true;
    }

    @Override
    public void process(String action, String target, Message message) {
        System.out.println( String.format("Runing action %s(%s) on %s", action, target, message.getBody()) );
    }

}
