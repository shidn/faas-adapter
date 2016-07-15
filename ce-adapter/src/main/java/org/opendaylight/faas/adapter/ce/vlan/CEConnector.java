/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import org.opendaylight.controller.config.yang.config.fabric.vlan.adapter.ce.ConnectionInfo;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.faas.adapter.ce.vlan.task.DiscoveryInterface;
import org.opendaylight.faas.fabric.utils.MdSalUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class CEConnector implements AutoCloseable {

    private final DataBroker databroker;
    private final RpcProviderRegistry rpcRegistry;
    private final ConnectionInfo ceInfo;

    private final CETelnetOperator oper;

    private boolean isConnected = false;

    public CEConnector(DataBroker databroker, RpcProviderRegistry rpcRegistry, ConnectionInfo ceInfo) {
        this.databroker = databroker;
        this.rpcRegistry = rpcRegistry;
        this.ceInfo = ceInfo;

        oper = new CETelnetOperator();
        CETelnetExecutor.getInstance().addOperator(ceInfo.getManagementIp(), oper);

        connect();

        DiscoveryInterface task = new DiscoveryInterface(ceInfo, databroker);

        CETelnetExecutor.getInstance().addTask(ceInfo.getManagementIp(), task);
    }

    private void connect() {
        boolean connected = oper.connect(ceInfo.getManagementIp(), ceInfo.getLogonUser(), ceInfo.getPassword());
        if (!connected) {
            return;
        }
        isConnected = true;
    }

    @Override
    public void close() throws Exception {
        clearDomstore();
        oper.close();
    }

    private void clearDomstore() {
        InstanceIdentifier<Node> path = MdSalUtils.createNodeIId("CE", ceInfo.getManagementIp());
        WriteTransaction wt = databroker.newWriteOnlyTransaction();
        wt.delete(LogicalDatastoreType.OPERATIONAL, path);
        wt.submit();
    }
}
