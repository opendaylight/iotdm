/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.dao;

import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChildKey;

import java.io.Closeable;

/**
 * Created by gguliash on 5/20/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public interface DaoResourceTreeWriter extends Closeable {
    void finalize() throws Throwable;

    void close();

    public String generateResourceId(String parentResourceId, Integer resourceType, Integer iotdmInstance);

    /**
     * Add the cse to the db store using its name/resourceId
     *
     * @param name       cse name
     * @param resourceId id
     * @return true if successfully created
     */
    boolean createCseByName(String name, String resourceId);

    /**
     * Add a resource to the data store
     *
     * @param transaction      transaction
     * @param onem2mRequest    request
     * @param parentResourceId id of the parent resource
     * @param resourceType     resourceType of the resource
     * @return this new resource
     */
    boolean createResource(Object transaction, RequestPrimitive onem2mRequest,
                           String parentResourceId, Integer resourceType);

    /**
     * @param transaction      transaction
     * @param resourceId          this resource
     * @param jsonResourceContent serailized JSON object
     * @return true if successfully updated
     */
    boolean updateJsonResourceContentString(Object transaction, String resourceId, String jsonResourceContent);

    /**
     * Delete the resource using its id
     *
     * @param resourceId the resource id
     * @return true if successfully deleted
     */
    boolean deleteResource(Object transaction, String resourceId, String parentResourceId, String resourceName);

    boolean moveParentChildLinkToDeleteParent(String resoruceId, String oldPrentResourceId, String childResourceName,
                                              String newParentResourceId);
    /**
     * Cleanup the data store.
     */
    void reInitializeDatastore();

    /**
     * Add the AE-ID to resourceID mapping into Onem2mCseList.
     * @param cseBaseName The name of cseBase
     * @param aeId The AE-ID
     * @param aeResourceId The resourceID related to the AE-ID
     * @return True in case of success, False otherwise
     */
    boolean createAeIdToResourceIdMapping(String cseBaseName,
                                          String aeId, String aeResourceId);

    /**
     * Delete AE-ID to resourceID mapping for specified cseBase.
     * @param cseBaseName The name of cseBase
     * @param aeId The AE-ID of the AE
     * @return True in case of success, False otherwise
     */
    boolean deleteAeIdToResourceIdMapping(String cseBaseName, String aeId);

    /**
     * Add the remoteCSE CSE-ID to resourceID mapping into Onem2mCseList.
     * @param cseBaseName The name of cseBase
     * @param remoteCseCseId The CSE-ID of the remoteCSE resource
     * @param remoteCseResourceId The resourceID related to the CSE-ID
     * @return True in case of success, False otherwise
     */
    boolean createRemoteCseIdToResourceIdMapping(String cseBaseName,
                                                 String remoteCseCseId, String remoteCseResourceId);

    /**
     * Delete remoteCSE CSE-ID to resourceID mapping for specified cseBase.
     * @param cseBaseName The name of cseBase
     * @param remoteCseCseId The CSE-ID of the remoteCSE
     * @return True in case of success, False otherwise
     */
    boolean deleteRemoteCseIdToResourceIdMapping(String cseBaseName, String remoteCseCseId);

    /**
     * Stores the last used resourceId in data store.
     * @param resourceId The last assigned resourceId
     */
    boolean writeSystemStartId(int resourceId);

    Object startTransaction();

    boolean endTransaction(Object transaction);
}
