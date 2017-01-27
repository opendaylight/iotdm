/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.protocols.coap.tx;

import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.EndpointManager;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.InMemoryPskStore;
import org.opendaylight.iotdm.onem2m.protocols.common.Onem2mProtocolTxChannel;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2mpluginmanager.rev161110.coaps.psk.config.CsePsk;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.security.config.dtls.certificates.config.TrustStoreConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev170116.coap.security.config.dtls.certificates.config.trust.store.config.TrustedCertificates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.List;

/**
 * Common COAP(S) client base class for COAP(S) notifier and routing plugins.
 */
public abstract class Onem2mCoapClient implements Onem2mProtocolTxChannel {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mCoapClient.class);

    protected String pluginName = "coap";
    protected DTLSConnector dtlsConnector = null;
    private final Onem2mCoapClientConfiguration configuration;

    public Onem2mCoapClient(Onem2mCoapClientConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws RuntimeException {

        if (null != configuration && configuration.isSecureConnection()) {
            if (null == configuration.getSecureConnectionConfig()) {
                throw new IllegalArgumentException("Configured secure connection without config passed");
            }

            if (configuration.isUsePsk()) {
                this.initializeDtlsPsk(configuration.getSecureConnectionConfig().getDtlsPskRemoteCse().getCsePsk());
            } else {
                TrustStoreConfig tConfig =
                        configuration.getSecureConnectionConfig().getDtlsCertificatesConfig().getTrustStoreConfig();
                this.initializeDtls(tConfig.getTrustStoreFile(), tConfig.getTrustStorePassword(),
                                    tConfig.getTrustedCertificates());
            }

            this.pluginName = "coaps";
        } else {
            this.pluginName = "coap";
        }
    }

    @Override
    public void close() {
        // nothing to do
    }

    // This method is common with Java IoTDM client
    // TODO: Think about keeping the Java IoTDM client in the same repository
    // TODO: with IoTDM
    private void initializeDtlsPsk(List<CsePsk> pskList) {
        if (null == pskList) {
            throw new IllegalArgumentException("No PSK list passed");
        }

        //Set up DTLS PSK
        DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
        builder.setClientOnly();

        InMemoryPskStore pskStore = new InMemoryPskStore();
        for(CsePsk entry : pskList) {
            pskStore.setKey(entry.getCseId(), entry.getPsk().getBytes());
        }

        builder.setPskStore(pskStore);
        builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_PSK_WITH_AES_128_CCM_8,
                                                           CipherSuite.TLS_PSK_WITH_AES_128_CBC_SHA256});

        dtlsConnector = new DTLSConnector(builder.build());
        EndpointManager.getEndpointManager().setDefaultSecureEndpoint(
                new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard()));
    }

    // This method is common with Java IoTDM client
    // TODO: Think about keeping the Java IoTDM client in the same repository
    // TODO: with IoTDM
    private void initializeDtls(final String trustStore, final String trustStorePassword,
                                final List<TrustedCertificates> trustedCertificates) {
        if (trustStore == null || trustStore.isEmpty() ||
            trustStorePassword == null || trustStorePassword.isEmpty() ||
            null == trustedCertificates) {
            throw new IllegalArgumentException("Invalid arguments passed");
        }
        //Set up DTLS
        try {
            // load trust store
            KeyStore trust = KeyStore.getInstance("JKS");
            InputStream inTrust = new FileInputStream(trustStore);
            trust.load(inTrust, trustStorePassword.toCharArray());

            // You can load multiple certificates if needed
            Certificate[] certs = new Certificate[trustedCertificates.size()];
            int i = 0;
            for (TrustedCertificates entry : trustedCertificates) {
                certs[i] = trust.getCertificate(entry.getCertificateName());
                i++;
            }

            DtlsConnectorConfig.Builder builder = new DtlsConnectorConfig.Builder(new InetSocketAddress(0));
            builder.setClientOnly();

            builder.setSupportedCipherSuites(new CipherSuite[]{CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,
                                                               CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});
            builder.setTrustStore(certs);

            dtlsConnector = new DTLSConnector(builder.build());
            EndpointManager.getEndpointManager().setDefaultSecureEndpoint(
                    new CoapEndpoint(dtlsConnector, NetworkConfig.getStandard()));
        } catch (GeneralSecurityException | IOException e) {
            System.err.println("Could not load the keystore");
            e.printStackTrace();
        }
    }

    /**
     * Executes given coap request.
     * @param rx Coap request including all data to be sent.
     * @throws IOException ioexception
     */
    public void send(Request rx) throws IOException {
        rx.send();
    }
}
