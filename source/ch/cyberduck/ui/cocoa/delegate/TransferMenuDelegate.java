package ch.cyberduck.ui.cocoa.delegate;

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

import ch.cyberduck.core.Path;
import ch.cyberduck.core.Local;
import ch.cyberduck.ui.cocoa.CDIconCache;
import ch.cyberduck.ui.cocoa.application.NSCell;
import ch.cyberduck.ui.cocoa.application.NSMenu;
import ch.cyberduck.ui.cocoa.application.NSMenuItem;
import ch.cyberduck.ui.cocoa.application.NSWorkspace;

import org.apache.log4j.Logger;
import org.rococoa.Foundation;

import java.util.List;

/**
 * @version $Id$
 */
public class TransferMenuDelegate extends MenuDelegate {
    private static Logger log = Logger.getLogger(TransferMenuDelegate.class);

    /**
     *
     */
    private List<Path> roots;

    public TransferMenuDelegate(List<Path> roots) {
        this.roots = roots;
    }

    public int numberOfItemsInMenu(NSMenu menu) {
        return roots.size();
    }

    public boolean menuUpdateItemAtIndex(NSMenu menu, NSMenuItem item, int index, boolean shouldCancel) {
        Path path = roots.get(index);
        item.setTitle(path.getName());
        if(path.getLocal().exists()) {
            item.setEnabled(true);
            item.setTarget(this.id());
            item.setAction(Foundation.selector("reveal:"));
        }
        else {
            item.setEnabled(false);
            item.setTarget(null);
        }
        item.setState(path.getLocal().exists() ? NSCell.NSOnState : NSCell.NSOffState);
        item.setRepresentedObject(path.getLocal().getAbsolute());
        item.setImage(CDIconCache.instance().iconForPath(path, 16));
        return !shouldCancel;
    }

    public void reveal(final NSMenuItem sender) {
        Local l = new Local(sender.representedObject());
        // If a second path argument is specified, a new file viewer is opened. If you specify an
        // empty string (@"") for this parameter, the file is selected in the main viewer.
        if(!NSWorkspace.sharedWorkspace().selectFile(l.getAbsolute(), l.getParent().getAbsolute())) {
            log.error("reveal:" + l.getAbsolute());
        }
    }
}