package ch.cyberduck.core.ftps;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
 *  http://cyberduck.ch/
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Bug fixes, suggestions and comments should be sent to:
 *  dkocher@cyberduck.ch
 */

import com.enterprisedt.net.ftp.FTPClient;
import com.enterprisedt.net.ftp.FTPException;
import com.enterprisedt.net.ftp.FTPMessageListener;

import ch.cyberduck.core.Preferences;

import org.apache.log4j.Logger;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;

/**
 * @version $Id$
 */
public class FTPSClient extends FTPClient {
    private static Logger log = Logger.getLogger(SSLProtocolSocketFactory.class);

    public FTPSClient(String remoteHost, int controlPort, int timeout, String encoding,
                      FTPMessageListener listener, X509TrustManager trustManager) throws IOException, FTPException {
        super(listener);
        this.control = new FTPSControlSocket(InetAddress.getByName(remoteHost),
                controlPort, timeout, encoding, listener, trustManager);
    }

    /**
     * @throws IOException
     * @throws FTPException
     */
    public void auth() throws IOException, FTPException {
        this.checkConnection(true);

        lastValidReply = control.validateReply(control.sendCommand("AUTH TLS"), "234");

        ((FTPSControlSocket) this.control).startHandshake();

        this.prot();
    }

    /**
     * The command defined in [RFC-2228] to negotiate data connection
     * security is the PROT command.
     * For TLS, the data connection can have one of two security levels.
     * 1) Clear (requested by 'PROT C')
     * 2) Private (requested by 'PROT P')
     */
    private void prot() throws IOException, FTPException {
        lastValidReply = control.validateReply(control.sendCommand("PBSZ 0"), "200");
        try {
            lastValidReply = control.validateReply(control.sendCommand("PROT "
                    + Preferences.instance().getProperty("ftp.tls.datachannel")), "200");
        }
        catch (FTPException e) {
            log.warn("No data channel security: " + e.getMessage());
            ((FTPSControlSocket) this.control).setUseDataConnectionSecurity(false);
            if (Preferences.instance().getBoolean("ftp.tls.datachannel.failOnError")) {
                throw new IOException("The data channel could not be secured: " + e.getMessage());
            }
        }
    }
}
