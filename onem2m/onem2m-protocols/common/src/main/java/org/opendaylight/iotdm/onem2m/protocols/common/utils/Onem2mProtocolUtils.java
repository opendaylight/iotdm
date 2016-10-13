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
    public static String processRequestPrimitiveFromJson(String jsonMessage,
                                                         Onem2mRequestPrimitiveClientBuilder clientBuilder) {
        String operation = null;

        Optional<JSONObject> jsonContent = JsonUtils.stringToJsonObject(jsonMessage);

        if(jsonContent.isPresent()) {
            Iterator<?> keys = jsonContent.get().keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Object o = jsonContent.get().opt(key);
                if (o != null) {
                    clientBuilder.setPrimitiveNameValue(key, o.toString());
                    if (key.contentEquals(RequestPrimitive.OPERATION)) {
                        operation = o.toString();
                    }
                }
            }

            clientBuilder.setContentFormat(Onem2m.ContentFormat.JSON);
            if (Objects.isNull(operation))
                LOG.warn("Operation not specified");
            return operation;
        }
        else {
            LOG.warn("Given string is not acceptable as json");
            return null;
        }
    }

}
