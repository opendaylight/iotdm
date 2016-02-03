package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.iotdm.tsdr.Onem2mTsdrImpl;

public class Onem2mTsdrModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203.AbstractOnem2mTsdrModule {
    public Onem2mTsdrModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mTsdrModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203.Onem2mTsdrModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        final Onem2mTsdrImpl impl = new Onem2mTsdrImpl(getRpcRegistryDependency());
        final RpcRegistration<Onem2mTsdrService> onem2mTsdrServiceRpcRegistration = getRpcRegistryDependency().addRpcImplementation(Onem2mTsdrService.class, impl);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                impl.shutdown();
                onem2mTsdrServiceRpcRegistration.close();
            }
        };
    }
}
