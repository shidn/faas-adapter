/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

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

    public void configVlan(int vlan) {
        String cmd = String.format("vlan %d", vlan);

        this.checkConnection();
        stat.systemView();

        write(cmd);
        readUntil(promptCharSys);

        sendQuitCmd();

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

    private void sendQuitCmd() {
        write("quit");
        readUntil(promptCharSys, promptCharUser);
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
                    write("quit");
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