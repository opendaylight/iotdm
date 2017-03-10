/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.rest.utils;

import java.util.*;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.primitive.list.Onem2mPrimitiveBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasePrimitive {

    private static final Logger LOG = LoggerFactory.getLogger(BasePrimitive.class);

    protected List<Onem2mPrimitive> onem2mPrimitivesList;
    protected Map<String,String> primitiveMap;
    protected Map<String,List<String>> primitiveManyMap;

    public BasePrimitive() {
        onem2mPrimitivesList = new ArrayList<Onem2mPrimitive>();
        primitiveMap = new HashMap<String,String>();
        primitiveManyMap = new HashMap<String,List<String>>();
   }

    /**
     * Prime the list with a LIST of primitives
     * @param onem2mPrimitivesList input list
     */
    public void setPrimitivesList(List<Onem2mPrimitive> onem2mPrimitivesList) {
        this.onem2mPrimitivesList = onem2mPrimitivesList;
        for (Onem2mPrimitive onem2mPrimitive : onem2mPrimitivesList) {
            primitiveMap.put(onem2mPrimitive.getName(), onem2mPrimitive.getValue());
        }
    }

    /**
     * Set a name value pair
     * @param primitiveName name
     * @param primitiveValue value
     */
    public void setPrimitive(String primitiveName, String primitiveValue) {
        if (delPrimitive(primitiveName)) {
            LOG.error("set Attr N={}, V={}", primitiveName, primitiveValue);
        }
        onem2mPrimitivesList.add(new Onem2mPrimitiveBuilder().setName(primitiveName).setValue(primitiveValue).build());
        primitiveMap.put(primitiveName, primitiveValue);
        //LOG.info("set Attr N={}, V={}", primitiveName, primitiveValue);
    }

    /**
     * Enable many values to be set for the same primitive (labels as an example)  These will ultimately
     * be stored in the DbAttrSets vs the DbAttrs
     * @param primitiveName name
     * @param primitiveValue value
     */
    public void setPrimitiveMany(String primitiveName, String primitiveValue) {
        if (delPrimitive(primitiveName)) {
            LOG.error("set Attr N={}, V={}", primitiveName, primitiveValue);
        }
        onem2mPrimitivesList.add(new Onem2mPrimitiveBuilder().setName(primitiveName).setValue(primitiveValue).build());
        List<String> valueArray = getPrimitiveMany(primitiveName);
        if (valueArray == null) {
            valueArray = new ArrayList<String>();
            valueArray.add(primitiveValue);
            primitiveManyMap.put(primitiveName, valueArray);
        } else {
            valueArray.add(primitiveValue);
        }
        //LOG.info("setMany Attr N={}, V={}, numValues={}", primitiveName, primitiveValue, valueArray.size());
    }

    /**
     * Return the value for this name
     * @param primitiveName name
     * @return value for this name
     */
    public String getPrimitive(String primitiveName) {
        return this.primitiveMap.get(primitiveName);
    }

    /**
     * Some names can have many values associated with them eg. labels
     * @param primitiveName name
     * @return the list of values
     */
    public List<String> getPrimitiveMany(String primitiveName) {
        return this.primitiveManyMap.get(primitiveName);
    }

    /**
     * Deletes primitive identified by name.
     * @param primitiveName The primitive name.
     * @return True if the primitive has been deleted False otherwise.
     */
    public boolean delPrimitive(String primitiveName) {
        if (primitiveMap.containsKey(primitiveName)) {
            primitiveMap.remove(primitiveName);

            Optional<Onem2mPrimitive> primitive = onem2mPrimitivesList.stream()
                                                .filter(p -> p.getName().equals(primitiveName))
                                                .findFirst();

            if(primitive.isPresent()) {
                onem2mPrimitivesList.remove(primitive.get());
            }
            return true;
        }
        return false;
    }

    /**
     * The input requestPrimitive builder needs access to the internal list
     * @return the list of primitives
     */
    public List<Onem2mPrimitive> getPrimitivesList() {
        return onem2mPrimitivesList;
    }
}

