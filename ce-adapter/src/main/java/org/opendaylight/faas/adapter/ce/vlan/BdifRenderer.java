/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.faas.adapter.ce.vlan.task.ConfigAcl;
import org.opendaylight.faas.adapter.ce.vlan.task.ConfigAclDenyAll;
import org.opendaylight.faas.adapter.ce.vlan.task.ConfigVlanIf;
import org.opendaylight.faas.adapter.ce.vlan.task.UndoConfigAcl;
import org.opendaylight.faas.adapter.ce.vlan.task.UndoConfigVlanIf;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.Bdif;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.acl.list.FabricAcl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BdifRenderer implements DataTreeChangeListener<Bdif> {

    private static final Logger LOG = LoggerFactory.getLogger(BdifRenderer.class);

    private String device;
    private DeviceContext ctx;
    private DataBroker databorker;

    public BdifRenderer(DeviceContext ctx, DataBroker databroker) {
        this.device = ctx.getBridgeName();
        this.ctx = ctx;
        this.databorker = databroker;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<Bdif>> changes) {
        for (DataTreeModification<Bdif> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    Bdif bdif = change.getRootNode().getDataBefore();
                    if (bdif.getFabricAcl() != null && !bdif.getFabricAcl().isEmpty()) {
                        Set<String> aclNames = Sets.newHashSet();
                        for (FabricAcl acl : bdif.getFabricAcl()) {
                            aclNames.add(acl.getFabricAclName());
                        }
                        ConfigAcl acltask = new UndoConfigAcl(aclNames, getVlanIfName(ctx.getVlanOfBd(bdif.getBdid())));
                        CETelnetExecutor.getInstance().addTask(device, acltask);
                    }

                    UndoConfigVlanIf task = new UndoConfigVlanIf(ctx.getVlanOfBd(bdif.getBdid()));
                    CETelnetExecutor.getInstance().addTask(device, task);
                    break;
                }
                case WRITE: {
                    Bdif bdif = change.getRootNode().getDataAfter();
                    int vrfCtx = bdif.getVrf();
                    IpAddress ip = bdif.getIpAddress();
                    int mask = bdif.getMask();

                    ConfigVlanIf task = new ConfigVlanIf(ctx.getVlanOfBd(bdif.getBdid()), vrfCtx, ip, mask);
                    CETelnetExecutor.getInstance().addTask(device, task);

                    if (bdif.getFabricAcl() != null && !bdif.getFabricAcl().isEmpty()) {
                        Set<String> aclNames = Sets.newHashSet();
                        for (FabricAcl acl : bdif.getFabricAcl()) {
                            aclNames.add(acl.getFabricAclName());
                        }
                        ConfigAcl acltask = new ConfigAcl(aclNames, getVlanIfName(ctx.getVlanOfBd(bdif.getBdid())), ctx.isDenyDefault(), this.databorker);
                        CETelnetExecutor.getInstance().addTask(device, acltask);
                    } else if (ctx.isDenyDefault()) {
                        ConfigAclDenyAll acltask = new ConfigAclDenyAll(getVlanIfName(ctx.getVlanOfBd(bdif.getBdid())));
                        CETelnetExecutor.getInstance().addTask(device, acltask);
                    }
                    break;
                }
                case SUBTREE_MODIFIED: {
                    final Bdif bdif = change.getRootNode().getDataAfter();
                    Collection<DataObjectModification<? extends DataObject>> subChanges = change.getRootNode().getModifiedChildren();
                    boolean aclChanged = false;
                    for (DataObjectModification<? extends DataObject> subChange : subChanges) {
                        if (subChange.getDataType().equals(FabricAcl.class)) {
                            aclChanged = true;
                        }
                    }
                    if (aclChanged) {
                        Set<String> aclNames = Sets.newHashSet();
                        for (FabricAcl acl : bdif.getFabricAcl()) {
                            aclNames.add(acl.getFabricAclName());
                        }
                        ConfigAcl task = new ConfigAcl(aclNames, getVlanIfName(ctx.getVlanOfBd(bdif.getBdid())), ctx.isDenyDefault(), this.databorker);
                        CETelnetExecutor.getInstance().addTask(device, task);
                    }
                    break;
                }
                default:
                    break;
            }
        }
    }

    private String getVlanIfName(int vlan) {
        return "Vlanif" + vlan;
    }
}