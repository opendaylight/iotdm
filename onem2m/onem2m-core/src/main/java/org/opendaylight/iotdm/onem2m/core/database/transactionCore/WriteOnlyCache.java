/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;

/**
 * Created by gguliash on 4/28/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public interface WriteOnlyCache {

    /**
     * Resource with resourceId is removed from the Cache if exists.
     *
     * @param resourceId key
     */
    void deleteResourceById(String resourceId);

    /**
     * Cleans Cache.
     */
    void reInitializeDatastore();

    /**
     * Cache is notified about the update of json content.
     * If Resource with resourceId is in Cache, updated resource, if not has no effect.
     *
     * @param resourceId          of the resource. Should not be null.
     * @param jsonResourceContent to set.
     */
    void updateJsonResourceContentString(String resourceId, String jsonResourceContent);

    /**
     * Cache is notified about creation of new Resource element. Saves Resource in cache.
     *
     * @param resourceId       of the resource. Should not be null.
     * @param resourceName     of the resource. Should not be null.
     * @param jsonContent      of the resource. Should not be null.
     * @param parentResourceId of the resource. Should not be null.
     * @param resourceType     of the resource. Should not be null.
     * @return Onem2mResource interface of the created resource.
     */
    Onem2mResource createResource(String resourceId, String resourceName, String jsonContent,
                                  String parentResourceId, Integer resourceType, String parentTargetUri);

    /**
     * Cache is notified about creation of new Cse element. Saves Cse in cache.
     *
     * @param name       of the Cse elem.
     * @param resourceId of the Cse elem.
     * @return true if successfully created.
     */
    boolean createCseByName(String name, String resourceId);


    /**
     * Cache is notified about creation of new AE element. Saves AE-ID to AE's resourceId mapping in cache.
     *
     * @param cseBaseCseId  CSE-ID of the cseBase resource as parent of the AE
     * @param aeId  AE-ID of the new AE resource
     * @param aeResourceId ResourceID of the new AE resource
     * @return  true if successfully created
     */
    boolean createAeResourceIdByAeId(final String cseBaseCseId, final String aeId, final String aeResourceId);

    /**
     * Remove AE-ID to resourceId mapping cached for cseBase if exists.
     * @param cseBaseCseId CSE-ID of the cseBase resource
     * @param aeId AE-ID of the AE resource
     */
    void deleteAeResourceIdByAeId(final String cseBaseCseId, final String aeId);
}
