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
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.threading.AbstractBackgroundAction;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.delegate.MenuDelegate;
import ch.cyberduck.ui.cocoa.foundation.*;
import ch.cyberduck.ui.cocoa.threading.AlertRepeatableBackgroundAction;
import ch.cyberduck.ui.cocoa.threading.WindowMainAction;
import ch.cyberduck.ui.cocoa.util.HyperlinkAttributedStringFactory;

import org.apache.log4j.Logger;
import org.rococoa.Foundation;
import org.rococoa.ID;
import org.rococoa.Rococoa;
import org.rococoa.Selector;
import org.rococoa.cocoa.CGFloat;
import org.rococoa.cocoa.foundation.NSInteger;
import org.rococoa.cocoa.foundation.NSUInteger;

import java.util.Map;

/**
 * @version $Id$
 */
public class CDTransferController extends CDWindowController implements NSToolbar.Delegate {
    private static Logger log = Logger.getLogger(CDTransferController.class);

    private static CDTransferController instance = null;

    private NSToolbar toolbar;

    @Override
    public void awakeFromNib() {
        this.toolbar = NSToolbar.toolbarWithIdentifier("Queue Toolbar");
        this.toolbar.setDelegate(this.id());
        this.toolbar.setAllowsUserCustomization(true);
        this.toolbar.setAutosavesConfiguration(true);
        this.window.setToolbar(toolbar);

        super.awakeFromNib();
    }

    @Override
    public void setWindow(NSWindow window) {
        this.window = window;
        this.window.setReleasedWhenClosed(false);
        this.window.setDelegate(this.id());
        this.window.setMovableByWindowBackground(true);
        this.window.setTitle(Locale.localizedString("Transfers"));
    }

    /**
     * @param notification
     */
    public void windowDidBecomeKey(NSNotification notification) {
        this.updateHighlight();
    }

    /**
     * @param notification
     */
    public void windowDidResignKey(NSNotification notification) {
        this.updateHighlight();
    }

    /**
     * @param notification
     */
    @Override
    public void windowWillClose(NSNotification notification) {
        // Do not call super as we are a singleton. super#windowWillClose would invalidate me
    }

    @Outlet
    private NSTextField urlField;

    public void setUrlField(NSTextField urlField) {
        this.urlField = urlField;
        this.urlField.setAllowsEditingTextAttributes(true);
        this.urlField.setSelectable(true);
    }

    @Outlet
    private NSTextField localField;

    public void setLocalField(NSTextField localField) {
        this.localField = localField;
    }

    @Outlet
    private NSImageView iconView;

    public void setIconView(final NSImageView iconView) {
        this.iconView = iconView;
    }

    @Outlet
    private NSStepper queueSizeStepper;

    public void setQueueSizeStepper(final NSStepper queueSizeStepper) {
        this.queueSizeStepper = queueSizeStepper;
        this.queueSizeStepper.setTarget(this.id());
        this.queueSizeStepper.setAction(Foundation.selector("queueSizeStepperChanged:"));
    }

    public void queueSizeStepperChanged(final NSObject sender) {
        synchronized(Queue.instance()) {
            Queue.instance().notify();
        }
    }

    @Outlet
    private NSTextField filterField;

    public void setFilterField(NSTextField filterField) {
        this.filterField = filterField;
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("filterFieldTextDidChange:"),
                NSControl.NSControlTextDidChangeNotification,
                this.filterField);
    }

    public void filterFieldTextDidChange(NSNotification notification) {
        NSDictionary userInfo = notification.userInfo();
        if(null != userInfo) {
            Object o = userInfo.allValues().lastObject();
            if(null != o) {
                final String searchString = ((NSText) o).string();
                transferModel.setFilter(searchString);
                this.reloadData();
            }
        }
    }

    /**
     * Change focus to filter field
     *
     * @param sender
     */
    public void searchButtonClicked(final NSObject sender) {
        this.window().makeFirstResponder(this.filterField);
    }

    private CDTranscriptController transcript;

    private NSDrawer logDrawer;

    public void drawerWillOpen(NSNotification notification) {
        logDrawer.setContentSize(new NSSize(logDrawer.contentSize().width.doubleValue(),
                Preferences.instance().getDouble("queue.logDrawer.size.height")
        ));
    }

    public void drawerDidOpen(NSNotification notification) {
        Preferences.instance().setProperty("queue.logDrawer.isOpen", true);
    }

    public void drawerWillClose(NSNotification notification) {
        Preferences.instance().setProperty("queue.logDrawer.size.height",
                logDrawer.contentSize().height.doubleValue());
    }

    public void drawerDidClose(NSNotification notification) {
        Preferences.instance().setProperty("queue.logDrawer.isOpen", false);
    }

    public void setLogDrawer(NSDrawer logDrawer) {
        this.logDrawer = logDrawer;
        this.transcript = new CDTranscriptController();
        this.logDrawer.setContentView(this.transcript.getLogView());
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("drawerWillOpen:"),
                NSDrawer.DrawerWillOpenNotification,
                this.logDrawer);
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("drawerDidOpen:"),
                NSDrawer.DrawerDidOpenNotification,
                this.logDrawer);
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("drawerWillClose:"),
                NSDrawer.DrawerWillCloseNotification,
                this.logDrawer);
        NSNotificationCenter.defaultCenter().addObserver(this.id(),
                Foundation.selector("drawerDidClose:"),
                NSDrawer.DrawerDidCloseNotification,
                this.logDrawer);
    }

    public void toggleLogDrawer(final ID sender) {
        this.logDrawer.toggle(sender);
    }

    @Outlet
    private NSPopUpButton bandwidthPopup;

    private MenuDelegate bandwidthPopupDelegate;

    public void setBandwidthPopup(NSPopUpButton bandwidthPopup) {
        this.bandwidthPopup = bandwidthPopup;
        this.bandwidthPopup.setEnabled(false);
        this.bandwidthPopup.setAllowsMixedState(true);
        this.bandwidthPopup.setTarget(this.id());
        this.bandwidthPopup.setAction(Foundation.selector("bandwidthPopupChanged:"));
        this.bandwidthPopup.itemAtIndex(0).setImage(CDIconCache.instance().iconForName("bandwidth", 16));
        this.bandwidthPopup.menu().setDelegate((this.bandwidthPopupDelegate = new BandwidthDelegate()).id());
    }

    private class BandwidthDelegate extends MenuDelegate {
        public int numberOfItemsInMenu(NSMenu menu) {
            return menu.numberOfItems();
        }

        public boolean menuUpdateItemAtIndex(NSMenu menu, NSMenuItem item, int i, boolean shouldCancel) {
            final int selected = transferTable.numberOfSelectedRows().intValue();
            final int tag = item.tag();
            NSIndexSet iterator = transferTable.selectedRowIndexes();
            for(NSUInteger index = iterator.firstIndex(); index.longValue() != NSIndexSet.NSNotFound; index = iterator.indexGreaterThanIndex(index)) {
                if(index.intValue() == -1) {
                    break;
                }
                Transfer transfer = TransferCollection.instance().get(index.intValue());
                if(BandwidthThrottle.UNLIMITED == transfer.getBandwidth()) {
                    if(BandwidthThrottle.UNLIMITED == tag) {
                        item.setState(selected > 1 ? NSCell.NSMixedState : NSCell.NSOnState);
                        break;
                    }
                    else {
                        item.setState(NSCell.NSOffState);
                    }
                }
                else {
                    int bandwidth = (int) transfer.getBandwidth() / 1024;
                    if(tag == bandwidth) {
                        item.setState(selected > 1 ? NSCell.NSMixedState : NSCell.NSOnState);
                        break;
                    }
                    else {
                        item.setState(NSCell.NSOffState);
                    }
                }
            }
            return !shouldCancel;
        }
    }

    public void bandwidthPopupChanged(NSPopUpButton sender) {
        NSIndexSet iterator = transferTable.selectedRowIndexes();
        int bandwidth = BandwidthThrottle.UNLIMITED;
        if(sender.selectedItem().tag() > 0) {
            bandwidth = sender.selectedItem().tag() * 1024; // from Kilobytes to Bytes
        }
        for(NSUInteger index = iterator.firstIndex(); index.longValue() != NSIndexSet.NSNotFound; index = iterator.indexGreaterThanIndex(index)) {
            if(index.intValue() == -1) {
                break;
            }
            Transfer transfer = TransferCollection.instance().get(index.intValue());
            transfer.setBandwidth(bandwidth);
        }
        this.updateBandwidthPopup();
    }

    private CDTransferController() {
        this.loadBundle();
    }

    public static CDTransferController instance() {
        synchronized(NSApplication.sharedApplication()) {
            if(null == instance) {
                instance = new CDTransferController();
            }
            return instance;
        }
    }

    @Override
    protected String getBundleName() {
        return "Transfer";
    }

    /*
      * @return NSApplication.TerminateLater or NSApplication.TerminateNow depending if there are
      * running transfers to be checked first
      */
    public static int applicationShouldTerminate(final NSApplication app) {
        if(null != instance) {
            //Saving state of transfer window
            Preferences.instance().setProperty("queue.openByDefault", instance.window().isVisible());
            if(TransferCollection.instance().numberOfRunningTransfers() > 0) {
                final NSAlert alert = NSAlert.alert(Locale.localizedString("Transfer in progress"), //title
                        Locale.localizedString("There are files currently being transferred. Quit anyway?"), // message
                        Locale.localizedString("Quit"), // defaultbutton
                        Locale.localizedString("Cancel"), //alternative button
                        null //other button
                );
                instance.alert(alert, new CDSheetCallback() {
                    public void callback(int returncode) {
                        if(returncode == DEFAULT_OPTION) { //Quit
                            for(int i = 0; i < TransferCollection.instance().size(); i++) {
                                Transfer transfer = TransferCollection.instance().get(i);
                                if(transfer.isRunning()) {
                                    transfer.interrupt();
                                }
                            }
                            app.replyToApplicationShouldTerminate(true);
                        }
                        if(returncode == OTHER_OPTION) { //Cancel
                            app.replyToApplicationShouldTerminate(false);
                        }
                    }
                });
                return NSApplication.NSTerminateLater; //break
            }
        }
        return NSApplication.NSTerminateNow;
    }

    private CDTransferTableDataSource transferModel;
    @Outlet
    private NSTableView transferTable;
    private CDAbstractTableDelegate<Transfer> delegate;

    public void setQueueTable(NSTableView view) {
        this.transferTable = view;
        this.transferTable.setDataSource((this.transferModel = new CDTransferTableDataSource()).id());
        this.transferTable.setDelegate((this.delegate = new CDAbstractTableDelegate<Transfer>() {
            public String tooltip(Transfer t) {
                return t.getName();
            }

            public void enterKeyPressed(final NSObject sender) {
                this.tableRowDoubleClicked(sender);
            }

            public void deleteKeyPressed(final NSObject sender) {
                deleteButtonClicked(sender);
            }

            public void tableColumnClicked(NSTableView view, NSTableColumn tableColumn) {
                ;
            }

            public void tableRowDoubleClicked(final NSObject sender) {
                reloadButtonClicked(sender);
            }

            public void selectionIsChanging(NSNotification notification) {
                updateHighlight();
            }

            public void selectionDidChange(NSNotification notification) {
                updateHighlight();
                updateSelection();
            }

            public void tableView_willDisplayCell_forTableColumn_row(NSTableView view, NSCell cell, NSTableColumn tableColumn, NSInteger row) {
                Rococoa.cast(cell, CDControllerCell.class).setView(transferModel.getController(row.intValue()).view());
            }
        }).id());
        // receive drag events from types
        // in fact we are not interested in file promises, but because the browser model can only initiate
        // a drag with tableView.dragPromisedFilesOfTypes(), we listens for those events
        // and then use the private pasteboard instead.
        this.transferTable.registerForDraggedTypes(NSArray.arrayWithObjects(
                PathPasteboard.identifier(),
                NSPasteboard.StringPboardType,
                NSPasteboard.FilesPromisePboardType));

        {
            NSTableColumn c = NSTableColumn.tableColumnWithIdentifier(CDTransferTableDataSource.PROGRESS_COLUMN);
            c.setMinWidth(80f);
            c.setWidth(300f);
            c.setResizingMask(NSTableColumn.NSTableColumnAutoresizingMask);
            c.setDataCell(prototype);
            this.transferTable.addTableColumn(c);
        }
        this.transferTable.setGridStyleMask(NSTableView.NSTableViewSolidHorizontalGridLineMask);
        this.transferTable.setRowHeight(new CGFloat(82));
        //selection properties
        this.transferTable.setAllowsMultipleSelection(true);
        this.transferTable.setAllowsEmptySelection(true);
        this.transferTable.setAllowsColumnReordering(false);
        this.transferTable.sizeToFit();
    }

    private final NSCell prototype = CDControllerCell.controllerCell();

    /**
     *
     */
    private void updateHighlight() {
        boolean isKeyWindow = window().isKeyWindow();
        for(int i = 0; i < transferModel.getSource().size(); i++) {
            transferModel.setHighlighted(i, transferTable.isRowSelected(new NSInteger(i)) && isKeyWindow);
        }
    }

    /**
     *
     */
    private void updateSelection() {
        log.debug("updateSelection");
        this.updateLabels();
        this.updateIcon();
        this.updateBandwidthPopup();
        toolbar.validateVisibleItems();
    }

    /**
     *
     */
    private void updateLabels() {
        log.debug("updateLabels");
        final int selected = transferTable.numberOfSelectedRows().intValue();
        if(1 == selected) {
            final Transfer transfer = transferModel.getSource().get(transferTable.selectedRow().intValue());
            // Draw text fields at the bottom
            final String url = transfer.getRoot().toURL();
            urlField.setAttributedStringValue(
                    HyperlinkAttributedStringFactory.create(
                            NSMutableAttributedString.create(url, TRUNCATE_MIDDLE_ATTRIBUTES), url)
            );
            if(transfer.numberOfRoots() == 1) {
                localField.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(transfer.getRoot().getLocal().getAbsolute(),
                        TRUNCATE_MIDDLE_ATTRIBUTES));
            }
            else {
                localField.setAttributedStringValue(NSAttributedString.attributedStringWithAttributes(Locale.localizedString("Multiple files"),
                        TRUNCATE_MIDDLE_ATTRIBUTES));
            }
        }
        else {
            urlField.setStringValue("");
            localField.setStringValue("");
        }
    }

    /**
     *
     */
    private void updateIcon() {
        log.debug("updateIcon");
        final int selected = transferTable.numberOfSelectedRows().intValue();
        if(1 != selected) {
            iconView.setImage(null);
            return;
        }
        final Transfer transfer = transferModel.getSource().get(transferTable.selectedRow().intValue());
        // Draw file type icon
        if(transfer.numberOfRoots() == 1) {
            iconView.setImage(CDIconCache.instance().iconForPath(transfer.getRoot().getLocal(), 32));
        }
        else {
            iconView.setImage(CDIconCache.instance().iconForName("multipleDocuments", 32));
        }
    }

    /**
     *
     */
    private void updateBandwidthPopup() {
        log.debug("updateBandwidthPopup");
        final int selected = transferTable.numberOfSelectedRows().intValue();
        bandwidthPopup.setEnabled(selected > 0);
        NSIndexSet iterator = transferTable.selectedRowIndexes();
        for(NSUInteger index = iterator.firstIndex(); index.longValue() != NSIndexSet.NSNotFound; index = iterator.indexGreaterThanIndex(index)) {
            if(index.intValue() == -1) {
                break;
            }
            final Transfer transfer = transferModel.getSource().get(index.intValue());
            if(transfer instanceof SyncTransfer) {
                // Currently we do not support bandwidth throtling for sync transfers due to
                // the problem of mapping both download and upload rate in the GUI
                bandwidthPopup.setEnabled(false);
                // Break through and set the standard icon below
                break;
            }
            if(transfer.getBandwidth() != BandwidthThrottle.UNLIMITED) {
                // Mark as throttled
                this.bandwidthPopup.itemAtIndex(0).setImage(NSImage.imageNamed("turtle.tiff"));
                return;
            }
        }
        // Set the standard icon
        this.bandwidthPopup.itemAtIndex(0).setImage(CDIconCache.instance().iconForName("bandwidth", 16));
    }

    private void reloadData() {
        while(transferTable.subviews().count().intValue() > 0) {
            (Rococoa.cast(transferTable.subviews().lastObject(), NSView.class)).removeFromSuperviewWithoutNeedingDisplay();
        }
        transferTable.reloadData();
        this.updateHighlight();
    }

    /**
     * Remove this item form the list
     *
     * @param transfer
     */
    public void removeTransfer(final Transfer transfer) {
        TransferCollection.instance().remove(transfer);
        this.reloadData();
    }

    /**
     * Add this item to the list; select it and scroll the view to make it visible
     *
     * @param transfer
     */
    public void addTransfer(final Transfer transfer) {
        TransferCollection.instance().add(transfer);
        final int row = TransferCollection.instance().size() - 1;
        this.reloadData();
        transferTable.selectRowIndexes(NSIndexSet.indexSetWithIndex(row), false);
        transferTable.scrollRowToVisible(new NSInteger(row));
    }

    /**
     * @param transfer
     */
    public void startTransfer(final Transfer transfer) {
        this.startTransfer(transfer, false, false);
    }

    /**
     * @param transfer
     * @param resumeRequested
     * @param reloadRequested
     */
    private void startTransfer(final Transfer transfer, final boolean resumeRequested, final boolean reloadRequested) {
        if(!TransferCollection.instance().contains(transfer)) {
            this.addTransfer(transfer);
        }
        if(Preferences.instance().getBoolean("queue.orderFrontOnStart")) {
            this.window.makeKeyAndOrderFront(null);
        }
        this.background(new AlertRepeatableBackgroundAction(this) {
            private boolean resume = resumeRequested;
            private boolean reload = reloadRequested;

            private TransferListener tl;

            public boolean prepare() {
                transfer.addListener(tl = new TransferAdapter() {
                    @Override
                    public void transferQueued() {
                        validateToolbar();
                    }

                    @Override
                    public void transferResumed() {
                        validateToolbar();
                    }

                    @Override
                    public void transferWillStart() {
                        validateToolbar();
                    }

                    @Override
                    public void transferDidEnd() {
                        validateToolbar();
                    }
                });
                if(transfer.getSession() instanceof ch.cyberduck.core.sftp.SFTPSession) {
                    ((ch.cyberduck.core.sftp.SFTPSession) transfer.getSession()).setHostKeyVerificationController(
                            new CDHostKeyController(CDTransferController.this));
                }
                transfer.getSession().setLoginController(new CDLoginController(CDTransferController.this));

                return super.prepare();
            }

            public void run() {
                final TransferOptions options = new TransferOptions();
                options.reloadRequested = reload;
                options.resumeRequested = resume;
                transfer.start(CDTransferPrompt.create(CDTransferController.this, transfer), options);
            }

            @Override
            public void finish() {
                super.finish();
                if(transfer.getSession() instanceof ch.cyberduck.core.sftp.SFTPSession) {
                    ((ch.cyberduck.core.sftp.SFTPSession) transfer.getSession()).setHostKeyVerificationController(null);
                }
                transfer.getSession().setLoginController(null);
                transfer.removeListener(tl);
            }

            public void cleanup() {
                if(transfer.isComplete() && !transfer.isCanceled()) {
                    if(transfer.isReset()) {
                        if(Preferences.instance().getBoolean("queue.removeItemWhenComplete")) {
                            removeTransfer(transfer);
                        }
                        if(Preferences.instance().getBoolean("queue.orderBackOnStop")) {
                            if(!(TransferCollection.instance().numberOfRunningTransfers() > 0)) {
                                window().close();
                            }
                        }
                    }
                }
                // Upon retry, use resume
                reload = false;
                resume = true;
                TransferCollection.instance().save();
            }

            public Session getSession() {
                return transfer.getSession();
            }

            @Override
            public void pause() {
                transfer.fireTransferQueued();
                super.pause();
                transfer.fireTransferResumed();
            }

            @Override
            public boolean isCanceled() {
                if((transfer.isRunning() || transfer.isQueued()) && transfer.isCanceled()) {
                    return true;
                }
                return super.isCanceled();
            }

            @Override
            public void log(final boolean request, final String message) {
                if(logDrawer.state() == NSDrawer.OpenState) {
                    invoke(new WindowMainAction(CDTransferController.this) {
                        public void run() {
                            CDTransferController.this.transcript.log(request, message);
                        }
                    });
                }
                super.log(request, message);
            }

            private final Object lock = new Object();

            @Override
            public Object lock() {
                // No synchronization with other tasks
                return lock;
            }
        });
    }

    private void validateToolbar() {
        invoke(new WindowMainAction(CDTransferController.this) {
            public void run() {
                window.toolbar().validateVisibleItems();
                updateIcon();
            }
        });
    }

    private static final String TOOLBAR_RESUME = "Resume";
    private static final String TOOLBAR_RELOAD = "Reload";
    private static final String TOOLBAR_STOP = "Stop";
    private static final String TOOLBAR_REMOVE = "Remove";
    private static final String TOOLBAR_CLEAN_UP = "Clean Up";
    private static final String TOOLBAR_OPEN = "Open";
    private static final String TOOLBAR_SHOW = "Show";
    private static final String TOOLBAR_TRASH = "Trash";
    private static final String TOOLBAR_FILTER = "Search";

    /**
     * NSToolbar.Delegate
     *
     * @param toolbar
     * @param itemIdentifier
     * @param flag
     */
    public NSToolbarItem toolbar_itemForItemIdentifier_willBeInsertedIntoToolbar(NSToolbar toolbar, final String itemIdentifier, boolean flag) {
        final NSToolbarItem item = NSToolbarItem.itemWithIdentifier(itemIdentifier);
        if(itemIdentifier.equals(TOOLBAR_STOP)) {
            item.setLabel(Locale.localizedString(TOOLBAR_STOP));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_STOP));
            item.setToolTip(Locale.localizedString(TOOLBAR_STOP));
            item.setImage(CDIconCache.instance().iconForName("stop", 32));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("stopButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_RESUME)) {
            item.setLabel(Locale.localizedString(TOOLBAR_RESUME));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_RESUME));
            item.setToolTip(Locale.localizedString(TOOLBAR_RESUME));
            item.setImage(NSImage.imageNamed("resume.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("resumeButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_RELOAD)) {
            item.setLabel(Locale.localizedString(TOOLBAR_RELOAD));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_RELOAD));
            item.setToolTip(Locale.localizedString(TOOLBAR_RELOAD));
            item.setImage(NSImage.imageNamed("reload.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("reloadButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_SHOW)) {
            item.setLabel(Locale.localizedString(TOOLBAR_SHOW));
            item.setPaletteLabel(Locale.localizedString("Show in Finder"));
            item.setToolTip(Locale.localizedString("Show in Finder"));
            item.setImage(NSImage.imageNamed("reveal.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("revealButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_OPEN)) {
            item.setLabel(Locale.localizedString(TOOLBAR_OPEN));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_OPEN));
            item.setToolTip(Locale.localizedString(TOOLBAR_OPEN));
            item.setImage(NSImage.imageNamed("open.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("openButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_REMOVE)) {
            item.setLabel(Locale.localizedString(TOOLBAR_REMOVE));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_REMOVE));
            item.setToolTip(Locale.localizedString(TOOLBAR_REMOVE));
            item.setImage(NSImage.imageNamed("clean.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("deleteButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_CLEAN_UP)) {
            item.setLabel(Locale.localizedString(TOOLBAR_CLEAN_UP));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_CLEAN_UP));
            item.setToolTip(Locale.localizedString(TOOLBAR_CLEAN_UP));
            item.setImage(NSImage.imageNamed("cleanAll.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("clearButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_TRASH)) {
            item.setLabel(Locale.localizedString(TOOLBAR_TRASH));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_TRASH));
            item.setToolTip(Locale.localizedString("Move to Trash"));
            item.setImage(NSImage.imageNamed("trash.tiff"));
            item.setTarget(this.id());
            item.setAction(Foundation.selector("trashButtonClicked:"));
            return item;
        }
        if(itemIdentifier.equals(TOOLBAR_FILTER)) {
            item.setLabel(Locale.localizedString(TOOLBAR_FILTER));
            item.setPaletteLabel(Locale.localizedString(TOOLBAR_FILTER));
            item.setView(this.filterField);
            item.setMinSize(this.filterField.frame().size);
            item.setMaxSize(this.filterField.frame().size);
            return item;
        }
        // itemIdent refered to a toolbar item that is not provide or supported by us or cocoa.
        // Returning null will inform the toolbar this kind of item is not supported.
        return null;
    }

    public void paste(final NSObject sender) {
        log.debug("paste");
        final Map<Host, PathPasteboard> boards = PathPasteboard.allPasteboards();
        if(!boards.isEmpty()) {
            for(PathPasteboard pasteboard : boards.values()) {
                TransferCollection.instance().add(new DownloadTransfer(pasteboard.getFiles()));
            }
            boards.clear();
            this.reloadData();
        }
    }

    public void stopButtonClicked(final NSObject sender) {
        NSIndexSet iterator = transferTable.selectedRowIndexes();
        for(NSUInteger index = iterator.firstIndex(); index.longValue() != NSIndexSet.NSNotFound; index = iterator.indexGreaterThanIndex(index)) {
            if(index.intValue() == -1) {
                break;
            }
            final Transfer transfer = transferModel.getSource().get(index.intValue());
            if(transfer.isRunning() || transfer.isQueued()) {
                this.background(new AbstractBackgroundAction() {
                    public void run() {
                        transfer.cancel();
                    }

                    public void cleanup() {
                        ;
                    }
                });
            }
        }
    }

    public void stopAllButtonClicked(final NSObject sender) {
        final Collection<Transfer> transfers = transferModel.getSource();
        for(final Transfer transfer : transfers) {
            if(transfer.isRunning() || transfer.isQueued()) {
                this.background(new AbstractBackgroundAction() {
                    public void run() {
                        transfer.cancel();
                    }

                    public void cleanup() {
                        ;
                    }
                });
            }
        }
    }

    public void resumeButtonClicked(final NSObject sender) {
        NSIndexSet iterator = transferTable.selectedRowIndexes();
        for(NSUInteger index = iterator.firstIndex(); index.longValue() != NSIndexSet.NSNotFound; index = iterator.indexGreaterThanIndex(index)) {
            if(index.intValue() == -1) {
                break;
            }
            final Collection<Transfer> transfers = transferModel.getSource();
            final Transfer transfer = transfers.get(index.intValue());
            if(!transfer.isRunning() && !transfer.isQueued()) {
                this.startTransfer(transfer, true, false);
            }
        }
    }

    public void reloadButtonClicked(final NSObject sender) {
        NSIndexSet iterator = transferTable.selectedRowIndexes();
        for(NSUInteger index = iterator.firstIndex(); index.longValue() != NSIndexSet.NSNotFound; index = iterator.indexGreaterThanIndex(index)) {
            if(index.intValue() == -1) {
                break;
            }
            final Collection<Transfer> transfers = transferModel.getSource();
            final Transfer transfer = transfers.get(index.intValue());
            if(!transfer.isRunning() && !transfer.isQueued()) {
                this.startTransfer(transfer, false, true);
            }
        }
    }

    public void openButtonClicked(final NSObject sender) {
        if(transferTable.numberOfSelectedRows().intValue() == 1) {
            final Transfer transfer = transferModel.getSource().get(transferTable.selectedRow().intValue());
            for(Path i : transfer.getRoots()) {
                Local l = i.getLocal();
                if(!NSWorkspace.sharedWorkspace().openFile(l.getAbsolute())) {
                    if(transfer.isComplete()) {
                        this.alert(NSAlert.alert(Locale.localizedString("Could not open the file"), //title
                                Locale.localizedString("Could not open the file") + " \""
                                        + l.getName()
                                        + "\". " + Locale.localizedString("It moved since you downloaded it."), // message
                                Locale.localizedString("OK"), // defaultbutton
                                null, //alternative button
                                null //other button
                        ));
                    }
                    else {
                        this.alert(NSAlert.alert(Locale.localizedString("Could not open the file"), //title
                                Locale.localizedString("Could not open the file") + " \""
                                        + l.getName()
                                        + "\". " + Locale.localizedString("The file has not yet been downloaded."), // message
                                Locale.localizedString("OK"), // defaultbutton
                                null, //alternative button
                                null //other button
                        ));
                    }
                }
            }
        }
    }

    public void revealButtonClicked(final NSObject sender) {
        if(transferTable.numberOfSelectedRows().intValue() == 1) {
            final Transfer transfer = transferModel.getSource().get(transferTable.selectedRow().intValue());
            for(Path i : transfer.getRoots()) {
                Local l = i.getLocal();
                // If a second path argument is specified, a new file viewer is opened. If you specify an
                // empty string (@"") for this parameter, the file is selected in the main viewer.
                if(!NSWorkspace.sharedWorkspace().selectFile(l.getAbsolute(), l.getParent().getAbsolute())) {
                    if(transfer.isComplete()) {
                        this.alert(NSAlert.alert(Locale.localizedString("Could not show the file in the Finder"), //title
                                Locale.localizedString("Could not show the file") + " \""
                                        + l.getName()
                                        + "\". " + Locale.localizedString("It moved since you downloaded it."), // message
                                Locale.localizedString("OK"), // defaultbutton
                                null, //alternative button
                                null //other button
                        ));
                    }
                    else {
                        this.alert(NSAlert.alert(Locale.localizedString("Could not show the file in the Finder"), //title
                                Locale.localizedString("Could not show the file") + " \""
                                        + l.getName()
                                        + "\". " + Locale.localizedString("The file has not yet been downloaded."), // message
                                Locale.localizedString("OK"), // defaultbutton
                                null, //alternative button
                                null //other button
                        ));
                    }
                }
                else {
                    break;
                }
            }
        }
    }

    public void deleteButtonClicked(final NSObject sender) {
        NSIndexSet iterator = transferTable.selectedRowIndexes();
        final Collection<Transfer> transfers = transferModel.getSource();
        for(NSUInteger index = iterator.firstIndex(); index.longValue() != NSIndexSet.NSNotFound; index = iterator.indexGreaterThanIndex(index)) {
            if(index.intValue() == -1) {
                break;
            }
            final Transfer transfer = transfers.get(index.intValue());
            if(!transfer.isRunning() && !transfer.isQueued()) {
                TransferCollection.instance().remove(transfer);
            }
        }
        TransferCollection.instance().save();
        this.reloadData();
    }

    public void clearButtonClicked(final NSObject sender) {
        final Collection<Transfer> transfers = transferModel.getSource();
        for(Transfer transfer : transfers) {
            if(!transfer.isRunning() && !transfer.isQueued() && transfer.isComplete()) {
                TransferCollection.instance().remove(transfer);
            }
        }
        TransferCollection.instance().save();
        this.reloadData();
    }

    public void trashButtonClicked(final NSObject sender) {
        NSIndexSet iterator = transferTable.selectedRowIndexes();
        final Collection<Transfer> transfers = transferModel.getSource();
        for(NSUInteger index = iterator.firstIndex(); index.longValue() != NSIndexSet.NSNotFound; index = iterator.indexGreaterThanIndex(index)) {
            if(index.intValue() == -1) {
                break;
            }
            final Transfer transfer = transfers.get(index.intValue());
            if(!transfer.isRunning() && !transfer.isQueued()) {
                for(Path path : transfer.getRoots()) {
                    path.getLocal().delete();
                }
            }
        }
        this.updateIcon();
    }

    /**
     * NSToolbar.Delegate
     *
     * @param toolbar
     */
    public NSArray toolbarDefaultItemIdentifiers(NSToolbar toolbar) {
        return NSArray.arrayWithObjects(
                TOOLBAR_RESUME,
                TOOLBAR_RELOAD,
                TOOLBAR_STOP,
                TOOLBAR_REMOVE,
                NSToolbarItem.NSToolbarFlexibleItemIdentifier,
                TOOLBAR_OPEN,
                TOOLBAR_SHOW,
                TOOLBAR_FILTER
        );
    }

    /**
     * NSToolbar.Delegate
     *
     * @param toolbar
     */
    public NSArray toolbarAllowedItemIdentifiers(NSToolbar toolbar) {
        return NSArray.arrayWithObjects(
                TOOLBAR_RESUME,
                TOOLBAR_RELOAD,
                TOOLBAR_STOP,
                TOOLBAR_REMOVE,
                TOOLBAR_CLEAN_UP,
                TOOLBAR_SHOW,
                TOOLBAR_OPEN,
                TOOLBAR_TRASH,
                TOOLBAR_FILTER,
                NSToolbarItem.NSToolbarCustomizeToolbarItemIdentifier,
                NSToolbarItem.NSToolbarSpaceItemIdentifier,
                NSToolbarItem.NSToolbarSeparatorItemIdentifier,
                NSToolbarItem.NSToolbarFlexibleSpaceItemIdentifier
        );
    }

    public NSArray toolbarSelectableItemIdentifiers(NSToolbar toolbar) {
        return NSArray.array();
    }

    /**
     * @param item
     */
    public boolean validateMenuItem(NSMenuItem item) {
        final Selector action = item.action();
        if(action.equals(Foundation.selector("paste:"))) {
            final Map<Host, PathPasteboard> boards = PathPasteboard.allPasteboards();
            if(!boards.isEmpty() && boards.size() == 1) {
                for(PathPasteboard pasteboard : boards.values()) {
                    if(pasteboard.size() == 1) {
                        item.setTitle(Locale.localizedString("Paste") + " \""
                                + pasteboard.getFiles().get(0).getName() + "\"");
                    }
                    else {
                        item.setTitle(Locale.localizedString("Paste")
                                + " (" + pasteboard.size() + " " +
                                Locale.localizedString("files") + ")");
                    }
                }
            }
            else {
                item.setTitle(Locale.localizedString("Paste"));
            }
        }
        return this.validateItem(action);
    }

    /**
     * @param item
     */
    public boolean validateToolbarItem(NSToolbarItem item) {
        return this.validateItem(item.action());
    }

    /**
     * Validates menu and toolbar items
     *
     * @param action
     * @return true if the item with the identifier should be selectable
     */
    private boolean validateItem(final Selector action) {
        if(action.equals(Foundation.selector("paste:"))) {
            return !PathPasteboard.allPasteboards().isEmpty();
        }
        if(action.equals(Foundation.selector("stopButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                public boolean validate(Transfer transfer) {
                    return transfer.isRunning() || transfer.isQueued();
                }
            });
        }
        if(action.equals(Foundation.selector("reloadButtonClicked:"))
                || action.equals(Foundation.selector("deleteButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                public boolean validate(Transfer transfer) {
                    return !transfer.isRunning() && !transfer.isQueued();
                }
            });
        }
        if(action.equals(Foundation.selector("resumeButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                public boolean validate(Transfer transfer) {
                    if(transfer.isRunning() || transfer.isQueued()) {
                        return false;
                    }
                    return transfer.isResumable();
                }
            });
        }
        if(action.equals(Foundation.selector("openButtonClicked:"))
                || action.equals(Foundation.selector("trashButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                public boolean validate(Transfer transfer) {
                    if(!transfer.isRunning()) {
                        for(Path i : transfer.getRoots()) {
                            if(i.getLocal().exists()) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
        }
        if(action.equals(Foundation.selector("revealButtonClicked:"))) {
            return this.validate(new TransferToolbarValidator() {
                public boolean validate(Transfer transfer) {
                    for(Path i : transfer.getRoots()) {
                        if(i.getLocal().exists()) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
        if(action.equals(Foundation.selector("clearButtonClicked:"))) {
            return transferTable.numberOfRows().intValue() > 0;
        }
        return true;
    }

    /**
     * Validates the selected items in the transfer window against the toolbar validator
     *
     * @param v The validator to use
     * @return True if one or more of the selected items passes the validation test
     */
    private boolean validate(TransferToolbarValidator v) {
        final NSIndexSet iterator = transferTable.selectedRowIndexes();
        final Collection<Transfer> transfers = transferModel.getSource();
        for(NSUInteger index = iterator.firstIndex(); index.longValue() != NSIndexSet.NSNotFound; index = iterator.indexGreaterThanIndex(index)) {
            if(index.intValue() == -1) {
                break;
            }
            final Transfer transfer = transfers.get(index.intValue());
            if(v.validate(transfer)) {
                return true;
            }
        }
        return false;
    }

    private interface TransferToolbarValidator {
        public boolean validate(Transfer transfer);
    }
}