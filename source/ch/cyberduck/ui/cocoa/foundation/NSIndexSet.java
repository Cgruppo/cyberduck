package ch.cyberduck.ui.cocoa.foundation;

import org.rococoa.cocoa.foundation.NSUInteger;

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

/// <i>native declaration : :28</i>
public abstract class NSIndexSet implements NSObject {
    private static final _Class CLASS = org.rococoa.Rococoa.createClass("NSIndexSet", _Class.class);

    public static NSIndexSet indexSetWithIndex(int value) {
        return CLASS.indexSetWithIndex(value);
    }

    public static final long NSNotFound = 2147483647L;

    public interface _Class extends org.rococoa.NSClass {
        /**
         * Original signature : <code>indexSet()</code><br>
         * <i>native declaration : :30</i>
         */
        NSIndexSet indexSet();

        /**
         * Original signature : <code>indexSetWithIndex(NSUInteger)</code><br>
         * <i>native declaration : :31</i>
         */
        NSIndexSet indexSetWithIndex(long value);
        /**
         * <i>native declaration : :32</i><br>
         * Conversion Error : /// Original signature : <code>indexSetWithIndexesInRange(null)</code><br>
         * + (null)indexSetWithIndexesInRange:(null)range; (Argument range cannot be converted)
         */
    }

    /**
     * Original signature : <code>init()</code><br>
     * <i>native declaration : :34</i>
     */
    public abstract NSIndexSet init();

    /**
     * Original signature : <code>initWithIndex(NSUInteger)</code><br>
     * <i>native declaration : :35</i>
     */
    public abstract NSIndexSet initWithIndex(long value);
    /**
     * <i>native declaration : :36</i><br>
     * Conversion Error : /// Original signature : <code>initWithIndexesInRange(null)</code><br>
     * - (null)initWithIndexesInRange:(null)range; // designated initializer<br>
     *  (Argument range cannot be converted)
     */
    /**
     * Original signature : <code>initWithIndexSet(NSIndexSet*)</code><br>
     * designated initializer<br>
     * <i>native declaration : :37</i>
     */
    public abstract NSIndexSet initWithIndexSet(NSIndexSet indexSet);

    /**
     * Original signature : <code>BOOL isEqualToIndexSet(NSIndexSet*)</code><br>
     * <i>native declaration : :39</i>
     */
    public abstract boolean isEqualToIndexSet(NSIndexSet indexSet);

    /**
     * Original signature : <code>NSUInteger count()</code><br>
     * <i>native declaration : :41</i>
     */
    public abstract NSUInteger count();

    /**
     * The following six methods will return NSNotFound if there is no index in the set satisfying the query.<br>
     * Original signature : <code>NSUInteger firstIndex()</code><br>
     * <i>native declaration : :45</i>
     */
    public abstract NSUInteger firstIndex();

    /**
     * Original signature : <code>NSUInteger lastIndex()</code><br>
     * <i>native declaration : :46</i>
     */
    public abstract NSUInteger lastIndex();

    /**
     * Original signature : <code>NSUInteger indexGreaterThanIndex(NSUInteger)</code><br>
     * <i>native declaration : :47</i>
     */
    public abstract NSUInteger indexGreaterThanIndex(NSUInteger value);

    /**
     * Original signature : <code>NSUInteger indexLessThanIndex(NSUInteger)</code><br>
     * <i>native declaration : :48</i>
     */
    public abstract NSUInteger indexLessThanIndex(NSUInteger value);

    /**
     * Original signature : <code>NSUInteger indexGreaterThanOrEqualToIndex(NSUInteger)</code><br>
     * <i>native declaration : :49</i>
     */
    public abstract NSUInteger indexGreaterThanOrEqualToIndex(NSUInteger value);

    /**
     * Original signature : <code>NSUInteger indexLessThanOrEqualToIndex(NSUInteger)</code><br>
     * <i>native declaration : :50</i>
     */
    public abstract NSUInteger indexLessThanOrEqualToIndex(NSUInteger value);
    /**
     * <i>native declaration : :54</i><br>
     * Conversion Error : /**<br>
     *  * Fills up to bufferSize indexes in the specified range into the buffer and returns the number of indexes actually placed in the buffer; also modifies the optional range passed in by pointer to be "positioned" after the last index filled into the buffer.Example: if the index set contains the indexes 0, 2, 4, ..., 98, 100, for a buffer of size 10 and the range (20, 80) the buffer would contain 20, 22, ..., 38 and the range would be modified to (40, 60).<br>
     *  * Original signature : <code>NSUInteger getIndexes(NSUInteger*, NSUInteger, null)</code><br>
     *  * /<br>
     * - (NSUInteger)getIndexes:(NSUInteger*)indexBuffer maxCount:(NSUInteger)bufferSize inIndexRange:(null)range; (Argument range cannot be converted)
     */
    /**
     * <i>native declaration : :57</i><br>
     * Conversion Error : /// Original signature : <code>NSUInteger countOfIndexesInRange(null)</code><br>
     * - (NSUInteger)countOfIndexesInRange:(null)range; (Argument range cannot be converted)
     */
    /**
     * Original signature : <code>BOOL containsIndex(NSUInteger)</code><br>
     * <i>native declaration : :60</i>
     */
    public abstract boolean containsIndex(NSUInteger value);
    /**
     * <i>native declaration : :61</i><br>
     * Conversion Error : /// Original signature : <code>BOOL containsIndexesInRange(null)</code><br>
     * - (BOOL)containsIndexesInRange:(null)range; (Argument range cannot be converted)
     */
    /**
     * Original signature : <code>BOOL containsIndexes(NSIndexSet*)</code><br>
     * <i>native declaration : :62</i>
     */
    public abstract boolean containsIndexes(NSIndexSet indexSet);
    /**
     * <i>native declaration : :64</i><br>
     * Conversion Error : /// Original signature : <code>BOOL intersectsIndexesInRange(null)</code><br>
     * - (BOOL)intersectsIndexesInRange:(null)range; (Argument range cannot be converted)
     */
}
