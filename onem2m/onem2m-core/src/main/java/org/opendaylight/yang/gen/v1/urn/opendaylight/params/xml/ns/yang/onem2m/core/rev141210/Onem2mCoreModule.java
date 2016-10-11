package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210;

import org.opendaylight.iotdm.onem2m.core.Onem2mCoreProvider;

public class Onem2mCoreModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.AbstractOnem2mCoreModule {
    public Onem2mCoreModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mCoreModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2m.core.rev141210.Onem2mCoreModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Onem2mCoreProvider provider = Onem2mCoreProvider.getInstance();
        provider.setSecurityConfig(getSecurityConfig());
        getBrokerDependency().registerProvider(provider);
        // register jmx access for jconsole to onem2m stats
        getRootRuntimeBeanRegistratorWrapper().register(provider);
        return provider;
    }

}
