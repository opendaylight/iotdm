/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.dao;

import org.opendaylight.iotdm.onem2m.core.database.transactionCore.Onem2mResourceElem;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;

/**
 * Created by gguliash on 5/20/16.
 */
public interface DaoResourceTreeReader {
    /**
     *
     * @param key name of the CSE element
     * @return Cse element
     */
    Onem2mCse retrieveCseByName(Onem2mCseKey key);

    /**
     *
     * @param key id of the Resource element
     * @return Resource element
     */
    Onem2mResourceElem retrieveResourceById(Onem2mResourceKey key);

    /**
     *
     * @return all existing CSE elements
     */
    Onem2mCseList retrieveFullCseList();

    /**
     *
     * @return all existing Resource elements
     */
    Onem2mResourceTree retrieveFullResourceList();
}
