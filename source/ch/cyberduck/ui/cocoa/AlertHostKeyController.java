package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2007 David Kocher. All rights reserved.
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

import ch.cyberduck.core.ConnectionCanceledException;
import ch.cyberduck.core.HostKeyControllerFactory;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.sftp.HostKeyController;
import ch.cyberduck.core.sftp.KnownHostsHostKeyVerifier;
import ch.cyberduck.ui.Controller;
import ch.cyberduck.ui.cocoa.application.NSAlert;
import ch.cyberduck.ui.cocoa.foundation.NSAutoreleasePool;
import ch.ethz.ssh2.KnownHosts;

import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public class AlertHostKeyController extends KnownHostsHostKeyVerifier {
    protected static Logger log = Logger.getLogger(AlertHostKeyController.class);

    public static void register() {
        HostKeyControllerFactory.addFactory(Factory.NATIVE_PLATFORM, new Factory());
    }

    private static class Factory extends HostKeyControllerFactory {
        @Override
        protected HostKeyController create() {
            return new AlertHostKeyController(TransferController.instance());
        }

        @Override
        public HostKeyController create(Controller c) {
            return new AlertHostKeyController((WindowController) c);
        }

        @Override
        public HostKeyController create(Session s) {
            for(BrowserController c : MainController.getBrowsers()) {
                if(c.getSession() == s) {
                    return this.create(c);
                }
            }
            return this.create();
        }
    }

    private WindowController parent;

    public AlertHostKeyController(WindowController c) {
        this.parent = c;
    }

    @Override
    protected boolean isUnknownKeyAccepted(final String hostname, final int port, final String serverHostKeyAlgorithm,
                                           final byte[] serverHostKey) throws ConnectionCanceledException {
        NSAlert alert = NSAlert.alert(Locale.localizedString("Unknown host key for") + " "
                + hostname, //title
                Locale.localizedString("The host is currently unknown to the system. The host key fingerprint is")
                        + ": " + KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey) + ".",
                Locale.localizedString("Allow"), // default button
                Locale.localizedString("Deny"), // alternate button
                isHostKeyDatabaseWritable() ? Locale.localizedString("Always") : null //other button
        );
        SheetController c = new AlertController(parent, alert) {
            public void callback(final int returncode) {
                if(returncode == DEFAULT_OPTION) {// allow host (once)
                    allow(hostname, serverHostKeyAlgorithm, serverHostKey, false);
                }
                if(returncode == OTHER_OPTION) {// allow host (always)
                    allow(hostname, serverHostKeyAlgorithm, serverHostKey, true);
                }
                if(returncode == ALTERNATE_OPTION) {
                    log.warn("Cannot continue without a valid host key");
                }
            }
        };
        c.beginSheet();
        if(c.returnCode() == SheetCallback.ALTERNATE_OPTION) {
            throw new ConnectionCanceledException();
        }
        return c.returnCode() == SheetCallback.DEFAULT_OPTION
                || c.returnCode() == SheetCallback.OTHER_OPTION;

    }

    @Override
    protected boolean isChangedKeyAccepted(final String hostname, final int port, final String serverHostKeyAlgorithm,
                                           final byte[] serverHostKey) throws ConnectionCanceledException {
        NSAlert alert = NSAlert.alert(Locale.localizedString("Host key mismatch:") + " " + hostname, //title
                Locale.localizedString("The host key supplied is") + ": "
                        + KnownHosts.createHexFingerprint(serverHostKeyAlgorithm, serverHostKey)
//                                + "\n" + Locale.localizedString("The current allowed key for this host is") + " : "
//                                + allowedHostKey.getFingerprint()
                        + "\n"
                        + Locale.localizedString("Do you want to allow the host access?"),
                Locale.localizedString("Allow"), // defaultbutton
                Locale.localizedString("Deny"), //alternative button
                isHostKeyDatabaseWritable() ? Locale.localizedString("Always") : null //other button
        );
        SheetController c = new AlertController(parent, alert) {
            public void callback(final int returncode) {
                if(returncode == DEFAULT_OPTION) {
                    allow(hostname, serverHostKeyAlgorithm, serverHostKey, false);
                }
                if(returncode == OTHER_OPTION) {
                    allow(hostname, serverHostKeyAlgorithm, serverHostKey, true);
                }
                if(returncode == ALTERNATE_OPTION) {
                    log.warn("Cannot continue without a valid host key");
                }
            }
        };
        c.beginSheet();
        if(c.returnCode() == SheetCallback.ALTERNATE_OPTION) {
            throw new ConnectionCanceledException();
        }
        return c.returnCode() == SheetCallback.DEFAULT_OPTION
                || c.returnCode() == SheetCallback.OTHER_OPTION;

    }

    @Override
    public boolean verifyServerHostKey(final String hostname, final int port, final String serverHostKeyAlgorithm,
                                       final byte[] serverHostKey) throws Exception {
        final NSAutoreleasePool pool = NSAutoreleasePool.push();
        try {
            return super.verifyServerHostKey(hostname, port, serverHostKeyAlgorithm, serverHostKey);
        }
        finally {
            pool.drain();
        }
    }
}