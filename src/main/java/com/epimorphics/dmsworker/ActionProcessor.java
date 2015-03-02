/******************************************************************
 * File:        ActionProcessor.java
 * Created by:  Dave Reynolds
 * Created on:  2 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsworker;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.jena.atlas.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.sqs.model.Message;
import com.epimorphics.appbase.tasks.Action;
import com.epimorphics.appbase.tasks.ActionExecution;
import com.epimorphics.appbase.tasks.ActionManager;
import com.epimorphics.json.JsonUtil;
import com.epimorphics.tasks.ProgressMessage;
import com.epimorphics.tasks.ProgressMonitor;
import com.epimorphics.tasks.ProgressMonitorReporter;
import com.epimorphics.tasks.TaskState;
import com.epimorphics.util.EpiException;
import com.epimorphics.util.FileUtil;

/**
 * Task processor that allows actions to be configured using
 * the appbase ActionManager machinery. Passes the target
 * as a "target" parameter. Passes the message body as either the "message" parameter
 * or via a file named by the "messageFile" parameter. To use
 * the pass-by-file mode set the action parameter "passByReference" to true. 
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class ActionProcessor implements TaskProcessor {
    public static final String TARGET_PARAM = "target";
    public static final String MESSAGE_PARAM = "message";
    public static final String BYREF_PARAM = "passByReference";
    public static final String MESSAGE_FILE_PARAM = "messageFile";
    public static final String MESSAGEID_PARAM = "messageID";
    
    static Logger log = LoggerFactory.getLogger( TaskProcessor.class );
    
    protected ActionManager actionManager;
    
    public void setActionManager(ActionManager actionManager) {
        this.actionManager = actionManager;
    }

    @Override
    public boolean canProcess(String action, String target) {
        return actionManager.has(action);
    }

    @Override
    public void process(String actionName, String target, Message message) {
        Action action = actionManager.get(actionName);
        JsonObject parameters = new JsonObject();
        parameters.put(MESSAGEID_PARAM, message.getMessageId());
        if (target != null) {
            parameters.put(TARGET_PARAM, target);
        }
        File temp = null;
        if ( JsonUtil.getBooleanValue(action.getConfig(), BYREF_PARAM, false) ) {
            try {
                temp = File.createTempFile("worker-message", ".txt");
                Reader in = new StringReader(message.getBody());
                Writer out = new FileWriterWithEncoding(temp, StandardCharsets.UTF_8);
                FileUtil.copyResource(in, out);
                out.close();
                parameters.put(MESSAGE_FILE_PARAM, temp.getAbsolutePath());
            } catch (IOException e) {
                throw new EpiException("Failed to create temporary file for message body", e);
            }
        } else {
            String body = message.getBody();
            if (body != null) {
                parameters.put(MESSAGE_PARAM, body);
            }
        }
        ActionExecution ae = actionManager.runAction(action, parameters);
        ProgressMonitorReporter monitor = ae.getMonitor();
        int messagesSeen = 0;
        while (monitor.getState() != TaskState.Terminated) {
            messagesSeen += showProgress(monitor, messagesSeen);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
        showProgress(monitor, messagesSeen);
        ae.waitForCompletion();
        if (temp != null) {
            temp.delete();
        }
    }

    private int showProgress(ProgressMonitor monitor, int messagesSeen) {
        if (monitor.moreMessagesSince(messagesSeen)) {
            List<ProgressMessage> messages = monitor.getMessagesSince(messagesSeen);
            for (ProgressMessage m : messages) {
                log.info( m.getMessage() );
            }
            return messages.size();
        }
        return 0;
    }
}
