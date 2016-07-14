/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.fabric.vlan.adapter.ce;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;

public class VlanFabricCEAdapterModule extends AbstractVlanFabricCEAdapterModule {
    public VlanFabricCEAdapterModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public VlanFabricCEAdapterModule(ModuleIdentifier identifier,
            DependencyResolver dependencyResolver,
            VlanFabricCEAdapterModule oldModule,
            AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        return new AutoCloseable() {

            @Override
            public void close() throws Exception {
                // TODO Auto-generated method stub

            }
        };
    }

}
