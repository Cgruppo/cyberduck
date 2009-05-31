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
import ch.cyberduck.ui.cocoa.CDBrowserController;
import ch.cyberduck.ui.cocoa.application.NSMenuItem;

import java.util.List;

/**
 * @version $Id:$
 */
public class BackPathHistoryMenuDelegate extends PathHistoryMenuDelegate {

    public BackPathHistoryMenuDelegate(CDBrowserController controller) {
        super(controller);
    }

    public List<Path> getHistory() {
        return controller.getBackHistory();
    }

    public void clearMenuItemClicked(NSMenuItem sender) {
        controller.clearBackHistory();
    }
}
