/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan.task;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.CheckedFuture;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.AccessLists;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.Ipv4Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.Acl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.AclKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.AceBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.ActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.MatchesBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.actions.packet.handling.DenyBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIpBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4Builder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigAcl extends AbstractTask {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigAcl.class);

    protected Set<String> aclNames;

    protected String portName;

    protected final DataBroker databroker;

    protected final boolean policyDriven;

    private static int nextAclNum = 3001;

    /* key : acl nubmer, value : port names */
    protected static Map<Integer, Set<String>> aclUsed = Maps.newHashMap();

    /* key : port name, value : acl number */
    protected static Map<String, Integer> portAcl = Maps.newHashMap();

    /* key : aclnames, value : acl number */
    protected static Map<Set<String>, Integer> acls = Maps.newHashMap();

    private static final Ace DenyAll = new AceBuilder()
            .setMatches(new MatchesBuilder()
                    .setAceType(new AceIpBuilder()
                            .setAceIpVersion(new AceIpv4Builder()
                                    .setDestinationIpv4Network(new Ipv4Prefix("0.0.0.0/32")).build())
                            .build())
                    .build())
            .setActions(new ActionsBuilder()
                    .setPacketHandling(new DenyBuilder().build())
                    .build())
            .build();

    public ConfigAcl(Set<String> aclNames, String portName, boolean policyDriven, DataBroker databroker) {
        super(false);
        this.aclNames = aclNames;
        this.portName = portName;
        this.policyDriven = policyDriven;
        this.databroker = databroker;
    }

    @Override
    void run() {
        if (aclNames.isEmpty()) {
            removePortAcl();
            return;
        }

        int newAclNum = 0;
        if (!acls.containsKey(aclNames)) {
            newAclNum = configNewAcl();
        } else {
            newAclNum = acls.get(aclNames);
        }

        setAcl2Port(newAclNum);

    }

    /**
     * configure new acl to acl entry.
     * @return
     */
    int configNewAcl() {
        int newAclNum = nextAclNum++;
        acls.put(aclNames, newAclNum);

        List<Ace> allRules = Lists.newArrayList();
        for (String aclname : this.aclNames) {
            Acl acl = read(aclname, databroker);
            allRules.addAll(acl.getAccessListEntries().getAce());
        }

        this.getOperator().configACL(newAclNum, allRules, policyDriven);

        return newAclNum;
    }

    /**
     * configure interface to enable acl by traffic-filter
     * @param newAcl
     */
    void setAcl2Port(int newAcl) {
        int oldAclNum = 0;
        if (portAcl.containsKey(portName)) {
            oldAclNum = portAcl.get(portName);
            aclUsed.get(oldAclNum).remove(portName);
        }

        if (aclUsed.containsKey(newAcl)) {
            aclUsed.get(newAcl).add(portName);
        } else {
            aclUsed.put(newAcl, Sets.newHashSet(portName));
        }

        portAcl.put(portName, newAcl);

        this.getOperator().configInterfaceAcl(portName, newAcl);
    }

    /**
     * configure interface to not use any acl.
     */
    void removePortAcl() {
        int oldAclNum = 0;
        if (portAcl.containsKey(portName)) {
            oldAclNum = portAcl.get(portName);
            aclUsed.get(oldAclNum).remove(portName);
        }
        portAcl.remove(portName);

        this.getOperator().clearInterfaceAcl(portName, oldAclNum);
    }

    /**
     * Executes read as a blocking transaction.
     *
     * @param store {@link LogicalDatastoreType} to read
     * @param path {@link InstanceIdentifier} for path to read
     * @param <D> the data object type
     * @return the result as the data object requested
     */
    private Acl read(final String aclname, DataBroker databroker)  {
        Acl result = null;
        InstanceIdentifier<Acl> path = InstanceIdentifier.create(AccessLists.class).child(Acl.class,
                new AclKey(aclname, Ipv4Acl.class));
        final ReadOnlyTransaction transaction = databroker.newReadOnlyTransaction();
        Optional<Acl> optionalDataObject;
        CheckedFuture<Optional<Acl>, ReadFailedException> future = transaction.read(LogicalDatastoreType.CONFIGURATION, path);
        try {
            optionalDataObject = future.checkedGet();
            if (optionalDataObject.isPresent()) {
                result = optionalDataObject.get();
            } else {
                LOG.debug("{}: Failed to read {}",
                        Thread.currentThread().getStackTrace()[1], path);
            }
        } catch (ReadFailedException e) {
            LOG.warn("Failed to read {} ", path, e);
        }
        transaction.close();
        return result;
    }
}