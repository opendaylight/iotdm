package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.iotdm.onem2m.protocols.coap.Onem2mCoapProvider;
import org.opendaylight.iotdm.onem2m.protocols.coap.rx.Onem2mCoapBaseIotdmPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.notification.Onem2mCoapNotifierPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.coap.tx.routing.Onem2mCoapRouterPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

import javax.annotation.Nonnull;

public class Onem2mCoapModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210.AbstractOnem2mCoapModule {
    private Onem2mCoapBaseIotdmPluginConfig serverCfg = null;
    private Onem2mCoapNotifierPluginConfig notifierCfg = null;
    private Onem2mCoapRouterPluginConfig routerCfg = null;

    public Onem2mCoapModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mCoapModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.coap.rev141210.Onem2mCoapModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    public static void onem2mHttpConfigValidationGeneric (@Nonnull final Onem2mCoapBaseIotdmPluginConfig serverCfg, final Onem2mCoapNotifierPluginConfig notifierCfg) {
        // Server configuration validation
        JmxAttributeValidationException.checkNotNull(serverCfg,
                "Server configuration not provided",
                serverConfigJmxAttribute);

        JmxAttributeValidationException.checkCondition((serverCfg.getServerPort() > 0 &&
                        serverCfg.getServerPort() < 0xFFFF),
                "Invalid port number " + serverCfg.getServerPort(),
                serverConfigJmxAttribute);

//        if (null != notifierCfg && null != notifierCfg.getSecureConnection() && notifierCfg.getSecureConnection()) {
//            JmxAttributeValidationException.checkNotNull(secCfg, "Secure connection enabled for notifier but " +
//                            "parameters are not configured",
//                    notifierPluginConfigJmxAttribute);
//            JmxAttributeValidationException.checkNotNull(secCfg.getTrustStoreConfig(),
//                    "Secure connection enabled for notifier but TrustStore is " +
//                            "not configured",
//                    notifierPluginConfigJmxAttribute);
//            // TODO remove this for jetty 9 and upper versions
//            JmxAttributeValidationException.checkNotNull(secCfg.getKeyStoreConfig(),
//                    "Secure connection enabled for notifier but KeyStore is " +
//                            "not configured",
//                    notifierPluginConfigJmxAttribute);
//        }
    }

    public static void onem2mHttpConfigValidationBaseSpecific(@Nonnull final Onem2mCoapBaseIotdmPluginConfig serverCfg) {
        JmxAttributeValidationException.checkCondition(serverCfg.getServerSecurityLevel() != SecurityLevel.L2,
                "Security level L2 is not supported by this module",
                serverConfigJmxAttribute);
    }

    @Override
    public void customValidation() {
        serverCfg = new Onem2mCoapBaseIotdmPluginConfig(getServerConfig());
        notifierCfg = new Onem2mCoapNotifierPluginConfig(getNotifierPluginConfig());
        routerCfg = new Onem2mCoapRouterPluginConfig(getRouterPluginConfig());
        onem2mHttpConfigValidationGeneric(serverCfg, notifierCfg); //TODO:add routerConfig and edit validation when needed
        onem2mHttpConfigValidationBaseSpecific(serverCfg);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Onem2mCoapProvider provider = new Onem2mCoapProvider(serverCfg, notifierCfg, routerCfg);
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
