/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mRequestPrimitiveClient extends RequestPrimitive {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mRequestPrimitiveClient.class);

    /**
     * The onem2m-protocols use this to create a new RequestPrimitive class
     */
    public Onem2mRequestPrimitiveClient() {
        super();
    }

    /**
     * Take a coap or http query string, parse the options add them to the onem2m request primitives.  The onem2m core
     * will validate the parameters.  The return of the existence of a resource type is a convenience hack so that
     * we can discern whether the operation is a CREATE or a NOTIFY.  Need to think about how 3rd party protocols
     * will encode their options ... maybe they will simply add each option as a primitive directly, or they can
     * construct an option string ... which ever makes more sense.
     * TODO: look at MQTT to see how it encodes some of its primitives; does it use a query string?
     * @param query
     * @return
     */
    public Boolean parseQueryStringIntoPrimitives(String query) {
        Boolean resourceTypePresent = false;
        if (query != null) {
            String[] pairs = query.split("[&]");
            for (String pair : pairs) {
                String[] param = pair.split("[=]");
                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = param[0];
                    if (param.length > 1) {
                        value = param[1];
                        this.setPrimitive(key, value);
                        if (key.equalsIgnoreCase(RequestPrimitive.RESOURCE_TYPE)) { resourceTypePresent = true; }
                    }
                }
            }
        }
        return resourceTypePresent;
    }
}

