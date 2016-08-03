/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.faas.adapter.ce.vlan.task.ConfigVlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.BridgeDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainRenderer implements DataTreeChangeListener<BridgeDomain> {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainRenderer.class);

    private String device;
    private DeviceContext ctx;

    public BridgeDomainRenderer(DeviceContext ctx) {
        this.device = ctx.getBridgeName();
        this.ctx = ctx;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<BridgeDomain>> changes) {

        for (DataTreeModification<BridgeDomain> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    BridgeDomain oldBd = change.getRootNode().getDataBefore();
                    ctx.rmBd(oldBd.getId());
                    break;
                }
                case WRITE: {
                    BridgeDomain newBd = change.getRootNode().getDataAfter();
                    int vlan = newBd.getSegment().intValue();

                    ctx.addBd(newBd.getId(), vlan);

                    ConfigVlan task = new ConfigVlan(vlan, false);

                    CETelnetExecutor.getInstance().addTask(device, task);
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
}