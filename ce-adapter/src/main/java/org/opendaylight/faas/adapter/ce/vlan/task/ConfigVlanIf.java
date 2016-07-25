/**
 * Copyright (c) 2015 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddress;

public class ConfigVlanIf extends AbstractTask {

    int vlan;
    IpAddress ip;
    int mask;
    int vrfCtx;

    public ConfigVlanIf(int vlan, int vrfCtx, IpAddress ip, int mask) {
        this.vlan = vlan;
        this.ip = ip;
        this.mask = mask;
        this.vrfCtx = vrfCtx;
    }

    @Override
    void run() {
        this.getOperator().configGatewayPort(new String(ip.getValue()), mask, vlan, vrfCtx);
    }
}
