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

import com.apple.cocoa.application.*;
import com.apple.cocoa.foundation.*;

import ch.cyberduck.core.*;
import ch.cyberduck.core.aquaticprime.License;
import ch.cyberduck.core.util.URLSchemeHandlerConfiguration;
import ch.cyberduck.ui.cocoa.growl.Growl;
import ch.cyberduck.ui.cocoa.threading.DefaultMainAction;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @version $Id$
 */
public class CDMainController extends CDController {
    private static Logger log = Logger.getLogger(CDMainController.class);

    // ----------------------------------------------------------
    // Outlets
    // ----------------------------------------------------------

    private NSMenu encodingMenu;

    public void setEncodingMenu(NSMenu encodingMenu) {
        this.encodingMenu = encodingMenu;
        String[] charsets = ((CDMainController) NSApplication.sharedApplication().delegate()).availableCharsets();
        for(int i = 0; i < charsets.length; i++) {
            NSMenuItem item = new NSMenuItem(charsets[i],
                    new NSSelector("encodingMenuClicked", new Class[]{Object.class}),
                    "");
            this.encodingMenu.addItem(item);
        }
    }

    private NSMenu columnMenu;

    public void setColumnMenu(NSMenu columnMenu) {
        this.columnMenu = columnMenu;
        Map<String, String> columns = new HashMap<String, String>();
        columns.put("browser.columnKind", NSBundle.localizedString("Kind", ""));
        columns.put("browser.columnSize", NSBundle.localizedString("Size", ""));
        columns.put("browser.columnModification", NSBundle.localizedString("Modified", ""));
        columns.put("browser.columnOwner", NSBundle.localizedString("Owner", ""));
        columns.put("browser.columnGroup", NSBundle.localizedString("Group", ""));
        columns.put("browser.columnPermissions", NSBundle.localizedString("Permissions", ""));
        Iterator identifiers = columns.keySet().iterator();
        int i = 0;
        for(Iterator iter = columns.values().iterator(); iter.hasNext(); i++) {
            NSMenuItem item = new NSMenuItem((String) iter.next(),
                    new NSSelector("columnMenuClicked", new Class[]{Object.class}),
                    "");
            final String identifier = (String) identifiers.next();
            item.setState(Preferences.instance().getBoolean(identifier) ? NSCell.OnState : NSCell.OffState);
            item.setRepresentedObject(identifier);
            this.columnMenu.insertItemAtIndex(item, i);
        }
    }

    public void columnMenuClicked(final NSMenuItem sender) {
        final String identifier = (String) sender.representedObject();
        final boolean enabled = !Preferences.instance().getBoolean(identifier);
        sender.setState(enabled ? NSCell.OnState : NSCell.OffState);
        Preferences.instance().setProperty(identifier, enabled);
        CDBrowserController.updateBrowserTableColumns();
    }

    public void historyMenuClicked(NSMenuItem sender) {
        NSWorkspace.sharedWorkspace().openFile(HistoryCollection.defaultCollection().getFile().getAbsolute());
    }

    public void bugreportMenuClicked(final Object sender) {
        try {
            NSWorkspace.sharedWorkspace().openURL(
                    new java.net.URL(Preferences.instance().getProperty("website.bug")));
        }
        catch(java.net.MalformedURLException e) {
            log.error(e.getMessage());
        }
    }

    public void helpMenuClicked(final Object sender) {
        try {
            NSWorkspace.sharedWorkspace().openURL(
                    new java.net.URL(Preferences.instance().getProperty("website.help"))
            );
        }
        catch(java.net.MalformedURLException e) {
            log.error(e.getMessage());
        }
    }

    public void faqMenuClicked(final Object sender) {
        NSWorkspace.sharedWorkspace().openFile(
                new File(NSBundle.mainBundle().pathForResource("Cyberduck FAQ", "rtfd")).toString());
    }

    public void licenseMenuClicked(final Object sender) {
        NSWorkspace.sharedWorkspace().openFile(
                new File(NSBundle.mainBundle().pathForResource("License", "txt")).toString());
    }

    public void acknowledgmentsMenuClicked(final Object sender) {
        NSWorkspace.sharedWorkspace().openFile(
                new File(NSBundle.mainBundle().pathForResource("Acknowledgments", "rtf")).toString());
    }

    public void websiteMenuClicked(final Object sender) {
        try {
            NSWorkspace.sharedWorkspace().openURL(new java.net.URL(Preferences.instance().getProperty("website.home")));
        }
        catch(java.net.MalformedURLException e) {
            log.error(e.getMessage());
        }
    }

    public void forumMenuClicked(final Object sender) {
        try {
            NSWorkspace.sharedWorkspace().openURL(new java.net.URL(Preferences.instance().getProperty("website.forum")));
        }
        catch(java.net.MalformedURLException e) {
            log.error(e.getMessage());
        }
    }

    public void donateMenuClicked(final Object sender) {
        try {
            NSWorkspace.sharedWorkspace().openURL(new java.net.URL(Preferences.instance().getProperty("website.donate")));
        }
        catch(java.net.MalformedURLException e) {
            log.error(e.getMessage());
        }
    }

    public void aboutMenuClicked(final Object sender) {
        NSDictionary dict = new NSDictionary(
                new String[]{(String) NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleShortVersionString"), ""},
                new String[]{"ApplicationVersion", "Version"}
        );
        NSApplication.sharedApplication().orderFrontStandardAboutPanelWithOptions(dict);
    }

    public void feedbackMenuClicked(final Object sender) {
        try {
            String versionString = (String) NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleVersion");
            NSWorkspace.sharedWorkspace().openURL(new java.net.URL(Preferences.instance().getProperty("mail.feedback")
                    + "?subject=" + NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleName") + "-" + versionString));
        }
        catch(java.net.MalformedURLException e) {
            log.error(e.getMessage());
        }
    }

    public void preferencesMenuClicked(final Object sender) {
        CDPreferencesController controller = CDPreferencesController.instance();
        controller.window().makeKeyAndOrderFront(null);
    }

    public void newDownloadMenuClicked(final Object sender) {
        this.showTransferQueueClicked(sender);
        CDSheetController c = new CDDownloadController(CDTransferController.instance());
        c.beginSheet();
    }

    public void newBrowserMenuClicked(final Object sender) {
        this.openDefaultBookmark(this.newDocument(true));
    }

    public void showTransferQueueClicked(final Object sender) {
        CDTransferController c = CDTransferController.instance();
        c.window().makeKeyAndOrderFront(null);
    }

    public void showActivityWindowClicked(final Object sender) {
        CDActivityController c = CDActivityController.instance();
        if(c.window().isVisible()) {
            c.window().close();
        }
        else {
            c.window().orderFront(null);
        }
    }

    public void downloadBookmarksFromDotMacClicked(final Object sender) {
        CDDotMacController controller = new CDDotMacController();
        controller.downloadBookmarks();
        controller.invalidate();
    }

    public void uploadBookmarksToDotMacClicked(final Object sender) {
        CDDotMacController c = new CDDotMacController();
        c.uploadBookmarks();
        c.invalidate();
    }

    // ----------------------------------------------------------
    // Application delegate methods
    // ----------------------------------------------------------

    /**
     * @param app
     * @param filename
     * @return
     */
    public boolean applicationOpenFile(NSApplication app, String filename) {
        log.debug("applicationOpenFile:" + filename);
        Local f = new Local(filename);
        if(f.exists()) {
            if("duck".equals(f.getExtension())) {
                try {
                    final Host host = new Host(f);
                    this.newDocument().mount(host);
                    return true;
                }
                catch(IOException e) {
                    return false;
                }
            }
            if("cyberducklicense".equals(f.getExtension())) {
                final License l = new License(f);
                if(l.verify()) {
                    int choice = NSAlertPanel.runInformationalAlert(
                            MessageFormat.format(NSBundle.localizedString("Registered to {0}", "License", ""), l.getValue("Name")),
                            NSBundle.localizedString("Thanks for your support! Your contribution helps to further advance development to make Cyberduck even better.", "License", "")
                                    + "\n\n"
                                    + NSBundle.localizedString("Your license file has been copied to the Application Support folder.", "License", ""),
                            NSBundle.localizedString("Continue", ""), //default
                            null, //other
                            null); //alternate
                    if(choice == CDSheetCallback.DEFAULT_OPTION) {
                        f.copy(new Local(Preferences.instance().getProperty("application.support.path"), f.getName()));
                    }
                }
                else {
                    int choice = NSAlertPanel.runCriticalAlert(
                            NSBundle.localizedString("Not a valid license", "License", ""),
                            NSBundle.localizedString("This license key does not appear to be valid.", "License", ""),
                            NSBundle.localizedString("Continue", ""), //default
                            null, //other
                            null); //alternate
                }
                return true;
            }
            final NSArray windows = NSApplication.sharedApplication().windows();
            int count = windows.count();
            while(0 != count--) {
                NSWindow window = (NSWindow) windows.objectAtIndex(count);
                final CDBrowserController controller = CDBrowserController.controllerForWindow(window);
                if(null != controller) {
                    if(controller.isMounted()) {
                        final Path workdir = controller.workdir();
                        final Session session = controller.getTransferSession();
                        final Transfer q = new UploadTransfer(
                                PathFactory.createPath(session, workdir.getAbsolute(), f)
                        );
                        controller.transfer(q, workdir);
                        break;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sent directly by theApplication to the delegate. The method should attempt to open the file filename,
     * returning true if the file is successfully opened, and false otherwise. By design, a
     * file opened through this method is assumed to be temporary its the application's
     * responsibility to remove the file at the appropriate time.
     *
     * @param app
     * @param filename
     * @return
     */
    public boolean applicationOpenTempFile(NSApplication app, String filename) {
        log.debug("applicationOpenTempFile:" + filename);
        return this.applicationOpenFile(app, filename);
    }

    /**
     * Invoked immediately before opening an untitled file. Return false to prevent
     * the application from opening an untitled file; return true otherwise.
     * Note that applicationOpenUntitledFile is invoked if this method returns true.
     *
     * @param sender
     * @return
     */
    public boolean applicationShouldOpenUntitledFile(NSApplication sender) {
        log.debug("applicationShouldOpenUntitledFile");
        return Preferences.instance().getBoolean("browser.openUntitled");
    }

    /**
     * @return true if the file was successfully opened, false otherwise.
     */
    public boolean applicationOpenUntitledFile(NSApplication app) {
        log.debug("applicationOpenUntitledFile");
        return false;
    }

    /**
     * Mounts the default bookmark if any
     *
     * @param controller
     */
    private void openDefaultBookmark(CDBrowserController controller) {
        String defaultBookmark = Preferences.instance().getProperty("browser.defaultBookmark");
        if(null == defaultBookmark) {
            return; //No default bookmark given
        }
        for(Host bookmark : HostCollection.defaultCollection()) {
            if(bookmark.getNickname().equals(defaultBookmark)) {
                controller.mount(bookmark);
                return;
            }
        }
    }

    /**
     * These events are sent whenever the Finder reactivates an already running application
     * because someone double-clicked it again or used the dock to activate it. By default
     * the Application Kit will handle this event by checking whether there are any visible
     * NSWindows (not NSPanels), and, if there are none, it goes through the standard untitled
     * document creation (the same as it does if theApplication is launched without any document
     * to open). For most document-based applications, an untitled document will be created.
     * The application delegate will also get a chance to respond to the normal untitled document
     * delegations. If you implement this method in your application delegate, it will be called
     * before any of the default behavior happens. If you return true, then NSApplication will
     * go on to do its normal thing. If you return false, then NSApplication will do nothing.
     * So, you can either implement this method, do nothing, and return false if you do not
     * want anything to happen at all (not recommended), or you can implement this method,
     * handle the event yourself in some custom way, and return false.
     *
     * @param app
     * @param visibleWindowsFound
     * @return
     */
    public boolean applicationShouldHandleReopen(NSApplication app, boolean visibleWindowsFound) {
        log.debug("applicationShouldHandleReopen");
        // While an application is open, the Dock icon has a symbol below it.
        // When a user clicks an open application’s icon in the Dock, the application
        // becomes active and all open unminimized windows are brought to the front;
        // minimized document windows remain in the Dock. If there are no unminimized
        // windows when the user clicks the Dock icon, the last minimized window should
        // be expanded and made active. If no documents are open, the application should
        // open a new window. (If your application is not document-based, display the
        // application’s main window.)
        final NSArray browsers = this.orderedBrowsers();
        if(browsers.count() == 0 && this.orderedTransfers().count() == 0) {
            this.openDefaultBookmark(this.newDocument());
        }
        java.util.Enumeration enumerator = browsers.objectEnumerator();
        NSWindow miniaturized = null;
        while(enumerator.hasMoreElements()) {
            CDBrowserController controller = (CDBrowserController) enumerator.nextElement();
            if(!controller.window().isMiniaturized()) {
                return false;
            }
            if(null == miniaturized) {
                miniaturized = controller.window();
            }
        }
        if(null == miniaturized) {
            return false;
        }
        miniaturized.deminiaturize(null);
        return false;
    }

    /**
     * Sent by the default notification center after the application has been launched and initialized but
     * before it has received its first event. aNotification is always an
     * ApplicationDidFinishLaunchingNotification. You can retrieve the NSApplication
     * object in question by sending object to aNotification. The delegate can implement
     * this method to perform further initialization. If the user started up the application
     * by double-clicking a file, the delegate receives the applicationOpenFile message before receiving
     * applicationDidFinishLaunching. (applicationWillFinishLaunching is sent before applicationOpenFile.)
     *
     * @param notification
     */
    public void applicationDidFinishLaunching(NSNotification notification) {
        log.info("Running Java " + System.getProperty("java.version"));
        if(log.isInfoEnabled()) {
            log.info("Available localizations:" + NSBundle.mainBundle().localizations());
        }
        if(Preferences.instance().getBoolean("queue.openByDefault")) {
            this.showTransferQueueClicked(null);
        }
        Rendezvous.instance().addListener(new RendezvousListener() {
            public void serviceResolved(final String identifier, final String hostname) {
                if(Preferences.instance().getBoolean("rendezvous.loopback.supress")) {
                    try {
                        if(InetAddress.getByName(hostname).equals(InetAddress.getLocalHost())) {
                            log.info("Supressed Rendezvous notification for " + hostname);
                            return;
                        }
                    }
                    catch(UnknownHostException e) {
                        ; //Ignore
                    }
                }
                synchronized(Rendezvous.instance()) {
                    CDMainApplication.invoke(new DefaultMainAction() {
                        public void run() {
                            Growl.instance().notifyWithImage("Bonjour", Rendezvous.instance().getDisplayedName(identifier), "rendezvous");
                        }
                    });
                }
            }

            public void serviceLost(String servicename) {
                ;
            }
        });
        if(Preferences.instance().getBoolean("browser.serialize")) {
            if(sessions.size() == 0) {
                // Open empty browser if no saved sessions
                if(Preferences.instance().getBoolean("browser.openUntitled")) {
                    this.openDefaultBookmark(this.newDocument());
                }
            }
            for(Host host : sessions) {
                this.newDocument(true).mount(host);
            }
            sessions.clear();
        }
        else if(Preferences.instance().getBoolean("browser.openUntitled")) {
            this.openDefaultBookmark(this.newDocument());
        }
        if(Preferences.instance().getBoolean("defaulthandler.reminder")) {
            if(!URLSchemeHandlerConfiguration.instance().isDefaultHandlerForURLScheme(
                    new String[]{Protocol.FTP.getScheme(), Protocol.FTP_TLS.getScheme(), Protocol.SFTP.getScheme()})) {
                int choice = NSAlertPanel.runInformationalAlert(
                        NSBundle.localizedString("Set Cyberduck as default application for FTP and SFTP locations?", "Configuration", ""),
                        NSBundle.localizedString("As the default application, Cyberduck will open when you click on FTP or SFTP links in other applications, such as your web browser. You can change this setting in the Preferences later.", "Configuration", ""),
                        NSBundle.localizedString("Change", "Configuration", ""), //default
                        NSBundle.localizedString("Don't Ask Again", "Configuration", ""), //other
                        NSBundle.localizedString("Cancel", "Configuration", "")); //alternate
                if(choice == CDSheetCallback.DEFAULT_OPTION) {
                    URLSchemeHandlerConfiguration.instance().setDefaultHandlerForURLScheme(
                            new String[]{Protocol.FTP.getScheme(), Protocol.FTP_TLS.getScheme(), Protocol.SFTP.getScheme()},
                            NSBundle.mainBundle().infoDictionary().objectForKey("CFBundleIdentifier").toString()
                    );
                }
                if(choice == CDSheetCallback.CANCEL_OPTION) {
                    Preferences.instance().setProperty("defaulthandler.reminder", false);
                }
            }
        }
        // NSWorkspace notifications are posted to a notification center provided by
        // the NSWorkspace object, instead of going through the application’s default
        // notification center as most notifications do. To receive NSWorkspace notifications,
        // your application must register an observer with the NSWorkspace notification center.
        NSWorkspace.sharedWorkspace().notificationCenter().addObserver(this,
                new NSSelector("workspaceWillPowerOff", new Class[]{NSNotification.class}),
                NSWorkspace.WorkspaceWillPowerOffNotification,
                null);
        NSWorkspace.sharedWorkspace().notificationCenter().addObserver(this,
                new NSSelector("workspaceWillLogout", new Class[]{NSNotification.class}),
                NSWorkspace.WorkspaceSessionDidResignActiveNotification,
                null);
        if(Preferences.instance().getBoolean("rendezvous.enable")) {
            Rendezvous.instance().init();
        }
        final License l = License.find();
        if(l.verify()) {
            donationBoxDisplayed = true;
        }
    }

    /**
     * The donation reminder dialog has been displayed already
     */
    private boolean donationBoxDisplayed = false;

    /**
     * Saved browsers
     */
    private Collection<Host> sessions = new HistoryCollection(
            new Local(Preferences.instance().getProperty("application.support.path"), "Sessions"));

    /**
     * Invoked from within the terminate method immediately before the
     * application terminates. sender is the NSApplication to be terminated.
     * If this method returns false, the application is not terminated,
     * and control returns to the main event loop.
     *
     * @param app
     * @return Return true to allow the application to terminate.
     */
    public int applicationShouldTerminate(NSApplication app) {
        log.debug("applicationShouldTerminate");
        if(!donationBoxDisplayed) {
            Object lastreminder = Preferences.instance().getObject("donate.reminder");
            if(null == lastreminder
                    || !NSBundle.mainBundle().infoDictionary().objectForKey("Version").toString().equals(lastreminder)) {
                // The donation dialog has not been displayed yet and the users has never choosen
                // not to show it again in the future
                final int uses = Preferences.instance().getInteger("uses");
                CDWindowController c = new CDWindowController() {
                    protected String getBundleName() {
                        return "Donate";
                    }

                    private NSButton neverShowDonationCheckbox;

                    public void setNeverShowDonationCheckbox(NSButton neverShowDonationCheckbox) {
                        this.neverShowDonationCheckbox = neverShowDonationCheckbox;
                        this.neverShowDonationCheckbox.setTarget(this);
                        this.neverShowDonationCheckbox.setState(NSCell.OffState);
                    }

                    public void awakeFromNib() {
                        this.window().setTitle(this.window().title() + " (" + uses + ")");
                        this.window().center();
                        this.window().makeKeyAndOrderFront(null);
                    }

                    public void closeDonationSheet(final NSButton sender) {
                        this.window().close();
                        boolean never = neverShowDonationCheckbox.state() == NSCell.OnState;
                        if(never) {
                            Preferences.instance().setProperty("donate.reminder",
                                    NSBundle.mainBundle().infoDictionary().objectForKey("Version").toString());
                        }
                        if(sender.tag() == CDSheetCallback.DEFAULT_OPTION) {
                            try {
                                NSWorkspace.sharedWorkspace().openURL(
                                        new java.net.URL(Preferences.instance().getProperty("website.donate")));
                            }
                            catch(java.net.MalformedURLException e) {
                                log.error(e.getMessage());
                            }
                        }
                        NSApplication.sharedApplication().terminate(null);
                    }
                };
                c.loadBundle();
                donationBoxDisplayed = true;
                // Cancel application termination. Dismissing the donation dialog will attempt to quit again.
                return NSApplication.TerminateCancel;
            }
        }
        NSArray windows = app.windows();
        int count = windows.count();
        // Determine if there are any open connections
        while(0 != count--) {
            NSWindow window = (NSWindow) windows.objectAtIndex(count);
            final CDBrowserController controller = CDBrowserController.controllerForWindow(window);
            if(null != controller) {
                if(Preferences.instance().getBoolean("browser.serialize")) {
                    if(controller.isMounted()) {
                        // The workspace should be saved. Serialize all open browser sessions
                        final Host serialized = new Host(controller.getSession().getHost().getAsDictionary());
                        try {
                            serialized.setDefaultPath(controller.getSession().workdir().getAbsolute());
                        }
                        catch(IOException e) {
                            log.warn(e.getMessage());
                        }
                        sessions.add(serialized);
                    }
                }
                if(controller.isConnected()) {
                    if(Preferences.instance().getBoolean("browser.confirmDisconnect")) {
                        int choice = NSAlertPanel.runAlert(NSBundle.localizedString("Quit", ""),
                                NSBundle.localizedString("You are connected to at least one remote site. Do you want to review open browsers?", ""),
                                NSBundle.localizedString("Quit Anyway", ""), //default
                                NSBundle.localizedString("Cancel", ""), //other
                                NSBundle.localizedString("Review...", "")); //alternate
                        if(choice == CDSheetCallback.ALTERNATE_OPTION) {
                            // Review if at least one window reqested to terminate later, we shall wait
                            final int result = CDBrowserController.applicationShouldTerminate(app);
                            if(NSApplication.TerminateNow == result) {
                                return CDTransferController.applicationShouldTerminate(app);
                            }
                            return result;
                        }
                        if(choice == CDSheetCallback.CANCEL_OPTION) {
                            // Cancel. Quit has been interrupted. Delete any saved sessions so far.
                            sessions.clear();
                            return NSApplication.TerminateCancel;
                        }
                        if(choice == CDSheetCallback.DEFAULT_OPTION) {
                            // Quit
                            return CDTransferController.applicationShouldTerminate(app);
                        }
                    }
                    else {
                        controller.unmount();
                    }
                }
            }
        }
        return CDTransferController.applicationShouldTerminate(app);
    }

    /**
     * Quits the Rendezvous daemon and saves all preferences
     *
     * @param notification
     */
    public void applicationWillTerminate(NSNotification notification) {
        log.debug("applicationWillTerminate");
        NSNotificationCenter.defaultCenter().removeObserver(this);
        //Terminating rendezvous discovery
        Rendezvous.instance().quit();
        //Writing usage info
        Preferences.instance().setProperty("uses", Preferences.instance().getInteger("uses") + 1);
        Preferences.instance().save();
    }

    /**
     * Posted when the user has requested a logout or that the machine be powered off.
     *
     * @param notification
     */
    public void workspaceWillPowerOff(NSNotification notification) {
        log.debug("workspaceWillPowerOff");
        donationBoxDisplayed = true;
    }

    /**
     * Posted before a user session is switched out. This allows an application to
     * disable some processing when its user session is switched out, and reenable when that
     * session gets switched back in, for example.
     *
     * @param notification
     */
    public void workspaceWillLogout(NSNotification notification) {
        log.debug("workspaceWillLogout");
        donationBoxDisplayed = true;
    }

    /**
     * Makes a unmounted browser window the key window and brings it to the front
     *
     * @return A reference to a browser window
     */
    public CDBrowserController newDocument() {
        return this.newDocument(false);
    }

    /**
     * Makes a unmounted browser window the key window and brings it to the front
     *
     * @param force If true, open a new browser regardeless of any unused browser window
     * @return A reference to a browser window
     */
    public CDBrowserController newDocument(boolean force) {
        log.debug("newDocument");
        final NSArray browsers = this.orderedBrowsers();
        if(!force) {
            java.util.Enumeration enumerator = browsers.objectEnumerator();
            while(enumerator.hasMoreElements()) {
                CDBrowserController controller = (CDBrowserController) enumerator.nextElement();
                if(!controller.hasSession()) {
                    controller.window().makeKeyAndOrderFront(null);
                    return controller;
                }
            }
        }
        CDBrowserController controller = new CDBrowserController();
        if(browsers.count() > 0) {
            controller.cascade();
        }
        controller.window().makeKeyAndOrderFront(null);
        return controller;
    }

    // ----------------------------------------------------------
    // Applescriptability
    // ----------------------------------------------------------

    public boolean applicationDelegateHandlesKey(NSApplication application, String key) {
        return key.equals("orderedBrowsers") || key.equals("orderedTransfers");
    }

    public NSArray orderedTransfers() {
        NSApplication app = NSApplication.sharedApplication();
        NSArray orderedWindows = (NSArray) NSKeyValue.valueForKey(app, "orderedWindows");
        int c = orderedWindows.count();
        NSMutableArray orderedDocs = new NSMutableArray();
        for(int i = 0; i < c; i++) {
            if(((NSWindow) orderedWindows.objectAtIndex(i)).isVisible()) {
                Object delegate = ((NSWindow) orderedWindows.objectAtIndex(i)).delegate();
                if((delegate != null) && (delegate instanceof CDTransferController)) {
                    orderedDocs.addObject(delegate);
                    return orderedDocs;
                }
            }
        }
        log.debug("orderedTransfers:" + orderedDocs);
        return orderedDocs;
    }

    public NSArray orderedBrowsers() {
        NSApplication app = NSApplication.sharedApplication();
        NSArray orderedWindows = (NSArray) NSKeyValue.valueForKey(app, "orderedWindows");
        int c = orderedWindows.count();
        NSMutableArray orderedDocs = new NSMutableArray();
        for(int i = 0; i < c; i++) {
            Object delegate = ((NSWindow) orderedWindows.objectAtIndex(i)).delegate();
            if((delegate != null) && (delegate instanceof CDBrowserController)) {
                orderedDocs.addObject(delegate);
            }
        }
        return orderedDocs;
    }

    /**
     * We are not a Windows application. Long live the application wide menu bar.
     *
     * @param app
     * @return
     */
    public boolean applicationShouldTerminateAfterLastWindowClosed(NSApplication app) {
        return false;
    }

    /**
     * @return The available character sets available on this platform
     */
    protected String[] availableCharsets() {
        List<String> charsets = new Collection<String>();
        for(Charset charset : Charset.availableCharsets().values()) {
            final String name = charset.displayName();
            if(!(name.startsWith("IBM") || name.startsWith("x-"))) {
                charsets.add(name);
            }
        }
        return charsets.toArray(new String[charsets.size()]);
    }
}