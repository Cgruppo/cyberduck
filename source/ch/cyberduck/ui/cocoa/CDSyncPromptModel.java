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

import ch.cyberduck.core.*;
import ch.cyberduck.ui.cocoa.foundation.NSAttributedString;
import ch.cyberduck.ui.cocoa.foundation.NSObject;

/**
 * @version $Id$
 */
public class CDSyncPromptModel extends CDTransferPromptModel {

    public CDSyncPromptModel(CDWindowController c, Transfer transfer) {
        super(c, transfer);
    }

    /**
     * Filtering what files are displayed. Used to
     * decide which files to include in the prompt dialog
     */
    private PathFilter<Path> filter;

    protected PathFilter<Path> filter() {
        if(null == filter) {
            filter = new PromptFilter() {
                @Override
                public boolean accept(Path child) {
                    log.debug("accept:" + child);
                    return super.accept(child);
                }
            };
        }
        return filter;
    }

    /**
     * A column indicating if the file will be uploaded or downloaded
     */
    protected static final String SYNC_COLUMN = "SYNC";
    /**
     * A column indicating if the file is missing and will be created
     */
    protected static final String CREATE_COLUMN = "CREATE";

    @Override
    protected NSObject objectValueForItem(final Path item, final String identifier) {
        if(identifier.equals(SIZE_COLUMN)) {
            SyncTransfer.Comparison compare = ((SyncTransfer) transfer).compare(item);
            return NSAttributedString.attributedStringWithAttributes(Status.getSizeAsString(
                    compare.equals(SyncTransfer.COMPARISON_REMOTE_NEWER) ? item.attributes.getSize() : item.getLocal().attributes.getSize()),
                    CDTableCellAttributes.browserFontRightAlignment());
        }
        if(identifier.equals(SYNC_COLUMN)) {
            SyncTransfer.Comparison compare = ((SyncTransfer) transfer).compare(item);
            if(compare.equals(SyncTransfer.COMPARISON_REMOTE_NEWER)) {
                return CDIconCache.instance().iconForName("arrowDown", 16);
            }
            if(compare.equals(SyncTransfer.COMPARISON_LOCAL_NEWER)) {
                return CDIconCache.instance().iconForName("arrowUp", 16);
            }
            return null;
        }
        if(identifier.equals(WARNING_COLUMN)) {
            if(item.attributes.isFile()) {
                if(transfer.exists(item)) {
                    if(item.attributes.getSize() == 0) {
                        return ALERT_ICON;
                    }
                }
                if(transfer.exists(item.getLocal())) {
                    if(item.getLocal().attributes.getSize() == 0) {
                        return ALERT_ICON;
                    }
                }
            }
            return null;
        }
        if(identifier.equals(CREATE_COLUMN)) {
            if(!(transfer.exists(item) && transfer.exists(item.getLocal()))) {
                return CDIconCache.instance().iconForName("plus", 16);
            }
            return null;
        }
        return super.objectValueForItem(item, identifier);
    }
}