/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.AccessType;

public class ConfigPortVlan extends AbstractTask {

    private String portName;

    private AccessType accessType;
    private long accessSeg;

    private int vlan;

    public ConfigPortVlan(String portName, boolean isDelete) {
        super(isDelete);
        this.portName = portName;
    }

    public void setAccessType(AccessType type) {
        this.accessType = type;
    }

    public void setAccessSegment(long seg) {
        this.accessSeg = seg;
    }

    public void setVlan(int vlan) {
        this.vlan = vlan;
    }

    @Override
    void run() {
        if (accessType.equals(AccessType.Exclusive)) {
            this.getOperator().configAccessPort(portName, vlan);
        } else {
            this.getOperator().configTrunkPort(portName, vlan, (int) accessSeg);
        }
    }

}
