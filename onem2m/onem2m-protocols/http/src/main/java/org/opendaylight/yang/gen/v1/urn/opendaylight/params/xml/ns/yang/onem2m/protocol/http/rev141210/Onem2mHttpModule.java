package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.iotdm.onem2m.protocols.http.Onem2mHttpProvider;
import org.opendaylight.iotdm.onem2m.protocols.http.Onem2mHttpSecureConnectionConfig;
import org.opendaylight.iotdm.onem2m.protocols.http.rx.Onem2mHttpBaseIotdmPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.notificaction.Onem2mHttpNotifierPluginConfig;
import org.opendaylight.iotdm.onem2m.protocols.http.tx.routing.Onem2mHttpRouterPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

public class Onem2mHttpModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.AbstractOnem2mHttpModule {
    private Onem2mHttpBaseIotdmPluginConfig serverCfg = null;
    private Onem2mHttpNotifierPluginConfig notifierCfg = null;
    private Onem2mHttpRouterPluginConfig routerCfg = null;
    private Onem2mHttpSecureConnectionConfig secCfg = null;

    public Onem2mHttpModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mHttpModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.http.rev141210.Onem2mHttpModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    public static void onem2mHttpConfigValidationGeneric (@Nonnull final Onem2mHttpBaseIotdmPluginConfig serverCfg,
                                                          final Onem2mHttpNotifierPluginConfig notifierCfg,
                                                          final Onem2mHttpRouterPluginConfig routerCfg,
                                                          final Onem2mHttpSecureConnectionConfig secCfg) {
        // Server configuration validation
        JmxAttributeValidationException.checkNotNull(serverCfg,
                                                     "Server configuration not provided",
                                                     serverConfigJmxAttribute);

        JmxAttributeValidationException.checkCondition((serverCfg.getServerPort() > 0 &&
                                                        serverCfg.getServerPort() < 0xFFFF),
                                                       "Invalid port number " + serverCfg.getServerPort(),
                                                       serverConfigJmxAttribute);

        if (null != serverCfg.getSecureConnection() && serverCfg.getSecureConnection()) {
            JmxAttributeValidationException.checkNotNull(secCfg, "Secure connection enabled for server but " +
                                                                 "parameters are not configured",
                                                         httpsConfigJmxAttribute);
            JmxAttributeValidationException.checkNotNull(secCfg.getKeyStoreConfig(),
                                                         "Secure connection enabled for server but KeyStore is " +
                                                         "not configured",
                                                         httpsConfigJmxAttribute);
        }

        if (null != notifierCfg && null != notifierCfg.getSecureConnection() && notifierCfg.getSecureConnection()) {
            JmxAttributeValidationException.checkNotNull(secCfg, "Secure connection enabled for notifier but " +
                                                                 "parameters are not configured",
                                                         httpsConfigJmxAttribute);
            JmxAttributeValidationException.checkNotNull(secCfg.getTrustStoreConfig(),
                                                         "Secure connection enabled for notifier but TrustStore is " +
                                                         "not configured",
                                                         httpsConfigJmxAttribute);
            // TODO remove this for jetty 9 and upper versions
            JmxAttributeValidationException.checkNotNull(secCfg.getKeyStoreConfig(),
                                                         "Secure connection enabled for notifier but KeyStore is " +
                                                         "not configured",
                                                         httpsConfigJmxAttribute);
        }

        if (null != routerCfg && null != routerCfg.getSecureConnection() && routerCfg.getSecureConnection()) {
            JmxAttributeValidationException.checkNotNull(secCfg, "Secure connection enabled for router plugin but " +
                                                                 "parameters are not configured",
                                                         httpsConfigJmxAttribute);
            JmxAttributeValidationException.checkNotNull(secCfg.getTrustStoreConfig(),
                                                         "Secure connection enabled for router plugin but TrustStore" +
                                                         "is not configured",
                                                         httpsConfigJmxAttribute);
            // TODO remove this for jetty 9 and upper versions
            JmxAttributeValidationException.checkNotNull(secCfg.getKeyStoreConfig(),
                                                         "Secure connection enabled for router plugin but KeyStore" +
                                                         "is not configured",
                                                         httpsConfigJmxAttribute);
        }
    }

    public static void onem2mHttpConfigValidationBaseSpecific(@Nonnull final Onem2mHttpBaseIotdmPluginConfig serverCfg,
                                                              @Nonnull final Onem2mHttpNotifierPluginConfig notifierCfg,
                                                              @Nonnull final Onem2mHttpRouterPluginConfig routerCfg) {
        JmxAttributeValidationException.checkCondition(serverCfg.getServerSecurityLevel() != SecurityLevel.L2,
                                                       "Security level L2 is not supported by this module",
                                                       serverConfigJmxAttribute);
    }

    @Override
    public void customValidation() {
        serverCfg = new Onem2mHttpBaseIotdmPluginConfig(getServerConfig());
        notifierCfg = new Onem2mHttpNotifierPluginConfig(getNotifierPluginConfig());
        routerCfg = new Onem2mHttpRouterPluginConfig(getRouterPluginConfig());
        secCfg = new Onem2mHttpSecureConnectionConfig(getHttpsConfig());
        onem2mHttpConfigValidationGeneric(serverCfg, notifierCfg, routerCfg, secCfg);
        onem2mHttpConfigValidationBaseSpecific(serverCfg, notifierCfg, routerCfg);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Onem2mHttpProvider provider = new Onem2mHttpProvider(serverCfg, notifierCfg, routerCfg, secCfg);
        getBrokerDependency().registerProvider(provider);
        return provider;
    }
}
