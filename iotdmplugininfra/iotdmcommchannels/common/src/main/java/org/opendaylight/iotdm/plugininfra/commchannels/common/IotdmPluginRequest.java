/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.plugininfra.commchannels.common;

/**
 * Interface describes generic implementation of classes wrapping
 * original protocol request allowing unified access to important parts
 * of the request.
 * @param <TOriginalRequest> Type of the original protocol request.
 */
public interface IotdmPluginRequest<TOriginalRequest> {
    /**
     * Returns request method specific to particular protocol.
     * @return Request method
     */
    String getMethod();

    /**
     * Returns received target endpoint URI
     * @return The target URI in original protocol specific format
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
     * Returns the original request.
     * @return Received request as object of the protocol specific class.
     */
    TOriginalRequest getOriginalRequest();
}
