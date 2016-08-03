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
import org.opendaylight.faas.adapter.ce.vlan.task.ConfigPortVlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.BdPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.acl.list.FabricAcl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainPortRenderer implements DataTreeChangeListener<BdPort> {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainPortRenderer.class);

    private String device;
    private DeviceContext ctx;

    public BridgeDomainPortRenderer(DeviceContext ctx) {
        this.device = ctx.getBridgeName();
        this.ctx = ctx;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<BdPort>> changes) {
        for (DataTreeModification<BdPort> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    BdPort oldport = change.getRootNode().getDataBefore();

                    break;
                }
                case WRITE: {
                    BdPort newPort = change.getRootNode().getDataAfter();

                    ConfigPortVlan task = new ConfigPortVlan(newPort.getRefTpId().getValue(), false);
                    task.setAccessType(newPort.getAccessType());
                    task.setAccessSegment(newPort.getAccessTag());
                    task.setVlan(ctx.getVlanOfBd(newPort.getBdid()));

                    CETelnetExecutor.getInstance().addTask(device, task);
                    break;
                }
                case SUBTREE_MODIFIED: {
                    final BdPort newPort = change.getRootNode().getDataAfter();
                    Collection<DataObjectModification<? extends DataObject>> subChanges = change.getRootNode().getModifiedChildren();
                    for (DataObjectModification<? extends DataObject> subChange : subChanges) {
                        if (subChange.getDataType().equals(FabricAcl.class)) {
                            switch(subChange.getModificationType()) {
                                case WRITE:
                                    break;
                                case DELETE:
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        }

    }

}
