/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.coap;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;
import org.opendaylight.iotdm.onem2m.commchannels.common.Onem2mKeyStoreFileConfig;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.channels.rx.IotdmRxPluginsRegistryReadOnly;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.commchannels.coap.rev170519.iotdm.plugin.manager.communication.channels.output.iotdm.communication.channel.protocols.iotdm.communication.channel.addresses.iotdm.communication.channel.ports.channel.data.channel.configuration.channel.specific.configuration.Onem2mCoapsChannelConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.commchannels.coap.rev170519.coaps.channel.config.definition.CoapsChannelConfigOptions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.commchannels.coap.rev170519.coaps.channel.config.definition.coaps.channel.config.options.CoapsKeystoreConfigOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.commchannels.coap.rev170519.coaps.channel.config.definition.coaps.channel.config.options.CoapsPskConfigOptionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.commchannels.coap.rev170519.coaps.psk.config.CsePsk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.commchannels.coap.rev170519.coaps.psk.config.CsePskBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.plugininfra.pluginmanager.rev161110.iotdm.communication.channel.data.definition.channel.data.ChannelConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class extends implementation of CoAP server with support of
 * DTLS using certificates or preshared keys.
 */
public class IotdmCoapsPluginServerRx extends IotdmCoapBaseRxChannel<IotdmCoapsPluginServerRx.CoapsConfig> {
    private static final Logger LOG = LoggerFactory.getLogger(IotdmCoapsPluginServerRx.class);
    public static final String ProtocolCoAPS = "coaps";

    public static class CoapsConfig extends Onem2mKeyStoreFileConfig {
        protected boolean usesPks;
        protected String keyAlias;
        protected Map<String, String> presharedKeys;

        protected CoapsConfig() {
            super();
        }

        public boolean getUsesPsk() {
            return this.usesPks;
        }

        public Map<String, String> getPresharedKeys() {
            return this.presharedKeys;
        }

        public String getKeyAlias() {
            return this.keyAlias;
        }

        protected boolean getDefaultConfigAvailable() { return super.getDefaultConfigAvailable(); }
        protected void setDefaultConfigAvailable(boolean available) {
            super.setDefaultConfigAvailable(available);
        }

        protected boolean compareConfig(CoapsConfig config) {
            if (null == config) {
                return false;
            }

            if (this.usesDefaultConfig != config.usesDefaultConfig) {
                return false;
            }

            if (this.getUsesPsk() != config.getUsesPsk()) {
                return false;
            }

            if (this.usesDefaultConfig) {
                // we can return true because we have only one default config
                return true;
            }

            if (this.getUsesPsk()) {
                if (config.getPresharedKeys().size() != this.getPresharedKeys().size()) {
                    return false;
                }

                for(Map.Entry<String, String> entry : this.getPresharedKeys().entrySet()) {
                    if (! config.getPresharedKeys().containsKey(entry.getKey())) {
                        return false;
                    }

                    if (! config.getPresharedKeys().get(entry.getKey()).equals(entry.getValue())) {
                        return false;
                    }
                }

                return true;
            } else {
                if (! this.getKeyAlias().equals(config.getKeyAlias())) {
                    return false;
                }
                return super.compareConfig(config);
            }
        }

        @Override
        public StringBuilder getConfigString() {
            StringBuilder builder = null;
            if (this.usesPks) {
                builder = new StringBuilder()
                    .append(this.usesDefaultConfig ? "(Default) " : "")
                    .append("PresharedKeys: [");

                boolean isFirst = true;
                for (Map.Entry<String, String> entry : this.presharedKeys.entrySet()) {
                    if (! isFirst) {
                        builder.append(", ");
                    } else {
                        isFirst = false;
                    }

                    builder
                            .append(entry.getKey())
                            .append(":")
                            .append("-"); // Don't send passwords due to security
                }

                builder.append("]");
            } else {
                builder = super.getConfigString()
                                  .append(", KeyAlias: ")
                                  .append(this.keyAlias);
            }

            return builder;
        }
    }

    public IotdmCoapsPluginServerRx(String ipAddress, int port, IotdmRxPluginsRegistryReadOnly registry,
                                    CoapsConfig config, boolean usesDefaultCfg) {
        super(ipAddress, port, registry, config, usesDefaultCfg);
    }


    @Override
    public boolean compareConfig(CoapsConfig config) {
        return this.configuration.compareConfig(config);
    }

    @Override
    public String getProtocol() {
        return "coaps";
    }

    @Override
    public boolean init() {
        // TODO: think about enabling DTLS debugging from configuration
//        ScandiumLogger.initialize();
//        ScandiumLogger.setLevel(Level.FINE);

        if (this.configuration.getUsesDefaultConfig() && (! this.configuration.getDefaultConfigAvailable())) {
            LOG.warn("Unable to start CoAPS sever, no default configuration available");
            this.setState(ChannelState.WAITINGDEFAULT);
            return true;
        }

        if (this.configuration.getUsesPsk()) {
            onem2mCoapBaseHandler = new Onem2mCoapsPskHandler(port, this.configuration);
        } else {
            onem2mCoapBaseHandler = new Onem2mCoapsCertificatesHandler(port, this.configuration);
        }

        onem2mCoapBaseHandler.addEndpoints();

        try {
            onem2mCoapBaseHandler.start();
            LOG.info("Started CoAPS Server: on port: {}", port);
            if (this.configuration.getUsesDefaultConfig()) {
                this.setState(ChannelState.RUNNINGDEFAULT);
            } else {
                this.setState(ChannelState.RUNNING);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Failed to start CoAPS server: {}", e.toString());
            this.setState(ChannelState.INITFAILED);
        }
        return true;
    }

    @Override
    public void close() {
        if (null == onem2mCoapBaseHandler) {
            return;
        }
        try {
            onem2mCoapBaseHandler.stop();
            onem2mCoapBaseHandler.destroy();
            LOG.info("Stopped CoAPS Server: on port: {}", port);
        } catch (Exception e) {
            LOG.error("Failed to stop CoAPS server: {}", e.toString());
        }
    }

    @Override
    public ChannelConfiguration getChannelConfig() {
        Onem2mCoapsChannelConfigBuilder builder = new Onem2mCoapsChannelConfigBuilder();
        CoapsChannelConfigOptions options = null;

        if (this.configuration.usesPks) {
            List<CsePsk> pskList = new LinkedList<>();
            for (Map.Entry<String, String> entry : this.configuration.getPresharedKeys().entrySet()) {
                CsePskBuilder pskBuilder = new CsePskBuilder()
                    .setCseId(entry.getKey())
                    .setPsk(entry.getValue()); // TODO exposure, think about this
                pskList.add(pskBuilder.build());
            }
            CoapsPskConfigOptionsBuilder optBuilder = new CoapsPskConfigOptionsBuilder()
                    .setCsePsk(pskList);
            options = optBuilder.build();
        } else {
            CoapsKeystoreConfigOptionsBuilder optBuilder = new CoapsKeystoreConfigOptionsBuilder()
                    .setKeyAlias(this.configuration.getKeyAlias())
                    .setKeyManagerPassword(this.configuration.getKeyManagerPassword()) // TODO exposure
                    .setKeyStoreFile(this.configuration.getKeyStoreFile())
                    .setKeyStorePassword(this.configuration.getKeyStorePassword()); // TODO exposure
            options = optBuilder.build();
        }

        builder.setCoapsChannelConfigOptions(options);
        return this.getChannelConfigBuilder().setChannelSpecificConfiguration(builder.build()).build();
    }

    protected class Onem2mCoapsCertificatesHandler extends Onem2mCoapBaseHandler {
        private final CoapsConfig config;
        private final int port;
        public Onem2mCoapsCertificatesHandler(int port, CoapsConfig config) {
            // Do not call constructor of superclass here !!!
            // Otherwise DTLS will not work in subclasses implementing CoAPS !!!
            this.port = port;
            if (config.getUsesPsk()) {
                throw new IllegalArgumentException("Configuration with PSK passed to handler using certificates");
            }
            this.config = config;
        }

        @Override
        protected void addEndpoints() {
            // load the key store
            KeyStore keyStore = null;
            try {
                keyStore = KeyStore.getInstance("JKS");
                InputStream in = new FileInputStream(config.getKeyStoreFile());
                keyStore.load(in, config.getKeyStorePassword().toCharArray());
            } catch (Exception e) {
                LOG.error("Failed to load KeyStore: {}", e);
                return;
            }

            DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(new InetSocketAddress(port));
            dtlsConfig.setClientAuthenticationRequired(false);

            dtlsConfig.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                                                                  CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});

            try {
                dtlsConfig.setIdentity((PrivateKey) keyStore.getKey(config.getKeyAlias(),
                                                                    config.getKeyManagerPassword().toCharArray()),
                                       keyStore.getCertificateChain(config.getKeyAlias()), true);
            } catch (Exception e) {
                LOG.error("Failed to configure DTLS: {}", e);
                return;
            }

            DTLSConnector connector = new DTLSConnector(dtlsConfig.build());
            this.addEndpoint(new CoapEndpoint(connector, NetworkConfig.getStandard()));

            // TODO: think about enabling debugging of messages
            // add special interceptor for message traces
//            for (Endpoint ep : this.getEndpoints()) {
//                ep.addInterceptor(new MessageTracer());
//            }
        }
    }

    protected class Onem2mCoapsPskHandler extends Onem2mCoapBaseHandler {
        private final CoapsConfig config;
        protected final int port;

        public Onem2mCoapsPskHandler(int port, CoapsConfig config) {
            // Do not call constructor of superclass here !!!
            // Otherwise DTLS will not work in subclasses implementing CoAPS !!!
            this.port = port;
            if (! config.getUsesPsk()) {
                throw new IllegalArgumentException("Configuration without PSK passed to handler using PSK");
            }
            this.config = config;
        }

        @Override
        protected void addEndpoints() {
            DtlsConnectorConfig.Builder dtlsConfig = new DtlsConnectorConfig.Builder(new InetSocketAddress(port));
            dtlsConfig.setClientAuthenticationRequired(false);

            dtlsConfig.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8,
                                                                  CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256});

            InMemoryPskStore pskStore = new InMemoryPskStore();
            for(Map.Entry<String, String> entry : config.getPresharedKeys().entrySet()) {
                pskStore.setKey(entry.getKey(), entry.getValue().getBytes());
            }

            dtlsConfig.setPskStore(pskStore);

            DTLSConnector connector = new DTLSConnector(dtlsConfig.build());
            this.addEndpoint(new CoapEndpoint(connector, NetworkConfig.getStandard()));

            // TODO: think about enabling debugging of messages
            // add special interceptor for message traces
//            for (Endpoint ep : this.getEndpoints()) {
//                ep.addInterceptor(new MessageTracer());
//            }
        }
    }
}
