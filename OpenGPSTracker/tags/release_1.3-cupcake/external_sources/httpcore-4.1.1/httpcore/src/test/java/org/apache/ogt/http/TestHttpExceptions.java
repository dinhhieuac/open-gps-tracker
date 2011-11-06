/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.ogt.http;

import org.apache.ogt.http.ConnectionClosedException;
import org.apache.ogt.http.HttpException;
import org.apache.ogt.http.MethodNotSupportedException;
import org.apache.ogt.http.NoHttpResponseException;
import org.apache.ogt.http.ProtocolException;
import org.apache.ogt.http.UnsupportedHttpVersionException;

import junit.framework.TestCase;

/**
 * Simple tests for various HTTP exception classes.
 *
 *
 */
public class TestHttpExceptions extends TestCase {

    // ------------------------------------------------------------ Constructor
    public TestHttpExceptions(String testName) {
        super(testName);
    }

    // ------------------------------------------------------- TestCase Methods

    public void testConstructor() {
        Throwable cause = new Exception();
        new HttpException();
        new HttpException("Oppsie");
        new HttpException("Oppsie", cause);
        new ProtocolException();
        new ProtocolException("Oppsie");
        new ProtocolException("Oppsie", cause);
        new NoHttpResponseException("Oppsie");
        new ConnectionClosedException("Oppsie");
        new MethodNotSupportedException("Oppsie");
        new MethodNotSupportedException("Oppsie", cause);
        new UnsupportedHttpVersionException();
        new UnsupportedHttpVersionException("Oppsie");
    }

}
