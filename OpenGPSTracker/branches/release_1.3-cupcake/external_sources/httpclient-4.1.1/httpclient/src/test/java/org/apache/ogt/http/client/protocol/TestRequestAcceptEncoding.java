/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.ogt.http.client.protocol;

import junit.framework.Assert;

import org.apache.ogt.http.Header;
import org.apache.ogt.http.HttpRequest;
import org.apache.ogt.http.HttpRequestInterceptor;
import org.apache.ogt.http.client.protocol.RequestAcceptEncoding;
import org.apache.ogt.http.message.BasicHttpRequest;
import org.apache.ogt.http.protocol.BasicHttpContext;
import org.apache.ogt.http.protocol.HttpContext;
import org.junit.Test;

public class TestRequestAcceptEncoding {

    @Test
    public void testAcceptEncoding() throws Exception {
        HttpRequest request = new BasicHttpRequest("GET", "/");
        HttpContext context = new BasicHttpContext();

        HttpRequestInterceptor interceptor = new RequestAcceptEncoding();
        interceptor.process(request, context);
        Header header = request.getFirstHeader("Accept-Encoding");
        Assert.assertNotNull(header);
        Assert.assertEquals("gzip,deflate", header.getValue());
    }

}
