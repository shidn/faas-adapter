/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import com.google.common.collect.Lists;

import java.util.List;

public class InterfaceParser implements ConsoleParser<String> {

    @Override
    public List<String> parseConsole(String str) {
        List<String> ret = Lists.newArrayList();

        String[] stmp = str.split("\n");
        boolean found = false;
        for (String s : stmp) {
            if (!found && s.startsWith("Interface")) {
                found = true;
                continue;
            }
            if (found) {
                String name = s.split(" ")[0];
                char firstchar = name.charAt(0);
                if (firstchar == 'M' || firstchar == 'N') {
                    continue;
                }
                ret.add(name);
            }
        }

        return ret;
    }
}
