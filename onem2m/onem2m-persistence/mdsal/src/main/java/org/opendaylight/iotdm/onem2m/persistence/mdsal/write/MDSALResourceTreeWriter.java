/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.persistence.mdsal.write;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Monitor;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.rest.utils.RequestPrimitive;
import org.opendaylight.iotdm.onem2m.persistence.mdsal.MDSALDaoResourceTreeFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.IotdmSpecificOperationalData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.IotdmSpecificOperationalDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mCseListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTree;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mResourceTreeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredRemoteCses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredRemoteCsesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredRemoteCsesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .Onem2mParentChildList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .Onem2mParentChildListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .Onem2mParentChildListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.parent.child.list.Onem2mParentChildBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree
        .onem2m.parent.child.list.Onem2mParentChildKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredAeIds;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredAeIdsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.onem2m.cse.Onem2mRegisteredAeIdsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by gguliash on 5/20/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class MDSALResourceTreeWriter implements DaoResourceTreeWriter {
    private final Logger LOG = LoggerFactory.getLogger(MDSALResourceTreeWriter.class);
    private MDSALTransactionWriter writer;
    private LogicalDatastoreType dsType = LogicalDatastoreType.CONFIGURATION;
    private MDSALDaoResourceTreeFactory factory;
    private List<Monitor> crudMonitor = Lists.newArrayList();

    private int numShards;

    public MDSALResourceTreeWriter(MDSALDaoResourceTreeFactory factory, MDSALTransactionWriter writer) {
        this.writer = writer;
        this.factory = factory;
        numShards = 1;
        for (int i = 0; i < numShards; i++)  {
            crudMonitor.add(i, new Monitor());
        }
        initDataStore();
    }

    public void finalize() throws Throwable {
        super.finalize();
    }

    private void initDataStore() {
        writer.reload();

        // overwrite the cseList with empty info
        InstanceIdentifier<Onem2mCseList> iidCseList = InstanceIdentifier.builder(Onem2mCseList.class).build();
        Onem2mCseList cseList = new Onem2mCseListBuilder().setOnem2mCse(Collections.<Onem2mCse>emptyList()).build();

        // overwrite the resource tree with empty information
        InstanceIdentifier<Onem2mResourceTree> iidTreeList = InstanceIdentifier.builder(Onem2mResourceTree.class).build();
        Onem2mResourceTree tree = new Onem2mResourceTreeBuilder().setOnem2mResource(Collections.<Onem2mResource>emptyList()).build();

        writer.update(iidCseList, cseList, LogicalDatastoreType.CONFIGURATION);
        writer.update(iidTreeList, tree, LogicalDatastoreType.CONFIGURATION);

        writer.close();
    }

    /**
     * generateResourceId: mdsal needs to generate id's for the purpose of sharding ... contentInstances are ultimately
     * created for each device.  Each device will ultimately have a container whose id will be used to decide which
     * shard will be used.  The CI's for the container will all go in the same shard as its parent container. This will
     * generally ensure that sensor updates per device are pipelined in order on the same shard.
     *
     * @param parentResourceId
     * @param resourceType
     * @param iotdmInstance
     * @return unique resource id
     */
    @Override
    public String generateResourceId(String parentResourceId,
                                     Integer resourceType,
                                     Integer iotdmInstance) {

        return factory.generateResourceId(parentResourceId, resourceType, iotdmInstance);
    }

    @Override
    public boolean createCseByName(String name, String resourceId) {
        boolean status = true;
        crudMonitor.get(0).enter();
        try {
            writer.reload();
            Onem2mCse onem2mCse = new Onem2mCseBuilder()
                    .setKey(new Onem2mCseKey(name))
                    .setName(name)
                    .setResourceId(resourceId)
                    .build();

            Onem2mCseKey key = onem2mCse.getKey();
            InstanceIdentifier<Onem2mCse> iid = InstanceIdentifier.create(Onem2mCseList.class)
                    .child(Onem2mCse.class, key);

            writer.create(iid, onem2mCse, dsType);

        } catch (Exception e) {
            LOG.error("exception : {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(0).leave();
            return status;
        }
    }

    @Override
    public boolean createResource(Object transaction, RequestPrimitive onem2mRequest, String parentResourceId, Integer resourceType) {
        int shard = factory.getShardFromResourceId(onem2mRequest.getResourceId()) % numShards;
        boolean status = true;
        crudMonitor.get(shard).enter();
        try {
            writer.reload();

            /**
             * Initialize the resource
             */
            Onem2mResourceKey key = new Onem2mResourceKey(onem2mRequest.getResourceId());
            String jsonContent = onem2mRequest.getJsonResourceContentString();
            Onem2mResource onem2mResource = new Onem2mResourceBuilder()
                    .setKey(key)
                    .setResourceId(onem2mRequest.getResourceId())
                    .setName(onem2mRequest.getResourceName())
                    .setResourceType(resourceType.toString())
                    .setParentId(parentResourceId) // parent resource
                    .setParentTargetUri(onem2mRequest.getParentTargetUri())
                    .setResourceContentJsonString(jsonContent)
                    .build();

            InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mResource.class, onem2mResource.getKey());


            writer.create(iid, onem2mResource, dsType);

            Onem2mParentChildListKey parentChildListKey = new Onem2mParentChildListKey(onem2mRequest.getResourceId());
            Onem2mParentChildList onem2mParentChildList = new Onem2mParentChildListBuilder()
                    .setKey(parentChildListKey)
                    .setParentResourceId(onem2mRequest.getResourceId())
                    .setOnem2mParentChild(Collections.<Onem2mParentChild>emptyList()) // new resource has NO children
                    .build();

            InstanceIdentifier<Onem2mParentChildList> pciid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, onem2mParentChildList.getKey());

            writer.create(pciid, onem2mParentChildList, dsType);

            createParentChildLink(parentResourceId, onem2mRequest.getResourceName(), onem2mRequest.getResourceId());

        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(shard).leave();
            return status;
        }
    }

    @Override
    public boolean updateJsonResourceContentString(Object transaction, String resourceId, String jsonResourceContent) {
        int shard = factory.getShardFromResourceId(resourceId) % numShards;
        boolean status = true;
        crudMonitor.get(shard).enter();
        try {
            writer.reload();

            Onem2mResourceKey key = new Onem2mResourceKey(resourceId);
            Onem2mResource onem2mResource = new Onem2mResourceBuilder()
                    .setKey(key)
                    .setResourceContentJsonString(jsonResourceContent)
                    .build();

            InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mResource.class, onem2mResource.getKey());

            writer.update(iid, onem2mResource, dsType);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(shard).leave();
            return status;
        }
    }

    @Override
    public boolean moveParentChildLink(String resourceId, String childResourceName,
                                       String oldPrentResourceId, String newParentResourceId) {

        int shard = factory.getShardFromResourceId(newParentResourceId) % numShards;
        boolean status = true;
        crudMonitor.get(shard).enter();
        try {
            writer.reload();

            removeParentChildLink(oldPrentResourceId, childResourceName);
            createParentChildLink(newParentResourceId, childResourceName, resourceId);

        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(shard).leave();
            return status;
        }
    }

    private void createParentChildLink(String parentResourceId, String childName, String childResourceId) {
//        int shard = factory.getShardFromResourceId(parentResourceId) % numShards;
//        boolean status = true;
//        crudMonitor.get(shard).enter();
//        try {
//            writer.reload();
//
//            Onem2mParentChild onem2mParentChild = new Onem2mParentChildBuilder()
//                    .setKey(new Onem2mParentChildKey(childName))
//                    .setName(childName)
//                    .setResourceId(childResourceId)
//                    .build();
//
//            InstanceIdentifier<Onem2mParentChild> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
//                    .child(Onem2mParentChildList.class, new Onem2mParentChildListKey(parentResourceId))
//                    .child(Onem2mParentChild.class, onem2mParentChild.getKey());
//
//            writer.create(iid, onem2mParentChild, dsType);
//        } catch (Exception e) {
//            LOG.error("Exception {}", e.getMessage());
//            status = false;
//        } finally {
//            writer.close();
//            crudMonitor.get(shard).leave();
//            return status;
//        }

            Onem2mParentChild onem2mParentChild = new Onem2mParentChildBuilder()
                    .setKey(new Onem2mParentChildKey(childName))
                    .setName(childName)
                    .setResourceId(childResourceId)
                    .build();

            InstanceIdentifier<Onem2mParentChild> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, new Onem2mParentChildListKey(parentResourceId))
                    .child(Onem2mParentChild.class, onem2mParentChild.getKey());

            writer.create(iid, onem2mParentChild, dsType);

    }

    private void removeParentChildLink(String parentResourceId, String childResourceName) {
//        int shard = factory.getShardFromResourceId(parentResourceId) % numShards;
//        boolean status = true;
//        crudMonitor.get(shard).enter();
//        try {
//            writer.reload();

            Onem2mParentChildKey childKey = new Onem2mParentChildKey(childResourceName);
            InstanceIdentifier<Onem2mParentChild> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, new Onem2mParentChildListKey(parentResourceId))
                    .child(Onem2mParentChild.class, childKey);

            writer.delete(iid, dsType);
//        } catch (Exception e) {
//            LOG.error("Exception {}", e.getMessage());
//            status = false;
//        } finally {
//            writer.close();
//            crudMonitor.get(shard).leave();
//            return status;
//        }
    }

    @Override
    public boolean deleteResource(Object transaction, String resourceId, String parentResourceId, String resourceName) {
        int shard = factory.getShardFromResourceId(resourceId) % numShards;
        boolean status = true;
        crudMonitor.get(shard).enter();
        try {
            writer.reload();

            Onem2mResourceKey key = new Onem2mResourceKey(resourceId);
            InstanceIdentifier<Onem2mResource> iid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mResource.class, key);


            writer.delete(iid, dsType);

            Onem2mParentChildListKey parentChildListKey = new Onem2mParentChildListKey(resourceId);

            InstanceIdentifier<Onem2mParentChildList> pciid = InstanceIdentifier.create(Onem2mResourceTree.class)
                    .child(Onem2mParentChildList.class, parentChildListKey);

            writer.delete(pciid, dsType);

            removeParentChildLink(parentResourceId, resourceName);

        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(shard).leave();
            return status;
        }
    }

    @Override
    public boolean createAeIdToResourceIdMapping(String cseBaseName,
                                                 String aeId, String aeResourceId) {
        boolean status = true;
        crudMonitor.get(0).enter();
        try {
            writer.reload();

            Onem2mRegisteredAeIds registeredAe = new Onem2mRegisteredAeIdsBuilder()
                                                         .setKey(new Onem2mRegisteredAeIdsKey(aeId))
                                                         .setRegisteredAeId(aeId)
                                                         .setResourceId(aeResourceId)
                                                         .build();

            InstanceIdentifier<Onem2mRegisteredAeIds> iid =
                    InstanceIdentifier.create(Onem2mCseList.class)
                            .child(Onem2mCse.class, new Onem2mCseKey(cseBaseName))
                            .child(Onem2mRegisteredAeIds.class, registeredAe.getKey());

            writer.create(iid, registeredAe, dsType);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(0).leave();
            return status;
        }
    }

    @Override
    public boolean deleteAeIdToResourceIdMapping(String cseBaseName, String aeId) {
        boolean status = true;
        crudMonitor.get(0).enter();
        try {
            writer.reload();
            InstanceIdentifier<Onem2mRegisteredAeIds> iid =
                    InstanceIdentifier.create(Onem2mCseList.class)
                        .child(Onem2mCse.class, new Onem2mCseKey(cseBaseName))
                        .child(Onem2mRegisteredAeIds.class, new Onem2mRegisteredAeIdsKey(aeId));

            writer.delete(iid, dsType);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(0).leave();
            return status;
        }
    }

    @Override
    public boolean createRemoteCseIdToResourceIdMapping(String cseBaseName,
                                                        String remoteCseCseId, String remoteCseResourceId) {
        boolean status = true;
        crudMonitor.get(0).enter();
        try {
            writer.reload();

            Onem2mRegisteredRemoteCses registeredCse = new Onem2mRegisteredRemoteCsesBuilder()
                .setRegisteredCseId(remoteCseCseId)
                .setResourceId(remoteCseResourceId)
                .build();

            InstanceIdentifier<Onem2mRegisteredRemoteCses> iid =
                InstanceIdentifier.create(Onem2mCseList.class)
                                  .child(Onem2mCse.class, new Onem2mCseKey(cseBaseName))
                                  .child(Onem2mRegisteredRemoteCses.class, registeredCse.getKey());

            writer.create(iid, registeredCse, dsType);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(0).leave();
            return status;
        }
    }

    @Override
    public boolean deleteRemoteCseIdToResourceIdMapping(String cseBaseName, String remoteCseCseId) {
        boolean status = true;
        crudMonitor.get(0).enter();
        try {
            writer.reload();

            InstanceIdentifier<Onem2mRegisteredRemoteCses> iid =
                InstanceIdentifier.create(Onem2mCseList.class)
                          .child(Onem2mCse.class, new Onem2mCseKey(cseBaseName))
                          .child(Onem2mRegisteredRemoteCses.class, new Onem2mRegisteredRemoteCsesKey(remoteCseCseId));

            writer.delete(iid, dsType);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(0).leave();
            return status;
        }
    }

    @Override
    public void reInitializeDatastore() {
        writer.reload();

        // overwrite the cseList with empty info
        InstanceIdentifier<Onem2mCseList> iidCseList = InstanceIdentifier.builder(Onem2mCseList.class).build();
        Onem2mCseList cseList = new Onem2mCseListBuilder().setOnem2mCse(Collections.<Onem2mCse>emptyList()).build();

        // overwrite the resource tree with empty information
        InstanceIdentifier<Onem2mResourceTree> iidTreeList = InstanceIdentifier.builder(Onem2mResourceTree.class).build();
        Onem2mResourceTree tree = new Onem2mResourceTreeBuilder().setOnem2mResource(Collections.<Onem2mResource>emptyList()).build();

        writer.create(iidCseList, cseList, LogicalDatastoreType.CONFIGURATION);
        writer.create(iidCseList, cseList, LogicalDatastoreType.OPERATIONAL);
        writer.create(iidTreeList, tree, LogicalDatastoreType.CONFIGURATION);
        writer.create(iidTreeList, tree, LogicalDatastoreType.OPERATIONAL);
        writer.close();

        factory.initSystemStartIds();

    }

    @Override
    public boolean writeSystemStartId(int systemStartId) {
        boolean status = true;
        crudMonitor.get(0).enter();
        try {
            writer.reload();

            IotdmSpecificOperationalDataBuilder builder = new IotdmSpecificOperationalDataBuilder()
                .setSystemStartId((long) systemStartId);

            InstanceIdentifier<IotdmSpecificOperationalData> iid =
                InstanceIdentifier.builder(IotdmSpecificOperationalData.class).build();

            writer.create(iid, builder.build(), dsType);
        } catch (Exception e) {
            LOG.error("Exception {}", e.getMessage());
            status = false;
        } finally {
            writer.close();
            crudMonitor.get(0).leave();
            return status;
        }
    }

    @Override
    public void close() {
        writer.close();
    }

    @Override
    public Object startTransaction() {
        return null;
    }

    @Override
    public boolean endTransaction(Object transaction) {
        return true;
    }
}
