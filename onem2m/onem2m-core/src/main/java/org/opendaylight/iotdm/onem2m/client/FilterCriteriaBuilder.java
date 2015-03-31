/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.client;

import org.json.JSONObject;
import org.opendaylight.iotdm.onem2m.core.resource.ResourceContent;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterCriteriaBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(FilterCriteriaBuilder.class);
    protected JSONObject jsonContent;

    public FilterCriteriaBuilder() {
        jsonContent = new JSONObject();
    }

    public String build() {
        return (jsonContent.toString());
    }
}