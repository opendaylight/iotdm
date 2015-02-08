package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.notifier.rev141210;

import org.opendaylight.iotdm.onem2m.notifier.Onem2mNotifierProvider;

public class Onem2mNotifierModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.notifier.rev141210.AbstractOnem2mNotifierModule {
    public Onem2mNotifierModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mNotifierModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.notifier.rev141210.Onem2mNotifierModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Onem2mNotifierProvider provider = new Onem2mNotifierProvider();
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
