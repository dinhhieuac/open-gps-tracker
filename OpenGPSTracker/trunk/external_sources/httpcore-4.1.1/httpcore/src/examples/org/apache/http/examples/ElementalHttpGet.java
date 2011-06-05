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

package org.apache.ogt.http.examples;

import java.net.Socket;

import org.apache.ogt.http.ConnectionReuseStrategy;
import org.apache.ogt.http.HttpHost;
import org.apache.ogt.http.HttpRequestInterceptor;
import org.apache.ogt.http.HttpResponse;
import org.apache.ogt.http.HttpVersion;
import org.apache.ogt.http.impl.DefaultConnectionReuseStrategy;
import org.apache.ogt.http.impl.DefaultHttpClientConnection;
import org.apache.ogt.http.message.BasicHttpRequest;
import org.apache.ogt.http.params.HttpParams;
import org.apache.ogt.http.params.HttpProtocolParams;
import org.apache.ogt.http.params.SyncBasicHttpParams;
import org.apache.ogt.http.protocol.HttpContext;
import org.apache.ogt.http.protocol.BasicHttpContext;
import org.apache.ogt.http.protocol.ExecutionContext;
import org.apache.ogt.http.protocol.HttpProcessor;
import org.apache.ogt.http.protocol.HttpRequestExecutor;
import org.apache.ogt.http.protocol.ImmutableHttpProcessor;
import org.apache.ogt.http.protocol.RequestConnControl;
import org.apache.ogt.http.protocol.RequestContent;
import org.apache.ogt.http.protocol.RequestExpectContinue;
import org.apache.ogt.http.protocol.RequestTargetHost;
import org.apache.ogt.http.protocol.RequestUserAgent;
import org.apache.ogt.http.util.EntityUtils;

/**
 * Elemental example for executing a GET request.
 * <p>
 * Please note the purpose of this application is demonstrate the usage of HttpCore APIs.
 * It is NOT intended to demonstrate the most efficient way of building an HTTP client. 
 *
 *
 *
 */
public class ElementalHttpGet {

    public static void main(String[] args) throws Exception {
        
        HttpParams params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, "UTF-8");
        HttpProtocolParams.setUserAgent(params, "HttpComponents/1.1");
        HttpProtocolParams.setUseExpectContinue(params, true);

        HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpRequestInterceptor[] {
                // Required protocol interceptors
                new RequestContent(),
                new RequestTargetHost(),
                // Recommended protocol interceptors
                new RequestConnControl(),
                new RequestUserAgent(),
                new RequestExpectContinue()});
        
        HttpRequestExecutor httpexecutor = new HttpRequestExecutor();
        
        HttpContext context = new BasicHttpContext(null);
        HttpHost host = new HttpHost("localhost", 8080);

        DefaultHttpClientConnection conn = new DefaultHttpClientConnection();
        ConnectionReuseStrategy connStrategy = new DefaultConnectionReuseStrategy();

        context.setAttribute(ExecutionContext.HTTP_CONNECTION, conn);
        context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, host);

        try {
            
            String[] targets = {
                    "/",
                    "/servlets-examples/servlet/RequestInfoExample", 
                    "/somewhere%20in%20pampa"};
            
            for (int i = 0; i < targets.length; i++) {
                if (!conn.isOpen()) {
                    Socket socket = new Socket(host.getHostName(), host.getPort());
                    conn.bind(socket, params);
                }
                BasicHttpRequest request = new BasicHttpRequest("GET", targets[i]);
                System.out.println(">> Request URI: " + request.getRequestLine().getUri());
                
                request.setParams(params);
                httpexecutor.preProcess(request, httpproc, context);
                HttpResponse response = httpexecutor.execute(request, conn, context);
                response.setParams(params);
                httpexecutor.postProcess(response, httpproc, context);
                
                System.out.println("<< Response: " + response.getStatusLine());
                System.out.println(EntityUtils.toString(response.getEntity()));
                System.out.println("==============");
                if (!connStrategy.keepAlive(response, context)) {
                    conn.close();
                } else {
                    System.out.println("Connection kept alive...");
                }
            }
        } finally {
            conn.close();
        }
    }
    
}
