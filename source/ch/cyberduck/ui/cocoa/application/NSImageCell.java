package ch.cyberduck.ui.cocoa.application;

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

import ch.cyberduck.ui.cocoa.foundation.NSCoding;
import ch.cyberduck.ui.cocoa.foundation.NSCopying;

import org.rococoa.Rococoa;

public abstract class NSImageCell implements NSCell, NSCopying, NSCoding {
    static final _Class CLASS = org.rococoa.Rococoa.createClass("NSImageCell", _Class.class);

    public static NSImageCell imageCell() {
        return Rococoa.cast(CLASS.alloc().init().autorelease(), NSImageCell.class);
    }

    public interface _Class extends org.rococoa.NSClass {
        NSImageCell alloc();
    }

    public abstract NSImageCell init();

    /**
     * Original signature : <code>NSImageAlignment imageAlignment()</code><br>
     * <i>native declaration : :51</i>
     */
    public abstract int imageAlignment();

    /**
     * Original signature : <code>void setImageAlignment(NSImageAlignment)</code><br>
     * <i>native declaration : :52</i>
     */
    public abstract void setImageAlignment(int newAlign);

    /**
     * Original signature : <code>imageScaling()</code><br>
     * <i>native declaration : :53</i>
     */
    public abstract com.sun.jna.Pointer imageScaling();
    /**
     * <i>native declaration : :54</i><br>
     * Conversion Error : /// Original signature : <code>void setImageScaling(null)</code><br>
     * - (void)setImageScaling:(null)newScaling; (Argument newScaling cannot be converted)
     */
    /**
     * Original signature : <code>NSImageFrameStyle imageFrameStyle()</code><br>
     * <i>native declaration : :55</i>
     */
    public abstract int imageFrameStyle();

    /**
     * Original signature : <code>void setImageFrameStyle(NSImageFrameStyle)</code><br>
     * <i>native declaration : :56</i>
     */
    public abstract void setImageFrameStyle(int newStyle);
}

