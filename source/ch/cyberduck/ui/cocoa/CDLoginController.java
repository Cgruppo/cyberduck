package ch.cyberduck.ui.cocoa;

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

import ch.cyberduck.core.*;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.foundation.*;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.Foundation;
import org.rococoa.Rococoa;

/**
 * @version $Id$
 */
public class CDLoginController extends AbstractLoginController implements LoginController {
    private static Logger log = Logger.getLogger(CDLoginController.class);

    private CDWindowController parent;

    public CDLoginController(final CDWindowController parent) {
        this.parent = parent;
    }

    public void prompt(final Host host, final String reason, final String message)
            throws LoginCanceledException {

        final Credentials credentials = host.getCredentials();

        CDSheetController c = new CDSheetController(parent) {
            @Override
            protected String getBundleName() {
                return "Login";
            }

            @Override
            public void awakeFromNib() {
                this.update();
            }

            @Outlet
            private NSTextField titleField;

            public void setTitleField(NSTextField titleField) {
                this.titleField = titleField;
                this.updateField(this.titleField, reason);
            }

            @Outlet
            private NSTextField userField;

            public void setUserField(NSTextField userField) {
                this.userField = userField;
                this.updateField(this.userField, credentials.getUsername());
                if(host.getProtocol().equals(Protocol.S3)) {
                    (Rococoa.cast(this.userField.cell(), NSTextFieldCell.class)).setPlaceholderString(
                            Locale.localizedString("Access Key ID", "S3")
                    );
                }
                NSNotificationCenter.defaultCenter().addObserver(this.id(),
                        Foundation.selector("userFieldTextDidChange:"),
                        NSControl.ControlTextDidChangeNotification,
                        this.userField);
            }

            public void userFieldTextDidChange(NSNotification notification) {
                credentials.setUsername(userField.stringValue());
                this.update();
            }

            @Outlet
            private NSTextField textField;

            public void setTextField(NSTextField textField) {
                this.textField = textField;
                this.updateField(this.textField, message);
            }

            @Outlet
            private NSSecureTextField passField;

            public void setPassField(NSSecureTextField passField) {
                this.passField = passField;
                this.updateField(this.passField, credentials.getPassword());
                if(host.getProtocol().equals(Protocol.S3)) {
                    (Rococoa.cast(this.passField.cell(), NSTextFieldCell.class)).setPlaceholderString(
                            Locale.localizedString("Secret Access Key", "S3")
                    );
                }
                NSNotificationCenter.defaultCenter().addObserver(this.id(),
                        Foundation.selector("passFieldTextDidChange:"),
                        NSControl.ControlTextDidChangeNotification,
                        this.passField);
            }

            public void passFieldTextDidChange(NSNotification notification) {
                credentials.setPassword(passField.stringValue());
            }

            @Outlet
            private NSButton keychainCheckbox;

            public void setKeychainCheckbox(NSButton keychainCheckbox) {
                this.keychainCheckbox = keychainCheckbox;
                this.keychainCheckbox.setState(Preferences.instance().getBoolean("connection.login.useKeychain")
                        && Preferences.instance().getBoolean("connection.login.addKeychain") ? NSCell.NSOnState : NSCell.NSOffState);
                this.keychainCheckbox.setTarget(this.id());
                this.keychainCheckbox.setAction(Foundation.selector("keychainCheckboxClicked:"));
            }

            public void keychainCheckboxClicked(final NSButton sender) {
                credentials.setUseKeychain(sender.state() == NSCell.NSOnState);
            }

            @Outlet
            private NSButton anonymousCheckbox;

            public void setAnonymousCheckbox(NSButton anonymousCheckbox) {
                this.anonymousCheckbox = anonymousCheckbox;
                this.anonymousCheckbox.setTarget(this.id());
                this.anonymousCheckbox.setAction(Foundation.selector("anonymousCheckboxClicked:"));
            }

            public void anonymousCheckboxClicked(final NSButton sender) {
                if(sender.state() == NSCell.NSOnState) {
                    credentials.setUsername(Preferences.instance().getProperty("connection.login.anon.name"));
                    credentials.setPassword(Preferences.instance().getProperty("connection.login.anon.pass"));
                }
                if(sender.state() == NSCell.NSOffState) {
                    credentials.setUsername(Preferences.instance().getProperty("connection.login.name"));
                    credentials.setPassword(null);
                }
                this.updateField(this.userField, credentials.getUsername());
                this.updateField(this.passField, credentials.getPassword());
                this.update();
            }

            @Outlet
            private NSTextField pkLabel;

            public void setPkLabel(NSTextField pkLabel) {
                this.pkLabel = pkLabel;
            }

            @Outlet
            private NSButton pkCheckbox;

            public void setPkCheckbox(NSButton pkCheckbox) {
                this.pkCheckbox = pkCheckbox;
                this.pkCheckbox.setTarget(this.id());
                this.pkCheckbox.setAction(Foundation.selector("pkCheckboxSelectionChanged:"));
            }

            private NSOpenPanel publicKeyPanel;

            public void pkCheckboxSelectionChanged(final NSButton sender) {
                log.debug("pkCheckboxSelectionChanged");
                if(this.pkLabel.stringValue().equals(Locale.localizedString("No Private Key selected"))) {
                    publicKeyPanel = NSOpenPanel.openPanel();
                    publicKeyPanel.setCanChooseDirectories(false);
                    publicKeyPanel.setCanChooseFiles(true);
                    publicKeyPanel.setAllowsMultipleSelection(false);
                    publicKeyPanel.beginSheetForDirectory(NSString.stringByExpandingTildeInPath("~/.ssh"),
                            null, this.window(),
                            new CDController() {
                                public void pkSelectionPanelDidEnd_returnCode_contextInfo(NSOpenPanel sheet, int returncode, Object context) {
                                    log.debug("pkSelectionPanelDidEnd");
                                    if(returncode == NSPanel.NSOKButton) {
                                        NSArray selected = sheet.filenames();
                                        final NSEnumerator enumerator = selected.objectEnumerator();
                                        NSObject next;
                                        while((next = enumerator.nextObject()) != null) {
                                            credentials.setIdentity(new Credentials.Identity(next.toString()));
                                        }
                                    }
                                    if(returncode == NSPanel.NSCancelButton) {
                                        credentials.setIdentity(null);
                                    }
                                    publicKeyPanel = null;
                                    update();
                                }
                            }.id(),
                            Foundation.selector("pkSelectionPanelDidEnd:returnCode:contextInfo:"), null);
                }
                else {
                    credentials.setIdentity(null);
                    this.update();
                }
            }

            private void update() {
                this.userField.setEnabled(!credentials.isAnonymousLogin());
                this.passField.setEnabled(!credentials.isAnonymousLogin());
                this.keychainCheckbox.setEnabled(!credentials.isAnonymousLogin());
                this.anonymousCheckbox.setState(credentials.isAnonymousLogin() ? NSCell.NSOnState : NSCell.NSOffState);
                this.pkCheckbox.setEnabled(host.getProtocol().equals(Protocol.SFTP));
                if(credentials.isPublicKeyAuthentication()) {
                    this.pkCheckbox.setState(NSCell.NSOnState);
                    this.updateField(this.pkLabel, credentials.getIdentity().toURL());
                }
                else {
                    this.pkCheckbox.setState(NSCell.NSOffState);
                    this.pkLabel.setStringValue(Locale.localizedString("No Private Key selected"));
                }
            }

            protected boolean validateInput() {
                return StringUtils.isNotEmpty(credentials.getUsername());
            }

            public void callback(final int returncode) {
                if(returncode == CDSheetCallback.DEFAULT_OPTION) {
                    this.window().endEditingFor(null);
                    credentials.setUsername(userField.stringValue());
                    credentials.setPassword(passField.stringValue());
                }
            }
        };
        c.beginSheet();

        if(c.returnCode() == CDSheetCallback.ALTERNATE_OPTION) {
            throw new LoginCanceledException();
        }
    }
}
