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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class deals specifically with putting attributes into a db resource.  An example is the label primitive
 * which can hold an arrayList of members
 */
public class DbAttrSet {

    private final Logger LOG = LoggerFactory.getLogger(DbAttrSet.class);

    private List<AttrSet> attrSetsList;
    private Map<String,List<Member>> attrMap;

    public DbAttrSet() {
        this.attrSetsList = new ArrayList<AttrSet>();
        this.attrMap = new HashMap<String,List<Member>>();
    }

    public DbAttrSet(List<AttrSet> onem2mAttrSetsList) {

        this.attrSetsList = onem2mAttrSetsList;
        this.attrMap = new HashMap<String,List<Member>>();
        for (AttrSet attrSet : onem2mAttrSetsList) {
            attrMap.put(attrSet.getName(), attrSet.getMember());
        }
    }

    public void setAttrSet(String attrSetName, List<Member> memberList) {
        List<Member> temp = getAttrSet(attrSetName);
        if (temp != null) {
            LOG.error("Duplicate attr: prim={}, old={}, new={}", attrSetName, temp, memberList);
            assert (false);
        }
        attrSetsList.add(new AttrSetBuilder().setKey(new AttrSetKey(attrSetName)).setName(attrSetName).setMember(memberList).build());
        attrMap.put(attrSetName, memberList);
        LOG.info("set Attr N={}, V={}", attrSetName, memberList);
    }


    public List<Member> getAttrSet(String attrSetName) {
        return this.attrMap.get(attrSetName);
    }

    public List<AttrSet> getAttrSetsList() {
        return this.attrSetsList;
    }
}

