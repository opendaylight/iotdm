/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.utils;

import com.google.common.base.Optional;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.IotdmSpecificOperationalData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.IotdmSpecificOperationalDataBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides utilities to work with IoTDM's implementation specific
 * operational data.
 */
public final class IotdmOperationalData {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmOperationalData.class);

    /**
     * Uses DataBroker object to persistently store last used resourceId
     * @param resourceId Last used resourceId
     * @param dataBroker The DataBroker object
     */
    public static void storeLastResourceId(int resourceId, DataBroker dataBroker) {
        IotdmSpecificOperationalDataBuilder builder = new IotdmSpecificOperationalDataBuilder()
            .setLastResourceId((long) resourceId);

        InstanceIdentifier<IotdmSpecificOperationalData> iid =
            InstanceIdentifier.builder(IotdmSpecificOperationalData.class).build();

        WriteTransaction writeTransaction = dataBroker.newWriteOnlyTransaction();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, iid, builder.build(), true);
        try {
            writeTransaction.submit().checkedGet();
        } catch (Exception e) {
            LOG.error("Failed to write last resource ID ({}) to data store: {}", resourceId, e);
        }
    }

    /**
     * Uses DataBroker object to retrieve last used resourceId from MD-SAL
     * @param dataBroker The DataBroker object
     * @return The last resourceId value if stored, zero otherwise.
     */
    public static int getLastResourceId(DataBroker dataBroker) {
        int resourceId = 0;
        Optional<IotdmSpecificOperationalData> iotdmData = null;
        InstanceIdentifier<IotdmSpecificOperationalData> iid =
            InstanceIdentifier.builder(IotdmSpecificOperationalData.class).build();

        ReadTransaction readTransaction = dataBroker.newReadOnlyTransaction();
        try {
            Long storedResourceId = null;
            iotdmData = readTransaction.read(LogicalDatastoreType.CONFIGURATION, iid).checkedGet();
            if (iotdmData.isPresent()) {
                storedResourceId = iotdmData.get().getLastResourceId();
                if (null != storedResourceId) {
                    resourceId = storedResourceId.intValue();
                }
            }

            if (null == storedResourceId) {
                LOG.info("Last Resource ID value not stored");
            } else {
                LOG.info("Retrieved Last Resource ID value: {}", storedResourceId);
            }

        } catch (Exception e) {
            LOG.error("Failed to read Last Resource ID: {}", e);
            // TODO how to handle error ? maybe return -1 ?
        }

        return resourceId;
    }
}
