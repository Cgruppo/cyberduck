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

import ch.cyberduck.ui.cocoa.foundation.NSArray;
import ch.cyberduck.ui.cocoa.foundation.NSObject;

import org.rococoa.cocoa.foundation.NSRect;
import org.rococoa.Rococoa;

/// <i>native declaration : :16</i>
public abstract class NSComboBox extends NSTextField {

    public static interface DataSource {
        int numberOfItemsInComboBox(NSComboBox combo);

        String comboBox_objectValueForItemAtIndex(final NSComboBox sender, final int row);
    }

    /**
     * Original signature : <code>BOOL hasVerticalScroller()</code><br>
     * <i>native declaration : :21</i>
     */
    public abstract boolean hasVerticalScroller();

    /**
     * Original signature : <code>void setHasVerticalScroller(BOOL)</code><br>
     * <i>native declaration : :22</i>
     */
    public abstract void setHasVerticalScroller(boolean flag);

    /**
     * Original signature : <code>intercellSpacing()</code><br>
     * <i>native declaration : :23</i>
     */
    public abstract NSObject intercellSpacing();
    /**
     * <i>native declaration : :24</i><br>
     * Conversion Error : /// Original signature : <code>void setIntercellSpacing(null)</code><br>
     * - (void)setIntercellSpacing:(null)aSize; (Argument aSize cannot be converted)
     */
    /**
     * Original signature : <code>CGFloat itemHeight()</code><br>
     * <i>native declaration : :25</i>
     */
    public abstract float itemHeight();

    /**
     * Original signature : <code>void setItemHeight(CGFloat)</code><br>
     * <i>native declaration : :26</i>
     */
    public abstract void setItemHeight(float itemHeight);

    /**
     * Original signature : <code>NSInteger numberOfVisibleItems()</code><br>
     * <i>native declaration : :27</i>
     */
    public abstract int numberOfVisibleItems();

    /**
     * Original signature : <code>void setNumberOfVisibleItems(NSInteger)</code><br>
     * <i>native declaration : :28</i>
     */
    public abstract void setNumberOfVisibleItems(int visibleItems);

    /**
     * Original signature : <code>void setButtonBordered(BOOL)</code><br>
     * <i>native declaration : :31</i>
     */
    public abstract void setButtonBordered(boolean flag);

    /**
     * Original signature : <code>BOOL isButtonBordered()</code><br>
     * <i>native declaration : :32</i>
     */
    public abstract boolean isButtonBordered();

    /**
     * Original signature : <code>void reloadData()</code><br>
     * <i>native declaration : :35</i>
     */
    public abstract void reloadData();

    /**
     * Original signature : <code>void noteNumberOfItemsChanged()</code><br>
     * <i>native declaration : :36</i>
     */
    public abstract void noteNumberOfItemsChanged();

    /**
     * Original signature : <code>void setUsesDataSource(BOOL)</code><br>
     * <i>native declaration : :38</i>
     */
    public abstract void setUsesDataSource(boolean flag);

    /**
     * Original signature : <code>BOOL usesDataSource()</code><br>
     * <i>native declaration : :39</i>
     */
    public abstract boolean usesDataSource();

    /**
     * Original signature : <code>void scrollItemAtIndexToTop(NSInteger)</code><br>
     * <i>native declaration : :41</i>
     */
    public abstract void scrollItemAtIndexToTop(int index);

    /**
     * Original signature : <code>void scrollItemAtIndexToVisible(NSInteger)</code><br>
     * <i>native declaration : :42</i>
     */
    public abstract void scrollItemAtIndexToVisible(int index);

    /**
     * Original signature : <code>void selectItemAtIndex(NSInteger)</code><br>
     * <i>native declaration : :44</i>
     */
    public abstract void selectItemAtIndex(int index);

    /**
     * Original signature : <code>void deselectItemAtIndex(NSInteger)</code><br>
     * <i>native declaration : :45</i>
     */
    public abstract void deselectItemAtIndex(int index);

    /**
     * Original signature : <code>NSInteger indexOfSelectedItem()</code><br>
     * <i>native declaration : :46</i>
     */
    public abstract int indexOfSelectedItem();

    /**
     * Original signature : <code>NSInteger numberOfItems()</code><br>
     * <i>native declaration : :47</i>
     */
    public abstract int numberOfItems();

    /**
     * Original signature : <code>BOOL completes()</code><br>
     * <i>native declaration : :49</i>
     */
    public abstract boolean completes();

    /**
     * Original signature : <code>void setCompletes(BOOL)</code><br>
     * <i>native declaration : :50</i>
     */
    public abstract void setCompletes(boolean completes);

    /**
     * These two methods can only be used when usesDataSource is YES<br>
     * Original signature : <code>id dataSource()</code><br>
     * <i>native declaration : :53</i>
     */
    public abstract org.rococoa.ID dataSource();

    /**
     * Original signature : <code>void setDataSource(id)</code><br>
     * <i>native declaration : :54</i>
     */
    public abstract void setDataSource(org.rococoa.ID aSource);

    /**
     * These methods can only be used when usesDataSource is NO<br>
     * Original signature : <code>void addItemWithObjectValue(id)</code><br>
     * <i>native declaration : :57</i>
     */
    public abstract void addItemWithObjectValue(NSObject object);

    /**
     * Original signature : <code>void addItemsWithObjectValues(NSArray*)</code><br>
     * <i>native declaration : :58</i>
     */
    public abstract void addItemsWithObjectValues(com.sun.jna.Pointer objects);

    /**
     * Original signature : <code>void insertItemWithObjectValue(id, NSInteger)</code><br>
     * <i>native declaration : :59</i>
     */
    public abstract void insertItemWithObjectValue_atIndex(NSObject object, int index);

    /**
     * Original signature : <code>void removeItemWithObjectValue(id)</code><br>
     * <i>native declaration : :60</i>
     */
    public abstract void removeItemWithObjectValue(NSObject object);

    /**
     * Original signature : <code>void removeItemAtIndex(NSInteger)</code><br>
     * <i>native declaration : :61</i>
     */
    public abstract void removeItemAtIndex(int index);

    /**
     * Original signature : <code>void removeAllItems()</code><br>
     * <i>native declaration : :62</i>
     */
    public abstract void removeAllItems();

    /**
     * Original signature : <code>void selectItemWithObjectValue(id)</code><br>
     * <i>native declaration : :63</i>
     */
    public abstract void selectItemWithObjectValue(NSObject object);

    /**
     * Original signature : <code>id itemObjectValueAtIndex(NSInteger)</code><br>
     * <i>native declaration : :64</i>
     */
    public abstract NSObject itemObjectValueAtIndex(int index);

    /**
     * Original signature : <code>id objectValueOfSelectedItem()</code><br>
     * <i>native declaration : :65</i>
     */
    public abstract NSObject objectValueOfSelectedItem();

    /**
     * Original signature : <code>NSInteger indexOfItemWithObjectValue(id)</code><br>
     * <i>native declaration : :66</i>
     */
    public abstract int indexOfItemWithObjectValue(NSObject object);

    /**
     * Original signature : <code>NSArray* objectValues()</code><br>
     * <i>native declaration : :67</i>
     */
    public abstract NSArray objectValues();

    public static final String ComboBoxWillPopUpNotification = "NSComboBoxWillPopUpNotification";
    public static final String ComboBoxWillDismissNotification = "NSComboBoxWillDismissNotification";
    public static final String ComboBoxSelectionDidChangeNotification = "NSComboBoxSelectionDidChangeNotification";
    public static final String ComboBoxSelectionIsChangingNotification = "NSComboBoxSelectionIsChangingNotification";

}
