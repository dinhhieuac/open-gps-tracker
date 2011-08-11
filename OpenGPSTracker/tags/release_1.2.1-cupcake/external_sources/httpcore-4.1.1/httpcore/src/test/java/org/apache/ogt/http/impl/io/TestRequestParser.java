/*
 * $HeadURL$
 * $Revision$
 * $Date$
 *
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

package org.apache.ogt.http.impl.io;

import java.io.InterruptedIOException;

import junit.framework.TestCase;

import org.apache.ogt.http.ConnectionClosedException;
import org.apache.ogt.http.Header;
import org.apache.ogt.http.HttpRequest;
import org.apache.ogt.http.HttpVersion;
import org.apache.ogt.http.RequestLine;
import org.apache.ogt.http.impl.DefaultHttpRequestFactory;
import org.apache.ogt.http.impl.io.HttpRequestParser;
import org.apache.ogt.http.io.SessionInputBuffer;
import org.apache.ogt.http.message.BasicLineParser;
import org.apache.ogt.http.mockup.SessionInputBufferMockup;
import org.apache.ogt.http.mockup.TimeoutByteArrayInputStream;
import org.apache.ogt.http.params.BasicHttpParams;

/**
 * Unit tests for {@link HttpRequestParser}.
 */
public class TestRequestParser extends TestCase {

    public TestRequestParser(String testName) {
        super(testName);
    }

    public void testInvalidConstructorInput() throws Exception {
        try {
            new HttpRequestParser(
                    null,
                    BasicLineParser.DEFAULT,
                    new DefaultHttpRequestFactory(),
                    new BasicHttpParams());
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        try {
            SessionInputBuffer inbuffer = new SessionInputBufferMockup(new byte[] {});
            new HttpRequestParser(
                    inbuffer,
                    BasicLineParser.DEFAULT,
                    null,
                    new BasicHttpParams());
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        try {
            SessionInputBuffer inbuffer = new SessionInputBufferMockup(new byte[] {});
            new HttpRequestParser(
                    inbuffer,
                    BasicLineParser.DEFAULT,
                    new DefaultHttpRequestFactory(),
                    null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    public void testBasicMessageParsing() throws Exception {
        String s =
            "GET / HTTP/1.1\r\n" +
            "Host: localhost\r\n" +
            "User-Agent: whatever\r\n" +
            "Cookie: c1=stuff\r\n" +
            "\r\n";
        SessionInputBuffer inbuffer = new SessionInputBufferMockup(s, "US-ASCII");

        HttpRequestParser parser = new HttpRequestParser(
                inbuffer,
                BasicLineParser.DEFAULT,
                new DefaultHttpRequestFactory(),
                new BasicHttpParams());

        HttpRequest httprequest = (HttpRequest) parser.parse();

        RequestLine reqline = httprequest.getRequestLine();
        assertNotNull(reqline);
        assertEquals("GET", reqline.getMethod());
        assertEquals("/", reqline.getUri());
        assertEquals(HttpVersion.HTTP_1_1, reqline.getProtocolVersion());
        Header[] headers = httprequest.getAllHeaders();
        assertEquals(3, headers.length);
    }

    public void testConnectionClosedException() throws Exception {
        SessionInputBuffer inbuffer = new SessionInputBufferMockup(new byte[] {});

        HttpRequestParser parser = new HttpRequestParser(
                inbuffer,
                BasicLineParser.DEFAULT,
                new DefaultHttpRequestFactory(),
                new BasicHttpParams());

        try {
            parser.parse();
            fail("ConnectionClosedException should have been thrown");
        } catch (ConnectionClosedException expected) {
        }
    }

    public void testMessageParsingTimeout() throws Exception {
        String s =
            "GET \000/ HTTP/1.1\r\000\n" +
            "Host: loca\000lhost\r\n" +
            "User-Agent: whatever\r\n" +
            "Coo\000kie: c1=stuff\r\n" +
            "\000\r\n";
        SessionInputBuffer inbuffer = new SessionInputBufferMockup(
                new TimeoutByteArrayInputStream(s.getBytes("US-ASCII")), 16);

        HttpRequestParser parser = new HttpRequestParser(
                inbuffer,
                BasicLineParser.DEFAULT,
                new DefaultHttpRequestFactory(),
                new BasicHttpParams());

        int timeoutCount = 0;

        HttpRequest httprequest = null;
        for (int i = 0; i < 10; i++) {
            try {
                httprequest = (HttpRequest) parser.parse();
                break;
            } catch (InterruptedIOException ex) {
                timeoutCount++;
            }

        }
        assertNotNull(httprequest);
        assertEquals(5, timeoutCount);

        RequestLine reqline = httprequest.getRequestLine();
        assertNotNull(reqline);
        assertEquals("GET", reqline.getMethod());
        assertEquals("/", reqline.getUri());
        assertEquals(HttpVersion.HTTP_1_1, reqline.getProtocolVersion());
        Header[] headers = httprequest.getAllHeaders();
        assertEquals(3, headers.length);
    }

}

