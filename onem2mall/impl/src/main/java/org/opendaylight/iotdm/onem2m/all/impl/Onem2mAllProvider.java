/*
 * Copyright © 2015 Cisco Systems, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.all.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mAllProvider implements BindingAwareProvider, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(Onem2mAllProvider.class);

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("Onem2mAllProvider Session Initiated");
    }

    @Override
    public void close() throws Exception {
        LOG.info("Onem2mAllProvider Closed");
    }

}
