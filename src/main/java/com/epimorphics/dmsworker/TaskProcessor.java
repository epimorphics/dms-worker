/******************************************************************
 * File:        TaskProcessor.java
 * Created by:  Dave Reynolds
 * Created on:  2 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsworker;

import com.amazonaws.services.sqs.model.Message;

/**
 * TaskProcessors encapsulate the knowledge of what tasks can be handled
 * and how to do so.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public interface TaskProcessor {

    /**
     * Test if the this could be executed.
     */
    public boolean canProcess(String action, String target);

    /**
     * Execute this action. Normally called on a separate thread.
     * @throws execption if there is a failure during execution
     */
    public void process(String action, String target, Message message);
}
