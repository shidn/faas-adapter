/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

public class UndoConfigVlanIf extends AbstractTask {

    int vlan;

    public UndoConfigVlanIf(int vlan) {
        super(true);
        this.vlan = vlan;
    }

    @Override
    void run() {
        this.getOperator().deleteGatewayPort(vlan);
    }
}