/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import com.google.common.collect.Lists;

import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.route.group.Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.type.rev150930.route.group.route.next.hop.options.SimpleNextHop;

public class ConfigRoute extends AbstractTask {

    int vrfCtx;

    private List<Route> routes = Lists.newArrayList();

    public ConfigRoute(int vrfCtx, boolean isDelete) {
        super(isDelete);
        this.vrfCtx = vrfCtx;
    }

    @Override
    void run() {
        if (isDelete()) {
            if (routes.isEmpty()) {
                getOperator().clearStaticRoute(vrfCtx);
            } else {
                for (Route route : routes) {
                    getOperator().rmStaticRoute(vrfCtx, toString(route.getDestinationPrefix()));
                }
            }
        } else {
            for (Route route : routes) {
                if (route.getNextHopOptions() instanceof SimpleNextHop) {
                    SimpleNextHop nexthop = (SimpleNextHop) route.getNextHopOptions();
                    getOperator().configStaticRoute(vrfCtx, toString(route.getDestinationPrefix()), nexthop.getNextHop().getValue());
                }
            }
        }
    }

    private String toString(Ipv4Prefix p) {
        StringBuilder buf = new StringBuilder();
        char[] ca = p.getValue().toCharArray();
        for (char c : ca) {
            buf.append(c == '/' ? ' ' : c);
        }
        return buf.toString();
    }

    public void addRoute(Route route) {
        routes.add(route);
    }
}