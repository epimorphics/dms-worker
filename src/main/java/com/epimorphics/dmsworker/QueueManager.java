/******************************************************************
 * File:        QueuePoller.java
 * Created by:  Dave Reynolds
 * Created on:  1 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsworker;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.epimorphics.appbase.core.App;
import com.epimorphics.appbase.core.ComponentBase;
import com.epimorphics.appbase.core.Startup;

/**
 * Monitor and SQS queue and run the requested tasks using a configurable TaskProcessor
 * implementation. Can limit the number of concurrent tasks accepted. Each task
 * is run it its own thread for generality even though ActionManager (if using that)
 * already runs actions as separate threads.
 * 
 * @author <a href="mailto:dave@epimorphics.com">Dave Reynolds</a>
 */
public class QueueManager extends ComponentBase implements Startup {
    public static final String ACTION_ATTR = "action";
    public static final String TARGET_ATTR = "target";
    
    static Logger log = LoggerFactory.getLogger( QueueManager.class );
            
    // Configurable
    protected String queueURL;
    protected long taskLimit = 2;
    protected long pollTime = 5;   
    protected long waitTime = 20;
    protected TaskProcessor processor;
    
    // Internal
    protected AmazonSQS sqs;
    protected AtomicInteger taskCount = new AtomicInteger(0);
    protected ExecutorService executor = Executors.newCachedThreadPool();
    protected boolean abort = false;
    
    public QueueManager() {
        sqs = new AmazonSQSClient();
        sqs.setRegion( Region.getRegion(Regions.EU_WEST_1) );
    }
    
    public void abort() {
        abort = true;
    }

    @Override
    public void startup(App app) {
        super.startup(app);
        log.info("Starting queue polling with visibility timeout of " + pollTime + "s and waitTime of" + waitTime + "s");
        executor.execute( new Poller() );
    }
    
    public String getQueueURL() {
        return queueURL;
    }

    /**
     * The URL of the AWS SQS queue, must already exist
     */
    public void setQueueURL(String queueURL) {
        this.queueURL = queueURL;
    }

    public long getTaskLimit() {
        return taskLimit;
    }

    /**
     * The maximum number of concurrent tasks this work should accept.
     * @param taskLimit
     */
    public void setTaskLimit(long taskLimit) {
        this.taskLimit = taskLimit;
    }

    public long getPollTime() {
        return pollTime;
    }

    /**
     * The visibility timeout for the SQS queue. This is the time available to decide
     * whether to accept the request or put it back. If we put a message back (because
     * we are busy) this is the time we will wait before polling the queue again.
     * @param pollTime time in seconds
     */
    public void setPollTime(long pollTime) {
        this.pollTime = pollTime;
    }
    
    /**
     * The wait time for long polling. A receive will wait up till this
     * long to get a request before returning a possibly empty request.
     * Maximum value is 20s
     * @param waitTime
     */
    public void setWaitTime(long waitTime) {
        this.waitTime = waitTime;
    }
    
    /**
     * The TaskProcessor which handles the actual work
     */
    public void setTaskProcessor(TaskProcessor processor) {
        this.processor = processor;
    }
    
    public class Poller implements Runnable {

        @Override
        public void run() {

            while (!abort) {
                ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueURL);
                receiveMessageRequest.setMessageAttributeNames(Collections.singleton("All"));
                receiveMessageRequest.setVisibilityTimeout( (int)pollTime );
                receiveMessageRequest.setWaitTimeSeconds( (int)waitTime );
                List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
                
                boolean putBackSome = false;
                for (Message message : messages) {
                    String action = getStringAttribute(message, ACTION_ATTR); 
                    String target = getStringAttribute(message, TARGET_ATTR);
                    if ( processor.canProcess(action, target) ) {
                        if ( taskCount.incrementAndGet() <= taskLimit ) {
                            executor.execute( new Processor(message, action, target) );
                        } else {
                            // We are full wait till request messages pops visible again
                            taskCount.decrementAndGet();
                        }
                    } else {
                        // Not a request we can handle, make it visible
                        log.info( String.format("Skipping request %s(%s), not configured for this worker", action, target) );
                        sqs.changeMessageVisibility(queueURL, message.getReceiptHandle(), 0);
                        putBackSome = true;
                    }
                }
                if (putBackSome) {
                    try {
                        // We've rejected some requests so give others a chance to pick them up
                        Thread.sleep( (pollTime + 3) * 1000 );
                    } catch (InterruptedException e) {
                        // Fall back to the outer loop
                    }
                }
            }
        }
    
        private String getStringAttribute(Message message, String attr) {
            MessageAttributeValue value = message.getMessageAttributes().get(attr);
            if (value != null) {
                return value.getStringValue();
            }
            return null;
        }
    }
    
    public class Processor implements Runnable {
        protected Message message;
        protected String action;
        protected String target;
        
        public Processor(Message message, String action, String target) {
            this.message = message;
            this.action = action;
            this.target = target;
        }
        
        @Override
        public void run() {
            // Logging suppressed here because the ActionManager is already logging
//            log.info( String.format("Starting task %s(%s) - %s", action, target, message.getMessageId()) );
            try {
                // At this stage we are committed to handling the task
                // Alternative would be to make the message invisible for some max period and delete only after completion
                // However, we don't know to decide when a task should be allow to retry if we fail
                sqs.deleteMessage(queueURL, message.getReceiptHandle());
                processor.process(action, target, message);
//                log.info(  String.format("Completed %s", message.getMessageId()) );
            } catch (Exception e) {
                log.error( String.format("Failed to run task: %s", message.getMessageId()), e);
            } finally {
                taskCount.decrementAndGet();
            }
        }
        
    }
    
}
