/**
 * Copyright (c) 2016 Huawei Technologies Co. Ltd. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.faas.adapter.ce.vlan;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.net.telnet.TelnetClient;
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

    private String devId;

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

    public boolean connect(String ip, String username, String password) {

        try {
            devId = ip;
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
            }

        } catch (IOException e) {
            LOG.error(devId, e);
            return false;
        }
        return true;
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
            LOG.error(this.devId, e);
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
    }


    @Override
    public void close() throws Exception {
        if (telnet.isConnected()) {
            telnet.disconnect();
        }
    }
}
