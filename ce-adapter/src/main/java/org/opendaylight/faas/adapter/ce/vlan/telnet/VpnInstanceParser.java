/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.telnet;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.StringTokenizer;

import org.opendaylight.faas.adapter.ce.vlan.ConsoleParser;

public class VpnInstanceParser implements ConsoleParser<List<String>> {

    @Override
    public List<String> parseConsole(String str) {
        List<String> ret = Lists.newArrayList();

        String[] stmp = str.split("\n");
        boolean found = false;
        for (String s : stmp) {
            if (!found && s.startsWith("  VPN-Instance Name")) {
                found = true;
                continue;
            }
            if (found) {
                char firstchar = s.charAt(0);
                if (firstchar == '<') {
                    continue;
                }
                StringTokenizer st = new StringTokenizer(s);
                if (st.hasMoreTokens()) {
                    ret.add(st.nextToken());
                }
            }
        }
       return ret;
    }
}
