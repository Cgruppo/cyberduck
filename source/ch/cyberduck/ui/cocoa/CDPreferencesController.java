package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2003 David Kocher. All rights reserved.
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

import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.Session;
import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.*;
import org.apache.log4j.Logger;

/**
* @version $Id$
 */
public class CDPreferencesController {
    private static Logger log = Logger.getLogger(CDPreferencesController.class);

    private static CDPreferencesController instance;
    
    // ----------------------------------------------------------
    // Outlets
    // ----------------------------------------------------------

    private NSButtonCell duplicateAskmeCheckbox;
    public void setDuplicateAskmeCheckbox(NSButtonCell duplicateAskmeCheckbox) {
	this.duplicateAskmeCheckbox = duplicateAskmeCheckbox;
	this.duplicateAskmeCheckbox.setTarget(this);
	this.duplicateAskmeCheckbox.setAction(new NSSelector("duplicateAskmeCheckboxClicked", new Class[] {NSButtonCell.class}));
    }

    public void duplicateAskmeCheckboxClicked(NSButtonCell sender) {
	switch(sender.state()) {
	    case NSCell.OnState:
		Preferences.instance().setProperty("connection.duplicate.ask", "true");
		break;
	    case NSCell.OffState:
		Preferences.instance().setProperty("connection.duplicate.ask", "false");
		break;
	}
    }
    
    private NSButtonCell duplicateOverwriteCheckbox;
    public void setDuplicateOverwriteCheckbox(NSButtonCell duplicateOverwriteCheckbox) {
	this.duplicateOverwriteCheckbox = duplicateOverwriteCheckbox;
	this.duplicateOverwriteCheckbox.setTarget(this);
	this.duplicateOverwriteCheckbox.setAction(new NSSelector("duplicateOverwriteCheckboxClicked", new Class[] {NSButtonCell.class}));
    }

    public void duplicateOverwriteCheckboxClicked(NSButtonCell sender) {
	switch(sender.state()) {
	    case NSCell.OnState:
		Preferences.instance().setProperty("connection.duplicate.overwrite", "true");
		break;
	    case NSCell.OffState:
		Preferences.instance().setProperty("connection.duplicate.overwrite", "false");
		break;
	}
    }
    
    private NSButtonCell duplicateResumeCheckbox;
    public void setDuplicateResumeCheckbox(NSButtonCell duplicateResumeCheckbox) {
	this.duplicateResumeCheckbox = duplicateResumeCheckbox;
	this.duplicateResumeCheckbox.setTarget(this);
	this.duplicateResumeCheckbox.setAction(new NSSelector("duplicateResumeCheckboxClicked", new Class[] {NSButtonCell.class}));
    }

    public void duplicateResumeCheckboxClicked(NSButtonCell sender) {
	switch(sender.state()) {
	    case NSCell.OnState:
		Preferences.instance().setProperty("connection.duplicate.resume", "true");
		break;
	    case NSCell.OffState:
		Preferences.instance().setProperty("connection.duplicate.resume", "false");
		break;
	}
    }

    private NSButton downloadPathButton;
    public void setDownloadPathButton(NSButton downloadPathButton) {
	this.downloadPathButton = downloadPathButton;
    }
    
    private NSTextField anonymousField;
    public void setAnonymousField(NSTextField anonymousField) {
	this.anonymousField = anonymousField;
    }

    private NSTextField downloadPathField;
    public void setDownloadPathField(NSTextField downloadPathField) {
	this.downloadPathField = downloadPathField;
    }

    private NSTextField loginField;
    public void setLoginField(NSTextField loginField) {
	this.loginField = loginField;
    }
    
    private NSButton showHiddenCheckbox;
    public void setShowHiddenCheckbox(NSButton showHiddenCheckbox) {
	this.showHiddenCheckbox = showHiddenCheckbox;
    }

    private NSButton newBrowserCheckbox;
    public void setNewBrowserCheckbox(NSButton newBrowserCheckbox) {
	this.newBrowserCheckbox = newBrowserCheckbox;
    }

    private NSButton closeTransferCheckbox;
    public void setCloseTransferCheckbox(NSButton closeTransferCheckbox) {
	this.closeTransferCheckbox = closeTransferCheckbox;
    }

    private NSButton processCheckbox;
    public void setProcessCheckbox(NSButton processCheckbox) {
	this.processCheckbox = processCheckbox;
    }
    
    private NSPopUpButton transfermodeCombo;
    public void setTransfermodeCombo(NSPopUpButton transfermodeCombo) {
	this.transfermodeCombo = transfermodeCombo;
    }

    private NSPopUpButton connectmodeCombo;
    public void setConnectmodeCombo(NSPopUpButton connectmodeCombo) {
	this.connectmodeCombo = connectmodeCombo;
    }
    
    private NSPopUpButton protocolCombo;
    public void setProtocolCombo(NSPopUpButton protocolCombo) {
	this.protocolCombo = protocolCombo;
    }

    private NSWindow window;
    public void setWindow(NSWindow window) {
	this.window = window;
    }

    private static NSMutableArray allDocuments = new NSMutableArray();

    public static CDPreferencesController instance() {
	if(null == instance) {
	    instance = new CDPreferencesController();
	    allDocuments.addObject(instance);
	}
        if (false == NSApplication.loadNibNamed("Preferences", instance)) {
            log.fatal("Couldn't load Preferences.nib");
        }
	instance.init();
	return instance;
    }
    
    private CDPreferencesController() {
	allDocuments.addObject(this);
    }

    public NSWindow window() {
	return this.window;
    }

    public void windowWillClose(NSNotification notification) {
	this.window().setDelegate(null);
	NSNotificationCenter.defaultCenter().removeObserver(this);
	allDocuments.removeObject(this);
    }
    
    private static String CONNECTMODE_ACTIVE = "Active";
    private static String CONNECTMODE_PASSIVE = "Passive";
    private static String TRANSFERMODE_BINARY = "Binary";
    private static String TRANSFERMODE_ASCII = "ASCII";
    private static String PROTOCOL_FTP = "FTP";
    private static String PROTOCOL_SFTP = "SFTP";
    
    private void init() {
	//setting values
	loginField.setStringValue(Preferences.instance().getProperty("connection.login.name"));
	anonymousField.setStringValue(Preferences.instance().getProperty("ftp.anonymous.pass"));
	downloadPathField.setStringValue(Preferences.instance().getProperty("connection.download.folder"));
	showHiddenCheckbox.setState(Preferences.instance().getProperty("browser.showHidden").equals("true") ? NSCell.OnState : NSCell.OffState);
	newBrowserCheckbox.setState(Preferences.instance().getProperty("browser.opendefault").equals("true") ? NSCell.OnState : NSCell.OffState);
	closeTransferCheckbox.setState(Preferences.instance().getProperty("transfer.close").equals("true") ? NSCell.OnState : NSCell.OffState);

	    
	connectmodeCombo.removeAllItems();
	connectmodeCombo.addItemsWithTitles(new NSArray(new String[]{CONNECTMODE_ACTIVE, CONNECTMODE_PASSIVE}));
	if(Preferences.instance().getProperty("ftp.connectmode").equals("passive"))
	    connectmodeCombo.setTitle(CONNECTMODE_PASSIVE);
	else
	    connectmodeCombo.setTitle(CONNECTMODE_ACTIVE);
	
	transfermodeCombo.removeAllItems();
	transfermodeCombo.addItemsWithTitles(new NSArray(new String[]{TRANSFERMODE_BINARY, TRANSFERMODE_ASCII}));
	if(Preferences.instance().getProperty("ftp.transfermode").equals("binary"))
	    transfermodeCombo.setTitle(TRANSFERMODE_BINARY);
	else
	    transfermodeCombo.setTitle(TRANSFERMODE_ASCII);
	
	protocolCombo.removeAllItems();
	protocolCombo.addItemsWithTitles(new NSArray(new String[]{PROTOCOL_FTP, PROTOCOL_SFTP}));
	if(Preferences.instance().getProperty("connection.protocol.default").equals("ftp"))
	    protocolCombo.setTitle(PROTOCOL_FTP);
	else
	    protocolCombo.setTitle(PROTOCOL_SFTP);
		
	NSNotificationCenter.defaultCenter().addObserver(
						  this,
						  new NSSelector("anonymousFieldDidChange", new Class[]{NSNotification.class}),
						  NSControl.ControlTextDidChangeNotification,
						  anonymousField);
	NSNotificationCenter.defaultCenter().addObserver(
						  this,
						  new NSSelector("loginFieldDidChange", new Class[]{NSNotification.class}),
						  NSControl.ControlTextDidChangeNotification,
						  loginField);
	NSNotificationCenter.defaultCenter().addObserver(
						  this,
						  new NSSelector("downloadPathFieldDidChange", new Class[]{NSNotification.class}),
						  NSControl.ControlTextDidChangeNotification,
						  downloadPathField);
	
    }

    // ----------------------------------------------------------
    // Notifications
    // ----------------------------------------------------------
    public void loginFieldDidChange(NSNotification sender) {
	Preferences.instance().setProperty("connection.login.name", loginField.stringValue());
    }

    public void anonymousFieldDidChange(NSNotification sender) {
	Preferences.instance().setProperty("ftp.anonymous.pass", anonymousField.stringValue());
    }

    public void downloadPathFieldDidChange(NSNotification sender) {
	Preferences.instance().setProperty("connection.download.folder", downloadPathField.stringValue());
    }

    public void showHiddenCheckboxClicked(NSButton sender) {
	switch(sender.state()) {
	    case NSCell.OnState:
		Preferences.instance().setProperty("browser.showHidden", "true");
		break;
	    case NSCell.OffState:
		Preferences.instance().setProperty("browser.showHidden", "false");
		break;
	}
    }


    public void newBrowserCheckboxClicked(NSButton sender) {
	switch(sender.state()) {
	    case NSCell.OnState:
		Preferences.instance().setProperty("browser.opendefault", "true");
		break;
	    case NSCell.OffState:
		Preferences.instance().setProperty("browser.opendefault", "false");
		break;
	}
    }

    public void closeTransferCheckboxClicked(NSButton sender) {
	switch(sender.state()) {
	    case NSCell.OnState:
		Preferences.instance().setProperty("transfer.close", "true");
		break;
	    case NSCell.OffState:
		Preferences.instance().setProperty("transfer.close", "false");
		break;
	}
    }

    public void processCheckboxClicked(NSButton sender) {
	switch(sender.state()) {
	    case NSCell.OnState:
		Preferences.instance().setProperty("connection.download.postprocess", "true");
		break;
	    case NSCell.OffState:
		Preferences.instance().setProperty("connection.download.postprocess", "false");
		break;
	}
    }
    
    public void downloadPathButtonClicked(NSButton sender) {
	NSOpenPanel panel = new NSOpenPanel();
	panel.setCanChooseFiles(false);
	panel.setCanChooseDirectories(true);
	panel.setAllowsMultipleSelection(false);
	panel.beginSheetForDirectory(System.getProperty("user.home"), null, null, this.window(), this, new NSSelector("openPanelDidEnd", new Class[]{NSOpenPanel.class, int.class, Object.class}), null);
    }

    
    public void openPanelDidEnd(NSOpenPanel sheet, int returnCode, Object contextInfo) {
	switch(returnCode) {
	    case(NSPanel.OKButton): {
		NSArray selected = sheet.filenames();
		String filename;
		if((filename = (String)selected.lastObject()) != null) {
		    Preferences.instance().setProperty("connection.download.folder", filename);
		    downloadPathField.setStringValue(Preferences.instance().getProperty("connection.download.folder"));
		}		
		break;
	    }
	    case(NSPanel.CancelButton): {
		break;
	    }		
	}
    }

    public void transfermodeComboClicked(NSPopUpButton sender) {
	if(sender.selectedItem().title().equals(TRANSFERMODE_ASCII))
	    Preferences.instance().setProperty("ftp.transfermode", "ascii");
	else
	    Preferences.instance().setProperty("ftp.transfermode", "binary");
    }

    public void connectmodeComboClicked(NSPopUpButton sender) {
	if(sender.selectedItem().title().equals(CONNECTMODE_ACTIVE))
	    Preferences.instance().setProperty("ftp.connectmode", "active");
	else
	    Preferences.instance().setProperty("ftp.connectmode", "passive");
    }
    
    public void protocolComboClicked(NSPopUpButton sender) {
	if(sender.selectedItem().title().equals(PROTOCOL_FTP)) {
	    Preferences.instance().setProperty("connection.protocol.default", Session.FTP);
	    Preferences.instance().setProperty("connection.port.default", Session.FTP_PORT);
	}
	else {
	    Preferences.instance().setProperty("connection.protocol.default", Session.SFTP);
	    Preferences.instance().setProperty("connection.port.default", Session.SSH_PORT);
	}
    }
}
