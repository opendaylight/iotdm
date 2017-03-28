/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mParentChildListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.parent.child.list.Onem2mParentChild;


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
     * Returns list of cseBase resources cached.
     * @return List of cached cseBase resources
     */
    List<Onem2mCse> retrieveCseBaseList();

    /**
     * Returns map of Onem2mCseKeys to cseBase resources cached.
     * @return Map of cached cseBase resources
     */
    Map<Onem2mCseKey, Onem2mCse> retrieveCseBaseMap();

    /**
     * If Resource with key is in cache retrieves from cache, if not retrieves from DB and saves in Cache.
     *
     * @param key of the resource
     * @return resource instance if exists, or else null
     */
    Onem2mResourceElem retrieveResourceById(Onem2mResourceKey key);

    /**
     * If AE with AE-ID is registered to cseBase with cseBaseCseId the resourceId of the AE is retrieved from
     * cache or from DB (if not cached). Null is returned if there's not such AE registered.
     *
     * @param cseBaseCseId  CSE-ID of the cseBase resource as parent of the AE
     * @param aeId  AE-ID of the new AE resource
     * @return  ResourceID of the AE registered to the cseBase if exists, null otherwise
     */
    String retrieveAeResourceIdByAeId(final String cseBaseCseId, final String aeId);

    /**
     * Retrieve child of parent resource.
     * @param resourceId ResourceId of the parent resource
     * @param name ResourceName of the child resource
     * @return Child resource data
     */
    Onem2mParentChild retrieveChildByName(String resourceId, String name);

    /**
     * Retrieve list of child resource data of specific parent resource
     * @param key Key identifying the parent resource
     * @param limit Number of child resource data items to be returned. All data are returned
     *              if set to value lower than 1.
     * @return  List including child resource data
     */
    List<Onem2mParentChild> retrieveParentChildListLimitN(Onem2mParentChildListKey key, int limit);
}
