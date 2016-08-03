/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import com.google.common.collect.Lists;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.telnet.TelnetClient;
import org.opendaylight.faas.adapter.ce.vlan.telnet.InterfaceParser;
import org.opendaylight.faas.adapter.ce.vlan.telnet.LldpNeighborParser;
import org.opendaylight.faas.adapter.ce.vlan.telnet.VpnInstanceParser;
import org.opendaylight.faas.fabric.utils.IpAddressUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.Ace;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.actions.PacketHandling;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.actions.packet.handling.Deny;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.actions.packet.handling.Permit;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.AceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.access.control.list.rev160218.access.lists.acl.access.list.entries.ace.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.packet.fields.rev160218.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.opendaylight.faas.device.ce.rev160615.grp.ce.tp.Neighbor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CETelnetOperator implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CETelnetOperator.class);

    private static final String TERM_TYPE = "VT100";

    private static final String promptCharUser = ">";
    private static final String promptCharSys = "]";

    private TelnetClient telnet;
    private InputStream in;
    private PrintStream out;

    private PrintStream mirrorOut;

    private TelnetStat stat = new TelnetStat();

    private String devIp;
    private String sysname;

    private String username;
    private String password;

    public CETelnetOperator() {
        telnet = new TelnetClient(TERM_TYPE);
    }

    public List<String> getInterfaces() {
        final String cmd = "display interface brief | no-more";

        write(cmd);
        String str = readUntil(promptCharUser);

        InterfaceParser parser = new InterfaceParser();
        return parser.parseConsole(str);
    }

    public Map<String, List<Neighbor>> getNeighbors() {
        final String cmd = "display lldp neighbor brief | no-more";

        write(cmd);
        String str = readUntil(promptCharUser);
        LldpNeighborParser parser = new LldpNeighborParser();
        return parser.parseConsole(str);
    }

    public List<String> getVpnInstances() {
        final String cmd = "display ip vpn-instance | no-more";

        write(cmd);
        String str = readUntil(promptCharUser);
        VpnInstanceParser parser = new VpnInstanceParser();
        return parser.parseConsole(str);

    }

    /**
     * Setup a connection to device.
     * @param ip Management Ip of device
     * @param username UserName
     * @param password Password
     * @return true:successful, false:can not connect or login
     */
    public boolean connect(String ip, String username, String password) {

        try {
            mirrorOut = new PrintStream(new FileOutputStream("/tmp/" + ip + "_dump"));
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (this.connectWithParam(ip, username, password)) {
            stat.opened();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Configure vlan on CE Device.
     * @param vlan Vlan Tag
     */
    public void configVlan(int vlan) {
        String cmd = String.format("vlan %d", vlan);

        this.checkConnection();
        stat.systemView();

        write(cmd);
        readUntil(promptCharSys);

        stat.userView();
    }

    /**
     * Configure acl rules
     * @param aclNum
     * @param rules
     * @param denyDefault
     */
    public void configACL(int aclNum, List<Ace> rules, boolean denyDefault) {
        List<String> cmds = Lists.newArrayList();
        cmds.add(String.format("acl %d", aclNum));
        for (Ace ace : rules) {
            PacketHandling action = ace.getActions().getPacketHandling();
            if (action instanceof Deny) {
                if (denyDefault) {
                    continue;
                }
            } else if (action instanceof Permit) {
                if (!denyDefault) {
                    continue;
                }
            } else {
                LOG.warn("not supported action : " + action.toString());
                continue;
            }
            AceType aceType = ace.getMatches().getAceType();
            if (aceType instanceof AceIp) {
                AceIp aceIp = (AceIp) aceType;
                DestinationPortRange portRange = aceIp.getDestinationPortRange();
                AceIpVersion aceIpVer = aceIp.getAceIpVersion();
                if (aceIpVer instanceof AceIpv4) {
                    Ipv4Prefix destIp = ((AceIpv4) aceIpVer).getDestinationIpv4Network();

                    cmds.add(convAce2Cmd(aceIp.getProtocol(), action, portRange, destIp));
                }
            }
        }

        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }

        this.checkConnection();
        stat.systemView();
    }

    private String convAce2Cmd(short protocol, PacketHandling action, DestinationPortRange portRange, Ipv4Prefix destIp ) {
        String[] ipv4 = destIp.getValue().split("/");
        int lowerport = portRange.getLowerPort().getValue();
        int upperport = -1;
            if (portRange.getUpperPort() != null) {
                upperport = portRange.getUpperPort().getValue();
            }

        switch (protocol) {
            case 1: //ICMP
                return String.format("rule %s icmp destination %s %s", action.toString(), ipv4[0], ipv4[1]);
            case 6: //TCP
                if (lowerport == upperport) {
                    return String.format("rule %s tcp destination %s %s destination-port eq %d",
                            action.toString(), ipv4[0], ipv4[1], lowerport);
                } else if (upperport == -1){
                    return String.format("rule %s tcp destination %s %s destination-port gt %d",
                            action.toString(), ipv4[0], ipv4[1], lowerport);
                } else {
                    return String.format("rule %s tcp destination %s %s destination-port range %d %d",
                            action.toString(), ipv4[0], ipv4[1], lowerport, upperport);
                }
            case 17: //UDP
                if (lowerport == upperport) {
                    return String.format("rule %s udp destination %s %s destination-port eq %d",
                            action.toString(), ipv4[0], ipv4[1], lowerport);
                } else if (upperport == -1){
                    return String.format("rule %s udp destination %s %s destination-port gt %d",
                            action.toString(), ipv4[0], ipv4[1], lowerport);
                } else {
                    return String.format("rule %s udp destination %s %s destination-port range %d %d",
                            action.toString(), ipv4[0], ipv4[1], lowerport, upperport);
                }
            default: //IP
                return String.format("rule %s ip destination %s %s", action.toString(), ipv4[0], ipv4[1]);
        }
    }

    /**
     * Configure vrf on CE Device.<br>
     * for CE device, vrf is vpn-instance
     * @param vrfCtx vrf Tag
     */
    public void configVrf(int vrfCtx) {
        String[] cmds = new String[] {
                String.format("ip vpn-instance tenant%d", vrfCtx),
                String.format("ipv4-family"),
                String.format("route-distinguisher %d:0", vrfCtx)
        };


        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }

        stat.userView();
    }

    /**
     * Configure access port.
     * @param portName name of port
     * @param interVlan vlan of port
     */
    public void configAccessPort(String portName, int interVlan) {
        String[] cmds = new String[] {
                String.format("interface %s", portName),
                String.format("port link-type access"),
                String.format("port default vlan %d", interVlan)
        };


        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }

        stat.userView();
    }

    /**
     * configure interface to enable acl
     * @param portName name of port
     * @param aclNum acl number to configure
     */
    public void configInterfaceAcl(String portName, int aclNum) {
        String[] cmds = new String[] {
                String.format("interface %s", portName),
                String.format("traffic-filter acl %d outbound", aclNum)
        };

        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }

        stat.userView();
    }

    /**
     * configure interface to disable acl
     * @param portName name of port
     * @param aclNum acl number to configure
     */
    public void clearInterfaceAcl(String portName, int aclNum) {
        String[] cmds = new String[] {
                String.format("interface %s", portName),
                String.format("undo traffic-filter acl %d outbound", aclNum)
        };

        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }

        stat.userView();
    }

    /**
    * Configure trunk port.
    * @param portName name of port
    * @param interVlan vlan of port
    * @param outerVlan outer vlan of port
    */
    public void configTrunkPort(String portName, int interVlan, int outerVlan) {
        String[] cmds = new String[] {
                String.format("interface %s", portName),
                String.format("port link-type trunk"),
                String.format("port trunk allow-pass vlan %d", interVlan),
                String.format("port vlan-mapping vlan %d map-vlan %d", outerVlan, interVlan)
        };

        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }

        stat.userView();
    }

    /**
     *
     * @param portName
     */
    public void configTrunkPortAllowAll(String portName) {
        String[] cmds = new String[] {
                String.format("interface %s", portName),
                String.format("port link-type trunk"),
                String.format("port trunk allow-pass vlan all")
        };


        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }

        stat.userView();
    }

    /**
     * Configure gateway ip on VlanIf
     * @param ip ip addresss
     * @param mask network mask
     * @param vlan vlan
     * @param vrfCtx vrfctx, for CE is vpn-instance
     */
    public void configGatewayPort(String ip, int mask, int vlan, int vrfCtx) {
        String[] cmds = new String[] {
                String.format("interface Vlanif%d", vlan),
                String.format("ip binding vpn-instance tenant%d", vrfCtx),
                String.format("ip address %s %d", ip, mask)
        };

        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }

        stat.userView();
    }

    public void deleteGatewayPort(int vrfCtx, int vlan) {
        String[] cmds = new String[] {
                String.format("interface Vlanif%d", vlan),
                String.format("undo ip binding vpn-instance tenant%d", vrfCtx),
                String.format("undo ip address ")
        };

        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }

        stat.userView();
    }

    /**
     * Configure static route.
     * @param vrfCtx
     * @param destIp
     * @param nexthop
     */
    public void configStaticRoute(int vrfCtx, String destIp, String nexthop) {
        String[] cmds = new String[] {
                String.format("ip route-static vpn-instance tenant%d %s %s", vrfCtx, destIp, nexthop)};

        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }
        stat.userView();
    }

    /**
     *
     * @param vrfCtx
     */
    public void clearStaticRoute(int vrfCtx) {
        String[] cmds = new String[] {
                String.format("undo ip route-static vpn-instance tenant%d all", vrfCtx)};

        this.checkConnection();
        stat.systemView();

        for (String cmd : cmds) {
            write(cmd);
            readUntil(promptCharSys);
        }
        stat.userView();
    }

   /**
    *
    * @param vrfCtx
    */
   public void rmStaticRoute(int vrfCtx, String destIp) {
       String[] cmds = new String[] {
               String.format("undo ip route-static vpn-instance tenant%d %s", vrfCtx, destIp)};

       this.checkConnection();
       stat.systemView();

       for (String cmd : cmds) {
           write(cmd);
           readUntil(promptCharSys);
       }
       stat.userView();
   }

   public void rmVrfs(List<String> vpnInstances) {
       String cmd = "undo ip vpn-instance %s";

       this.checkConnection();
       stat.systemView();

       for (String vpnInst : vpnInstances) {
           write(String.format(cmd, vpnInst));
           readUntil(promptCharSys);
       }
       stat.userView();
   }

   /**
    *
    * @param portNames
    */
   public void clearInterfaceConfig(List<String> portNames) {
       String cmd = "clear configuration interface %s";

       this.checkConnection();
       stat.systemView();

       for (String port : portNames) {
           write(String.format(cmd, port));
           readUntil(promptCharSys);
           write("Y");
           readUntil(promptCharSys);
       }
       stat.userView();
   }

   private String readUntil(String pattern) {
        return readUntil(pattern, null);
    }

    private String readUntil(String pattern1, String pattern2) {
        StringBuilder buf = new StringBuilder();
        try {
            char lastChar1 = pattern1.charAt(pattern1.length() - 1);
            char lastChar2 = pattern2 == null ? (char) -1 : pattern2.charAt(pattern2.length() - 1);

            int len1 = pattern1.length();
            int len2 = pattern2 == null ? 0 : pattern2.length();

            char ch;
            int code = -1;
            int idx = 0;
            while ((code = in.read()) != -1) {
                ch = (char) code;
                mirrorOut.print(ch);
                buf.append(ch);
                idx++;

                if (ch == lastChar1 && idx > len1 && buf.substring(idx - len1).equals(pattern1)) {
                    return buf.toString();
                }

                if (ch == lastChar2 && idx > len2 && buf.substring(idx - len2).equals(pattern2)) {
                    return buf.toString();
                }
            }
        } catch (IOException e) {
            LOG.error(this.devIp, e);
        }
        return buf.toString();
    }

    private void write(String value) {
        out.println(value);
        out.flush();
    }

    public void disConnect() {
        if (telnet.isConnected()) {
            try {
                telnet.disconnect();
            } catch (IOException e) {
                LOG.warn("", e);
            }
        }
        mirrorOut.close();
    }

    @Override
    public void close() throws Exception {
        if (telnet.isConnected()) {
            telnet.disconnect();
        }
    }

    public String getSysname() {
        return this.sysname;
    }

    private void reconnect() {
        connectWithParam(this.devIp, this.username, this.password);
    }

    private boolean connectWithParam(String ip, String username, String password) {

        try {
            devIp = ip;
            telnet.connect(ip);

            in = telnet.getInputStream();
            out = new PrintStream(telnet.getOutputStream());

            readUntil("Username:");
            write(username);

            readUntil("Password:");
            write(password);

            String console = readUntil("Username:", promptCharUser);

            if (console.contains("Error")) {
                LOG.error(console);
                return false;
            } else {
                int pos = console.lastIndexOf('<');
                sysname = console.substring(pos + 1, console.length() - 1);
            }

        } catch (IOException e) {
            LOG.error(devIp, e);
            return false;
        }
        return true;
    }

    private void checkConnection() {
    }

    private class TelnetStat {
        private static final int CLOSED = -1;
        private static final int CLOSING = -2;
        private static final int USERVIEW = -3;
        private static final int SYSVIEW = -4;

        private int state = CLOSED;

        public void systemView() {
            switch (state) {
                case CLOSING:
                case CLOSED:
                    reconnect();
                    write("system-view immediately");
                    readUntil(promptCharSys);
                    break;
                case USERVIEW:
                    write("system-view immediately");
                    readUntil(promptCharSys);
                    break;
                default:
                    break;
            }
            state = SYSVIEW;
        }

        public void userView() {
            switch (state) {
                case CLOSING:
                case CLOSED:
                    reconnect();
                    break;
                case SYSVIEW:
                    write("return");
                    readUntil(promptCharUser);
                    break;
                default:
                    break;
            }
            state = USERVIEW;
        }

        public void opened() {
            state = USERVIEW;
        }

        public void timeouted() {
            state = CLOSING;
        }

        public void disconnected() {
            state = CLOSED;
        }
    }
}
