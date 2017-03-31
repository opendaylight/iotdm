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

    private static final String IDDELIMITER = "-"; //update also regex
    private static final String IDREGEX = ".+-.+-.+-.+";

    private static final int IDRADIX = 36;
    private static final int IDSHARDPOSITION = 0;

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
                                     Integer resourceType,
                                     Integer iotdmInstance) {

        int baseResourceId = nextId.incrementAndGet();
        StringBuilder builder = new StringBuilder();

        switch(resourceType) {
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                // take the shard of the parent as all content instances for a container goto the same shard
                builder
                    .append(getShardFromResourceId(parentResourceId))
                    .append(IDDELIMITER);
                break;

            case Onem2m.ResourceType.CSE_BASE:
                // just use + with string constants
                builder.append("0" + IDDELIMITER);
                break;

            default:
                int shard = baseResourceId % numShards;
                String b36ShardId = Integer.toString(shard, IDRADIX);
                if (b36ShardId.length() != 1) {
                    LOG.error("generateResourceId: max shards exceeded");
                    return "0";
                }

                builder
                    .append(b36ShardId)
                    .append(IDDELIMITER);
        }

        builder
            .append(Integer.toString(baseResourceId, IDRADIX))
            .append(IDDELIMITER)
            .append(Integer.toString(systemStartId, IDRADIX))
            .append(IDDELIMITER)
            .append(Integer.toString(iotdmInstance, IDRADIX));

        return builder.toString();
    }

    public int getShardFromResourceId(String resourceId) {
        if (! resourceId.matches(IDREGEX)) {
            LOG.error("Invalid resourceId format: {}", resourceId);
            throw new IllegalArgumentException("Invalid resourceId format: " + resourceId);
        }
        return (int) Integer.valueOf(resourceId.split(IDDELIMITER)[IDSHARDPOSITION], IDRADIX);
    }
}
