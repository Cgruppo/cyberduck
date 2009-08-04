package ch.cyberduck.ui.cocoa.model;

/*
 * Copyright (c) 2002-2009 David Kocher. All rights reserved.
 *
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.PathReference;
import ch.cyberduck.ui.cocoa.foundation.NSObject;
import ch.cyberduck.ui.cocoa.foundation.NSString;

/**
 * Mapper between path references returned from the outline view model and its internal
 * string representation.
 *
 * @version $Id$
 */
public class CDPathReference extends PathReference<NSObject> {

    private NSObject reference;

    private int hashcode;

    public CDPathReference(String absolute) {
        this.reference = NSString.stringWithString(absolute);
        this.hashcode = absolute.hashCode();
    }

    public CDPathReference(NSObject absolute) {
        this.reference = absolute;
        this.hashcode = absolute.toString().hashCode();
    }

    public NSObject unique() {
        return reference;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }
}