/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.core.rest.utils.ResponsePrimitive;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSetBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.resource.AttrSetKey;
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
     * @param bindingTransactionChain
     */
    public DbResourceTree(BindingTransactionChain bindingTransactionChain) {
        this.bindingTransactionChain = bindingTransactionChain;
    }

    /**
     * Add the cse to the db store using its name/resourceId
     * @param dbTxn
     * @param name
     * @param resourceId
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
     * @param name
     * @return
     */
    public Onem2mCse retrieveCseByName(String name) {

        InstanceIdentifier<Onem2mCse> iid = InstanceIdentifier.create(Onem2mCseList.class)
                .child(Onem2mCse.class, new Onem2mCseKey(name));

        return DbTransaction.retrieve(bindingTransactionChain, iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Add a resource to the data store
     * @param dbTxn
     * @param onem2mRequest
     * @param parentResourceId
     */
    public Onem2mResource createResource(DbTransaction dbTxn, RequestPrimitive onem2mRequest, String parentResourceId) {

        Onem2mResource onem2mResource = new Onem2mResourceBuilder()
                .setKey(new Onem2mResourceKey(onem2mRequest.getResourceId()))
                .setResourceId(onem2mRequest.getResourceId())
                .setName(onem2mRequest.getResourceName())
                .setParentId(parentResourceId) // parent resource
                .setChild(Collections.<Child>emptyList()) // new resource has NO children
                .setAttr(onem2mRequest.getResourceContent().getAttrList())
                //.setAttr(Collections.<Attr>emptyList())
                //.setAttr(tempAttrList)
                //.setAttrSet(onem2mRequest.getDbAttrSets().getAttrSetsList())
                .setAttrSet(Collections.<AttrSet>emptyList())
                .build();

        InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, onem2mResource.getKey());

        dbTxn.create(iid, onem2mResource, LogicalDatastoreType.OPERATIONAL);

        return onem2mResource;
    }

    /**
     * Get a resource from the data store using its key, ie. its resourceId
     * @param resourceId
     * @return
     */
    public Onem2mResource retrieveResourceById(String resourceId) {

        InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId));

        return DbTransaction.retrieve(bindingTransactionChain, iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Link the parent resource to the child resource in the data store.
     * @param dbTxn
     * @param parentResourceId
     * @param childName
     * @param childResourceId
     */
    public void createParentChildLink(DbTransaction dbTxn, String parentResourceId,
                                               String childName, String childResourceId) {
        Child child = new ChildBuilder()
                .setKey(new ChildKey(childName))
                .setName(childName)
                .setResourceId(childResourceId)
                .build();

        InstanceIdentifier<Child> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(parentResourceId))
                .child(Child.class, child.getKey());

        dbTxn.create(iid, child, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * Unlink the child resource from the parent resource
     * @param dbTxn
     * @param parentResourceId
     * @param childResourceName
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
     * @param resourceId
     * @param name
     * @return
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
     * Delete the resource using its key, ie. the resourceId
     *
     * @param resourceId
     * @return
     */
    public void deleteResourceById(DbTransaction dbTxn, String resourceId) {

        InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                .child(Onem2mResource.class, new Onem2mResourceKey(resourceId));

        dbTxn.delete(iid, LogicalDatastoreType.OPERATIONAL);
    }

    /**
     * This routine walks the hierarchy and recursively finds the children and adds them to the list.
     * @param resourceId
     * @param resourceIdList
     */
    public void hierarchicalFindResource(String resourceId, List<String> resourceIdList) {

        Onem2mResource onem2mResource = retrieveResourceById(resourceId);
        List<Child> childList = onem2mResource.getChild();
        for (Child child : childList) {
            hierarchicalFindResource(child.getResourceId(), resourceIdList);
        }
        resourceIdList.add(resourceId);
    }

    /**
     * Initialize the data store: this can also be used to cleanup the data store.
     */
    public void initializeDatastore() {

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

    public int numResourcesInTree() {
        InstanceIdentifier<Onem2mResourceTree> iidTree = InstanceIdentifier.builder(Onem2mResourceTree.class).build();

        Onem2mResourceTree tree = DbTransaction.retrieve(bindingTransactionChain, iidTree, LogicalDatastoreType.OPERATIONAL);

        List<Onem2mResource> onem2mResourceList = tree.getOnem2mResource();
        return onem2mResourceList.size();
    }

    public void dumpResourceToLog(Onem2mResource onem2mResource, boolean dumpChildList) {
        LOG.info("    Resource: id: {}, name: {}, parentId: {}",
                onem2mResource.getResourceId(), onem2mResource.getName(), onem2mResource.getParentId());
        List<Child> childList = onem2mResource.getChild();
        LOG.info("    Child List: count: {}", childList.size());
        if (dumpChildList) {
            for (Child child : childList) {
                LOG.info("        Child: name: {}, id: {}", child.getName(), child.getResourceId());
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

    // debug function to dump the resource tree to the karaf log
    public void dumpRawTreeToLog() {

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

    // debug function to dump the resource tree in hierarchical format to the karaf log
    public void dumpHierarchicalTreeToLog() {

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