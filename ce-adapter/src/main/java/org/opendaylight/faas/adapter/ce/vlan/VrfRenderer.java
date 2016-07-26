/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.faas.adapter.ce.vlan.task.ConfigRoute;
import org.opendaylight.faas.adapter.ce.vlan.task.ConfigVrf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.Vrf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.device.adapter.vlan.rev160615.VlanVrfRoute;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.route.group.Route;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VrfRenderer implements DataTreeChangeListener<Vrf> {

    private static final Logger LOG = LoggerFactory.getLogger(VrfRenderer.class);

    private String device;
    private DeviceContext ctx;

    public VrfRenderer(DeviceContext ctx) {
        this.device = ctx.getBridgeName();
        this.ctx = ctx;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Vrf>> changes) {

        for (DataTreeModification<Vrf> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    Vrf oldVrf = change.getRootNode().getDataBefore();
                    ctx.rmVrf(oldVrf.getId());

                    ConfigVrf task = new ConfigVrf(oldVrf.getVrfCtx(), true);

                    CETelnetExecutor.getInstance().addTask(device, task);
                    break;
                }
                case WRITE: {
                    Vrf newVrf = change.getRootNode().getDataAfter();
                    int vrfCtx = newVrf.getVrfCtx().intValue();

                    ctx.addVrf(newVrf.getId(), vrfCtx);

                    ConfigVrf task = new ConfigVrf(vrfCtx, false);

                    CETelnetExecutor.getInstance().addTask(device, task);
                    break;
                }
                case SUBTREE_MODIFIED: {
                    Vrf newVrf = change.getRootNode().getDataAfter();
                    int vrfCtx = newVrf.getVrfCtx().intValue();
                    DataObjectModification<VlanVrfRoute> aug = change.getRootNode().getModifiedAugmentation(VlanVrfRoute.class);
                    if (aug == null) {
                        break;
                    }
                    switch (aug.getModificationType()) {
                        case DELETE: {
                            ConfigRoute task = new ConfigRoute(vrfCtx, true);
                            CETelnetExecutor.getInstance().addTask(device, task);
                            break;
                        }
                        case WRITE: {
                            ConfigRoute task = new ConfigRoute(vrfCtx, false);
                            for (DataObjectModification<? extends DataObject> route : aug.getModifiedChildren()) {
                                task.addRoute((Route) route.getDataAfter());
                            }
                            CETelnetExecutor.getInstance().addTask(device, task);
                            break;
                        }
                        case SUBTREE_MODIFIED:
                            for (DataObjectModification<? extends DataObject> route : aug.getModifiedChildren()) {
                                switch (route.getModificationType()) {
                                    case DELETE: {
                                        ConfigRoute task = new ConfigRoute(vrfCtx, true);
                                        task.addRoute((Route) route.getDataAfter());
                                        CETelnetExecutor.getInstance().addTask(device, task);
                                        break;
                                    }
                                    case WRITE : {
                                        ConfigRoute task = new ConfigRoute(vrfCtx, false);
                                        task.addRoute((Route) route.getDataAfter());
                                        CETelnetExecutor.getInstance().addTask(device, task);
                                        break;
                                    }
                                default:
                                    break;
                                }
                            }
                            break;
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }
}