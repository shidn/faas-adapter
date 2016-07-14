/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import com.google.common.collect.Lists;

import java.util.List;

import org.opendaylight.controller.config.yang.config.fabric.vlan.adapter.ce.ConnectionInfo;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.faas.fabric.utils.MdSalUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
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

        discoveryDevice();
    }

    private void discoveryDevice() {
        boolean connected = oper.connect(ceInfo.getManagementIp(), ceInfo.getLogonUser(), ceInfo.getPassword());
        if (!connected) {
            return;

        }
        isConnected = true;
        List<String> interfaces = oper.getInterfaces();

        List<TerminationPoint> tps = Lists.newArrayList();
        for (String ifname : interfaces) {
            TerminationPointBuilder builder = new TerminationPointBuilder();
            builder.setTpId(new TpId(ifname));
            tps.add(builder.build());
        }

        InstanceIdentifier<Node> path = MdSalUtils.createNodeIId("CE", ceInfo.getManagementIp());
        NodeBuilder builder = new NodeBuilder();
        builder.setNodeId(new NodeId(ceInfo.getManagementIp()));
        builder.setTerminationPoint(tps);

        WriteTransaction wt = databroker.newWriteOnlyTransaction();
        wt.put(LogicalDatastoreType.OPERATIONAL, path, builder.build(), true);
        wt.submit();
    }

    @Override
    public void close() throws Exception {
        clearDomstore();
        oper.close();
    }

    private void clearDomstore() {

    }
}
