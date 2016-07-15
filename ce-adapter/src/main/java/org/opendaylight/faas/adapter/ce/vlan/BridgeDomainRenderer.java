/**
 * Copyright (c) 2015 Huawei Technologies Co. Ltd. and others. All rights reserved.
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

    public BridgeDomainRenderer(String device) {
        this.device = device;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<BridgeDomain>> changes) {

        for (DataTreeModification<BridgeDomain> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    break;
                }
                case WRITE: {
                    int vlan = change.getRootNode().getDataAfter().getSegment().intValue();
                    ConfigVlan task = new ConfigVlan(vlan);

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