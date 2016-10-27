package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev141210;

import org.opendaylight.controller.config.api.JmxAttributeValidationException;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.Onem2mMqttProvider;
import org.opendaylight.iotdm.onem2m.protocols.mqtt.rx.Onem2mMqttIotdmPluginConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.SecurityLevel;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Onem2mMqttModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev141210.AbstractOnem2mMqttModule {
    private Onem2mMqttIotdmPluginConfig serverCfg;

    public Onem2mMqttModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mMqttModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.protocol.mqtt.rev141210.Onem2mMqttModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        serverCfg = new Onem2mMqttIotdmPluginConfig(getServerConfig());
        // Server configuration validation
        JmxAttributeValidationException.checkNotNull(serverCfg,
                "Server configuration not provided",
                serverConfigJmxAttribute);

        JmxAttributeValidationException.checkCondition((serverCfg.getMqttBrokerPort() > 0 &&
                        serverCfg.getMqttBrokerPort() < 0xFFFF),
                "Invalid port number " + serverCfg.getMqttBrokerPort(),
                serverConfigJmxAttribute);
        JmxAttributeValidationException.checkCondition(validateBrokerAddress(serverCfg.getMqttBrokerIp()),
                "Invalid broker address " + serverCfg.getMqttBrokerIp(),
                serverConfigJmxAttribute);
        JmxAttributeValidationException.checkNotNull(serverCfg.getServerSecurityLevel(),
                "Security level is not defined",
                serverConfigJmxAttribute);
        JmxAttributeValidationException.checkCondition(serverCfg.getServerSecurityLevel() != SecurityLevel.L2,
                "Security level L2 is not supported by this module",
                serverConfigJmxAttribute);
    }

    private boolean validateBrokerAddress(String address) {

        if (address == null) return false;
        //address in format tcp://hostOrIp
        //String addressParts[] = address.split("://");
        try {
            InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            return false;
        }
        return true; //addressParts[0].equalsIgnoreCase("TCP");
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Onem2mMqttProvider provider = new Onem2mMqttProvider(serverCfg);
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
