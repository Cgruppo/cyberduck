package ch.cyberduck.ui.cocoa.delegate;

/*
 *  Copyright (c) 2008 David Kocher. All rights reserved.
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

import ch.cyberduck.core.Archive;
import ch.cyberduck.ui.cocoa.application.NSMenu;
import ch.cyberduck.ui.cocoa.application.NSMenuItem;

import org.rococoa.Foundation;

/**
 * @version $Id$
 */
public class ArchiveMenuDelegate extends MenuDelegate {

    public int numberOfItemsInMenu(NSMenu menu) {
        return Archive.getKnownArchives().length;
    }

    public boolean menuUpdateItemAtIndex(NSMenu menu, NSMenuItem item, int index, boolean shouldCancel) {
        final Archive archive = Archive.getKnownArchives()[index];
        item.setRepresentedObject(archive.getIdentifier());
        item.setTitle(archive.getIdentifier());
        item.setAction(Foundation.selector("archiveMenuClicked:"));
        return !shouldCancel;
    }
}
