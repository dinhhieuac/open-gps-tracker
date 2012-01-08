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

package org.apache.ogt.http.mockup;

import java.io.IOException;

import org.apache.ogt.http.HttpConnection;
import org.apache.ogt.http.HttpConnectionMetrics;

/**
 * {@link HttpConnection} mockup implementation.
 *
 */
public class HttpConnectionMockup implements HttpConnection {

    private boolean open = true;

    public HttpConnectionMockup() {
        super();
    }

    public void close() throws IOException {
        this.open = false;
    }

    public void shutdown() throws IOException {
        this.open = false;
    }

    public void setSocketTimeout(int timeout) {
    }

    public int getSocketTimeout() {
        return -1;
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isStale() {
        return false;
    }

    public HttpConnectionMetrics getMetrics() {
        return null;
    }

}
