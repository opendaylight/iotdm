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

    public String generateResourceId(String parentResourceId, String resourceType, Integer iotdmInstance);
    /**
     * Add the cse to the db store using its name/resourceId
     *
     * @param name       cse name
     * @param resourceId id
     * @return true if successfully created
     */
    boolean createCseByName(String name, String resourceId) throws IotdmDaoWriteException;

    /**
     * Add a resource to the data store
     *
     * @param onem2mRequest    request
     * @param parentResourceId id of the parent resource
     * @param resourceType     resourceType of the resource
     * @return this new resource
     */
    boolean createResource(RequestPrimitive onem2mRequest,
                           String parentResourceId, String resourceType) throws IotdmDaoWriteException;

    /**
     * Update the pointers to the oldest and latest children
     *
     * @param resourceId the resource id
     * @param resourceType     resourceType of the resource
     * @param oldest     pointer to the tail
     * @param latest     pointer to the head
     * @return the resource
     */
    boolean updateResourceOldestLatestInfo(String resourceId,
                                           String resourceType,
                                           String oldest,
                                           String latest) throws IotdmDaoWriteException;

    /**
     * @param resourceId          this resource
     * @param jsonResourceContent serailized JSON object
     * @return true if successfully updated
     */
    boolean updateJsonResourceContentString(String resourceId, String jsonResourceContent) throws IotdmDaoWriteException;

    /**
     * Link the parent resource to the child resource in the data store.
     *
     * @param parentResourceId parent
     * @param childName        name of child
     * @param childResourceId  child res id
     * @param prevId           pointer to prev sibling
     * @param nextId           pointer to next sibling
     * @return true if successfully created
     */
    boolean createParentChildLink(String parentResourceId,
                                  String childName, String childResourceId,
                                  String prevId, String nextId) throws IotdmDaoWriteException;

    /**
     * Update the Next pointer
     *
     * @param parentResourceId the parent
     * @param child            the child
     * @param nextId           its next pointer
     * @return true if successfully updated
     */
    boolean updateChildSiblingNextInfo(String parentResourceId,
                                       Onem2mParentChild child,
                                       String nextId) throws IotdmDaoWriteException;

    /**
     * Unlink the child resource from the parent resource
     *
     * @param parentResourceId  the parent
     * @param childResourceName child name
     * @return true if successfully removed
     */
    boolean removeParentChildLink(String parentResourceId, String childResourceName) throws IotdmDaoWriteException;

    /**
     * Delete the resource using its id
     *
     * @param resourceId the resource id
     * @return true if successfully deleted
     */
    boolean deleteResourceById(String resourceId) throws IotdmDaoWriteException;

    /**
     * Cleanup the data store.
     */
    void reInitializeDatastore();

    /**
     * Update the prev pointer
     *
     * @param parentResourceId the parent
     * @param child            the child
     * @param prevId           prev pointer
     * @return true if successfully updated
     */
    boolean updateChildSiblingPrevInfo(String parentResourceId,
                                       Onem2mParentChild child,
                                       String prevId) throws IotdmDaoWriteException;

    /**
     * Add the AE-ID to resourceID mapping into Onem2mCseList.
     * @param cseBaseName The name of cseBase
     * @param aeId The AE-ID
     * @param aeResourceId The resourceID related to the AE-ID
     * @return True in case of success, False otherwise
     */
    boolean createAeIdToResourceIdMapping(String cseBaseName,
                                          String aeId, String aeResourceId) throws IotdmDaoWriteException;

    /**
     * Delete AE-ID to resourceID mapping for specified cseBase.
     * @param cseBaseName The name of cseBase
     * @param aeId The AE-ID of the AE
     * @return True in case of success, False otherwise
     */
    boolean deleteAeIdToResourceIdMapping(String cseBaseName, String aeId) throws IotdmDaoWriteException;
}
