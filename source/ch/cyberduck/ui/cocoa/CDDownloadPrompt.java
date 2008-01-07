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

import ch.cyberduck.core.Transfer;

import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public class CDDownloadPrompt extends CDTransferPrompt {
    private static Logger log = Logger.getLogger(CDDownloadPrompt.class);

    public CDDownloadPrompt(final CDWindowController parent, Transfer transfer) {
        super(parent, transfer);
    }

    public void awakeFromNib() {
        this.browserView.setDataSource(this.browserModel = new CDDownloadPromptModel(this, transfer));
        super.awakeFromNib();
    }
}