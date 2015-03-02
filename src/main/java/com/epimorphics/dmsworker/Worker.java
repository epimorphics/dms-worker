/******************************************************************
 * File:        Worker.java
 * Created by:  Dave Reynolds
 * Created on:  1 Mar 2015
 * 
 * (c) Copyright 2015, Epimorphics Limited
 *
 *****************************************************************/

package com.epimorphics.dmsworker;

import com.epimorphics.appbase.core.App;

public class Worker {

    public static void main(String[] args) {
        new QueueManager().startup( new App("test app") );
    }
}
