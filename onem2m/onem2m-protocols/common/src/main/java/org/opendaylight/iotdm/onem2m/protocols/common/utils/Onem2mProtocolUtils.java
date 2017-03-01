/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.protocols.common.utils;

import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.client.Onem2mRequestPrimitiveClientBuilder;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

/**
 * Utility class implementing common static methods for all protocols.
 */
public class Onem2mProtocolUtils {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2m.class);

    /**
     * @param jsonMessage - json message with onem2m primitive content
     * @param clientBuilder - request primitive builder
     * @return request operation
     */
    public static boolean processRequestPrimitiveFromJson(String jsonMessage,
                                                          Onem2mRequestPrimitiveClientBuilder clientBuilder) {
        Optional<JSONObject> jsonContent = JsonUtils.stringToJsonObject(jsonMessage);

        if(! jsonContent.isPresent()) {
            LOG.error("Given string is not acceptable as json");
            return false;
        }

        Iterator<?> keys = jsonContent.get().keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            Object o = jsonContent.get().opt(key);
            if (o != null) {
                clientBuilder.setPrimitiveNameValue(key, o.toString());
            }
        }

        clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
        return true;
    }

    public static String verifyRequestPrimitive(Onem2mRequestPrimitiveClientBuilder clientBuilder) {
        String operation = clientBuilder.getPrimitiveValue(RequestPrimitive.OPERATION);
        if (null == operation) {
            return "Operation not specified.";
        }

        String originator = clientBuilder.getPrimitiveValue(RequestPrimitive.FROM);
        if (null == originator || originator.isEmpty()) {
            return "Originator ID not specified.";
        }

        String requestId = clientBuilder.getPrimitiveValue(RequestPrimitive.REQUEST_IDENTIFIER);
        if (null == requestId || requestId.isEmpty()) {
            return "Request ID not specified.";
        }

        String resourceType = clientBuilder.getPrimitiveValue(RequestPrimitive.RESOURCE_TYPE);
        switch(operation) {
            case Onem2m.Operation.CREATE:
                if (null == resourceType || resourceType.isEmpty()) {
                    return "Resource type is missing.";
                }
                break;

            case Onem2m.Operation.RETRIEVE:
            case Onem2m.Operation.UPDATE:
            case Onem2m.Operation.DELETE:
            case Onem2m.Operation.NOTIFY:
                if (null != resourceType) {
                    return "Resource type parameter not supported for operation: " + operation;
                }
                break;
            default:
                return "Invalid operation: " + operation;
        }

        return null;
    }
}
