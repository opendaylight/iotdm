package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.websocket.rev141210;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.iotdm.onem2m.protocols.websocket.Onem2mWebsocketProvider;
import org.opendaylight.iotdm.onem2m.protocols.websocket.rx.Onem2mWebsocketIotdmPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

import java.util.Objects;

public class Onem2mWebsocketModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.websocket.rev141210.AbstractOnem2mWebsocketModule {
    private Onem2mWebsocketIotdmPluginConfig serverCfg;

    public Onem2mWebsocketModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mWebsocketModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.websocket.rev141210.Onem2mWebsocketModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        serverCfg = new Onem2mWebsocketIotdmPluginConfig(getServerConfig());
        // Server configuration validation
        JmxAttributeValidationException.checkNotNull(serverCfg,
                "Server configuration not provided",
                serverConfigJmxAttribute);

        JmxAttributeValidationException.checkCondition((serverCfg.getServerPort() > 0 &&
                        serverCfg.getServerPort() < 0xFFFF),
                "Invalid port number " + serverCfg.getServerPort(),
                serverConfigJmxAttribute);
        JmxAttributeValidationException.checkNotNull(serverCfg.getServerSecurityLevel(),
                "Security level is not defined",
                serverConfigJmxAttribute);
        JmxAttributeValidationException.checkCondition(serverCfg.getServerSecurityLevel() != SecurityLevel.L2,
                "Security level L2 is not supported by this module",
                serverConfigJmxAttribute);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Onem2mWebsocketProvider provider = new Onem2mWebsocketProvider(serverCfg);
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
