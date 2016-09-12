/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.persistence.mdsal;


import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mPersistenceMdsalProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mPersistenceMdsalProvider.class);

    public Onem2mPersistenceMdsalProvider() {
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        DataBroker dataBroker = session.getSALService(DataBroker.class);
        Onem2mCoreProvider.getInstance().registerDaoPlugin(new MDSALDaoResourceTreeFactory(dataBroker));
        LOG.info("Onem2mPersistenceMdsalProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mPersistenceMdsalProvider Closed");
    }

}
