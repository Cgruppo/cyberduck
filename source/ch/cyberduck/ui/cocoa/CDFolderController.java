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

import ch.cyberduck.core.Path;
import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.NSMutableArray;
import com.apple.cocoa.foundation.NSNotification;
import org.apache.log4j.Logger;

/**
* @version $Id$
 */
public class CDFolderController {
    private static Logger log = Logger.getLogger(CDFolderController.class);
	
	//    private Path parent;
    
    private NSWindow sheet;
    public void setSheet(NSWindow sheet) {
		this.sheet = sheet;
    }
	
    private NSTextField folderField; /* IBOutlet */
    public void setFolderField(NSTextField folderField) {
		this.folderField = folderField;
    }
	
    public NSWindow window() {
		return this.sheet;
    }
	
    private static NSMutableArray allDocuments = new NSMutableArray();
	
    public CDFolderController() {
		//    public CDFolderController(Path parent) {
  //	this.parent = parent;
		allDocuments.addObject(this);
        if (false == NSApplication.loadNibNamed("Folder", this)) {
            log.fatal("Couldn't load Folder.nib");
            return;
        }
		}
	
    public void windowWillClose(NSNotification notification) {
		this.window().setDelegate(null);
		//	NSNotificationCenter.defaultCenter().removeObserver(this);
		allDocuments.removeObject(this);
    }
    
    public void closeSheet(Object sender) {
		// Ends a document modal session by specifying the sheet window, sheet. Also passes along a returnCode to the delegate.
		NSApplication.sharedApplication().endSheet(this.window(), ((NSButton)sender).tag());
    }
	
    public void newfolderSheetDidEnd(NSPanel sheet, int returncode, Object contextInfo) {
        log.debug("newfolderSheetDidEnd");
		sheet.orderOut(null);
		switch(returncode) {
			case(NSAlertPanel.DefaultReturn):
				((Path)contextInfo).mkdir(folderField.stringValue());
				break;
			case(NSAlertPanel.AlternateReturn):
				break;
		}
    }
    
	}
