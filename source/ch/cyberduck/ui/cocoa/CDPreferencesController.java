package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2005 David Kocher. All rights reserved.
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
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.util.URLSchemeHandlerConfiguration;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.foundation.*;
import ch.cyberduck.ui.cocoa.odb.EditorFactory;

import org.apache.log4j.Logger;
import org.apache.commons.lang.StringUtils;
import org.jets3t.service.model.S3Bucket;
import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.Selector;
import org.rococoa.cocoa.foundation.NSUInteger;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.enterprisedt.net.ftp.FTPTransferType;

/**
 * @version $Id$
 */
public class CDPreferencesController extends CDWindowController {
    private static Logger log = Logger.getLogger(CDPreferencesController.class);

    private static CDPreferencesController instance = null;

    public static CDPreferencesController instance() {
        synchronized(NSApplication.sharedApplication()) {
            if(null == instance) {
                instance = new CDPreferencesController();
                instance.loadBundle();
            }
            return instance;
        }
    }

    private CDPreferencesController() {
        ;
    }

    @Override
    protected String getBundleName() {
        return "Preferences";
    }

    private NSTabView tabView;

    public void setTabView(NSTabView tabView) {
        this.tabView = tabView;
    }

    @Outlet
    private NSView panelGeneral;
    @Outlet
    private NSView panelInterface;
    @Outlet
    private NSView panelTransfer;
    @Outlet
    private NSView panelFTP;
    @Outlet
    private NSView panelSFTP;
    @Outlet
    private NSView panelS3;
    @Outlet
    private NSView panelBandwidth;
    @Outlet
    private NSView panelAdvanced;
    @Outlet
    private NSView panelUpdate;

    public void setPanelUpdate(NSView panelUpdate) {
        this.panelUpdate = panelUpdate;
    }

    public void setPanelAdvanced(NSView panelAdvanced) {
        this.panelAdvanced = panelAdvanced;
    }

    public void setPanelBandwidth(NSView panelBandwidth) {
        this.panelBandwidth = panelBandwidth;
    }

    public void setPanelSFTP(NSView panelSFTP) {
        this.panelSFTP = panelSFTP;
    }

    public void setPanelFTP(NSView panelFTP) {
        this.panelFTP = panelFTP;
    }

    public void setPanelS3(NSView panelS3) {
        this.panelS3 = panelS3;
    }

    public void setPanelTransfer(NSView panelTransfer) {
        this.panelTransfer = panelTransfer;
    }

    public void setPanelInterface(NSView panelInterface) {
        this.panelInterface = panelInterface;
    }

    public void setPanelGeneral(NSView panelGeneral) {
        this.panelGeneral = panelGeneral;
    }

    @Override
    protected void invalidate() {
        HostCollection.defaultCollection().removeListener(bookmarkCollectionListener);
        super.invalidate();
        instance = null;
    }

    @Override
    public void setWindow(NSWindow window) {
        window.setExcludedFromWindowsMenu(true);
        super.setWindow(window);
    }

    @Override
    public void awakeFromNib() {
        this.window.setShowsToolbarButton(false);
        this.window.center();

        this.transfermodeComboboxClicked(this.transfermodeCombobox);
        this.chmodDownloadTypePopupChanged(this.chmodDownloadTypePopup);
        this.chmodUploadTypePopupChanged(this.chmodUploadTypePopup);

        boolean chmodDownloadDefaultEnabled = Preferences.instance().getBoolean("queue.download.changePermissions")
                && Preferences.instance().getBoolean("queue.download.permissions.useDefault");
        this.downerr.setEnabled(chmodDownloadDefaultEnabled);
        this.downerr.setTarget(this.id());
        this.downerr.setAction(Foundation.selector("defaultPermissionsDownloadChanged:"));
        this.downerw.setEnabled(chmodDownloadDefaultEnabled);
        this.downerw.setTarget(this.id());
        this.downerw.setAction(Foundation.selector("defaultPermissionsDownloadChanged:"));
        this.downerx.setEnabled(chmodDownloadDefaultEnabled);
        this.downerx.setTarget(this.id());
        this.downerx.setAction(Foundation.selector("defaultPermissionsDownloadChanged:"));

        this.dgroupr.setEnabled(chmodDownloadDefaultEnabled);
        this.dgroupr.setTarget(this.id());
        this.dgroupr.setAction(Foundation.selector("defaultPermissionsDownloadChanged:"));
        this.dgroupw.setEnabled(chmodDownloadDefaultEnabled);
        this.dgroupw.setTarget(this.id());
        this.dgroupw.setAction(Foundation.selector("defaultPermissionsDownloadChanged:"));
        this.dgroupx.setEnabled(chmodDownloadDefaultEnabled);
        this.dgroupx.setTarget(this.id());
        this.dgroupx.setAction(Foundation.selector("defaultPermissionsDownloadChanged:"));

        this.dotherr.setEnabled(chmodDownloadDefaultEnabled);
        this.dotherr.setTarget(this.id());
        this.dotherr.setAction(Foundation.selector("defaultPermissionsDownloadChanged:"));
        this.dotherw.setEnabled(chmodDownloadDefaultEnabled);
        this.dotherw.setTarget(this.id());
        this.dotherw.setAction(Foundation.selector("defaultPermissionsDownloadChanged:"));
        this.dotherx.setEnabled(chmodDownloadDefaultEnabled);
        this.dotherx.setTarget(this.id());
        this.dotherx.setAction(Foundation.selector("defaultPermissionsDownloadChanged:"));

        boolean chmodUploadDefaultEnabled = Preferences.instance().getBoolean("queue.upload.changePermissions")
                && Preferences.instance().getBoolean("queue.upload.permissions.useDefault");
        this.uownerr.setEnabled(chmodUploadDefaultEnabled);
        this.uownerr.setTarget(this.id());
        this.uownerr.setAction(Foundation.selector("defaultPermissionsUploadChanged:"));
        this.uownerw.setEnabled(chmodUploadDefaultEnabled);
        this.uownerw.setTarget(this.id());
        this.uownerw.setAction(Foundation.selector("defaultPermissionsUploadChanged:"));
        this.uownerx.setEnabled(chmodUploadDefaultEnabled);
        this.uownerx.setTarget(this.id());
        this.uownerx.setAction(Foundation.selector("defaultPermissionsUploadChanged:"));

        this.ugroupr.setEnabled(chmodUploadDefaultEnabled);
        this.ugroupr.setTarget(this.id());
        this.ugroupr.setAction(Foundation.selector("defaultPermissionsUploadChanged:"));
        this.ugroupw.setEnabled(chmodUploadDefaultEnabled);
        this.ugroupw.setTarget(this.id());
        this.ugroupw.setAction(Foundation.selector("defaultPermissionsUploadChanged:"));
        this.ugroupx.setEnabled(chmodUploadDefaultEnabled);
        this.ugroupx.setTarget(this.id());
        this.ugroupx.setAction(Foundation.selector("defaultPermissionsUploadChanged:"));

        this.uotherr.setEnabled(chmodUploadDefaultEnabled);
        this.uotherr.setTarget(this.id());
        this.uotherr.setAction(Foundation.selector("defaultPermissionsUploadChanged:"));
        this.uotherw.setEnabled(chmodUploadDefaultEnabled);
        this.uotherw.setTarget(this.id());
        this.uotherw.setAction(Foundation.selector("defaultPermissionsUploadChanged:"));
        this.uotherx.setEnabled(chmodUploadDefaultEnabled);
        this.uotherx.setTarget(this.id());
        this.uotherx.setAction(Foundation.selector("defaultPermissionsUploadChanged:"));

        int i = -1;
        tabView.tabViewItemAtIndex(++i).setView(panelGeneral);
        tabView.tabViewItemAtIndex(++i).setView(panelInterface);
        tabView.tabViewItemAtIndex(++i).setView(panelTransfer);
        tabView.tabViewItemAtIndex(++i).setView(panelFTP);
        tabView.tabViewItemAtIndex(++i).setView(panelSFTP);
        tabView.tabViewItemAtIndex(++i).setView(panelS3);
        tabView.tabViewItemAtIndex(++i).setView(panelBandwidth);
        tabView.tabViewItemAtIndex(++i).setView(panelAdvanced);
        tabView.tabViewItemAtIndex(++i).setView(panelUpdate);

        super.awakeFromNib();
    }

    private static final String TRANSFERMODE_AUTO = Locale.localizedString("Auto");
    private static final String TRANSFERMODE_BINARY = Locale.localizedString("Binary");
    private static final String TRANSFERMODE_ASCII = Locale.localizedString("ASCII");

    private static final String UNIX_LINE_ENDINGS = Locale.localizedString("Unix Line Endings (LF)");
    private static final String MAC_LINE_ENDINGS = Locale.localizedString("Mac Line Endings (CR)");
    private static final String WINDOWS_LINE_ENDINGS = Locale.localizedString("Windows Line Endings (CRLF)");

    // ----------------------------------------------------------
    // Outlets
    // ----------------------------------------------------------

    @Outlet
    private NSPopUpButton editorCombobox; //IBOutlet

    public void setEditorCombobox(NSPopUpButton editorCombobox) {
        this.editorCombobox = editorCombobox;
        this.editorCombobox.setAutoenablesItems(false);
        this.editorCombobox.removeAllItems();
        java.util.Map editors = EditorFactory.getSupportedOdbEditors();
        java.util.Iterator editorNames = editors.keySet().iterator();
        java.util.Iterator editorIdentifiers = editors.values().iterator();
        while(editorNames.hasNext()) {
            String editor = (String) editorNames.next();
            String identifier = (String) editorIdentifiers.next();
            this.editorCombobox.addItemWithTitle(editor);
            final boolean enabled = EditorFactory.getInstalledOdbEditors().containsValue(identifier);
            this.editorCombobox.itemWithTitle(editor).setEnabled(enabled);
            if(enabled) {
                final String path = NSWorkspace.sharedWorkspace().absolutePathForAppBundleWithIdentifier(identifier);
                if(StringUtils.isNotEmpty(path)) {
                    this.editorCombobox.itemWithTitle(editor).setImage(CDIconCache.instance().iconForPath(
                        LocalFactory.createLocal(path), 16));
                }
            }
        }
        this.editorCombobox.setTarget(this.id());
        this.editorCombobox.setAction(Foundation.selector("editorComboboxClicked:"));
        this.editorCombobox.selectItemWithTitle(Preferences.instance().getProperty("editor.name"));
    }

    public void editorComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("editor.name", sender.titleOfSelectedItem());
        final String selected = EditorFactory.getSupportedOdbEditors().get(sender.titleOfSelectedItem());
        Preferences.instance().setProperty("editor.bundleIdentifier", selected);
        EditorFactory.setSelectedEditor(selected);
        CDBrowserController.validateToolbarItems();
    }

    @Outlet
    private NSPopUpButton bookmarkSizePopup; //IBOutlet

    public void setBookmarkSizePopup(NSPopUpButton bookmarkSizePopup) {
        this.bookmarkSizePopup = bookmarkSizePopup;
        this.bookmarkSizePopup.setTarget(this.id());
        this.bookmarkSizePopup.setAction(Foundation.selector("bookmarkSizePopupClicked:"));
        final int size = Preferences.instance().getInteger("bookmark.icon.size");
        for(int i = 0; i < this.bookmarkSizePopup.numberOfItems(); i++) {
            this.bookmarkSizePopup.itemAtIndex(i).setState(NSCell.NSOffState);
        }
        if(CDBookmarkCell.SMALL_BOOKMARK_SIZE == size) {
            this.bookmarkSizePopup.selectItemAtIndex(0);
        }
        if(CDBookmarkCell.MEDIUM_BOOKMARK_SIZE == size) {
            this.bookmarkSizePopup.selectItemAtIndex(1);
        }
        if(CDBookmarkCell.LARGE_BOOKMARK_SIZE == size) {
            this.bookmarkSizePopup.selectItemAtIndex(2);
        }
    }

    public void bookmarkSizePopupClicked(NSPopUpButton sender) {
        if(sender.indexOfSelectedItem() == 0) {
            Preferences.instance().setProperty("bookmark.icon.size", CDBookmarkCell.SMALL_BOOKMARK_SIZE);
        }
        if(sender.indexOfSelectedItem() == 1) {
            Preferences.instance().setProperty("bookmark.icon.size", CDBookmarkCell.MEDIUM_BOOKMARK_SIZE);
        }
        if(sender.indexOfSelectedItem() == 2) {
            Preferences.instance().setProperty("bookmark.icon.size", CDBookmarkCell.LARGE_BOOKMARK_SIZE);
        }
        CDBrowserController.updateBookmarkTableRowHeight();
    }

    @Outlet
    private NSButton openUntitledBrowserCheckbox; //IBOutlet

    public void setOpenUntitledBrowserCheckbox(NSButton openUntitledBrowserCheckbox) {
        this.openUntitledBrowserCheckbox = openUntitledBrowserCheckbox;
        this.openUntitledBrowserCheckbox.setTarget(this.id());
        this.openUntitledBrowserCheckbox.setAction(Foundation.selector("openUntitledBrowserCheckboxClicked:"));
        this.openUntitledBrowserCheckbox.setState(Preferences.instance().getBoolean("browser.openUntitled") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void openUntitledBrowserCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.openUntitled", enabled);
    }

    @Outlet
    private NSButton browserSerializeCheckbox; //IBOutlet

    public void setBrowserSerializeCheckbox(NSButton browserSerializeCheckbox) {
        this.browserSerializeCheckbox = browserSerializeCheckbox;
        this.browserSerializeCheckbox.setTarget(this.id());
        this.browserSerializeCheckbox.setAction(Foundation.selector("browserSerializeCheckboxClicked:"));
        this.browserSerializeCheckbox.setState(Preferences.instance().getBoolean("browser.serialize") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void browserSerializeCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.serialize", enabled);
    }

    @Outlet
    private NSPopUpButton defaultBookmarkCombobox; //IBOutlet

    private final CollectionListener<Host> bookmarkCollectionListener = new CollectionListener<Host>() {
        public void collectionItemAdded(Host bookmark) {
            CDPreferencesController.this.defaultBookmarkCombobox.addItemWithTitle(bookmark.getNickname());
            CDPreferencesController.this.defaultBookmarkCombobox.itemWithTitle(bookmark.getNickname()).setImage(CDIconCache.instance().iconForName("cyberduck-document", 16));
            CDPreferencesController.this.defaultBookmarkCombobox.lastItem().setRepresentedObject(bookmark.getNickname());
        }

        public void collectionItemRemoved(Host bookmark) {
            if(CDPreferencesController.this.defaultBookmarkCombobox.titleOfSelectedItem().equals(bookmark.getNickname())) {
                Preferences.instance().deleteProperty("browser.defaultBookmark");
            }
            int i = CDPreferencesController.this.defaultBookmarkCombobox.menu().indexOfItemWithRepresentedObject(bookmark.getNickname());
            if(i > -1) {
                CDPreferencesController.this.defaultBookmarkCombobox.removeItemAtIndex(i);
            }
        }

        public void collectionItemChanged(Host bookmark) {
            ;
        }
    };

    public void setDefaultBookmarkCombobox(NSPopUpButton defaultBookmarkCombobox) {
        this.defaultBookmarkCombobox = defaultBookmarkCombobox;
        this.defaultBookmarkCombobox.setToolTip(Locale.localizedString("Bookmarks"));
        this.defaultBookmarkCombobox.removeAllItems();
        this.defaultBookmarkCombobox.addItemWithTitle(Locale.localizedString("None"));
        this.defaultBookmarkCombobox.menu().addItem(NSMenuItem.separatorItem());
        for(Host bookmark : HostCollection.defaultCollection()) {
            this.defaultBookmarkCombobox.addItemWithTitle(bookmark.getNickname());
            this.defaultBookmarkCombobox.itemWithTitle(bookmark.getNickname()).setImage(CDIconCache.instance().iconForName("cyberduck-document", 16));
            this.defaultBookmarkCombobox.lastItem().setRepresentedObject(bookmark.getNickname());
        }
        HostCollection.defaultCollection().addListener(bookmarkCollectionListener);
        this.defaultBookmarkCombobox.setTarget(this.id());
        final Selector action = Foundation.selector("defaultBookmarkComboboxClicked:");
        this.defaultBookmarkCombobox.setAction(action);
        String defaultBookmarkNickname = Preferences.instance().getProperty("browser.defaultBookmark");
        if(null == defaultBookmarkNickname) {
            this.defaultBookmarkCombobox.selectItemWithTitle(Locale.localizedString("None"));

        }
        else {
            int i = this.defaultBookmarkCombobox.indexOfItemWithTitle(defaultBookmarkNickname);
            if(i > -1) {
                this.defaultBookmarkCombobox.selectItemAtIndex(i);
            }
            else {
                this.defaultBookmarkCombobox.selectItemWithTitle(Locale.localizedString("None"));
            }
        }
    }

    public void defaultBookmarkComboboxClicked(NSPopUpButton sender) {
        if(Locale.localizedString("None").equals(sender.titleOfSelectedItem())) {
            Preferences.instance().deleteProperty("browser.defaultBookmark");
        }
        else {
            Preferences.instance().setProperty("browser.defaultBookmark", sender.titleOfSelectedItem());
        }
    }

    @Outlet
    private NSPopUpButton encodingCombobox; //IBOutlet

    public void setEncodingCombobox(NSPopUpButton encodingCombobox) {
        this.encodingCombobox = encodingCombobox;
        this.encodingCombobox.setTarget(this.id());
        this.encodingCombobox.setAction(Foundation.selector("encodingComboboxClicked:"));
        this.encodingCombobox.removeAllItems();
        this.encodingCombobox.addItemsWithTitles(NSArray.arrayWithObjects(CDMainController.availableCharsets()));
        this.encodingCombobox.selectItemWithTitle(Preferences.instance().getProperty("browser.charset.encoding"));
    }

    public void encodingComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("browser.charset.encoding", sender.titleOfSelectedItem());
    }

    @Outlet
    private NSPopUpButton chmodUploadTypePopup;

    public void setChmodUploadTypePopup(NSPopUpButton chmodUploadTypePopup) {
        this.chmodUploadTypePopup = chmodUploadTypePopup;
        this.chmodUploadTypePopup.selectItemAtIndex(0);
        this.chmodUploadTypePopup.setTarget(this.id());
        final Selector action = Foundation.selector("chmodUploadTypePopupChanged:");
        this.chmodUploadTypePopup.setAction(action);
    }

    private void chmodUploadTypePopupChanged(NSPopUpButton sender) {
        Permission p = null;
        if(sender.selectedItem().tag() == 0) {
            p = new Permission(Preferences.instance().getInteger("queue.upload.permissions.file.default"));
        }
        if(sender.selectedItem().tag() == 1) {
            p = new Permission(Preferences.instance().getInteger("queue.upload.permissions.folder.default"));
        }
        if(null == p) {
            log.error("No selected item for:" + sender);
            return;
        }
        boolean[] ownerPerm = p.getOwnerPermissions();
        boolean[] groupPerm = p.getGroupPermissions();
        boolean[] otherPerm = p.getOtherPermissions();

        uownerr.setState(ownerPerm[Permission.READ] ? NSCell.NSOnState : NSCell.NSOffState);
        uownerw.setState(ownerPerm[Permission.WRITE] ? NSCell.NSOnState : NSCell.NSOffState);
        uownerx.setState(ownerPerm[Permission.EXECUTE] ? NSCell.NSOnState : NSCell.NSOffState);

        ugroupr.setState(groupPerm[Permission.READ] ? NSCell.NSOnState : NSCell.NSOffState);
        ugroupw.setState(groupPerm[Permission.WRITE] ? NSCell.NSOnState : NSCell.NSOffState);
        ugroupx.setState(groupPerm[Permission.EXECUTE] ? NSCell.NSOnState : NSCell.NSOffState);

        uotherr.setState(otherPerm[Permission.READ] ? NSCell.NSOnState : NSCell.NSOffState);
        uotherw.setState(otherPerm[Permission.WRITE] ? NSCell.NSOnState : NSCell.NSOffState);
        uotherx.setState(otherPerm[Permission.EXECUTE] ? NSCell.NSOnState : NSCell.NSOffState);
    }

    @Outlet
    private NSPopUpButton chmodDownloadTypePopup;

    public void setChmodDownloadTypePopup(NSPopUpButton chmodDownloadTypePopup) {
        this.chmodDownloadTypePopup = chmodDownloadTypePopup;
        this.chmodDownloadTypePopup.selectItemAtIndex(0);
        this.chmodDownloadTypePopup.setTarget(this.id());
        final Selector action = Foundation.selector("chmodDownloadTypePopupChanged:");
        this.chmodDownloadTypePopup.setAction(action);
    }

    private void chmodDownloadTypePopupChanged(NSPopUpButton sender) {
        Permission p = null;
        if(sender.selectedItem().tag() == 0) {
            p = new Permission(Preferences.instance().getInteger("queue.download.permissions.file.default"));
        }
        if(sender.selectedItem().tag() == 1) {
            p = new Permission(Preferences.instance().getInteger("queue.download.permissions.folder.default"));
        }
        if(null == p) {
            log.error("No selected item for:" + sender);
            return;
        }
        boolean[] ownerPerm = p.getOwnerPermissions();
        boolean[] groupPerm = p.getGroupPermissions();
        boolean[] otherPerm = p.getOtherPermissions();

        downerr.setState(ownerPerm[Permission.READ] ? NSCell.NSOnState : NSCell.NSOffState);
        downerw.setState(ownerPerm[Permission.WRITE] ? NSCell.NSOnState : NSCell.NSOffState);
        downerx.setState(ownerPerm[Permission.EXECUTE] ? NSCell.NSOnState : NSCell.NSOffState);

        dgroupr.setState(groupPerm[Permission.READ] ? NSCell.NSOnState : NSCell.NSOffState);
        dgroupw.setState(groupPerm[Permission.WRITE] ? NSCell.NSOnState : NSCell.NSOffState);
        dgroupx.setState(groupPerm[Permission.EXECUTE] ? NSCell.NSOnState : NSCell.NSOffState);

        dotherr.setState(otherPerm[Permission.READ] ? NSCell.NSOnState : NSCell.NSOffState);
        dotherw.setState(otherPerm[Permission.WRITE] ? NSCell.NSOnState : NSCell.NSOffState);
        dotherx.setState(otherPerm[Permission.EXECUTE] ? NSCell.NSOnState : NSCell.NSOffState);
    }

    @Outlet
    private NSButton chmodUploadCheckbox; //IBOutlet

    public void setChmodUploadCheckbox(NSButton chmodUploadCheckbox) {
        this.chmodUploadCheckbox = chmodUploadCheckbox;
        this.chmodUploadCheckbox.setTarget(this.id());
        this.chmodUploadCheckbox.setAction(Foundation.selector("chmodUploadCheckboxClicked:"));
        this.chmodUploadCheckbox.setState(Preferences.instance().getBoolean("queue.upload.changePermissions") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void chmodUploadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.upload.changePermissions", enabled);
        this.chmodUploadDefaultCheckbox.setEnabled(enabled);
        this.chmodUploadCustomCheckbox.setEnabled(enabled);
        boolean chmodUploadDefaultChecked = this.chmodUploadDefaultCheckbox.state() == NSCell.NSOnState;
        this.uownerr.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uownerw.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uownerx.setEnabled(enabled && chmodUploadDefaultChecked);
        this.ugroupr.setEnabled(enabled && chmodUploadDefaultChecked);
        this.ugroupw.setEnabled(enabled && chmodUploadDefaultChecked);
        this.ugroupx.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uotherr.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uotherw.setEnabled(enabled && chmodUploadDefaultChecked);
        this.uotherx.setEnabled(enabled && chmodUploadDefaultChecked);
    }

    @Outlet
    private NSButton chmodUploadDefaultCheckbox; //IBOutlet

    public void setChmodUploadDefaultCheckbox(NSButton chmodUploadDefaultCheckbox) {
        this.chmodUploadDefaultCheckbox = chmodUploadDefaultCheckbox;
        this.chmodUploadDefaultCheckbox.setTarget(this.id());
        this.chmodUploadDefaultCheckbox.setAction(Foundation.selector("chmodUploadDefaultCheckboxClicked:"));
        this.chmodUploadDefaultCheckbox.setState(Preferences.instance().getBoolean("queue.upload.permissions.useDefault") ? NSCell.NSOnState : NSCell.NSOffState);
        this.chmodUploadDefaultCheckbox.setEnabled(Preferences.instance().getBoolean("queue.upload.changePermissions"));
    }

    public void chmodUploadDefaultCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.upload.permissions.useDefault", enabled);
        this.uownerr.setEnabled(enabled);
        this.uownerw.setEnabled(enabled);
        this.uownerx.setEnabled(enabled);
        this.ugroupr.setEnabled(enabled);
        this.ugroupw.setEnabled(enabled);
        this.ugroupx.setEnabled(enabled);
        this.uotherr.setEnabled(enabled);
        this.uotherw.setEnabled(enabled);
        this.uotherx.setEnabled(enabled);
        this.chmodUploadCustomCheckbox.setState(!enabled ? NSCell.NSOnState : NSCell.NSOffState);
    }

    @Outlet
    private NSButton chmodUploadCustomCheckbox; //IBOutlet

    public void setChmodUploadCustomCheckbox(NSButton chmodUploadCustomCheckbox) {
        this.chmodUploadCustomCheckbox = chmodUploadCustomCheckbox;
        this.chmodUploadCustomCheckbox.setTarget(this.id());
        this.chmodUploadCustomCheckbox.setAction(Foundation.selector("chmodUploadCustomCheckboxClicked:"));
        this.chmodUploadCustomCheckbox.setState(!Preferences.instance().getBoolean("queue.upload.permissions.useDefault") ? NSCell.NSOnState : NSCell.NSOffState);
        this.chmodUploadCustomCheckbox.setEnabled(Preferences.instance().getBoolean("queue.upload.changePermissions"));
    }

    public void chmodUploadCustomCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.upload.permissions.useDefault", !enabled);
        this.uownerr.setEnabled(!enabled);
        this.uownerw.setEnabled(!enabled);
        this.uownerx.setEnabled(!enabled);
        this.ugroupr.setEnabled(!enabled);
        this.ugroupw.setEnabled(!enabled);
        this.ugroupx.setEnabled(!enabled);
        this.uotherr.setEnabled(!enabled);
        this.uotherw.setEnabled(!enabled);
        this.uotherx.setEnabled(!enabled);
        this.chmodUploadDefaultCheckbox.setState(!enabled ? NSCell.NSOnState : NSCell.NSOffState);
    }

    @Outlet
    private NSButton chmodDownloadCheckbox; //IBOutlet

    public void setChmodDownloadCheckbox(NSButton chmodDownloadCheckbox) {
        this.chmodDownloadCheckbox = chmodDownloadCheckbox;
        this.chmodDownloadCheckbox.setTarget(this.id());
        this.chmodDownloadCheckbox.setAction(Foundation.selector("chmodDownloadCheckboxClicked:"));
        this.chmodDownloadCheckbox.setState(Preferences.instance().getBoolean("queue.download.changePermissions") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void chmodDownloadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.download.changePermissions", enabled);
        this.chmodDownloadDefaultCheckbox.setEnabled(enabled);
        this.chmodDownloadCustomCheckbox.setEnabled(enabled);
        boolean chmodDownloadDefaultChecked = this.chmodDownloadDefaultCheckbox.state() == NSCell.NSOnState;
        this.downerr.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.downerw.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.downerx.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dgroupr.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dgroupw.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dgroupx.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dotherr.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dotherw.setEnabled(enabled && chmodDownloadDefaultChecked);
        this.dotherx.setEnabled(enabled && chmodDownloadDefaultChecked);
    }

    @Outlet
    private NSButton chmodDownloadDefaultCheckbox; //IBOutlet

    public void setChmodDownloadDefaultCheckbox(NSButton chmodDownloadDefaultCheckbox) {
        this.chmodDownloadDefaultCheckbox = chmodDownloadDefaultCheckbox;
        this.chmodDownloadDefaultCheckbox.setTarget(this.id());
        this.chmodDownloadDefaultCheckbox.setAction(Foundation.selector("chmodDownloadDefaultCheckboxClicked:"));
        this.chmodDownloadDefaultCheckbox.setState(Preferences.instance().getBoolean("queue.download.permissions.useDefault") ? NSCell.NSOnState : NSCell.NSOffState);
        this.chmodDownloadDefaultCheckbox.setEnabled(Preferences.instance().getBoolean("queue.download.changePermissions"));
    }

    public void chmodDownloadDefaultCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.download.permissions.useDefault", enabled);
        this.downerr.setEnabled(enabled);
        this.downerw.setEnabled(enabled);
        this.downerx.setEnabled(enabled);
        this.dgroupr.setEnabled(enabled);
        this.dgroupw.setEnabled(enabled);
        this.dgroupx.setEnabled(enabled);
        this.dotherr.setEnabled(enabled);
        this.dotherw.setEnabled(enabled);
        this.dotherx.setEnabled(enabled);
        this.chmodDownloadCustomCheckbox.setState(!enabled ? NSCell.NSOnState : NSCell.NSOffState);
    }

    @Outlet
    private NSButton chmodDownloadCustomCheckbox; //IBOutlet

    public void setChmodDownloadCustomCheckbox(NSButton chmodDownloadCustomCheckbox) {
        this.chmodDownloadCustomCheckbox = chmodDownloadCustomCheckbox;
        this.chmodDownloadCustomCheckbox.setTarget(this.id());
        this.chmodDownloadCustomCheckbox.setAction(Foundation.selector("chmodDownloadCustomCheckboxClicked:"));
        this.chmodDownloadCustomCheckbox.setState(!Preferences.instance().getBoolean("queue.download.permissions.useDefault") ? NSCell.NSOnState : NSCell.NSOffState);
        this.chmodDownloadCustomCheckbox.setEnabled(Preferences.instance().getBoolean("queue.download.changePermissions"));
    }

    public void chmodDownloadCustomCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.download.permissions.useDefault", !enabled);
        this.downerr.setEnabled(!enabled);
        this.downerw.setEnabled(!enabled);
        this.downerx.setEnabled(!enabled);
        this.dgroupr.setEnabled(!enabled);
        this.dgroupw.setEnabled(!enabled);
        this.dgroupx.setEnabled(!enabled);
        this.dotherr.setEnabled(!enabled);
        this.dotherw.setEnabled(!enabled);
        this.dotherx.setEnabled(!enabled);
        this.chmodDownloadDefaultCheckbox.setState(!enabled ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void setDownerr(NSButton downerr) {
        this.downerr = downerr;
    }

    public void setDownerw(NSButton downerw) {
        this.downerw = downerw;
    }

    public void setDownerx(NSButton downerx) {
        this.downerx = downerx;
    }

    public void setDgroupr(NSButton dgroupr) {
        this.dgroupr = dgroupr;
    }

    public void setDgroupw(NSButton dgroupw) {
        this.dgroupw = dgroupw;
    }

    public void setDgroupx(NSButton dgroupx) {
        this.dgroupx = dgroupx;
    }

    public void setDotherr(NSButton dotherr) {
        this.dotherr = dotherr;
    }

    public void setDotherw(NSButton dotherw) {
        this.dotherw = dotherw;
    }

    public void setDotherx(NSButton dotherx) {
        this.dotherx = dotherx;
    }

    public void setUownerr(NSButton uownerr) {
        this.uownerr = uownerr;
    }

    public void setUownerw(NSButton uownerw) {
        this.uownerw = uownerw;
    }

    public void setUownerx(NSButton uownerx) {
        this.uownerx = uownerx;
    }

    public void setUgroupr(NSButton ugroupr) {
        this.ugroupr = ugroupr;
    }

    public void setUgroupw(NSButton ugroupw) {
        this.ugroupw = ugroupw;
    }

    public void setUgroupx(NSButton ugroupx) {
        this.ugroupx = ugroupx;
    }

    public void setUotherr(NSButton uotherr) {
        this.uotherr = uotherr;
    }

    public void setUotherw(NSButton uotherw) {
        this.uotherw = uotherw;
    }

    public void setUotherx(NSButton uotherx) {
        this.uotherx = uotherx;
    }

    @Outlet
    private NSButton downerr; //IBOutlet
    @Outlet
    private NSButton downerw; //IBOutlet
    @Outlet
    private NSButton downerx; //IBOutlet
    @Outlet
    private NSButton dgroupr; //IBOutlet
    @Outlet
    private NSButton dgroupw; //IBOutlet
    @Outlet
    private NSButton dgroupx; //IBOutlet
    @Outlet
    private NSButton dotherr; //IBOutletdownerr
    @Outlet
    private NSButton dotherw; //IBOutlet
    @Outlet
    private NSButton dotherx; //IBOutlet

    public void defaultPermissionsDownloadChanged(final ID sender) {
        boolean[][] p = new boolean[3][3];

        p[Permission.OWNER][Permission.READ] = (downerr.state() == NSCell.NSOnState);
        p[Permission.OWNER][Permission.WRITE] = (downerw.state() == NSCell.NSOnState);
        p[Permission.OWNER][Permission.EXECUTE] = (downerx.state() == NSCell.NSOnState);

        p[Permission.GROUP][Permission.READ] = (dgroupr.state() == NSCell.NSOnState);
        p[Permission.GROUP][Permission.WRITE] = (dgroupw.state() == NSCell.NSOnState);
        p[Permission.GROUP][Permission.EXECUTE] = (dgroupx.state() == NSCell.NSOnState);

        p[Permission.OTHER][Permission.READ] = (dotherr.state() == NSCell.NSOnState);
        p[Permission.OTHER][Permission.WRITE] = (dotherw.state() == NSCell.NSOnState);
        p[Permission.OTHER][Permission.EXECUTE] = (dotherx.state() == NSCell.NSOnState);

        Permission permission = new Permission(p);
        if(chmodDownloadTypePopup.selectedItem().tag() == 0) {
            Preferences.instance().setProperty("queue.download.permissions.file.default", permission.getOctalString());
        }
        if(chmodDownloadTypePopup.selectedItem().tag() == 1) {
            Preferences.instance().setProperty("queue.download.permissions.folder.default", permission.getOctalString());
        }
    }

    public NSButton uownerr; //IBOutlet
    public NSButton uownerw; //IBOutlet
    public NSButton uownerx; //IBOutlet
    public NSButton ugroupr; //IBOutlet
    public NSButton ugroupw; //IBOutlet
    public NSButton ugroupx; //IBOutlet
    public NSButton uotherr; //IBOutlet
    public NSButton uotherw; //IBOutlet
    public NSButton uotherx; //IBOutlet

    public void defaultPermissionsUploadChanged(final ID sender) {
        boolean[][] p = new boolean[3][3];

        p[Permission.OWNER][Permission.READ] = (uownerr.state() == NSCell.NSOnState);
        p[Permission.OWNER][Permission.WRITE] = (uownerw.state() == NSCell.NSOnState);
        p[Permission.OWNER][Permission.EXECUTE] = (uownerx.state() == NSCell.NSOnState);

        p[Permission.GROUP][Permission.READ] = (ugroupr.state() == NSCell.NSOnState);
        p[Permission.GROUP][Permission.WRITE] = (ugroupw.state() == NSCell.NSOnState);
        p[Permission.GROUP][Permission.EXECUTE] = (ugroupx.state() == NSCell.NSOnState);

        p[Permission.OTHER][Permission.READ] = (uotherr.state() == NSCell.NSOnState);
        p[Permission.OTHER][Permission.WRITE] = (uotherw.state() == NSCell.NSOnState);
        p[Permission.OTHER][Permission.EXECUTE] = (uotherx.state() == NSCell.NSOnState);

        Permission permission = new Permission(p);
        if(chmodUploadTypePopup.selectedItem().tag() == 0) {
            Preferences.instance().setProperty("queue.upload.permissions.file.default", permission.getOctalString());
        }
        if(chmodUploadTypePopup.selectedItem().tag() == 1) {
            Preferences.instance().setProperty("queue.upload.permissions.folder.default", permission.getOctalString());
        }
    }

    @Outlet
    private NSButton preserveModificationDownloadCheckbox; //IBOutlet

    public void setPreserveModificationDownloadCheckbox(NSButton preserveModificationDownloadCheckbox) {
        this.preserveModificationDownloadCheckbox = preserveModificationDownloadCheckbox;
        this.preserveModificationDownloadCheckbox.setTarget(this.id());
        this.preserveModificationDownloadCheckbox.setAction(Foundation.selector("preserveModificationDownloadCheckboxClicked:"));
        this.preserveModificationDownloadCheckbox.setState(Preferences.instance().getBoolean("queue.download.preserveDate") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void preserveModificationDownloadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.download.preserveDate", enabled);
    }

    @Outlet
    private NSButton preserveModificationUploadCheckbox; //IBOutlet

    public void setPreserveModificationUploadCheckbox(NSButton preserveModificationUploadCheckbox) {
        this.preserveModificationUploadCheckbox = preserveModificationUploadCheckbox;
        this.preserveModificationUploadCheckbox.setTarget(this.id());
        this.preserveModificationUploadCheckbox.setAction(Foundation.selector("preserveModificationUploadCheckboxClicked:"));
        this.preserveModificationUploadCheckbox.setState(Preferences.instance().getBoolean("queue.upload.preserveDate") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void preserveModificationUploadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.upload.preserveDate", enabled);
    }

    @Outlet
    private NSButton horizontalLinesCheckbox; //IBOutlet

    public void setHorizontalLinesCheckbox(NSButton horizontalLinesCheckbox) {
        this.horizontalLinesCheckbox = horizontalLinesCheckbox;
        this.horizontalLinesCheckbox.setTarget(this.id());
        this.horizontalLinesCheckbox.setAction(Foundation.selector("horizontalLinesCheckboxClicked:"));
        this.horizontalLinesCheckbox.setState(Preferences.instance().getBoolean("browser.horizontalLines") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void horizontalLinesCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.horizontalLines", enabled);
        CDBrowserController.updateBrowserTableAttributes();
    }

    @Outlet
    private NSButton verticalLinesCheckbox; //IBOutlet

    public void setVerticalLinesCheckbox(NSButton verticalLinesCheckbox) {
        this.verticalLinesCheckbox = verticalLinesCheckbox;
        this.verticalLinesCheckbox.setTarget(this.id());
        this.verticalLinesCheckbox.setAction(Foundation.selector("verticalLinesCheckboxClicked:"));
        this.verticalLinesCheckbox.setState(Preferences.instance().getBoolean("browser.verticalLines") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void verticalLinesCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.verticalLines", enabled);
        CDBrowserController.updateBrowserTableAttributes();
    }

    @Outlet
    private NSButton alternatingRowBackgroundCheckbox; //IBOutlet

    public void setAlternatingRowBackgroundCheckbox(NSButton alternatingRowBackgroundCheckbox) {
        this.alternatingRowBackgroundCheckbox = alternatingRowBackgroundCheckbox;
        this.alternatingRowBackgroundCheckbox.setTarget(this.id());
        this.alternatingRowBackgroundCheckbox.setAction(Foundation.selector("alternatingRowBackgroundCheckboxClicked:"));
        this.alternatingRowBackgroundCheckbox.setState(Preferences.instance().getBoolean("browser.alternatingRows") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void alternatingRowBackgroundCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.alternatingRows", enabled);
        CDBrowserController.updateBrowserTableAttributes();
    }

    @Outlet
    private NSButton infoWindowAsInspectorCheckbox; //IBOutlet

    public void setInfoWindowAsInspectorCheckbox(NSButton infoWindowAsInspectorCheckbox) {
        this.infoWindowAsInspectorCheckbox = infoWindowAsInspectorCheckbox;
        this.infoWindowAsInspectorCheckbox.setTarget(this.id());
        this.infoWindowAsInspectorCheckbox.setAction(Foundation.selector("infoWindowAsInspectorCheckboxClicked:"));
        this.infoWindowAsInspectorCheckbox.setState(Preferences.instance().getBoolean("browser.info.isInspector") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void infoWindowAsInspectorCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.info.isInspector", enabled);
    }

    @Outlet
    private NSPopUpButton downloadPathPopup; //IBOutlet

    private static final String CHOOSE = Locale.localizedString("Choose") + "...";

    // The currently set download folder
    private final Local DEFAULT_DOWNLOAD_FOLDER = LocalFactory.createLocal(Preferences.instance().getProperty("queue.download.folder"));

    public void setDownloadPathPopup(NSPopUpButton downloadPathPopup) {
        this.downloadPathPopup = downloadPathPopup;
        this.downloadPathPopup.setTarget(this.id());
        final Selector action = Foundation.selector("downloadPathPopupClicked:");
        this.downloadPathPopup.setAction(action);
        this.downloadPathPopup.removeAllItems();
        // Default download folder
        this.addDownloadPath(action, DEFAULT_DOWNLOAD_FOLDER);
        this.downloadPathPopup.menu().addItem(NSMenuItem.separatorItem());
        // Shortcut to the Desktop
        this.addDownloadPath(action, LocalFactory.createLocal("~/Desktop"));
        // Shortcut to user home
        this.addDownloadPath(action, LocalFactory.createLocal("~"));
        // Shortcut to user downloads for 10.5
        this.addDownloadPath(action, LocalFactory.createLocal("~/Downloads"));
        // Choose another folder
        this.downloadPathPopup.menu().addItem(NSMenuItem.separatorItem());
        this.downloadPathPopup.menu().addItem(NSMenuItem.itemWithTitle(CHOOSE, action, ""));
        this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems() - 1).setTarget(this.id());
    }

    private void addDownloadPath(Selector action, Local f) {
        if(f.exists()) {
            this.downloadPathPopup.menu().addItem(NSMenuItem.itemWithTitle(NSFileManager.defaultManager().displayNameAtPath(
                    f.getAbsolute()), action, ""));
            this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems() - 1).setTarget(this.id());
            this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems() - 1).setImage(
                    CDIconCache.instance().iconForPath(f, 16)
            );
            this.downloadPathPopup.itemAtIndex(this.downloadPathPopup.numberOfItems() - 1).setRepresentedObject(
                    f.getAbsolute());
            if(DEFAULT_DOWNLOAD_FOLDER.equals(f)) {
                this.downloadPathPopup.selectItemAtIndex(this.downloadPathPopup.numberOfItems() - 1);
            }
        }
    }

    private NSOpenPanel downloadPathPanel;

    public void downloadPathPopupClicked(final NSMenuItem sender) {
        if(sender.title().equals(CHOOSE)) {
            downloadPathPanel = NSOpenPanel.openPanel();
            downloadPathPanel.setCanChooseFiles(false);
            downloadPathPanel.setCanChooseDirectories(true);
            downloadPathPanel.setAllowsMultipleSelection(false);
            downloadPathPanel.setCanCreateDirectories(true);
            downloadPathPanel.beginSheetForDirectory(null, null, this.window, this.id(),
                    Foundation.selector("downloadPathPanelDidEnd:returnCode:contextInfo:"), null);
        }
        else {
            Preferences.instance().setProperty("queue.download.folder", LocalFactory.createLocal(
                    sender.representedObject()).getAbbreviatedPath());
        }
    }

    public void downloadPathPanelDidEnd_returnCode_contextInfo(NSOpenPanel sheet, int returncode, ID contextInfo) {
        if(returncode == CDSheetCallback.DEFAULT_OPTION) {
            NSArray selected = sheet.filenames();
            String filename;
            if((filename = selected.lastObject().toString()) != null) {
                Preferences.instance().setProperty("queue.download.folder",
                        LocalFactory.createLocal(filename).getAbbreviatedPath());
            }
        }
        Local custom = LocalFactory.createLocal(Preferences.instance().getProperty("queue.download.folder"));
        downloadPathPopup.itemAtIndex(0).setTitle(NSFileManager.defaultManager().displayNameAtPath(custom.getAbsolute()));
        downloadPathPopup.itemAtIndex(0).setRepresentedObject(custom.getAbsolute());
        downloadPathPopup.itemAtIndex(0).setImage(CDIconCache.instance().iconForPath(custom, 16));
        downloadPathPopup.selectItemAtIndex(0);
        downloadPathPanel = null;
    }

    @Outlet
    private NSPopUpButton transferPopup; //IBOutlet

    public void setTransferPopup(NSPopUpButton transferPopup) {
        this.transferPopup = transferPopup;
        this.transferPopup.setTarget(this.id());
        this.transferPopup.setAction(Foundation.selector("transferPopupClicked:"));
        this.transferPopup.selectItemAtIndex(
                Preferences.instance().getInteger("connection.host.max") == 1 ? USE_BROWSER_SESSION_INDEX : USE_QUEUE_SESSION_INDEX);
    }

    private final int USE_QUEUE_SESSION_INDEX = 0;
    private final int USE_BROWSER_SESSION_INDEX = 1;

    public void transferPopupClicked(final NSPopUpButton sender) {
        if(sender.indexOfSelectedItem() == USE_BROWSER_SESSION_INDEX) {
            Preferences.instance().setProperty("connection.host.max", 1);
        }
        else if(sender.indexOfSelectedItem() == USE_QUEUE_SESSION_INDEX) {
            Preferences.instance().setProperty("connection.host.max", -1);
        }
    }

    @Outlet
    private NSTextField anonymousField; //IBOutlet

    public void setAnonymousField(NSTextField anonymousField) {
        this.anonymousField = anonymousField;
        this.anonymousField.setStringValue(Preferences.instance().getProperty("connection.login.anon.pass"));
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("anonymousFieldDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                this.anonymousField);
    }

    public void anonymousFieldDidChange(NSNotification sender) {
        Preferences.instance().setProperty("connection.login.anon.pass", this.anonymousField.stringValue());
    }

    @Outlet
    private NSTextField textFileTypeRegexField; //IBOutlet

    public void setTextFileTypeRegexField(NSTextField textFileTypeRegexField) {
        this.textFileTypeRegexField = textFileTypeRegexField;
        this.textFileTypeRegexField.setStringValue(Preferences.instance().getProperty("filetype.text.regex"));
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("textFileTypeRegexFieldDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                this.textFileTypeRegexField);
    }

    public void textFileTypeRegexFieldDidChange(NSNotification sender) {
        String value = this.textFileTypeRegexField.stringValue().trim();
        try {
            Pattern compiled = Pattern.compile(value);
            this.textFileTypeRegexField.setTextColor(NSColor.controlTextColor());
            Preferences.instance().setProperty("filetype.text.regex",
                    compiled.pattern());
        }
        catch(PatternSyntaxException e) {
            this.textFileTypeRegexField.setTextColor(NSColor.redColor());
        }
    }

//    @Outlet private NSTextField binaryFileTypeRegexField; //IBOutlet
//
//    public void setBinaryFileTypeRegexField(NSTextField binaryFileTypeRegexField) {
//        this.binaryFileTypeRegexField = binaryFileTypeRegexField;
//        this.binaryFileTypeRegexField.setStringValue(Preferences.instance().getProperty("filetype.binary.regex"));
//        NSNotificationCenter.defaultCenter().addObserver(this.id(),
//                Foundation.selector("binaryFileTypeRegexFieldDidChange:"),
//                NSControl.ControlTextDidChangeNotification,
//                this.binaryFileTypeRegexField);
//    }
//
//    public void binaryFileTypeRegexFieldDidChange(NSNotification sender) {
//        String value = this.binaryFileTypeRegexField.stringValue().trim();
//        try {
//            Pattern compiled = Pattern.compile(value);
//            Preferences.instance().setProperty("filetype.binary.regex",
//                    compiled.pattern());
//        }
//        catch(PatternSyntaxException e) {
//            log.warn("Invalid regex:"+e.getMessage());
//        }
//    }

    @Outlet
    private NSButton downloadSkipButton; //IBOutlet

    public void setDownloadSkipButton(NSButton downloadSkipButton) {
        this.downloadSkipButton = downloadSkipButton;
        this.downloadSkipButton.setTarget(this.id());
        this.downloadSkipButton.setAction(Foundation.selector("downloadSkipButtonClicked:"));
        this.downloadSkipButton.setState(Preferences.instance().getBoolean("queue.download.skip.enable") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void downloadSkipButtonClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        downloadSkipRegexField.setSelectable(enabled);
        downloadSkipRegexField.setEditable(enabled);
        downloadSkipRegexField.setTextColor(enabled ? NSColor.controlTextColor() : NSColor.disabledControlTextColor());
        Preferences.instance().setProperty("queue.download.skip.enable", enabled);
    }

    @Outlet
    private NSButton downloadSkipRegexDefaultButton; //IBOutlet

    public void setDownloadSkipRegexDefaultButton(NSButton downloadSkipRegexDefaultButton) {
        this.downloadSkipRegexDefaultButton = downloadSkipRegexDefaultButton;
        this.downloadSkipRegexDefaultButton.setTarget(this.id());
        this.downloadSkipRegexDefaultButton.setAction(Foundation.selector("downloadSkipRegexDefaultButtonClicked:"));
    }

    public void downloadSkipRegexDefaultButtonClicked(final NSButton sender) {
        final String regex = Preferences.instance().getProperty("queue.download.skip.regex.default");
        this.downloadSkipRegexField.setString(regex);
        Preferences.instance().setProperty("queue.download.skip.regex", regex);
    }

    private NSTextView downloadSkipRegexField; //IBOutlet

    public void setDownloadSkipRegexField(NSTextView downloadSkipRegexField) {
        this.downloadSkipRegexField = downloadSkipRegexField;
        this.downloadSkipRegexField.setFont(NSFont.userFixedPitchFontOfSize(9.0f));
        this.downloadSkipRegexField.setString(Preferences.instance().getProperty("queue.download.skip.regex"));
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("downloadSkipRegexFieldDidChange:"),
                NSText.TextDidChangeNotification,
                this.downloadSkipRegexField);
    }

    public void downloadSkipRegexFieldDidChange(NSNotification sender) {
        String value = this.downloadSkipRegexField.string().trim();
        if("".equals(value)) {
            Preferences.instance().setProperty("queue.download.skip.enable", false);
            Preferences.instance().setProperty("queue.download.skip.regex", value);
            this.downloadSkipButton.setState(NSCell.NSOffState);
        }
        try {
            Pattern compiled = Pattern.compile(value);
            Preferences.instance().setProperty("queue.download.skip.regex",
                    compiled.pattern());
            this.mark(this.downloadSkipRegexField.textStorage(), null);
        }
        catch(PatternSyntaxException e) {
            this.mark(this.downloadSkipRegexField.textStorage(), e);
        }
    }

    @Outlet
    private NSButton uploadSkipButton; //IBOutlet

    public void setUploadSkipButton(NSButton uploadSkipButton) {
        this.uploadSkipButton = uploadSkipButton;
        this.uploadSkipButton.setTarget(this.id());
        this.uploadSkipButton.setAction(Foundation.selector("uploadSkipButtonClicked:"));
        this.uploadSkipButton.setState(Preferences.instance().getBoolean("queue.upload.skip.enable") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void uploadSkipButtonClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        uploadSkipRegexField.setSelectable(enabled);
        uploadSkipRegexField.setEditable(enabled);
        uploadSkipRegexField.setTextColor(enabled ? NSColor.controlTextColor() : NSColor.disabledControlTextColor());
        Preferences.instance().setProperty("queue.upload.skip.enable", enabled);
    }

    @Outlet
    private NSButton uploadSkipRegexDefaultButton; //IBOutlet

    public void setUploadSkipRegexDefaultButton(NSButton uploadSkipRegexDefaultButton) {
        this.uploadSkipRegexDefaultButton = uploadSkipRegexDefaultButton;
        this.uploadSkipRegexDefaultButton.setTarget(this.id());
        this.uploadSkipRegexDefaultButton.setAction(Foundation.selector("uploadSkipRegexDefaultButtonClicked:"));
    }

    public void uploadSkipRegexDefaultButtonClicked(final NSButton sender) {
        final String regex = Preferences.instance().getProperty("queue.upload.skip.regex.default");
        this.uploadSkipRegexField.setString(regex);
        Preferences.instance().setProperty("queue.upload.skip.regex", regex);
    }

    private NSTextView uploadSkipRegexField; //IBOutlet

    public void setUploadSkipRegexField(NSTextView uploadSkipRegexField) {
        this.uploadSkipRegexField = uploadSkipRegexField;
        this.uploadSkipRegexField.setFont(NSFont.userFixedPitchFontOfSize(9.0f));
        this.uploadSkipRegexField.setString(Preferences.instance().getProperty("queue.upload.skip.regex"));
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("uploadSkipRegexFieldDidChange:"),
                NSText.TextDidChangeNotification,
                this.uploadSkipRegexField);
    }

    public void uploadSkipRegexFieldDidChange(NSNotification sender) {
        String value = this.uploadSkipRegexField.string().trim();
        if("".equals(value)) {
            Preferences.instance().setProperty("queue.upload.skip.enable", false);
            Preferences.instance().setProperty("queue.upload.skip.regex", value);
            this.uploadSkipButton.setState(NSCell.NSOffState);
        }
        try {
            Pattern compiled = Pattern.compile(value);
            Preferences.instance().setProperty("queue.upload.skip.regex",
                    compiled.pattern());
            this.mark(this.uploadSkipRegexField.textStorage(), null);
        }
        catch(PatternSyntaxException e) {
            this.mark(this.uploadSkipRegexField.textStorage(), e);
        }
    }

    protected static NSDictionary RED_FONT = NSDictionary.dictionaryWithObjectsForKeys(
            NSArray.arrayWithObjects(NSColor.redColor()),
            NSArray.arrayWithObjects(NSAttributedString.ForegroundColorAttributeName)
    );

    private void mark(NSMutableAttributedString text, PatternSyntaxException e) {
        if(null == e) {
            text.removeAttributeInRange(
                    NSAttributedString.ForegroundColorAttributeName,
                    NSRange.NSMakeRange(new NSUInteger(0), text.length()));
            return;
        }
        int index = e.getIndex(); //The approximate index in the pattern of the error
        NSRange range = null;
        if(-1 == index) {
            range = NSRange.NSMakeRange(new NSUInteger(0), text.length());
        }
        if(index < text.length().intValue()) {
            //Initializes the NSRange with the range elements of location and length;
            range = NSRange.NSMakeRange(new NSUInteger(index), new NSUInteger(1));
        }
        text.addAttributesInRange(RED_FONT, range);
    }

    @Outlet
    private NSTextField loginField; //IBOutlet

    /**
     * Default SSH login name
     *
     * @param loginField
     */
    public void setLoginField(NSTextField loginField) {
        this.loginField = loginField;
        this.loginField.setStringValue(Preferences.instance().getProperty("connection.login.name"));
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("loginFieldDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                this.loginField);
    }

    public void loginFieldDidChange(NSNotification sender) {
        Preferences.instance().setProperty("connection.login.name", this.loginField.stringValue());
    }

    @Outlet
    private NSButton keychainCheckbox; //IBOutlet

    public void setKeychainCheckbox(NSButton keychainCheckbox) {
        this.keychainCheckbox = keychainCheckbox;
        this.keychainCheckbox.setTarget(this.id());
        this.keychainCheckbox.setAction(Foundation.selector("keychainCheckboxClicked:"));
        this.keychainCheckbox.setState(Preferences.instance().getBoolean("connection.login.useKeychain") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void keychainCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("connection.login.useKeychain", enabled);
    }

    @Outlet
    private NSButton doubleClickCheckbox; //IBOutlet

    public void setDoubleClickCheckbox(NSButton doubleClickCheckbox) {
        this.doubleClickCheckbox = doubleClickCheckbox;
        this.doubleClickCheckbox.setTarget(this.id());
        this.doubleClickCheckbox.setAction(Foundation.selector("doubleClickCheckboxClicked:"));
        this.doubleClickCheckbox.setState(Preferences.instance().getBoolean("browser.doubleclick.edit") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void doubleClickCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.doubleclick.edit", enabled);
    }

    @Outlet
    private NSButton returnKeyCheckbox; //IBOutlet

    public void setReturnKeyCheckbox(NSButton returnKeyCheckbox) {
        this.returnKeyCheckbox = returnKeyCheckbox;
        this.returnKeyCheckbox.setTarget(this.id());
        this.returnKeyCheckbox.setAction(Foundation.selector("returnKeyCheckboxClicked:"));
        this.returnKeyCheckbox.setState(Preferences.instance().getBoolean("browser.enterkey.rename") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void returnKeyCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.enterkey.rename", enabled);
    }

    @Outlet
    private NSButton showHiddenCheckbox; //IBOutlet

    public void setShowHiddenCheckbox(NSButton showHiddenCheckbox) {
        this.showHiddenCheckbox = showHiddenCheckbox;
        this.showHiddenCheckbox.setTarget(this.id());
        this.showHiddenCheckbox.setAction(Foundation.selector("showHiddenCheckboxClicked:"));
        this.showHiddenCheckbox.setState(Preferences.instance().getBoolean("browser.showHidden") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void showHiddenCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.showHidden", enabled);
    }

    @Outlet
    private NSButton bringQueueToFrontCheckbox; //IBOutlet

    public void setBringQueueToFrontCheckbox(NSButton bringQueueToFrontCheckbox) {
        this.bringQueueToFrontCheckbox = bringQueueToFrontCheckbox;
        this.bringQueueToFrontCheckbox.setTarget(this.id());
        this.bringQueueToFrontCheckbox.setAction(Foundation.selector("bringQueueToFrontCheckboxClicked:"));
        this.bringQueueToFrontCheckbox.setState(Preferences.instance().getBoolean("queue.orderFrontOnStart") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void bringQueueToFrontCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.orderFrontOnStart", enabled);
    }

    @Outlet
    private NSButton bringQueueToBackCheckbox; //IBOutlet

    public void setBringQueueToBackCheckbox(NSButton bringQueueToBackCheckbox) {
        this.bringQueueToBackCheckbox = bringQueueToBackCheckbox;
        this.bringQueueToBackCheckbox.setTarget(this.id());
        this.bringQueueToBackCheckbox.setAction(Foundation.selector("bringQueueToBackCheckboxClicked:"));
        this.bringQueueToBackCheckbox.setState(Preferences.instance().getBoolean("queue.orderBackOnStop") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void bringQueueToBackCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.orderBackOnStop", enabled);
    }

    @Outlet
    private NSButton removeFromQueueCheckbox; //IBOutlet

    public void setRemoveFromQueueCheckbox(NSButton removeFromQueueCheckbox) {
        this.removeFromQueueCheckbox = removeFromQueueCheckbox;
        this.removeFromQueueCheckbox.setTarget(this.id());
        this.removeFromQueueCheckbox.setAction(Foundation.selector("removeFromQueueCheckboxClicked:"));
        this.removeFromQueueCheckbox.setState(Preferences.instance().getBoolean("queue.removeItemWhenComplete") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void removeFromQueueCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.removeItemWhenComplete", enabled);
    }

    @Outlet
    private NSButton openAfterDownloadCheckbox; //IBOutlet

    public void setOpenAfterDownloadCheckbox(NSButton openAfterDownloadCheckbox) {
        this.openAfterDownloadCheckbox = openAfterDownloadCheckbox;
        this.openAfterDownloadCheckbox.setTarget(this.id());
        this.openAfterDownloadCheckbox.setAction(Foundation.selector("openAfterDownloadCheckboxClicked:"));
        this.openAfterDownloadCheckbox.setState(Preferences.instance().getBoolean("queue.postProcessItemWhenComplete") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void openAfterDownloadCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("queue.postProcessItemWhenComplete", enabled);
    }

    private void duplicateComboboxClicked(String selected, String property) {
        if(selected.equals(TransferAction.ACTION_CALLBACK.getLocalizableString())) {
            Preferences.instance().setProperty(property, TransferAction.ACTION_CALLBACK.toString());
        }
        else if(selected.equals(TransferAction.ACTION_OVERWRITE.getLocalizableString())) {
            Preferences.instance().setProperty(property, TransferAction.ACTION_OVERWRITE.toString());
        }
        else if(selected.equals(TransferAction.ACTION_RESUME.getLocalizableString())) {
            Preferences.instance().setProperty(property, TransferAction.ACTION_RESUME.toString());
        }
        else if(selected.equals(TransferAction.ACTION_RENAME.getLocalizableString())) {
            Preferences.instance().setProperty(property, TransferAction.ACTION_RENAME.toString());
        }
        else if(selected.equals(TransferAction.ACTION_SKIP.getLocalizableString())) {
            Preferences.instance().setProperty(property, TransferAction.ACTION_SKIP.toString());
        }
    }

    @Outlet
    private NSPopUpButton duplicateDownloadCombobox; //IBOutlet

    public void setDuplicateDownloadCombobox(NSPopUpButton duplicateDownloadCombobox) {
        this.duplicateDownloadCombobox = duplicateDownloadCombobox;
        this.duplicateDownloadCombobox.setTarget(this.id());
        this.duplicateDownloadCombobox.setAction(Foundation.selector("duplicateDownloadComboboxClicked:"));
        this.duplicateDownloadCombobox.removeAllItems();
        this.duplicateDownloadCombobox.addItemsWithTitles(NSArray.arrayWithObjects(
                TransferAction.ACTION_CALLBACK.getLocalizableString(), TransferAction.ACTION_OVERWRITE.getLocalizableString(),
                TransferAction.ACTION_RESUME.getLocalizableString(), TransferAction.ACTION_RENAME.getLocalizableString(),
                TransferAction.ACTION_SKIP.getLocalizableString())
        );
        if(Preferences.instance().getProperty("queue.download.fileExists").equals(TransferAction.ACTION_CALLBACK.toString())) {
            this.duplicateDownloadCombobox.selectItemWithTitle(TransferAction.ACTION_CALLBACK.getLocalizableString());
        }
        else if(Preferences.instance().getProperty("queue.download.fileExists").equals(TransferAction.ACTION_OVERWRITE.toString())) {
            this.duplicateDownloadCombobox.selectItemWithTitle(TransferAction.ACTION_OVERWRITE.getLocalizableString());
        }
        else if(Preferences.instance().getProperty("queue.download.fileExists").equals(TransferAction.ACTION_RESUME.toString())) {
            this.duplicateDownloadCombobox.selectItemWithTitle(TransferAction.ACTION_RESUME.getLocalizableString());
        }
        else if(Preferences.instance().getProperty("queue.download.fileExists").equals(TransferAction.ACTION_RENAME.toString())) {
            this.duplicateDownloadCombobox.selectItemWithTitle(TransferAction.ACTION_RENAME.getLocalizableString());
        }
        else if(Preferences.instance().getProperty("queue.download.fileExists").equals(TransferAction.ACTION_SKIP.toString())) {
            this.duplicateDownloadCombobox.selectItemWithTitle(TransferAction.ACTION_SKIP.getLocalizableString());
        }
    }

    public void duplicateDownloadComboboxClicked(NSPopUpButton sender) {
        this.duplicateComboboxClicked(sender.selectedItem().title(), "queue.download.fileExists");
        this.duplicateDownloadOverwriteButtonClicked(duplicateDownloadOverwriteButton);
    }

    @Outlet
    private NSButton duplicateDownloadOverwriteButton;

    public void setDuplicateDownloadOverwriteButton(NSButton duplicateDownloadOverwriteButton) {
        this.duplicateDownloadOverwriteButton = duplicateDownloadOverwriteButton;
        this.duplicateDownloadOverwriteButton.setTarget(this.id());
        this.duplicateDownloadOverwriteButton.setAction(Foundation.selector("duplicateDownloadOverwriteButtonClicked:"));
        this.duplicateDownloadOverwriteButton.setState(
                Preferences.instance().getProperty("queue.download.reload.fileExists").equals(
                        TransferAction.ACTION_OVERWRITE.toString()) ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void duplicateDownloadOverwriteButtonClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        if(enabled) {
            Preferences.instance().setProperty("queue.download.reload.fileExists", TransferAction.ACTION_OVERWRITE.toString());
        }
        else {
            Preferences.instance().setProperty("queue.download.reload.fileExists",
                    Preferences.instance().getProperty("queue.download.fileExists"));
        }
    }

    @Outlet
    private NSPopUpButton duplicateUploadCombobox; //IBOutlet

    public void setDuplicateUploadCombobox(NSPopUpButton duplicateUploadCombobox) {
        this.duplicateUploadCombobox = duplicateUploadCombobox;
        this.duplicateUploadCombobox.setTarget(this.id());
        this.duplicateUploadCombobox.setAction(Foundation.selector("duplicateUploadComboboxClicked:"));
        this.duplicateUploadCombobox.removeAllItems();
        this.duplicateUploadCombobox.addItemsWithTitles(NSArray.arrayWithObjects(
                TransferAction.ACTION_CALLBACK.getLocalizableString(), TransferAction.ACTION_OVERWRITE.getLocalizableString(),
                TransferAction.ACTION_RESUME.getLocalizableString(), TransferAction.ACTION_RENAME.getLocalizableString(),
                TransferAction.ACTION_SKIP.getLocalizableString())
        );
        if(Preferences.instance().getProperty("queue.upload.fileExists").equals(TransferAction.ACTION_CALLBACK.toString())) {
            this.duplicateUploadCombobox.selectItemWithTitle(TransferAction.ACTION_CALLBACK.getLocalizableString());
        }
        else if(Preferences.instance().getProperty("queue.upload.fileExists").equals(TransferAction.ACTION_OVERWRITE.toString())) {
            this.duplicateUploadCombobox.selectItemWithTitle(TransferAction.ACTION_OVERWRITE.getLocalizableString());
        }
        else if(Preferences.instance().getProperty("queue.upload.fileExists").equals(TransferAction.ACTION_RESUME.toString())) {
            this.duplicateUploadCombobox.selectItemWithTitle(TransferAction.ACTION_RESUME.getLocalizableString());
        }
        else if(Preferences.instance().getProperty("queue.upload.fileExists").equals(TransferAction.ACTION_RENAME.toString())) {
            this.duplicateUploadCombobox.selectItemWithTitle(TransferAction.ACTION_RENAME.getLocalizableString());
        }
        else if(Preferences.instance().getProperty("queue.upload.fileExists").equals(TransferAction.ACTION_SKIP.toString())) {
            this.duplicateUploadCombobox.selectItemWithTitle(TransferAction.ACTION_SKIP.getLocalizableString());
        }
    }

    public void duplicateUploadComboboxClicked(NSPopUpButton sender) {
        this.duplicateComboboxClicked(sender.selectedItem().title(), "queue.upload.fileExists");
        this.duplicateUploadOverwriteButtonClicked(duplicateUploadOverwriteButton);
    }

    @Outlet
    private NSButton duplicateUploadOverwriteButton;

    public void setDuplicateUploadOverwriteButton(NSButton duplicateUploadOverwriteButton) {
        this.duplicateUploadOverwriteButton = duplicateUploadOverwriteButton;
        this.duplicateUploadOverwriteButton.setTarget(this.id());
        this.duplicateUploadOverwriteButton.setAction(Foundation.selector("duplicateUploadOverwriteButtonClicked:"));
        this.duplicateUploadOverwriteButton.setState(
                Preferences.instance().getProperty("queue.upload.reload.fileExists").equals(
                        TransferAction.ACTION_OVERWRITE.toString()) ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void duplicateUploadOverwriteButtonClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        if(enabled) {
            Preferences.instance().setProperty("queue.upload.reload.fileExists", TransferAction.ACTION_OVERWRITE.toString());
        }
        else {
            Preferences.instance().setProperty("queue.upload.reload.fileExists",
                    Preferences.instance().getProperty("queue.upload.fileExists"));
        }
    }

    @Outlet
    private NSPopUpButton lineEndingCombobox; //IBOutlet

    public void setLineEndingCombobox(NSPopUpButton lineEndingCombobox) {
        this.lineEndingCombobox = lineEndingCombobox;
        this.lineEndingCombobox.setTarget(this.id());
        this.lineEndingCombobox.setAction(Foundation.selector("lineEndingComboboxClicked:"));
        this.lineEndingCombobox.removeAllItems();
        this.lineEndingCombobox.addItemsWithTitles(NSArray.arrayWithObjects(UNIX_LINE_ENDINGS, MAC_LINE_ENDINGS, WINDOWS_LINE_ENDINGS));
        if(Preferences.instance().getProperty("ftp.line.separator").equals("unix")) {
            this.lineEndingCombobox.selectItemWithTitle(UNIX_LINE_ENDINGS);
        }
        else if(Preferences.instance().getProperty("ftp.line.separator").equals("mac")) {
            this.lineEndingCombobox.selectItemWithTitle(MAC_LINE_ENDINGS);
        }
        else if(Preferences.instance().getProperty("ftp.line.separator").equals("win")) {
            this.lineEndingCombobox.selectItemWithTitle(WINDOWS_LINE_ENDINGS);
        }
    }

    public void lineEndingComboboxClicked(NSPopUpButton sender) {
        if(sender.selectedItem().title().equals(UNIX_LINE_ENDINGS)) {
            Preferences.instance().setProperty("ftp.line.separator", "unix");
        }
        else if(sender.selectedItem().title().equals(MAC_LINE_ENDINGS)) {
            Preferences.instance().setProperty("ftp.line.separator", "mac");
        }
        else if(sender.selectedItem().title().equals(WINDOWS_LINE_ENDINGS)) {
            Preferences.instance().setProperty("ftp.line.separator", "win");
        }
    }


    @Outlet
    private NSPopUpButton transfermodeCombobox; //IBOutlet

    public void setTransfermodeCombobox(NSPopUpButton transfermodeCombobox) {
        this.transfermodeCombobox = transfermodeCombobox;
        this.transfermodeCombobox.setTarget(this.id());
        this.transfermodeCombobox.setAction(Foundation.selector("transfermodeComboboxClicked:"));
        this.transfermodeCombobox.removeAllItems();
        this.transfermodeCombobox.addItemsWithTitles(NSArray.arrayWithObjects(TRANSFERMODE_AUTO, TRANSFERMODE_BINARY, TRANSFERMODE_ASCII));
        if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.BINARY.toString())) {
            this.transfermodeCombobox.selectItemWithTitle(TRANSFERMODE_BINARY);
        }
        else if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.ASCII.toString())) {
            this.transfermodeCombobox.selectItemWithTitle(TRANSFERMODE_ASCII);
        }
        else if(Preferences.instance().getProperty("ftp.transfermode").equals(FTPTransferType.AUTO.toString())) {
            this.transfermodeCombobox.selectItemWithTitle(TRANSFERMODE_AUTO);
        }
    }

    public void transfermodeComboboxClicked(NSPopUpButton sender) {
        if(sender.selectedItem().title().equals(TRANSFERMODE_BINARY)) {
            Preferences.instance().setProperty("ftp.transfermode", FTPTransferType.BINARY.toString());
            this.lineEndingCombobox.setEnabled(false);
            this.textFileTypeRegexField.setEnabled(false);
        }
        else if(sender.selectedItem().title().equals(TRANSFERMODE_ASCII)) {
            Preferences.instance().setProperty("ftp.transfermode", FTPTransferType.ASCII.toString());
            this.lineEndingCombobox.setEnabled(true);
            this.textFileTypeRegexField.setEnabled(false);
        }
        else if(sender.selectedItem().title().equals(TRANSFERMODE_AUTO)) {
            Preferences.instance().setProperty("ftp.transfermode", FTPTransferType.AUTO.toString());
            this.lineEndingCombobox.setEnabled(true);
            this.textFileTypeRegexField.setEnabled(true);
        }
    }

    @Outlet
    private NSPopUpButton protocolCombobox; //IBOutlet

    public void setProtocolCombobox(NSPopUpButton protocolCombobox) {
        this.protocolCombobox = protocolCombobox;
        this.protocolCombobox.setTarget(this.id());
        this.protocolCombobox.setAction(Foundation.selector("protocolComboboxClicked:"));
        this.protocolCombobox.removeAllItems();
        this.protocolCombobox.addItemsWithTitles(NSArray.arrayWithObjects(Protocol.getProtocolDescriptions()));
        final Protocol[] protocols = Protocol.getKnownProtocols();
        for(Protocol protocol : protocols) {
            final NSMenuItem item = this.protocolCombobox.itemWithTitle(protocol.getDescription());
            item.setRepresentedObject(protocol.getIdentifier());
            item.setImage(CDIconCache.instance().iconForName(protocol.icon(), 16));
        }

        final Protocol defaultProtocol
                = Protocol.forName(Preferences.instance().getProperty("connection.protocol.default"));
        this.protocolCombobox.selectItemWithTitle(defaultProtocol.getDescription());
    }

    public void protocolComboboxClicked(NSPopUpButton sender) {
        final Protocol selected = Protocol.forName(sender.selectedItem().representedObject());
        Preferences.instance().setProperty("connection.protocol.default", selected.getIdentifier());
        Preferences.instance().setProperty("connection.port.default", selected.getDefaultPort());
    }

    @Outlet
    private NSButton confirmDisconnectCheckbox; //IBOutlet

    public void setConfirmDisconnectCheckbox(NSButton confirmDisconnectCheckbox) {
        this.confirmDisconnectCheckbox = confirmDisconnectCheckbox;
        this.confirmDisconnectCheckbox.setTarget(this.id());
        this.confirmDisconnectCheckbox.setAction(Foundation.selector("confirmDisconnectCheckboxClicked:"));
        this.confirmDisconnectCheckbox.setState(Preferences.instance().getBoolean("browser.confirmDisconnect") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void confirmDisconnectCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("browser.confirmDisconnect", enabled);
    }

    @Outlet
    private NSButton secureDataChannelCheckbox; //IBOutlet

    /**
     * FTPS Data Channel Security
     *
     * @param secureDataChannelCheckbox
     */
    public void setSecureDataChannelCheckbox(NSButton secureDataChannelCheckbox) {
        this.secureDataChannelCheckbox = secureDataChannelCheckbox;
        this.secureDataChannelCheckbox.setTarget(this.id());
        this.secureDataChannelCheckbox.setAction(Foundation.selector("secureDataChannelCheckboxClicked:"));
        this.secureDataChannelCheckbox.setState(
                Preferences.instance().getProperty("ftp.tls.datachannel").equals("P") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void secureDataChannelCheckboxClicked(final NSButton sender) {
        if(sender.state() == NSCell.NSOnState) {
            Preferences.instance().setProperty("ftp.tls.datachannel", "P");
        }
        if(sender.state() == NSCell.NSOffState) {
            Preferences.instance().setProperty("ftp.tls.datachannel", "C");
        }
    }

    @Outlet
    private NSButton failInsecureDataChannelCheckbox; //IBOutlet

    /**
     * FTPS Data Channel Security
     *
     * @param failInsecureDataChannelCheckbox
     *
     */
    public void setFailInsecureDataChannelCheckbox(NSButton failInsecureDataChannelCheckbox) {
        this.failInsecureDataChannelCheckbox = failInsecureDataChannelCheckbox;
        this.failInsecureDataChannelCheckbox.setTarget(this.id());
        this.failInsecureDataChannelCheckbox.setAction(Foundation.selector("failInsecureDataChannelCheckboxClicked:"));
        this.failInsecureDataChannelCheckbox.setState(
                Preferences.instance().getBoolean("ftp.tls.datachannel.failOnError") ? NSCell.NSOffState : NSCell.NSOnState);
    }

    public void failInsecureDataChannelCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("ftp.tls.datachannel.failOnError", !enabled);
    }

    @Outlet
    private NSPopUpButton sshTransfersCombobox; //IBOutlet

    /**
     * SSH Transfers (SFTP or SCP)
     *
     * @param sshTransfersCombobox
     */
    public void setSshTransfersCombobox(NSPopUpButton sshTransfersCombobox) {
        this.sshTransfersCombobox = sshTransfersCombobox;
        this.sshTransfersCombobox.setTarget(this.id());
        this.sshTransfersCombobox.setAction(Foundation.selector("sshTransfersComboboxClicked:"));
        this.sshTransfersCombobox.removeAllItems();
        this.sshTransfersCombobox.addItemsWithTitles(NSArray.arrayWithObjects(Protocol.SFTP.getDescription(), Protocol.SCP.getDescription()));
        this.sshTransfersCombobox.itemWithTitle(Protocol.SFTP.getDescription()).setRepresentedObject(Protocol.SFTP.getIdentifier());
        this.sshTransfersCombobox.itemWithTitle(Protocol.SCP.getDescription()).setRepresentedObject(Protocol.SCP.getIdentifier());
        if(Preferences.instance().getProperty("ssh.transfer").equals(Protocol.SFTP.toString())) {
            this.sshTransfersCombobox.selectItemWithTitle(Protocol.SFTP.getDescription());
        }
        else if(Preferences.instance().getProperty("ssh.transfer").equals(Protocol.SCP.toString())) {
            this.sshTransfersCombobox.selectItemWithTitle(Protocol.SCP.getDescription());
        }
    }

    public void sshTransfersComboboxClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("ssh.transfer", sender.selectedItem().representedObject());
    }

    private void configureDefaultProtocolHandlerCombobox(NSPopUpButton defaultProtocolHandlerCombobox, Protocol protocol) {
        final String defaultHandler = URLSchemeHandlerConfiguration.instance().getDefaultHandlerForURLScheme(protocol.getScheme());
        if(null == defaultHandler) {
            defaultProtocolHandlerCombobox.addItemWithTitle(Locale.localizedString("Unknown"));
            defaultProtocolHandlerCombobox.setEnabled(false);
            return;
        }
        log.debug("Default Protocol Handler for " + protocol + ":" + defaultHandler);
        final String[] bundleIdentifiers = URLSchemeHandlerConfiguration.instance().getAllHandlersForURLScheme(protocol.getScheme());
        for(String bundleIdentifier : bundleIdentifiers) {
            final String path = NSWorkspace.sharedWorkspace().absolutePathForAppBundleWithIdentifier(bundleIdentifier);
            if(StringUtils.isEmpty(path)) {
                continue;
            }
            NSBundle app = NSBundle.bundleWithPath(path);
            if(null == app) {
                continue;
            }
            if(null == app.infoDictionary().objectForKey("CFBundleName")) {
                continue;
            }
            defaultProtocolHandlerCombobox.addItemWithTitle(app.infoDictionary().objectForKey("CFBundleName").toString());
            final NSMenuItem item = defaultProtocolHandlerCombobox.lastItem();
            item.setImage(CDIconCache.instance().iconForPath(LocalFactory.createLocal(path), 16));
            item.setRepresentedObject(bundleIdentifier);
            if(bundleIdentifier.equals(defaultHandler)) {
                defaultProtocolHandlerCombobox.selectItem(item);
            }
        }
    }

    @Outlet
    private NSPopUpButton defaultFTPHandlerCombobox; //IBOutlet

    /**
     * Protocol Handler FTP
     *
     * @param defaultFTPHandlerCombobox
     */
    public void setDefaultFTPHandlerCombobox(NSPopUpButton defaultFTPHandlerCombobox) {
        this.defaultFTPHandlerCombobox = defaultFTPHandlerCombobox;
        this.defaultFTPHandlerCombobox.setTarget(this.id());
        this.defaultFTPHandlerCombobox.setAction(Foundation.selector("defaultFTPHandlerComboboxClicked:"));
        this.defaultFTPHandlerCombobox.removeAllItems();
        this.configureDefaultProtocolHandlerCombobox(this.defaultFTPHandlerCombobox, Protocol.FTP);
    }

    public void defaultFTPHandlerComboboxClicked(NSPopUpButton sender) {
        final Protocol protocol = Protocol.forName(sender.selectedItem().representedObject());
        URLSchemeHandlerConfiguration.instance().setDefaultHandlerForURLScheme(
                new String[]{Protocol.FTP.getScheme(), Protocol.FTP_TLS.getScheme()}, protocol.getIdentifier()
        );
    }

    @Outlet
    private NSPopUpButton defaultSFTPHandlerCombobox; //IBOutlet

    /**
     * Protocol Handler SFTP
     *
     * @param defaultSFTPHandlerCombobox
     */
    public void setDefaultSFTPHandlerCombobox(NSPopUpButton defaultSFTPHandlerCombobox) {
        this.defaultSFTPHandlerCombobox = defaultSFTPHandlerCombobox;
        this.defaultSFTPHandlerCombobox.setTarget(this.id());
        this.defaultSFTPHandlerCombobox.setAction(Foundation.selector("defaultSFTPHandlerComboboxClicked:"));
        this.defaultSFTPHandlerCombobox.removeAllItems();
        this.configureDefaultProtocolHandlerCombobox(this.defaultSFTPHandlerCombobox, Protocol.SFTP);
    }

    public void defaultSFTPHandlerComboboxClicked(NSPopUpButton sender) {
        URLSchemeHandlerConfiguration.instance().setDefaultHandlerForURLScheme(
                Protocol.SFTP.getScheme(), sender.selectedItem().representedObject()
        );
    }

    @Outlet
    private NSPopUpButton defaultDownloadThrottleCombobox; //IBOutlet

    /**
     * Download Bandwidth
     *
     * @param defaultDownloadThrottleCombobox
     *
     */
    public void setDefaultDownloadThrottleCombobox(NSPopUpButton defaultDownloadThrottleCombobox) {
        this.defaultDownloadThrottleCombobox = defaultDownloadThrottleCombobox;
        this.defaultDownloadThrottleCombobox.setTarget(this.id());
        this.defaultDownloadThrottleCombobox.setAction(Foundation.selector("defaultDownloadThrottleComboboxClicked:"));
        float bandwidth = (int) Preferences.instance().getFloat("queue.download.bandwidth.bytes");
        if(-1 == bandwidth) {
            this.defaultDownloadThrottleCombobox.selectItemWithTag(-1);
        }
        else {
            this.defaultDownloadThrottleCombobox.selectItemWithTag((int) bandwidth / 1024);
        }
    }

    public void defaultDownloadThrottleComboboxClicked(NSPopUpButton sender) {
        int tag = sender.selectedItem().tag();
        if(-1 == tag) {
            Preferences.instance().setProperty("queue.download.bandwidth.bytes", -1);
        }
        else {
            Preferences.instance().setProperty("queue.download.bandwidth.bytes", (float) tag * 1024);
        }
    }

    @Outlet
    private NSPopUpButton defaultUploadThrottleCombobox; //IBOutlet

    /**
     * Upload Bandwidth
     *
     * @param defaultUploadThrottleCombobox
     */
    public void setDefaultUploadThrottleCombobox(NSPopUpButton defaultUploadThrottleCombobox) {
        this.defaultUploadThrottleCombobox = defaultUploadThrottleCombobox;
        this.defaultUploadThrottleCombobox.setTarget(this.id());
        this.defaultUploadThrottleCombobox.setAction(Foundation.selector("defaultUploadThrottleComboboxClicked:"));
        float bandwidth = (int) Preferences.instance().getFloat("queue.upload.bandwidth.bytes");
        if(-1 == bandwidth) {
            this.defaultUploadThrottleCombobox.selectItemWithTag(-1);
        }
        else {
            this.defaultUploadThrottleCombobox.selectItemWithTag((int) bandwidth / 1024);
        }
    }

    public void defaultUploadThrottleComboboxClicked(NSPopUpButton sender) {
        int tag = sender.selectedItem().tag();
        if(-1 == tag) {
            Preferences.instance().setProperty("queue.upload.bandwidth.bytes", -1);
        }
        else {
            Preferences.instance().setProperty("queue.upload.bandwidth.bytes", (float) tag * 1024);
        }
    }

    @Outlet
    private NSButton updateCheckbox; //IBOutlet

    public void setUpdateCheckbox(NSButton updateCheckbox) {
        this.updateCheckbox = updateCheckbox;
        this.updateCheckbox.setTarget(this.id());
        this.updateCheckbox.setAction(Foundation.selector("updateCheckboxClicked:"));
        this.updateCheckbox.setState(Preferences.instance().getBoolean("update.check") ? NSCell.NSOnState : NSCell.NSOffState);
    }

    public void updateCheckboxClicked(final NSButton sender) {
        boolean enabled = sender.state() == NSCell.NSOnState;
        Preferences.instance().setProperty("update.check", enabled);
        // Update the Sparkle property
        if(enabled) {
            Preferences.instance().setProperty("SUScheduledCheckInterval", Preferences.instance().getProperty("update.check.interval"));
        }
        else {
            Preferences.instance().deleteProperty("SUScheduledCheckInterval");
        }
    }

    @Outlet
    private NSPopUpButton defaultBucketLocation; //IBOutlet

    public void setDefaultBucketLocation(NSPopUpButton defaultBucketLocation) {
        this.defaultBucketLocation = defaultBucketLocation;
        this.defaultBucketLocation.setAutoenablesItems(false);
        this.defaultBucketLocation.removeAllItems();
        this.defaultBucketLocation.addItemWithTitle("US");
        this.defaultBucketLocation.addItemWithTitle(S3Bucket.LOCATION_EUROPE);
        this.defaultBucketLocation.setTarget(this.id());
        this.defaultBucketLocation.setAction(Foundation.selector("defaultBucketLocationClicked:"));
        this.defaultBucketLocation.selectItemWithTitle(Preferences.instance().getProperty("s3.location"));
    }

    public void defaultBucketLocationClicked(NSPopUpButton sender) {
        Preferences.instance().setProperty("s3.location", sender.titleOfSelectedItem());
    }
}
