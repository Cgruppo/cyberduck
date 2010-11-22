package ch.cyberduck.ui;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
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

/**
 * @version $Id:$
 */
public abstract class AbstractDateFormatter implements DateFormatter {

    public String getShortFormat(long milliseconds) {
        return this.getShortFormat(milliseconds, true);
    }

    public String getMediumFormat(long milliseconds) {
        return this.getMediumFormat(milliseconds, true);
    }

    public String getLongFormat(long milliseconds) {
        return this.getLongFormat(milliseconds, true);
    }
}
