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
import org.opendaylight.faas.adapter.ce.vlan.task.ConfigVlanIf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.Bdif;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BdifRenderer implements DataTreeChangeListener<Bdif> {

    private static final Logger LOG = LoggerFactory.getLogger(BdifRenderer.class);

    private String device;
    private DeviceContext ctx;

    public BdifRenderer(DeviceContext ctx) {
        this.device = ctx.getBridgeName();
        this.ctx = ctx;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Bdif>> changes) {
        for (DataTreeModification<Bdif> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    break;
                }
                case WRITE: {
                    Bdif bdif = change.getRootNode().getDataAfter();
                    int vrfCtx = bdif.getVrf();
                    IpAddress ip = bdif.getIpAddress();
                    int mask = bdif.getMask();

                    ConfigVlanIf task = new ConfigVlanIf(ctx.getVlanOfBd(bdif.getBdid()), vrfCtx, ip, mask);
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