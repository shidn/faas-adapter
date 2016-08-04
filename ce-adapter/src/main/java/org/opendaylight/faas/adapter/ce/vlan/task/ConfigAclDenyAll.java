/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigAclDenyAll extends ConfigAcl {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigAclDenyAll.class);

    private static final int AclNumDenyAll = 3000;

    private static boolean aclHasCreated = false;

    public ConfigAclDenyAll(String portName) {
        super(null, portName, true, null);
    }

    @Override
    void run() {
        if (!aclHasCreated) {
            getOperator().configACL(AclNumDenyAll, Lists.newArrayList(), true);
            aclHasCreated = true;
        }
        getOperator().configInterfaceAcl(portName, AclNumDenyAll);
    }
}