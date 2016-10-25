/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChildKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .Onem2mParentChildListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.OldestLatest;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.OldestLatestKey;

/**
 * Created by gguliash on 4/28/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public interface ReadOnlyCache {
    /**
     * If element is in cache retrieves from cache, if not retrieves from DB and saves in Cache.
     *
     * @param key of the Cse element.
     * @return returns null if Cse element with key does not exist, or DB threw error(Writes log.error).
     */
    Onem2mCse retrieveCseByName(Onem2mCseKey key);

    /**
     * If Resource with key is in cache retrieves from cache, if not retrieves from DB and saves in Cache. Does not support partial caching of the OldestLatest.
     *
     * @param key             of the resource
     * @param oldestLatestKey key of the oldestLatest. Only allowed keys are Onem2m.ResourceType.SUBSCRIPTION and Onem2m.ResourceType.CONTENT_INSTANCE
     * @return oldestLatest instance if exists, or else null
     */
    OldestLatest retrieveOldestLatestByResourceType(Onem2mResourceKey key, OldestLatestKey oldestLatestKey);

    /**
     * If Resource with key is in cache retrieves from cache, if not retrieves from DB and saves in Cache.
     *
     * @param key of the resource
     * @return resource instance if exists, or else null
     */
    Onem2mResourceElem retrieveResourceById(Onem2mResourceKey key);

}
