/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.rest.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePrimitive {

    private static final Logger LOG = LoggerFactory.getLogger(BasePrimitive.class);

    protected List<Onem2mPrimitive> onem2mPrimitivesList;
    private Map<String,String> primitiveMap;

    public BasePrimitive() {
        onem2mPrimitivesList = new ArrayList<Onem2mPrimitive>();
        primitiveMap = new HashMap<String,String>();
   }

    public BasePrimitive(List<Onem2mPrimitive> onem2mPrimitivesList) {
        this.onem2mPrimitivesList = onem2mPrimitivesList;
        this.primitiveMap = new HashMap<String,String>();
        for (Onem2mPrimitive onem2mPrimitive : onem2mPrimitivesList) {
            primitiveMap.put(onem2mPrimitive.getName(), onem2mPrimitive.getValue());
        }
    }

    public void setPrimitive(String primitiveName, String primitiveValue) {
        String temp = getPrimitive(primitiveName);
        if (temp != null) {
            LOG.error("Duplicate attr: prim={}, old={}, new={}", primitiveName, temp, primitiveValue);
            return;
        }
        onem2mPrimitivesList.add(new Onem2mPrimitiveBuilder().setName(primitiveName).setValue(primitiveValue).build());
        primitiveMap.put(primitiveName, primitiveValue);
        LOG.info("set Attr N={}, V={}", primitiveName, primitiveValue);
    }

    public String getPrimitive(String primitiveName) {
        return this.primitiveMap.get(primitiveName);
    }

    /**
     * The input requestPrimitive builder needs access to the internal list
     * @return
     */
    public List<Onem2mPrimitive> getPrimitivesList() {
        return onem2mPrimitivesList;
    }
}

