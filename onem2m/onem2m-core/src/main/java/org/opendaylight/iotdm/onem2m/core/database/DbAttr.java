/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class deals specifically with putting attributes into a db resource.
 */
public class DbAttr {

    private final Logger LOG = LoggerFactory.getLogger(DbAttr.class);

    private List<Attr> attrsList;
    private Map<String,String> attrMap;

    public DbAttr() {
        this.attrsList = new ArrayList<Attr>();
        this.attrMap = new HashMap<String,String>();
    }

    public DbAttr(List<Attr> onem2mAttrsList) {

        this.attrsList = onem2mAttrsList;
        this.attrMap = new HashMap<String,String>();
        for (Attr attr : onem2mAttrsList) {
            attrMap.put(attr.getName(), attr.getValue());
        }
    }

    public void setAttr(String attrName, String attrValue) {
        String temp = getAttr(attrName);
        if (temp != null) {
            LOG.error("Duplicate attr: prim={}, old={}, new={}", attrName, temp, attrValue);
            assert (false);
        }
        attrsList.add(new AttrBuilder().setKey(new AttrKey(attrName)).setName(attrName).setValue(attrValue).build());
        attrMap.put(attrName, attrValue);
        //LOG.info("set Attr N={}, V={}", attrName, attrValue);
    }

    public String getAttr(String attrName) {
        return this.attrMap.get(attrName);
    }

    public List<Attr> getAttrList() {
        return this.attrsList;
    }
}
