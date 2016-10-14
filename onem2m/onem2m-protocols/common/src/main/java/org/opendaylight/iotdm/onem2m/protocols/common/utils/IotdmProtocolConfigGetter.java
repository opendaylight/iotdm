/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Utility class implementing static methods for processing configuration.
 */
public class IotdmProtocolConfigGetter {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmProtocolConfigGetter.class);

    /**
     * Generic method for calling methods identified by
     * method name as string parameter.
     * @param src The object of type implementing desired method.
     * @param getterName Name of the method which will be called.
     *                   Method does not have defined any parameters and returns
     *                   value of type "type" (passed as last argument).
     * @param type The return type of called method.
     * @param <T> The same return type as return type of called method.
     * @return Value returned by called method. Null is returned in case of failure.
     */
    public static <T extends Object> T getAttribute(Object src, String getterName, Class<T> type) {
        try {
            Method m = src.getClass().getMethod(getterName);
            return (T) m.invoke(src);
        } catch (Exception e) {
            LOG.trace("Failed to get attribute: {}, msg: {}", getterName, e);
            return null;
        }
    }
}
