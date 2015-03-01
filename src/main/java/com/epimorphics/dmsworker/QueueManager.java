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
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;

public class QueueManager {
    protected AmazonSQS sqs;
    
    protected String queueURL = "https://sqs.eu-west-1.amazonaws.com/853478862498/lds-automation";
    
    public QueueManager() {
        sqs = new AmazonSQSClient();
        sqs.setRegion( Region.getRegion(Regions.EU_WEST_1) );
    }
    
    // TODO configurable queue name
    
    // TODO configurable poll interval
    
    // TODO poll using appbase TimerManager
    
    public void poll() {
        ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueURL);
        receiveMessageRequest.setMessageAttributeNames(Collections.singleton("All"));
        List<Message> messages = sqs.receiveMessage(receiveMessageRequest).getMessages();
        for (Message message : messages) {
            System.out.println("  Message");
            System.out.println("    MessageId:     " + message.getMessageId());
            System.out.println("    Body:          " + message.getBody());
            for (Entry<String, MessageAttributeValue> entry : message.getMessageAttributes().entrySet()) {
                System.out.println("  Attribute");
                System.out.println("    Name:  " + entry.getKey());
                System.out.println("    Value: " + entry.getValue().getStringValue());
            }
        }        
    }
    
}
