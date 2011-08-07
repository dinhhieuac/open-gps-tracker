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

package org.apache.ogt.http.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import junit.framework.TestCase;

import org.apache.ogt.http.Header;
import org.apache.ogt.http.HeaderElement;
import org.apache.ogt.http.message.BasicHeader;

/**
 * Unit tests for {@link Header}.
 *
 */
public class TestHeader extends TestCase {

    public TestHeader(String testName) {
        super(testName);
    }

    public void testBasicConstructor() {
        Header header = new BasicHeader("name", "value");
        assertEquals("name", header.getName());
        assertEquals("value", header.getValue());
    }

    public void testBasicConstructorNullValue() {
        Header header = new BasicHeader("name", null);
        assertEquals("name", header.getName());
        assertEquals(null, header.getValue());
    }

    public void testInvalidName() {
        try {
            new BasicHeader(null, null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException ex) {
            //expected
        }
    }

    public void testToString() {
        Header header1 = new BasicHeader("name1", "value1");
        assertEquals("name1: value1", header1.toString());
        Header header2 = new BasicHeader("name2", null);
        assertEquals("name2: ", header2.toString());
    }

    public void testHeaderElements() {
        Header header = new BasicHeader("name", "element1 = value1, element2; param1 = value1, element3");
        HeaderElement[] elements = header.getElements();
        assertNotNull(elements);
        assertEquals(3, elements.length);
        assertEquals("element1", elements[0].getName());
        assertEquals("value1", elements[0].getValue());
        assertEquals("element2", elements[1].getName());
        assertEquals(null, elements[1].getValue());
        assertEquals("element3", elements[2].getName());
        assertEquals(null, elements[2].getValue());
        assertEquals(1, elements[1].getParameters().length);

        header = new BasicHeader("name", null);
        elements = header.getElements();
        assertNotNull(elements);
        assertEquals(0, elements.length);
    }

    public void testCloning() throws Exception {
        BasicHeader orig = new BasicHeader("name1", "value1");
        BasicHeader clone = (BasicHeader) orig.clone();
        assertEquals(orig.getName(), clone.getName());
        assertEquals(orig.getValue(), clone.getValue());
    }

    public void testSerialization() throws Exception {
        BasicHeader orig = new BasicHeader("name1", "value1");
        ByteArrayOutputStream outbuffer = new ByteArrayOutputStream();
        ObjectOutputStream outstream = new ObjectOutputStream(outbuffer);
        outstream.writeObject(orig);
        outstream.close();
        byte[] raw = outbuffer.toByteArray();
        ByteArrayInputStream inbuffer = new ByteArrayInputStream(raw);
        ObjectInputStream instream = new ObjectInputStream(inbuffer);
        BasicHeader clone = (BasicHeader) instream.readObject();
        assertEquals(orig.getName(), clone.getName());
        assertEquals(orig.getValue(), clone.getValue());
    }

}

