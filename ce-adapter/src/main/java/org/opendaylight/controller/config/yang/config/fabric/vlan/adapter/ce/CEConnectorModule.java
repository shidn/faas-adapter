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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.faas.adapter.ce.vlan.CEConnector;

public class CEConnectorModule extends AbstractCEConnectorModule {
    public CEConnectorModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public CEConnectorModule(ModuleIdentifier identifier,
            DependencyResolver dependencyResolver,
            CEConnectorModule oldModule,
            AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        DataBroker databroker = this.getDataBrokerDependency();
        RpcProviderRegistry rpcRegistry = this.getRpcRegistryDependency();

        this.getConnectionInfo();

        return new CEConnector(databroker, rpcRegistry, this.getConnectionInfo());
    }

}
