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
import org.opendaylight.faas.adapter.ce.vlan.task.ConfigPortVlan;
import org.opendaylight.faas.adapter.ce.vlan.task.UndoConfigAcl;
import org.opendaylight.faas.adapter.ce.vlan.task.UndoConfigVlanIf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.BdPort;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.capable.device.rev150930.fabric.capable.device.config.Bdif;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.acl.list.FabricAcl;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeDomainPortRenderer implements DataTreeChangeListener<BdPort> {

    private static final Logger LOG = LoggerFactory.getLogger(BridgeDomainPortRenderer.class);

    private String device;
    private DeviceContext ctx;
    private DataBroker databorker;

    public BridgeDomainPortRenderer(DeviceContext ctx, DataBroker databroker) {
        this.device = ctx.getBridgeName();
        this.ctx = ctx;
        this.databorker = databroker;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<BdPort>> changes) {
        for (DataTreeModification<BdPort> change : changes) {
            switch (change.getRootNode().getModificationType()) {
                case DELETE: {
                    BdPort oldport = change.getRootNode().getDataBefore();
                    if (oldport.getFabricAcl() != null && !oldport.getFabricAcl().isEmpty()) {
                        Set<String> aclNames = Sets.newHashSet();
                        for (FabricAcl acl : oldport.getFabricAcl()) {
                            aclNames.add(acl.getFabricAclName());
                        }
                        ConfigAcl acltask = new UndoConfigAcl(aclNames, oldport.getRefTpId().getValue());
                        CETelnetExecutor.getInstance().addTask(device, acltask);
                    }

                    ConfigPortVlan task = new ConfigPortVlan(oldport.getRefTpId().getValue(), true);
                    task.setAccessType(oldport.getAccessType());
                    task.setAccessSegment(oldport.getAccessTag());
                    task.setVlan(ctx.getVlanOfBd(oldport.getBdid()));

                    CETelnetExecutor.getInstance().addTask(device, task);
                    break;
                }
                case WRITE: {
                    BdPort newPort = change.getRootNode().getDataAfter();

                    ConfigPortVlan task = new ConfigPortVlan(newPort.getRefTpId().getValue(), false);
                    task.setAccessType(newPort.getAccessType());
                    task.setAccessSegment(newPort.getAccessTag());
                    task.setVlan(ctx.getVlanOfBd(newPort.getBdid()));

                    CETelnetExecutor.getInstance().addTask(device, task);

                    if (newPort.getFabricAcl() != null && !newPort.getFabricAcl().isEmpty()) {
                        Set<String> aclNames = Sets.newHashSet();
                        for (FabricAcl acl : newPort.getFabricAcl()) {
                            aclNames.add(acl.getFabricAclName());
                        }
                        ConfigAcl acltask = new ConfigAcl(aclNames, newPort.getRefTpId().getValue(), ctx.isDenyDefault(), this.databorker);
                        CETelnetExecutor.getInstance().addTask(device, acltask);
                    } else if (ctx.isDenyDefault()) {
//                        ConfigAclDenyAll acltask = new ConfigAclDenyAll(newPort.getRefTpId().getValue());
//                        CETelnetExecutor.getInstance().addTask(device, acltask);
                    }
                    break;
                }
                case SUBTREE_MODIFIED: {
                    final BdPort newPort = change.getRootNode().getDataAfter();
                    Collection<DataObjectModification<? extends DataObject>> subChanges = change.getRootNode().getModifiedChildren();
                    boolean aclChanged = false;
                    for (DataObjectModification<? extends DataObject> subChange : subChanges) {
                        if (subChange.getDataType().equals(FabricAcl.class)) {
                            aclChanged = true;
                        }
                    }
                    if (aclChanged) {
                        Set<String> aclNames = Sets.newHashSet();
                        for (FabricAcl acl : newPort.getFabricAcl()) {
                            aclNames.add(acl.getFabricAclName());
                        }
                        ConfigAcl task = new ConfigAcl(aclNames, newPort.getRefTpId().getValue(), ctx.isDenyDefault(), this.databorker);
                        CETelnetExecutor.getInstance().addTask(device, task);
                    }
                    break;
                }
                default:
                    break;
            }
        }

    }

}
