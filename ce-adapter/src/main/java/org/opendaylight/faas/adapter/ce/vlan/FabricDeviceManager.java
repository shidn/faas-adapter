/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RoutedRpcRegistration;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.faas.fabric.general.Constants;
import org.opendaylight.faas.fabric.utils.InterfaceManager;
import org.opendaylight.faas.fabric.utils.MdSalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.FabricCapableDevice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.FabricCapableDeviceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.FabricCapableDeviceContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.FabricPortAug;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.FabricPortAugBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.network.topology.topology.node.AttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.device.adapter.vlan.rev160615.AddToVlanFabricInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.device.adapter.vlan.rev160615.FabricVlanDeviceAdapterService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.device.adapter.vlan.rev160615.RmFromVlanFabricInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.rev150930.FabricId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.rev150930.FabricPortAugment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.rev150930.FabricPortAugmentBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.rev150930.network.topology.topology.node.termination.point.FportAttributeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.FabricPortRole;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.TpRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.VlanFabric;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricDeviceManager implements FabricVlanDeviceAdapterService,
        DataTreeChangeListener<Node>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(FabricDeviceManager.class);

    private ListeningExecutorService executor;
    private final DataBroker databroker;

    private final Map<InstanceIdentifier<Node>, DeviceRenderer> renderers = Maps.newHashMap();

    private final RoutedRpcRegistration<FabricVlanDeviceAdapterService> rpcRegistration;

    final InstanceIdentifier<Node> targetPath = InstanceIdentifier.create(NetworkTopology.class)
            .child(Topology.class, new TopologyKey(new TopologyId("CE"))).child(Node.class);

    public FabricDeviceManager(final DataBroker databroker,
            final RpcProviderRegistry rpcRegistry) {
        this.databroker = databroker;
        executor = MoreExecutors.listeningDecorator(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));

        rpcRegistration = rpcRegistry.addRoutedRpcImplementation(FabricVlanDeviceAdapterService.class, this);

        DataTreeIdentifier<Node> dtid = new DataTreeIdentifier<Node>(LogicalDatastoreType.OPERATIONAL,
                targetPath);

        databroker.registerDataTreeChangeListener(dtid, this);
    }

    public DataBroker getDatabroker() {
        return databroker;
    }

    @Override
    public void close() throws Exception {
        if (rpcRegistration != null) {
            rpcRegistration.close();
        }
    }

    @Override
    public Future<RpcResult<Void>> rmFromVlanFabric(RmFromVlanFabricInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Future<RpcResult<Void>> addToVlanFabric(AddToVlanFabricInput input) {
        Preconditions.checkNotNull(input);
        Preconditions.checkNotNull(input.getNodeId());
        Preconditions.checkNotNull(input.getFabricId());

        final RpcResult<Void> result = RpcResultBuilder.<Void>success().build();

        @SuppressWarnings("unchecked")
        final InstanceIdentifier<Node> deviceIId = (InstanceIdentifier<Node>) input.getNodeId();
        final FabricId fabricId = new FabricId(input.getFabricId());

        FabricCapableDeviceBuilder deviceBuilder = new FabricCapableDeviceBuilder();
        AttributesBuilder attributesBuilder = new AttributesBuilder();

        attributesBuilder.setFabricId(input.getFabricId());

        InstanceIdentifier<Node> fabricpath =
                Constants.DOM_FABRICS_PATH.child(Node.class, new NodeKey(input.getFabricId()));
        attributesBuilder.setFabricRef(new NodeRef(fabricpath));

        deviceBuilder.setAttributes(attributesBuilder.build());

        @SuppressWarnings("unchecked")
        final InstanceIdentifier<FabricCapableDevice> path
            = ((InstanceIdentifier<Node>) input.getNodeId()).augmentation(FabricCapableDevice.class);

        final Node bridgeNode = MdSalUtils.syncReadOper(databroker, deviceIId).get();

        WriteTransaction wt = databroker.newWriteOnlyTransaction();
        wt.merge(LogicalDatastoreType.OPERATIONAL, path, deviceBuilder.build(), true);
        addTp2Fabric(wt, bridgeNode, deviceIId, fabricId);

        CheckedFuture<Void,TransactionCommitFailedException> future = wt.submit();

        return Futures.transform(future, new AsyncFunction<Void, RpcResult<Void>>() {

            @Override
            public ListenableFuture<RpcResult<Void>> apply(Void input) throws Exception {
                renderers.put(deviceIId, new DeviceRenderer(executor, databroker, deviceIId, bridgeNode, fabricId));

                return Futures.immediateFuture(result);
            }
        }, executor);
    }

    private void addTp2Fabric(WriteTransaction wt,
            Node bridgeNode,
            InstanceIdentifier<Node> deviceIId,
            FabricId fabricId) {

        NodeBuilder devBuilder = new NodeBuilder().setKey(bridgeNode.getKey());

        NodeBuilder fabricBuilder = new NodeBuilder().setNodeId(fabricId);

        List<TerminationPoint> updTps = Lists.newArrayList();
        List<TerminationPoint> fabricTps = Lists.newArrayList();

        for (TerminationPoint tp : bridgeNode.getTerminationPoint()) {
            String bridgeName = bridgeNode.getNodeId().getValue();
            if (tp.getTpId().getValue().equals(bridgeName)) {
                continue;
            }
            TpId fabricTpid = InterfaceManager.createFabricPort(bridgeNode.getNodeId(), tp.getTpId());

            // add ref to device tp
            updTps.add(new TerminationPointBuilder()
                    .setKey(tp.getKey())
                    .addAugmentation(FabricPortAug.class, new FabricPortAugBuilder()
                            .setPortRole(FabricPortRole.Access)
                            .setPortRef(new TpRef(MdSalUtils.createFabricPortIId(fabricId, fabricTpid)))
                            .build())
                    .build());

            // add tp on fabric
            fabricTps.add(new TerminationPointBuilder()
                    .setKey(new TerminationPointKey(fabricTpid))
                    .setTpRef(Lists.newArrayList(tp.getTpId()))
                    .addAugmentation(FabricPortAugment.class, new FabricPortAugmentBuilder()
                            .setFportAttribute(new FportAttributeBuilder()
                                    .setDevicePort(
                                            new TpRef(deviceIId.child(TerminationPoint.class, tp.getKey()))).build())
                            .build())
                    .build());

        }
        devBuilder.setTerminationPoint(updTps);
        fabricBuilder.setTerminationPoint(fabricTps);

        wt.merge(LogicalDatastoreType.OPERATIONAL, deviceIId, devBuilder.build(), false);
        wt.merge(LogicalDatastoreType.OPERATIONAL, MdSalUtils.createFNodeIId(fabricId), fabricBuilder.build(), false);
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Node>> changes) {
        for (DataTreeModification<Node> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    InstanceIdentifier<Node> targetIId = change.getRootPath().getRootIdentifier();

                    this.rpcRegistration.unregisterPath(FabricCapableDeviceContext.class, targetIId);
                    break;
                }
                case WRITE: {
                    InstanceIdentifier<Node> targetIId = change.getRootPath().getRootIdentifier();
                    this.rpcRegistration.registerPath(FabricCapableDeviceContext.class, targetIId);
                    setupDeviceAttribute(targetIId);
                    break;
                }
                case SUBTREE_MODIFIED: {
                    // DO NOTHING
                    break;
                }
                default:
                    break;
            }
        }
    }

    private void setupDeviceAttribute(final InstanceIdentifier<Node> nodeIId) {
        WriteTransaction wt = databroker.newWriteOnlyTransaction();

        /* setup supported-fabric-type */
        {
            InstanceIdentifier<FabricCapableDevice> deviceIid = nodeIId.augmentation(FabricCapableDevice.class);
            FabricCapableDeviceBuilder builder = new FabricCapableDeviceBuilder();
            builder.setSupportedFabric(Lists.newArrayList(VlanFabric.class));
            wt.put(LogicalDatastoreType.OPERATIONAL, deviceIid, builder.build(), true);
        }
        wt.submit();
    }
}