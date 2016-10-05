/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.plugins;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

/**
 * Interface describes generic implementation of classes wrapping
 * original protocol request allowing unified access to important parts
 * of the request.
 * @param <TOriginalRequest> Type of the original protocol request.
 */
public interface IotdmPluginRequest<TOriginalRequest> {

    /**
     * Returns string representation of the Onem2m operation type
     * of the request.
     * @return Onem2m operation type.
     */
    String getOnem2mOperation();

    /**
     * Returns received target URI translated to Onem2m format.
     * @return The URI in Onem2m format.
     */
    String getOnem2mUri();

    /**
     * Returns request method specific to particular protocol i.e.:
     * method is not mapped to corresponding Onem2m operation.
     * @return Request method.
     */
    String getMethod();

    /**
     * Returns received target URI which is not translated to Onem2m format.
     * @return The target URI in original protocol specific format.
     */
    String getUrl();

    /**
     * Returns complete received payload as string.
     * @return Received payload.
     */
    String getPayLoad();

    /**
     * Returns string identifying type of the content (payload).
     * @return Content type.
     */
    String getContentType();

    /**
     * Returns all received headers and their values in a HashMap with
     * header names as keys and values stored in arrays of strings.
     * These arrays includes more than one value in case of multiple occurrence
     * of header with the same name.
     * @return HashMap with header names and values.
     */
    HashMap<String, String[]> getHeadersAll();

    /**
     * Returns all values of the header with name passed as parameter key.
     * @param key Name of the header.
     * @return Array of values as strings.
     */
    String[] getHeaders(String key);

    /**
     * Returns value of the first headers with the name specified by the parameter key.
     * @param key Name of the header.
     * @return The first value found.
     */
    String getHeader(String key);

    /**
     * Returns the original request.
     * @return Received request as object of the protocol specific class.
     */
    TOriginalRequest getOriginalRequest();
}
