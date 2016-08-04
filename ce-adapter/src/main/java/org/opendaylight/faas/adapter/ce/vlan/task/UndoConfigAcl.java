/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UndoConfigAcl extends ConfigAcl {

    private static final Logger LOG = LoggerFactory.getLogger(UndoConfigAcl.class);

    public UndoConfigAcl(Set<String> aclNames, String portName) {
        super(aclNames, portName, false, null);
    }

    @Override
    void run() {
        if (!ConfigAcl.portAcl.containsKey(portName)) {
            return;
        }

        int aclNum = ConfigAcl.portAcl.get(portName);

        Set<String> ports = ConfigAcl.aclUsed.get(aclNum);
        ports.remove(portName);
        if (ports.isEmpty()) {
            //TODO delete acl which is not in use.
        }
    }
}