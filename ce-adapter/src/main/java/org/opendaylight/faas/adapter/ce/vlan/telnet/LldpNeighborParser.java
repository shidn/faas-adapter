/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.telnet;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import org.opendaylight.faas.adapter.ce.vlan.ConsoleParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.device.ce.rev160615.grp.ce.tp.Neighbor;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.device.ce.rev160615.grp.ce.tp.NeighborBuilder;

public class LldpNeighborParser implements ConsoleParser<Map<String, List<Neighbor>>> {

    @Override
    public Map<String, List<Neighbor>> parseConsole(String str) {
        Map<String, List<Neighbor>> ret = Maps.newHashMap();

        String[] stmp = str.split("\r\n");
        boolean found = false;
        for (String s : stmp) {
            if (!found && s.startsWith("------------")) {
                found = true;
                continue;
            }
            if (found) {
                String[] tmp = s.split(" +");
                if (tmp.length < 4) {
                    continue;
                }
                Neighbor neighbor = new NeighborBuilder().setInterface(tmp[2]).setSysname(tmp[3]).build();
                if (ret.containsKey(tmp[0])) {
                    ret.get(tmp[0]).add(neighbor);
                } else {
                    ret.put(tmp[0], Lists.newArrayList(neighbor));
                }
            }
        }
        return ret;
    }
}
