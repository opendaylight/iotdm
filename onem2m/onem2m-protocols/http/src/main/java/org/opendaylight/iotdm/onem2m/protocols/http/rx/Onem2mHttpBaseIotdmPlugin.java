/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.http.rx;

import org.opendaylight.iotdm.onem2m.plugins.*;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxHandler;
import org.opendaylight.iotdm.onem2m.protocols.http.Onem2mHttpSecureConnectionConfig;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.routing.Onem2mHttpRouterPlugin;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.routing.Onem2mHttpRouterPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.Onem2mService;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolRxChannel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class Onem2mHttpBaseIotdmPlugin implements Onem2mProtocolRxChannel<Onem2mHttpBaseIotdmPluginConfig>,
                                                  AbstractIotDMPlugin<IotDMPluginHttpRequest,
                                                                      IotDMPluginHttpResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mHttpBaseIotdmPlugin.class);

    private final Onem2mProtocolRxHandler requestHandler;
    private final Onem2mHttpRxRequestAbstractFactory requestFactory;
    private final Onem2mService onem2mService;
    private final Onem2mHttpSecureConnectionConfig secConfig;

    private SecurityLevel securityLevel = SecurityLevel.L0;
    private ServerConfig currentConfig = null;

    public Onem2mHttpBaseIotdmPlugin(@Nonnull final Onem2mProtocolRxHandler requestHandler,
                                     @Nonnull final Onem2mHttpRxRequestAbstractFactory requestFactory,
                                     @Nonnull final Onem2mService onem2mService,
                                     final Onem2mHttpSecureConnectionConfig secConfig) {
        this.requestHandler = requestHandler;
        this.requestFactory = requestFactory;
        this.onem2mService = onem2mService;
        this.secConfig = secConfig;
    }

    @Override
    public String pluginName() {
        return "http(s)-base";
    }

    @Override
    public void init() {
        return;
    }

    @Override
    public void start(Onem2mHttpBaseIotdmPluginConfig configuration)
            throws IllegalArgumentException, RuntimeException {
        if (null == configuration) {
            throw new IllegalArgumentException("Starting Http base server without configuration");
        }

        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();

        if ((null != configuration.getSecureConnection()) && (true == configuration.getSecureConnection())) {
            if (null == this.secConfig || null == this.secConfig.getKeyStoreConfig()) {
                throw new IllegalArgumentException("No HTTPS KeyStore configuration set");
            }

            Onem2mHttpsPluginServer.HttpsServerConfiguration httpsConfig = null;
            if (null == this.secConfig.getKeyStoreConfig().getKeyManagerPassword()) {
                httpsConfig = new Onem2mHttpsPluginServer.HttpsServerConfiguration(
                                this.secConfig.getKeyStoreConfig().getKeyStoreFile(),
                                this.secConfig.getKeyStoreConfig().getKeyStorePassword());
            } else {
                httpsConfig = new Onem2mHttpsPluginServer.HttpsServerConfiguration(
                                this.secConfig.getKeyStoreConfig().getKeyStoreFile(),
                                this.secConfig.getKeyStoreConfig().getKeyStorePassword(),
                                this.secConfig.getKeyStoreConfig().getKeyManagerPassword());
            }
            mgr.registerPluginAtPort("https", this, configuration.getServerPort(), Onem2mPluginManager.Mode.Exclusive,
                                     httpsConfig);
        } else {
            mgr.registerPluginAtPort("http", this, configuration.getServerPort(), Onem2mPluginManager.Mode.Exclusive);

            LOG.info("Started HTTP Base IoTDM plugin at port: {}, security level: {}",
                     configuration.getServerPort(), configuration.getServerSecurityLevel());
        }

        this.currentConfig = configuration;
        this.securityLevel = configuration.getServerSecurityLevel();
    }

    @Override
    public void close() {
        Onem2mPluginManager mgr = Onem2mPluginManager.getInstance();
        mgr.deRegisterPlugin(this.pluginName());

        LOG.info("Closed HTTP Base IoTDM plugin at port: {}, security level: {}",
                 currentConfig.getServerPort(), currentConfig.getServerSecurityLevel());
    }

    @Override
    public void handle(IotDMPluginHttpRequest request, IotDMPluginHttpResponse response) {
        // TODO: how to remove the type casting ?
        Onem2mHttpRxRequest rxRequest =
                    requestFactory.createHttpRxRequest((request),
                                                       (response),
                                                       onem2mService, securityLevel);
        requestHandler.handleRequest(rxRequest);
    }
}
