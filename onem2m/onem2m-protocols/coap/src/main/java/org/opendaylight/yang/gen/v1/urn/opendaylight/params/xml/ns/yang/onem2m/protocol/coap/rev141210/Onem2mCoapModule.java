package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.iotdm.onem2m.protocols.coap.Onem2mCoapProvider;
import org.opendaylight.iotdm.onem2m.protocols.coap.Onem2mCoapSecureConnectionConfig;
import org.opendaylight.iotdm.onem2m.protocols.coap.rx.Onem2mCoapBaseIotdmPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification.Onem2mCoapNotifierPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing.Onem2mCoapRouterPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

import javax.annotation.Nonnull;

public class Onem2mCoapModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210.AbstractOnem2mCoapModule {
    private Onem2mCoapBaseIotdmPluginConfig serverCfg = null;
    private Onem2mCoapNotifierPluginConfig notifierCfg = null;
    private Onem2mCoapRouterPluginConfig routerCfg = null;
    private Onem2mCoapSecureConnectionConfig secureConnectionConfig = null;

    public Onem2mCoapModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mCoapModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210.Onem2mCoapModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    public static void onem2mCoapConfigValidationGeneric(@Nonnull final Onem2mCoapBaseIotdmPluginConfig serverCfg,
                                                         final Onem2mCoapNotifierPluginConfig notifierCfg,
                                                         final Onem2mCoapRouterPluginConfig routerCfg,
                                                         final Onem2mCoapSecureConnectionConfig secCfg) {
        // Server configuration validation
        JmxAttributeValidationException.checkNotNull(serverCfg,
                "Server configuration not provided",
                serverConfigJmxAttribute);

        JmxAttributeValidationException.checkCondition((serverCfg.getServerPort() > 0 &&
                        serverCfg.getServerPort() < 0xFFFF),
                "Invalid port number " + serverCfg.getServerPort(),
                serverConfigJmxAttribute);

        if (null != notifierCfg && null != notifierCfg.getSecureConnection() && notifierCfg.getSecureConnection()) {
            JmxAttributeValidationException.checkNotNull(
                    secCfg,
                    "Secure connection enabled for notifier but parameters are not configured",
                    notifierPluginConfigJmxAttribute);

            if (null == notifierCfg.getUsePresharedKeys() || false == notifierCfg.getUsePresharedKeys()) {
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsCertificatesConfig(),
                        "Secure connection enabled for notifier but TrustStore is not configured",
                        notifierPluginConfigJmxAttribute);
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsCertificatesConfig().getTrustStoreConfig(),
                        "Secure connection enabled for notifier but TrustStore is not configured",
                        notifierPluginConfigJmxAttribute);
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsCertificatesConfig().getTrustStoreConfig().getTrustedCertificates(),
                        "Trust store configuration without list of trusted certificates",
                        notifierPluginConfigJmxAttribute);
            } else {
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsPskRemoteCse(),
                        "Secure connection using PSK enabled for notifier but PSK is not configured",
                        notifierPluginConfigJmxAttribute);
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsPskRemoteCse().getCsePsk(),
                        "PSK list not configured",
                        notifierPluginConfigJmxAttribute);
            }
        }

        if (null != routerCfg && null != routerCfg.getSecureConnection() && routerCfg.getSecureConnection()) {
            JmxAttributeValidationException.checkNotNull(
                    secCfg,
                    "Secure connection enabled for notifier but parameters are not configured",
                    routerPluginConfigJmxAttribute);

            if (null == routerCfg.getUsePresharedKeys() || false == routerCfg.getUsePresharedKeys()) {
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsCertificatesConfig(),
                        "Secure connection enabled for router but TrustStore is not configured",
                        routerPluginConfigJmxAttribute);
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsCertificatesConfig().getTrustStoreConfig(),
                        "Secure connection enabled for router but TrustStore is not configured",
                        routerPluginConfigJmxAttribute);
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsCertificatesConfig().getTrustStoreConfig().getTrustedCertificates(),
                        "Trust store configuration without list of trusted certificates",
                        routerPluginConfigJmxAttribute);
            } else {
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsPskRemoteCse(),
                        "Secure connection using PSK enabled for router but PSK is not configured",
                        routerPluginConfigJmxAttribute);
                JmxAttributeValidationException.checkNotNull(
                        secCfg.getDtlsPskRemoteCse().getCsePsk(),
                        "PSK list not configured",
                        routerPluginConfigJmxAttribute);
            }
        }
    }

    public static void onem2mCoapConfigValidationBaseSpecific(@Nonnull final Onem2mCoapBaseIotdmPluginConfig serverCfg) {
        JmxAttributeValidationException.checkCondition(serverCfg.getServerSecurityLevel() != SecurityLevel.L2,
                "Security level L2 is not supported by this module",
                serverConfigJmxAttribute);
    }

    @Override
    public void customValidation() {
        serverCfg = new Onem2mCoapBaseIotdmPluginConfig(getServerConfig());
        notifierCfg = new Onem2mCoapNotifierPluginConfig(getNotifierPluginConfig());
        routerCfg = new Onem2mCoapRouterPluginConfig(getRouterPluginConfig());
        secureConnectionConfig = new Onem2mCoapSecureConnectionConfig(getCoapsConfig());
        onem2mCoapConfigValidationGeneric(serverCfg, notifierCfg, routerCfg, secureConnectionConfig);
        onem2mCoapConfigValidationBaseSpecific(serverCfg);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Onem2mCoapProvider provider = new Onem2mCoapProvider(serverCfg, notifierCfg, routerCfg, secureConnectionConfig);
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
