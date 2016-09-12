package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.persistence.mdsal.rev141210;

import org.opendaylight.iotdm.onem2m.persistence.mdsal.Onem2mPersistenceMdsalProvider;

public class Onem2mPersistenceMdsalModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.persistence.mdsal.rev141210.AbstractOnem2mPersistenceMdsalModule {
    public Onem2mPersistenceMdsalModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mPersistenceMdsalModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.persistence.mdsal.rev141210.Onem2mPersistenceMdsalModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Onem2mPersistenceMdsalProvider provider = new Onem2mPersistenceMdsalProvider();
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
