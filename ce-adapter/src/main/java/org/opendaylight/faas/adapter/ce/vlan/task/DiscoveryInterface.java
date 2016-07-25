/**
 * Copyright (c) 2015 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.config.yang.config.fabric.vlan.adapter.ce.ConnectionInfo;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.faas.fabric.utils.MdSalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.device.ce.rev160615.CeNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.device.ce.rev160615.CeNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.device.ce.rev160615.CeTp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.device.ce.rev160615.CeTpBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.device.ce.rev160615.grp.ce.tp.Neighbor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DiscoveryInterface extends AbstractTask {

    private ConnectionInfo ceInfo;
    private DataBroker dataBroker;

    private boolean clearConfig = false;

    public DiscoveryInterface(ConnectionInfo ceInfo, DataBroker dataBroker, boolean clearConfig) {
        this.ceInfo = ceInfo;
        this.dataBroker = dataBroker;
        this.clearConfig = clearConfig;
    }

    private void discoveryDevice() {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(ceInfo.getManagementIp()));

        nodeBuilder.addAugmentation(CeNode.class,
                new CeNodeBuilder().setSysname(getOperator().getSysname()).build());

        // discovery interfaces
        List<String> interfaces = getOperator().getInterfaces();
        // discovery lldp neighbors
        Map<String, List<Neighbor>> neighbors = getOperator().getNeighbors();

        List<TerminationPoint> tps = Lists.newArrayList();
        for (String ifname : interfaces) {
            TerminationPointBuilder builder = new TerminationPointBuilder();
            builder.setTpId(new TpId(ifname));

            if (neighbors.containsKey(ifname)) {
                builder.addAugmentation(CeTp.class,
                        new CeTpBuilder().setNeighbor(neighbors.get(ifname)).setHasNeighbor(true).build());
            }
            tps.add(builder.build());
        }

        nodeBuilder.setTerminationPoint(tps);

        InstanceIdentifier<Node> path = MdSalUtils.createNodeIId("CE", ceInfo.getManagementIp());
        WriteTransaction wt = dataBroker.newWriteOnlyTransaction();
        wt.put(LogicalDatastoreType.OPERATIONAL, path, nodeBuilder.build(), true);
        wt.submit();

        if (clearConfig) {
            getOperator().clearInterfaceConfig(interfaces);

            List<String> vpnInsts = getOperator().getVpnInstances();
            getOperator().rmVrfs(vpnInsts);
        }
    }

    @Override
    void run() {
        discoveryDevice();
    }
}