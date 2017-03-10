/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChildKey;
import org.opendaylight.yangtools.yang.binding.Augmentation;

import java.lang.ref.WeakReference;
import java.util.*;

/**
 * Created by gguliash on 4/27/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class Onem2mResourceElem implements Onem2mResource {
    private static final int JSON_LENGTH_THRESHOLD = 1000;
    private final DaoResourceTreeReader daoResourceTreeReader;
    private String resourceId;
    private String parentTargetUri;
    private String parentId;
    private String name;
    private String resourceType;
    private String resourceContentJsonString;
    private WeakReference<String> resourceContentJsonStringReference;


    public Onem2mResourceElem(DaoResourceTreeReader daoResourceTreeReader, String resourceId, String parentId, String name,
                              String resourceType, String resourceContentJsonString, String parentTargetUri) {
        this.daoResourceTreeReader = daoResourceTreeReader;

        this.resourceId = resourceId;
        this.parentId = parentId;
        this.parentTargetUri = parentTargetUri;
        this.name = name;
        this.resourceType = resourceType;

        setResourceContentJsonString(resourceContentJsonString);
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getParentTargetUri() {
        return parentTargetUri;
    }

    protected void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getParentId() {
        return parentId;
    }

    protected void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    public String getResourceType() {
        return resourceType;
    }

    protected void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceContentJsonString() {
        String ret = resourceContentJsonStringReference.get();
        if (ret == null) {

            ret = daoResourceTreeReader.retrieveResourceById(new Onem2mResourceKey(resourceId)).getResourceContentJsonString();
            setResourceContentJsonString(ret);
        }
        return ret;
    }

    protected void setResourceContentJsonString(String resourceContentJsonString) {
        if (resourceContentJsonString.length() > JSON_LENGTH_THRESHOLD) {
            this.resourceContentJsonString = null;
        } else {
            this.resourceContentJsonString = resourceContentJsonString;
        }
        resourceContentJsonStringReference = new WeakReference<String>(resourceContentJsonString);
    }

    /**
     * Returns Primary Key of Yang List Type
     */
    public Onem2mResourceKey getKey() {
        return new Onem2mResourceKey(resourceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Onem2mResourceElem that = (Onem2mResourceElem) o;

        if (resourceId != null ? !resourceId.equals(that.resourceId) : that.resourceId != null) return false;
        if (parentId != null ? !parentId.equals(that.parentId) : that.parentId != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (parentTargetUri != null ? !parentTargetUri.equals(that.parentTargetUri) : that.parentTargetUri != null) return false;
        if (resourceType != null ? !resourceType.equals(that.resourceType) : that.resourceType != null) return false;
        return resourceContentJsonString != null ? resourceContentJsonString.equals(that.resourceContentJsonString) : that.resourceContentJsonString == null;

    }

    @SuppressWarnings("unchecked")
    @Override
    public <E extends Augmentation<Onem2mResource>> E getAugmentation(Class<E> augmentationType) {
        return null;
    }

    @Override
    public Class<Onem2mResource> getImplementedInterface() {
        return null;
    }
}