/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification;

import javax.annotation.Nonnull;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierPlugin;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.Onem2mCoapClient;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.Onem2mCoapClientConfiguration;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxHandler;


public class Onem2mCoapNotifierPlugin extends Onem2mCoapClient implements Onem2mNotifierPlugin {
    protected final Onem2mProtocolTxHandler onem2mHandler;
    protected final Onem2mCoapNotifierRequestAbstractFactory requestFactory;

    public Onem2mCoapNotifierPlugin(@Nonnull final Onem2mProtocolTxHandler onem2mHandler,
                                    @Nonnull final Onem2mCoapNotifierRequestAbstractFactory requestFactory,
                                    @Nonnull final Onem2mCoapClientConfiguration configuration) {
        super(configuration);
        this.onem2mHandler = onem2mHandler;
        this.requestFactory = requestFactory;
    }

    /**
     * Implements the Onem2mNotifierPlugin interface
     */
    @Override
    public String getNotifierPluginName() {
        return this.pluginName;
    }

    /**
     * COAP notifications will be set out to subscribers interested in resources from the tree where they have have hung
     * onem2m subscription resources
     * @param url where do i send this onem2m notify message
     * @param payload contents of the notification
     */
    @Override
    public void sendNotification(String url, String payload, String cseBaseId) {
        this.onem2mHandler.handle(
                requestFactory.createCoapNotifierRequest(url, payload, cseBaseId, this));
    }

}
