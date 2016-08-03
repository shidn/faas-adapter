/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.faas.fabric.utils.MdSalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.FabricCapableDevice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.BdPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.Bdif;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.BridgeDomain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.Vrf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.network.topology.topology.node.Config;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.rev150930.FabricId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.rev150930.FabricOptions.TrafficBehavior;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.rev150930.fabric.attributes.Options;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.rev150930.network.topology.topology.node.FabricAttribute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceRenderer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceRenderer.class);

    private DeviceContext ctx;

    private FabricId fabricId;

    private final DataBroker databroker;

    private final ListenerRegistration<BridgeDomainRenderer> bridgeDomainListener;
    private final ListenerRegistration<BridgeDomainPortRenderer> bdPortListener;
    private final ListenerRegistration<BdifRenderer> bdifListener;
    private final ListenerRegistration<VrfRenderer> vrfListener;

    public DeviceRenderer(ExecutorService exector, DataBroker databroker, InstanceIdentifier<Node> iid, Node node,
            FabricId fabricId) {

        this.databroker = databroker;

        this.fabricId = fabricId;

        ctx = new DeviceContext(node, iid);
        ctx.setBridgeName(node.getNodeId().getValue());

        BridgeDomainRenderer bdRenderer = new BridgeDomainRenderer(ctx);
        DataTreeIdentifier<BridgeDomain> bdDtid = new DataTreeIdentifier<BridgeDomain>(LogicalDatastoreType.OPERATIONAL,
                iid.augmentation(FabricCapableDevice.class).child(Config.class).child(BridgeDomain.class));
        bridgeDomainListener = databroker.registerDataTreeChangeListener(bdDtid, bdRenderer);

        BridgeDomainPortRenderer bdPortRenderer = new BridgeDomainPortRenderer(ctx);
        DataTreeIdentifier<BdPort> bdPortDtid = new DataTreeIdentifier<BdPort>(LogicalDatastoreType.OPERATIONAL,
                iid.augmentation(FabricCapableDevice.class).child(Config.class).child(BdPort.class));
        bdPortListener = databroker.registerDataTreeChangeListener(bdPortDtid, bdPortRenderer);


        BdifRenderer bdifRenderer = new BdifRenderer(ctx, databroker);
        DataTreeIdentifier<Bdif> bdifDtid = new DataTreeIdentifier<Bdif>(LogicalDatastoreType.OPERATIONAL,
                iid.augmentation(FabricCapableDevice.class).child(Config.class).child(Bdif.class));
        bdifListener = databroker.registerDataTreeChangeListener(bdifDtid, bdifRenderer);

        VrfRenderer vrfRenderer = new VrfRenderer(ctx);
        DataTreeIdentifier<Vrf> vrfDtid = new DataTreeIdentifier<Vrf>(LogicalDatastoreType.OPERATIONAL,
                iid.augmentation(FabricCapableDevice.class).child(Config.class).child(Vrf.class));
        vrfListener = databroker.registerDataTreeChangeListener(vrfDtid, vrfRenderer);

        readFabricOptions(node);
    }

    private void readFabricOptions(final Node node) {
        ReadOnlyTransaction trans = databroker.newReadOnlyTransaction();
        InstanceIdentifier<Options> iid = MdSalUtils.createFabricIId(this.fabricId).child(FabricAttribute.class)
                .child(Options.class);

        ListenableFuture<Optional<Options>> future = trans.read(LogicalDatastoreType.OPERATIONAL, iid);
        Futures.addCallback(future, new FutureCallback<Optional<Options>>() {

            @Override
            public void onSuccess(Optional<Options> result) {
                if (result.isPresent()) {
                    Options opt = result.get();
                    TrafficBehavior behavior = opt.getTrafficBehavior();
                    ctx.setTrafficBehavior(behavior == null ? TrafficBehavior.Normal : behavior);
                } else {
                    ctx.setTrafficBehavior(TrafficBehavior.Normal);
                }
            }

            @Override
            public void onFailure(Throwable ex) {
                LOG.error("unexcepted exception", ex);
            }
        });
    }

    @Override
    public void close() {
        bridgeDomainListener.close();
        bdPortListener.close();
        bdifListener.close();
        vrfListener.close();
    }
}