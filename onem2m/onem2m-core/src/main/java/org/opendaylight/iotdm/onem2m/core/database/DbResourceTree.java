/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database;

import java.util.Collections;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTreeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Attr;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.Child;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.ChildBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.ChildKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.attr.set.Member;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contain static functions invoked by the Onem2mDb class.  They are turn invoke the data store API
 * class to interact with the data store.  The DbResourceTree is responsible for the maintenance of the cse
 * entries in the cse list as well as the resource entries in the resource list.c
 */
public class DbResourceTree {

    private final Logger LOG = LoggerFactory.getLogger(DbResourceTree.class);

    private BindingTransactionChain bindingTransactionChain;

    /**
     * Pass the bindingTransactionChain to the resource tree and cache it for db retrieve txns.
     * @param bindingTransactionChain transaction chain
     */
    public DbResourceTree(BindingTransactionChain bindingTransactionChain) {
        this.bindingTransactionChain = bindingTransactionChain;
    }

    /**
     * Add the cse to the db store using its name/resourceId
     * @param dbTxn transaction id
     * @param name cse name
     * @param resourceId id
     */
    public void createCseByName(DbTransaction dbTxn, String name, String resourceId) {

        Onem2mCse onem2mCse = new Onem2mCseBuilder()
                .setKey(new Onem2mCseKey(name))
                .setName(name)
                .setResourceId(resourceId)
                .build();

        InstanceIdentifier<Onem2mCse> iid = InstanceIdentifier.create(Onem2mCseList.class)
                .child(Onem2mCse.class, onem2mCse.getKey());

        dbTxn.create(iid, onem2mCse, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Gwet the cse from the db store using its key ie. the cseName
     * @param name cse name
     * @return the cse object
     */
    public Onem2mCse retrieveCseByName(String name) {

        InstanceIdentifier<Onem2mCse> iid = InstanceIdentifier.create(Onem2mCseList.class)
                .child(Onem2mCse.class, new Onem2mCseKey(name));

        return DbTransaction.retrieve(bindingTransactionChain, iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Add a resource to the data store
     * @param dbTxn transaction id
     * @param onem2mRequest request
     * @param parentResourceId id of the parent resource
     * @return this new resource
     */
    public Onem2mResource createResource(DbTransaction dbTxn, RequestPrimitive onem2mRequest, String parentResourceId) {

        Onem2mResource onem2mResource = new Onem2mResourceBuilder()
                .setKey(new Onem2mResourceKey(onem2mRequest.getResourceId()))
                .setResourceId(onem2mRequest.getResourceId())
                .setName(onem2mRequest.getResourceName())
                .setParentId(parentResourceId) // parent resource
                .setLatestId((Onem2mDb.NULL_RESOURCE_ID))
                .setOldestId((Onem2mDb.NULL_RESOURCE_ID))
                .setChild(Collections.<Child>emptyList()) // new resource has NO children
                .setAttr(onem2mRequest.getResourceContent().getAttrList())
                //.setAttrSet(onem2mRequest.getDbAttrSets().getAttrSetsList())
                .setAttrSet(Collections.<AttrSet>emptyList())
                .build();

        InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, onem2mResource.getKey());

        dbTxn.create(iid, onem2mResource, LogicalDatastoreType.OPERATIONAL);

        return onem2mResource;
    }

    /**
     * Update the pointers to the oldest and latest children
     * @param dbTxn transaction id
     * @param onem2mResource the current parameters
     * @param oldest pointer to the tail
     * @param latest pointer to the head
     * @return the resource
     */
    public Onem2mResource updateResourceOldestLatestInfo(DbTransaction dbTxn,
                                                         Onem2mResource onem2mResource,
                                                         String oldest,
                                                         String latest) {

        Onem2mResource updateOnem2mResource = new Onem2mResourceBuilder(onem2mResource)
                .setLatestId(latest)
                .setOldestId(oldest)
                .build();

        InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, onem2mResource.getKey());

        dbTxn.update(iid, updateOnem2mResource, LogicalDatastoreType.OPERATIONAL);

        return updateOnem2mResource;
    }

    /**
     * Get a resource from the data store using its key, ie. its resourceId
     * @param resourceId the resource id
     * @return the resource
     */
    public Onem2mResource retrieveResourceById(String resourceId) {

        InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId));

        return DbTransaction.retrieve(bindingTransactionChain, iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Retrieve the attr by name from the data store
     * @param resourceId resource id of the attr
     * @param attrName name of attr
     * @return Attr
     */
    public Attr retrieveAttrByName(String resourceId, String attrName) {

        InstanceIdentifier<Attr> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId))
                .child(Attr.class, new AttrKey(attrName));

        return DbTransaction.retrieve(bindingTransactionChain, iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Delete the attr by name from the data store
     * @param dbTxn transaction id
     * @param resourceId this resource
     * @param attrName name
     */
    public void deleteAttr(DbTransaction dbTxn, String resourceId, String attrName) {

        InstanceIdentifier<Attr> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId))
                .child(Attr.class, new AttrKey(attrName));

        dbTxn.delete(iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     *
     * @param dbTxn transaction id
     * @param resourceId this resource
     * @param attr updated attr
     */
    public void updateAttr(DbTransaction dbTxn, String resourceId, Attr attr) {

        InstanceIdentifier<Attr> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId))
                .child(Attr.class, attr.getKey());

        dbTxn.update(iid, attr, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Retrieve teh child using its resource name
     * @param resourceId this id
     * @param childName name of child
     * @return the child
     */
    public Child retrieveChildByName(String resourceId, String childName) {

        InstanceIdentifier<Child> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId))
                .child(Child.class, new ChildKey(childName));

        return DbTransaction.retrieve(bindingTransactionChain, iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Link the parent resource to the child resource in the data store.
     * @param dbTxn transaction id
     * @param parentResourceId parent
     * @param childName name of child
     * @param childResourceId child res id
     * @param prevId pointer to prev sibling
     * @param nextId pointer to next sibling
     */
    public void createParentChildLink(DbTransaction dbTxn, String parentResourceId,
                                               String childName, String childResourceId,
                                               String prevId, String nextId) {
        Child child = new ChildBuilder()
                .setKey(new ChildKey(childName))
                .setName(childName)
                .setResourceId(childResourceId)
                .setNextId(nextId)
                .setPrevId(prevId)
                .build();

        InstanceIdentifier<Child> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(parentResourceId))
                .child(Child.class, child.getKey());

        dbTxn.create(iid, child, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Update the Next pointer
     * @param dbTxn transaction id
     * @param parentResourceId the parent
     * @param child the child
     * @param nextId its next pointer
     */
    public void updateChildSiblingNextInfo(DbTransaction dbTxn,
                                           String parentResourceId,
                                           Child child,
                                           String nextId) {

        Child updateChild = new ChildBuilder(child)
                .setNextId(nextId)
                .build();

        InstanceIdentifier<Child> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(parentResourceId))
                .child(Child.class, updateChild.getKey());

        dbTxn.update(iid, updateChild, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Update the prev pointer
     * @param dbTxn transaction id
     * @param parentResourceId the parent
     * @param child the child
     * @param prevId prev pointer
     */
    public void updateChildSiblingPrevInfo(DbTransaction dbTxn,
                                           String parentResourceId,
                                           Child child,
                                           String prevId) {
        Child updateChild = new ChildBuilder(child)
                .setPrevId(prevId)
                .build();

        InstanceIdentifier<Child> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(parentResourceId))
                .child(Child.class, updateChild.getKey());

        dbTxn.update(iid, updateChild, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Unlink the child resource from the parent resource
     * @param dbTxn transaction id
     * @param parentResourceId the parent
     * @param childResourceName child name
     */
    public void removeParentChildLink(DbTransaction dbTxn, String parentResourceId, String childResourceName) {

        InstanceIdentifier<Child> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(parentResourceId))
                .child(Child.class, new ChildKey(childResourceName));

        dbTxn.delete(iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Using the parent resourceId, lookup its child by name.
     *
     * @param resourceId this resource
     * @param name name
     * @return the resource
     */
    public Onem2mResource retrieveChildResourceByName(String resourceId, String name) {

        InstanceIdentifier<Child> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId))
                .child(Child.class, new ChildKey(name));

        Child child = DbTransaction.retrieve(bindingTransactionChain, iid, LogicalDatastoreType.OPERATIONAL);
        if (child == null)
            return null;

        return retrieveResourceById(child.getResourceId());
    }

    /**
     * Delete the resource using its id
     * @param dbTxn transaction id
     * @param resourceId the resource id
     */
    public void deleteResourceById(DbTransaction dbTxn, String resourceId) {

        InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId));

        dbTxn.delete(iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Cleanup the data store.
     */
    public void reInitializeDatastore() {

        // overwrite the cseList with empty info
        InstanceIdentifier<Onem2mCseList> iidCseList = InstanceIdentifier.builder(Onem2mCseList.class).build();
        Onem2mCseList cseList = new Onem2mCseListBuilder().setOnem2mCse(Collections.<Onem2mCse>emptyList()).build();

        // overwrite the resource tree with empty information
        InstanceIdentifier<Onem2mResourceTree> iidTreeList = InstanceIdentifier.builder(Onem2mResourceTree.class).build();
        Onem2mResourceTree tree = new Onem2mResourceTreeBuilder().setOnem2mResource(Collections.<Onem2mResource>emptyList()).build();

        DbTransaction dbTxn = new DbTransaction(bindingTransactionChain);
        dbTxn.create(iidCseList, cseList, LogicalDatastoreType.OPERATIONAL);
        dbTxn.create(iidTreeList, tree, LogicalDatastoreType.OPERATIONAL);
        dbTxn.commitTransaction();
    }

    /**
     * Dump resource info to the log
     * @param onem2mResource this resource
     * @param dumpChildList child verbose dump option
     */
    public void dumpResourceToLog(Onem2mResource onem2mResource, boolean dumpChildList) {
        LOG.info("    Resource: id: {}, name: {}, parentId: {}, latestId: {}, oldestId: {}",
                onem2mResource.getResourceId(), onem2mResource.getName(), onem2mResource.getParentId(),
                onem2mResource.getLatestId(), onem2mResource.getOldestId());
        List<Child> childList = onem2mResource.getChild();
        LOG.info("    Child List: count: {}", childList.size());
        if (dumpChildList) {
            for (Child child : childList) {
                LOG.info("        Child: name: {}, id: {}, prev: {}, next: {}",
                        child.getName(), child.getResourceId(),
                        child.getPrevId(), child.getNextId());
            }
        }
        List<Attr> attrList = onem2mResource.getAttr();
        LOG.info("    Attr List: count: {}", attrList.size());
        for (Attr attr : attrList) {
            LOG.info("        Attr: name: {}, value: {}", attr.getName(), attr.getValue());
        }
        List<AttrSet> attrSetList = onem2mResource.getAttrSet();
        LOG.info("    AttrSet List: count: {}", attrSetList.size());
        for (AttrSet attrSet : attrSetList) {
            LOG.info("        AttrSet: name: {}", attrSet.getName());
            List<Member> memberList = attrSet.getMember();
            LOG.info("        AttrSet: name: {}, count: {}", attrSet.getName(), memberList.size());
            for (Member member : memberList) {
                LOG.info("            Member: name: {}, value: {}", member.getMember(), member.getValue());
            }
        }
    }

    /**
     * Dump in raw form (no hierarchy) the resource tree
     * @param resourceId this resource
     */
    public void dumpRawTreeToLog(String resourceId) {

        // if a resource id is supplied, dump the specific resource or fall thru and dump them all
        if (resourceId != null) {
            LOG.info("Dumping ResourceId: {}, Start", resourceId);

            Onem2mResource onem2mResource = retrieveResourceById(resourceId);
            if (onem2mResource != null) {
                dumpResourceToLog(onem2mResource, false);
            }
            LOG.info("Dumping ResourceId: {}, End", resourceId);

            return;
        }

        InstanceIdentifier<Onem2mCseList> iidCseList = InstanceIdentifier.builder(Onem2mCseList.class).build();

        Onem2mCseList cseList = DbTransaction.retrieve(bindingTransactionChain, iidCseList, LogicalDatastoreType.OPERATIONAL);

        InstanceIdentifier<Onem2mResourceTree> iidTree = InstanceIdentifier.builder(Onem2mResourceTree.class).build();

        Onem2mResourceTree tree = DbTransaction.retrieve(bindingTransactionChain, iidTree, LogicalDatastoreType.OPERATIONAL);

        LOG.info("Dumping Resource Tree: Start ...");
        List<Onem2mCse> onem2mCseList = cseList.getOnem2mCse();
        LOG.info("CSEBASE List: count: {}", onem2mCseList.size());
        for (Onem2mCse onem2mCse : onem2mCseList) {
            LOG.info("Cse: id: {}, name: {}", onem2mCse.getResourceId(), onem2mCse.getName());
        }

        List<Onem2mResource> onem2mResourceList = tree.getOnem2mResource();
        LOG.info("Resource List: count: {}", onem2mResourceList.size());
        for (Onem2mResource onem2mResource : onem2mResourceList) {
            dumpResourceToLog(onem2mResource, true);
        }
        LOG.info("Dumping Resource Tree: End ...");
    }

    private void dumpHierarchicalResourceToLog(String resourceId) {
        Onem2mResource onem2mResource = this.retrieveResourceById(resourceId);
        dumpResourceToLog(onem2mResource, false);
        List<Child> childList = onem2mResource.getChild();
        for (Child child : childList) {
            dumpHierarchicalResourceToLog(child.getResourceId());
        }
    }

    /**
     * Dump in hierarchical form
     * @param resourceId the resource tree
     */
    public void dumpHierarchicalTreeToLog(String resourceId) {

        // if a resource id is supplied, dump the specific resource or fall thru and dump them all
        if (resourceId != null) {
            LOG.info("Dumping ResourceId: {}, Start", resourceId);
            dumpHierarchicalResourceToLog(resourceId);
            LOG.info("Dumping ResourceId: {}, End", resourceId);
            return;
        }
        InstanceIdentifier<Onem2mCseList> iidCseList = InstanceIdentifier.builder(Onem2mCseList.class).build();

        Onem2mCseList cseList = DbTransaction.retrieve(bindingTransactionChain, iidCseList, LogicalDatastoreType.OPERATIONAL);

        LOG.info("Dumping Hierarchical Resource Tree: Start ...");
        List<Onem2mCse> onem2mCseList = cseList.getOnem2mCse();
        LOG.info("CSEBASE List: count: {}", onem2mCseList.size());
        for (Onem2mCse onem2mCse : onem2mCseList) {
            LOG.info("Cse: id: {}, name: {}", onem2mCse.getResourceId(), onem2mCse.getName());
            dumpHierarchicalResourceToLog(onem2mCse.getResourceId());
        }
        LOG.info("Dumping Hierarchical Resource Tree: End ...");
    }
}