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

import ch.cyberduck.core.Preferences;
import ch.cyberduck.core.SyncTransfer;
import ch.cyberduck.core.Transfer;
import ch.cyberduck.core.TransferAction;
import ch.cyberduck.ui.cocoa.application.*;

import org.apache.log4j.Logger;
import org.rococoa.Foundation;

/**
 * @version $Id$
 */
public class CDSyncPrompt extends CDTransferPrompt {
    private static Logger log = Logger.getLogger(CDSyncPrompt.class);

    public CDSyncPrompt(final CDWindowController parent, final Transfer transfer) {
        super(parent, transfer);
    }

    @Override
    public TransferAction prompt() {
        this.browserModel = new CDSyncPromptModel(this, transfer);
        return super.prompt();
    }

    public void setBrowserView(NSOutlineView view) {
        super.setBrowserView(view);
        {
            NSTableColumn c = NSTableColumn.tableColumnWithIdentifier(CDSyncPromptModel.SYNC_COLUMN);
            c.headerCell().setStringValue("");
            c.setMinWidth(20f);
            c.setWidth(20f);
            c.setMaxWidth(20f);
            c.setResizingMask(NSTableColumn.NSTableColumnAutoresizingMask);
            c.setEditable(false);
            c.setDataCell(NSImageCell.imageCell());
            c.dataCell().setAlignment(NSText.NSCenterTextAlignment);
            view.addTableColumn(c);
        }
        {
            NSTableColumn c = NSTableColumn.tableColumnWithIdentifier(CDSyncPromptModel.CREATE_COLUMN);
            c.headerCell().setStringValue("");
            c.setMinWidth(20f);
            c.setWidth(20f);
            c.setMaxWidth(20f);
            c.setResizingMask(NSTableColumn.NSTableColumnAutoresizingMask);
            c.setEditable(false);
            c.setDataCell(NSImageCell.imageCell());
            c.dataCell().setAlignment(NSText.NSCenterTextAlignment);
            view.addTableColumn(c);
        }
        view.sizeToFit();
    }

    public void callback(final int returncode) {
        if(returncode == DEFAULT_OPTION) { // Continue
            action = TransferAction.ACTION_OVERWRITE;
        }
        else if(returncode == ALTERNATE_OPTION) { // Abort
            action = TransferAction.ACTION_CANCEL;
        }
    }

    // ----------------------------------------------------------
    // Outlets
    // ----------------------------------------------------------

    public void setActionPopup(final NSPopUpButton actionPopup) {
        this.actionPopup = actionPopup;
        this.actionPopup.removeAllItems();

        final TransferAction defaultAction
                = ((SyncTransfer) transfer).getAction();

        final TransferAction[] actions = new TransferAction[]{
                SyncTransfer.ACTION_DOWNLOAD,
                SyncTransfer.ACTION_UPLOAD,
                SyncTransfer.ACTION_MIRROR
        };

        for(TransferAction action : actions) {
            if(null == action) {
                continue; //Not resumeable
            }
            this.actionPopup.addItemWithTitle(action.getLocalizableString());
            this.actionPopup.lastItem().setRepresentedObject(action.toString());
            if(action.equals(defaultAction)) {
                this.actionPopup.selectItem(actionPopup.lastItem());
            }
        }
        this.actionPopup.setTarget(this.id());
        this.actionPopup.setAction(Foundation.selector("actionPopupClicked:"));
    }

    public void actionPopupClicked(NSPopUpButton sender) {
        final TransferAction current = ((SyncTransfer) transfer).getAction();
        final TransferAction selected = TransferAction.forName(sender.selectedItem().representedObject());

        if(current.equals(selected)) {
            return;
        }

        Preferences.instance().setProperty("queue.sync.action.default", selected.toString());
        ((SyncTransfer) transfer).setTransferAction(selected);

        this.browserView.reloadData();
    }
}