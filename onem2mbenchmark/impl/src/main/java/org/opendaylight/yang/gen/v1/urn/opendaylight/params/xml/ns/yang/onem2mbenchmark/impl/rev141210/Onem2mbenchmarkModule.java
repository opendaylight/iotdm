/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.impl.rev141210;

import org.opendaylight.iotdm.impl.Onem2mbenchmarkProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203.Onem2mTsdrService;

public class Onem2mbenchmarkModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.impl.rev141210.AbstractOnem2mbenchmarkModule {
    public Onem2mbenchmarkModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public Onem2mbenchmarkModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.onem2mbenchmark.impl.rev141210.Onem2mbenchmarkModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        Onem2mbenchmarkProvider provider = new Onem2mbenchmarkProvider();
        if(getRpcRegistryDependency()!=null) {
            provider.setTSDRService(getRpcRegistryDependency().getRpcService(Onem2mTsdrService.class));
        }
        getBrokerDependency().registerProvider(provider);
        return provider;
    }
}
