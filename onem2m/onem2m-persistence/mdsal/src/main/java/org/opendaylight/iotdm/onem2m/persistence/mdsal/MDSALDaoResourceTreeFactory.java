/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.persistence.mdsal;

import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.core.database.dao.factory.DaoResourceTreeFactory;
import org.opendaylight.iotdm.onem2m.persistence.mdsal.read.MDSALResourceTreeReader;
import org.opendaylight.iotdm.onem2m.persistence.mdsal.write.MDSALResourceTreeWriter;
import org.opendaylight.iotdm.onem2m.persistence.mdsal.write.MDSALTransactionWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by gguliash on 5/20/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class MDSALDaoResourceTreeFactory implements DaoResourceTreeFactory {
    private final Logger LOG = LoggerFactory.getLogger(MDSALDaoResourceTreeFactory.class);
    private AtomicInteger nextId;
    private Integer systemStartId = 0;

    private DataBroker dataBroker;
    private int numShards = 1;

    public MDSALDaoResourceTreeFactory(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        initSystemStartIds();
    }

    @Override
    public DaoResourceTreeWriter getDaoResourceTreeWriter() {
        return new MDSALResourceTreeWriter(this, new MDSALTransactionWriter(this, dataBroker));
    }

    @Override
    public DaoResourceTreeReader getDaoResourceTreeReader() {
        return new MDSALResourceTreeReader(this, dataBroker);
    }

    @Override
    public String getName() { return "MDSALDaoResourceTreeFactory"; }

    @Override
    public void close() {
    }

    public void initSystemStartIds() {
        nextId = new AtomicInteger();
        systemStartId = this.getDaoResourceTreeReader().retrieveSystemStartId();
        this.getDaoResourceTreeWriter().writeSystemStartId(++systemStartId);
    }

    public String generateResourceId(String parentResourceId,
                                     String resourceType,
                                     Integer iotdmInstance) {

        int baseResourceId = nextId.incrementAndGet();

        String resourceIdSuffix = Integer.toString(baseResourceId, 36) +
                Integer.toString(systemStartId, 36) +
                Integer.toString(iotdmInstance, 36);

        if (resourceType.contentEquals(Onem2m.ResourceType.CONTENT_INSTANCE)) {
            // take the shard of the parent as all content instances for a container goto the same shard
            return parentResourceId.charAt(0) + resourceIdSuffix;
        } else if (resourceType.contentEquals(Onem2m.ResourceType.CSE_BASE)) {
            return "0" + resourceIdSuffix;
        }

        int shard = baseResourceId % numShards;
        String b36ShardId = Integer.toString(shard, 36);
        if (b36ShardId.length() != 1) {
            LOG.error("generateResourceId: max shards exceeded");
            return "0";
        }
        return b36ShardId + resourceIdSuffix;
    }

    public int getShardFromResourceId(String resourceId) {
        return (int) Integer.valueOf(resourceId.substring(0,1), 36);
    }
}
