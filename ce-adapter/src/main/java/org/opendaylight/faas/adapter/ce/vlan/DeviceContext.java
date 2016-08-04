/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.faas.adapter.ce.vlan;

import com.google.common.collect.Maps;

import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.fabric.rev150930.FabricOptions.TrafficBehavior;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DeviceContext {

    private InstanceIdentifier<Node> myIId;

    private String bridgeName;

    private TrafficBehavior trafficBehavior = TrafficBehavior.Normal;

    Map<String, Integer> bdCache = Maps.newHashMap();
    Map<String, Integer> vrfCache = Maps.newHashMap();

    DeviceContext(Node node, InstanceIdentifier<Node> nodeIid) {
        myIId = nodeIid;
    }

    public InstanceIdentifier<Node> getMyIId() {
        return myIId;
    }

    public void setMyIId(InstanceIdentifier<Node> myIId) {
        this.myIId = myIId;
    }


    public String getBridgeName() {
        return bridgeName;
    }

    public void setBridgeName(String bridgeName) {
        this.bridgeName = bridgeName;
    }


    void setTrafficBehavior(TrafficBehavior newBehavior) {
        this.trafficBehavior = newBehavior;
    }

    public boolean isDenyDefault() {
        return trafficBehavior.equals(TrafficBehavior.PolicyDriven);
    }

    public TrafficBehavior getTrafficBehavior() {
        return trafficBehavior;
    }

    public void addBd(String name, int vlan) {
        this.bdCache.put(name, vlan);
    }

    public void rmBd(String name) {
        this.bdCache.remove(name);
    }

    public int getVlanOfBd(String name) {
        if (bdCache.containsKey(name)) {
            return bdCache.get(name);
        } else {
            return 0;
        }
    }

    public void addVrf(String name, int vlan) {
        this.vrfCache.put(name, vlan);
    }

    public void rmVrf(String name) {
        this.vrfCache.remove(name);
    }

    public int getVrf(String name) {
        if (vrfCache.containsKey(name)) {
            return vrfCache.get(name);
        } else {
            return 0;
        }
    }
}
