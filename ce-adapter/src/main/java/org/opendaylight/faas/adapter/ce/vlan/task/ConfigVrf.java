/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import com.google.common.collect.Lists;

public class ConfigVrf extends AbstractTask {

    int vrfCtx;
    private boolean isDelete;

    public ConfigVrf(int vrfCtx, boolean isDelete) {
        this.vrfCtx = vrfCtx;
        this.isDelete = isDelete;
    }

    @Override
    void run() {
        if (isDelete) {
            getOperator().rmVrfs(Lists.newArrayList("tenant" + vrfCtx));
        } else {
            getOperator().configVrf(vrfCtx);
        }
    }

}