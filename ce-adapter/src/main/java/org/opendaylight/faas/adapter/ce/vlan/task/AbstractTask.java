/**
 * Copyright (c) 2015 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import java.util.concurrent.Callable;

import org.opendaylight.faas.adapter.ce.vlan.CETelnetOperator;

public abstract class AbstractTask implements Callable<Void> {

    private CETelnetOperator oper;

    abstract void run();

    @Override
    public Void call() throws Exception {
        run();
        return null;
    }

    public void setOperator(CETelnetOperator oper) {
        this.oper = oper;
    }

    CETelnetOperator getOperator() {
        return this.oper;
    }
}
