/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.tx.notificaction;

import javax.annotation.Nonnull;
import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierPlugin;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxHandler;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClient;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.Onem2mHttpClientConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Onem2mHttpNotifierPlugin extends Onem2mHttpClient implements Onem2mNotifierPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpNotifierPlugin.class);

    protected final Onem2mProtocolTxHandler onem2mHandler;
    protected final Onem2mHttpNotifierRequestAbstractFactory requestFactory;

    public Onem2mHttpNotifierPlugin(@Nonnull final Onem2mProtocolTxHandler onem2mHandler,
                                    @Nonnull final Onem2mHttpNotifierRequestAbstractFactory requestFactory,
                                    @Nonnull final Onem2mHttpClientConfiguration configuration) {
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
     * HTTP notifications will be set out to subscribers interested in resources from the tree where they have have hung
     * onem2m subscription resources
     * @param url where do i send this onem2m notify message
     * @param payload contents of the notification
     */
    @Override
    public void sendNotification(String url, String payload, String cseBaseId) {
        this.onem2mHandler.handle(
                requestFactory.createHttpNotifierRequest(url, payload, cseBaseId, this));
    }

}
