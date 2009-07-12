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

import ch.cyberduck.core.Collection;
import ch.cyberduck.core.*;
import ch.cyberduck.core.aquaticprime.License;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.util.URLSchemeHandlerConfiguration;
import ch.cyberduck.ui.cocoa.application.*;
import ch.cyberduck.ui.cocoa.delegate.*;
import ch.cyberduck.ui.cocoa.foundation.*;
import ch.cyberduck.ui.cocoa.growl.Growl;
import ch.cyberduck.ui.cocoa.threading.DefaultMainAction;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.rococoa.Foundation;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;

/**
 * Setting the main menu and implements application delegate methods
 *
 * @version $Id$
 */
public class CDMainController extends CDBundleController {
    private static Logger log = Logger.getLogger(CDMainController.class);

    public CDMainController() {
        this.loadBundle();
    }

    @Outlet
    private NSMenu encodingMenu;

    public void setEncodingMenu(NSMenu encodingMenu) {
        this.encodingMenu = encodingMenu;
        for(String charset : availableCharsets()) {
            NSMenuItem item = NSMenuItem.itemWithTitle(charset, Foundation.selector("encodingMenuClicked:"), "");
            this.encodingMenu.addItem(item);
        }
    }

    @Outlet
    private NSMenu columnMenu;

    public void setColumnMenu(NSMenu columnMenu) {
        this.columnMenu = columnMenu;
        Map<String, String> columns = new HashMap<String, String>();
        columns.put("browser.columnKind", Locale.localizedString("Kind", ""));
        columns.put("browser.columnSize", Locale.localizedString("Size", ""));
        columns.put("browser.columnModification", Locale.localizedString("Modified", ""));
        columns.put("browser.columnOwner", Locale.localizedString("Owner", ""));
        columns.put("browser.columnGroup", Locale.localizedString("Group", ""));
        columns.put("browser.columnPermissions", Locale.localizedString("Permissions", ""));
        Iterator identifiers = columns.keySet().iterator();
        int i = 0;
        for(Iterator iter = columns.values().iterator(); iter.hasNext(); i++) {
            NSMenuItem item = NSMenuItem.itemWithTitle((String) iter.next(),
                    Foundation.selector("columnMenuClicked:"),
                    "");
            final String identifier = (String) identifiers.next();
            item.setState(Preferences.instance().getBoolean(identifier) ? NSCell.NSOnState : NSCell.NSOffState);
            item.setRepresentedObject(identifier);
            this.columnMenu.insertItem_atIndex(item, i);
        }
    }

    public void columnMenuClicked(final NSMenuItem sender) {
        final String identifier = sender.representedObject();
        final boolean enabled = !Preferences.instance().getBoolean(identifier);
        sender.setState(enabled ? NSCell.NSOnState : NSCell.NSOffState);
        Preferences.instance().setProperty(identifier, enabled);
        CDBrowserController.updateBrowserTableColumns();
    }

    @Outlet
    private NSMenu editMenu;
    private EditMenuDelegate editMenuDelegate;

    public void setEditMenu(NSMenu editMenu) {
        this.editMenu = editMenu;
        this.editMenuDelegate = new EditMenuDelegate();
        this.editMenu.setDelegate(editMenuDelegate.id());
    }

    @Outlet
    private NSMenu archiveMenu;
    private ArchiveMenuDelegate archiveMenuDelegate;

    public void setArchiveMenu(NSMenu archiveMenu) {
        this.archiveMenu = archiveMenu;
        this.archiveMenuDelegate = new ArchiveMenuDelegate();
        this.archiveMenu.setDelegate(archiveMenuDelegate.id());
    }

    @Outlet
    private NSMenu bookmarkMenu;
    private BookmarkMenuDelegate bookmarkMenuDelegate;

    public void setBookmarkMenu(NSMenu bookmarkMenu) {
        this.bookmarkMenu = bookmarkMenu;
        this.bookmarkMenuDelegate = new BookmarkMenuDelegate();
        this.bookmarkMenu.setDelegate(bookmarkMenuDelegate.id());
    }

    @Outlet
    private NSMenu historyMenu;
    private HistoryMenuDelegate historyMenuDelegate;

    public void setHistoryMenu(NSMenu historyMenu) {
        this.historyMenu = historyMenu;
        this.historyMenuDelegate = new HistoryMenuDelegate();
        this.historyMenu.setDelegate(historyMenuDelegate.id());
    }

    @Outlet
    private NSMenu rendezvousMenu;
    private RendezvousMenuDelegate rendezvousMenuDelegate;

    public void setRendezvousMenu(NSMenu rendezvousMenu) {
        this.rendezvousMenu = rendezvousMenu;
        this.rendezvousMenuDelegate = new RendezvousMenuDelegate();
        this.rendezvousMenu.setDelegate(rendezvousMenuDelegate.id());
    }

    public void historyMenuClicked(NSMenuItem sender) {
        NSWorkspace.sharedWorkspace().openFile(HistoryCollection.defaultCollection().getFile().getAbsolute());
    }

    public void bugreportMenuClicked(final NSObject sender) {
        NSWorkspace.sharedWorkspace().openURL(
                NSURL.URLWithString(Preferences.instance().getProperty("website.bug")));
    }

    public void helpMenuClicked(final NSObject sender) {
        NSWorkspace.sharedWorkspace().openURL(
                NSURL.URLWithString(Preferences.instance().getProperty("website.help"))
        );
    }

    public void faqMenuClicked(final NSObject sender) {
        NSWorkspace.sharedWorkspace().openFile(
                new File(NSBundle.mainBundle().pathForResource_ofType("Cyberduck FAQ", "rtfd")).toString());
    }

    public void licenseMenuClicked(final NSObject sender) {
        NSWorkspace.sharedWorkspace().openFile(
                new File(NSBundle.mainBundle().pathForResource_ofType("License", "txt")).toString());
    }

    public void acknowledgmentsMenuClicked(final NSObject sender) {
        NSWorkspace.sharedWorkspace().openFile(
                new File(NSBundle.mainBundle().pathForResource_ofType("Acknowledgments", "rtf")).toString());
    }

    public void websiteMenuClicked(final NSObject sender) {
        NSWorkspace.sharedWorkspace().openURL(NSURL.URLWithString(Preferences.instance().getProperty("website.home")));
    }

    public void forumMenuClicked(final NSObject sender) {
        NSWorkspace.sharedWorkspace().openURL(NSURL.URLWithString(Preferences.instance().getProperty("website.forum")));
    }

    public void donateMenuClicked(final NSObject sender) {
        NSWorkspace.sharedWorkspace().openURL(NSURL.URLWithString(Preferences.instance().getProperty("website.donate")));
    }

    public void aboutMenuClicked(final NSObject sender) {
        NSDictionary dict = NSDictionary.dictionaryWithObjectsForKeys(
                NSArray.arrayWithObjects(NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleShortVersionString").toString(), ""),
                NSArray.arrayWithObjects("ApplicationVersion", "Version")
        );
        NSApplication.sharedApplication().orderFrontStandardAboutPanelWithOptions(dict);
    }

    public void feedbackMenuClicked(final NSObject sender) {
        String versionString = NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleVersion").toString();
        NSWorkspace.sharedWorkspace().openURL(NSURL.URLWithString(Preferences.instance().getProperty("mail.feedback")
                + "?subject=" + NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleName") + "-" + versionString));
    }

    public void preferencesMenuClicked(final NSObject sender) {
        CDPreferencesController controller = CDPreferencesController.instance();
        controller.window().makeKeyAndOrderFront(null);
    }

    public void newDownloadMenuClicked(final NSObject sender) {
        this.showTransferQueueClicked(sender);
        CDSheetController c = new CDDownloadController(CDTransferController.instance());
        c.beginSheet();
    }

    public void newBrowserMenuClicked(final NSObject sender) {
        this.openDefaultBookmark(CDMainController.newDocument(true));
    }

    public void showTransferQueueClicked(final NSObject sender) {
        CDTransferController c = CDTransferController.instance();
        c.window().makeKeyAndOrderFront(null);
    }

    public void showActivityWindowClicked(final NSObject sender) {
        CDActivityController c = CDActivityController.instance();
        if(c.isVisible()) {
            c.window().close();
        }
        else {
            c.window().orderFront(null);
        }
    }

    public void downloadBookmarksFromDotMacClicked(final NSObject sender) {
        CDDotMacController controller = new CDDotMacController();
        controller.downloadBookmarks();
        controller.invalidate();
    }

    public void uploadBookmarksToDotMacClicked(final NSObject sender) {
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
    public boolean application_openFile(NSApplication app, String filename) {
        log.debug("applicationOpenFile:" + filename);
        Local f = new Local(filename);
        if(f.exists()) {
            if("duck".equals(f.getExtension())) {
                try {
                    final Host host = new Host(f);
                    CDMainController.newDocument().mount(host);
                    return true;
                }
                catch(IOException e) {
                    return false;
                }
            }
            if("cyberducklicense".equals(f.getExtension())) {
                final License l = new License(f);
                if(l.verify()) {
                    String to = l.getValue("Name");
                    if(StringUtils.isBlank(to)) {
                        to = l.getValue("Email"); // primary key
                    }
                    final NSAlert alert = NSAlert.alert(
                            MessageFormat.format(Locale.localizedString("Registered to {0}", "License"), to),
                            Locale.localizedString("Thanks for your support! Your contribution helps to further advance development to make Cyberduck even better.", "License")
                                    + "\n\n"
                                    + Locale.localizedString("Your donation key has been copied to the Application Support folder.", "License"),
                            Locale.localizedString("Continue", ""), //default
                            null, //other
                            null);
                    alert.setAlertStyle(NSAlert.NSInformationalAlertStyle);
                    if(alert.runModal() == CDSheetCallback.DEFAULT_OPTION) {
                        f.copy(new Local(Preferences.instance().getProperty("application.support.path"), f.getName()));
                    }
                }
                else {
                    final NSAlert alert = NSAlert.alert(
                            Locale.localizedString("Not a valid donation key", "License"),
                            Locale.localizedString("This donation key does not appear to be valid.", "License"),
                            Locale.localizedString("Continue", ""), //default
                            null, //other
                            null);
                    alert.setAlertStyle(NSAlert.NSWarningAlertStyle);
                    alert.runModal(); //alternate
                }
                return true;
            }
            for(CDBrowserController controller : browsers) {
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
    public boolean application_openTempFile(NSApplication app, String filename) {
        log.debug("applicationOpenTempFile:" + filename);
        return this.application_openFile(app, filename);
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
    public boolean applicationShouldHandleReopen_hasVisibleWindows(NSApplication app, boolean visibleWindowsFound) {
        log.debug("applicationShouldHandleReopen");
        // While an application is open, the Dock icon has a symbol below it.
        // When a user clicks an open application’s icon in the Dock, the application
        // becomes active and all open unminimized windows are brought to the front;
        // minimized document windows remain in the Dock. If there are no unminimized
        // windows when the user clicks the Dock icon, the last minimized window should
        // be expanded and made active. If no documents are open, the application should
        // open a new window. (If your application is not document-based, display the
        // application’s main window.)
        if(browsers.size() == 0 && !CDTransferController.instance().isVisible()) {
            this.openDefaultBookmark(CDMainController.newDocument());
        }
        NSWindow miniaturized = null;
        for(CDBrowserController controller : browsers) {
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
        log.info("Running Java " + System.getProperty("java.version") + " on " + System.getProperty("os.arch"));
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
                    this.openDefaultBookmark(CDMainController.newDocument());
                }
            }
            for(Host host : sessions) {
                CDMainController.newDocument(true).mount(host);
            }
            sessions.clear();
        }
        else if(Preferences.instance().getBoolean("browser.openUntitled")) {
            this.openDefaultBookmark(CDMainController.newDocument());
        }
        if(Preferences.instance().getBoolean("defaulthandler.reminder")) {
            if(!URLSchemeHandlerConfiguration.instance().isDefaultHandlerForURLScheme(
                    new String[]{Protocol.FTP.getScheme(), Protocol.FTP_TLS.getScheme(), Protocol.SFTP.getScheme()})) {
                final NSAlert alert = NSAlert.alert(
                        Locale.localizedString("Set Cyberduck as default application for FTP and SFTP locations?", "Configuration"),
                        Locale.localizedString("As the default application, Cyberduck will open when you click on FTP or SFTP links in other applications, such as your web browser. You can change this setting in the Preferences later.", "Configuration"),
                        Locale.localizedString("Change", "Configuration"), //default
                        Locale.localizedString("Don't Ask Again", "Configuration"), //other
                        Locale.localizedString("Cancel", "Configuration"));
                alert.setAlertStyle(NSAlert.NSInformationalAlertStyle);
                int choice = alert.runModal(); //alternate
                if(choice == CDSheetCallback.DEFAULT_OPTION) {
                    URLSchemeHandlerConfiguration.instance().setDefaultHandlerForURLScheme(
                            new String[]{Protocol.FTP.getScheme(), Protocol.FTP_TLS.getScheme(), Protocol.SFTP.getScheme()},
                            NSBundle.mainBundle().infoDictionary().objectForKey("CFBundleIdentifier").toString()
                    );
                }
                if(choice == CDSheetCallback.ALTERNATE_OPTION) {
                    Preferences.instance().setProperty("defaulthandler.reminder", false);
                }
            }
        }
        // NSWorkspace notifications are posted to a notification center provided by
        // the NSWorkspace object, instead of going through the application’s default
        // notification center as most notifications do. To receive NSWorkspace notifications,
        // your application must register an observer with the NSWorkspace notification center.
        NSWorkspace.sharedWorkspace().notificationCenter().addObserver(this.id(),
                Foundation.selector("workspaceWillPowerOff:"),
                NSWorkspace.WorkspaceWillPowerOffNotification,
                null);
        NSWorkspace.sharedWorkspace().notificationCenter().addObserver(this.id(),
                Foundation.selector("workspaceWillLogout:"),
                NSWorkspace.WorkspaceSessionDidResignActiveNotification,
                null);
        if(Preferences.instance().getBoolean("rendezvous.enable")) {
            Rendezvous.instance().init();
        }
    }

    /**
     * Saved browsers
     */
    private Collection<Host> sessions = new HistoryCollection(
            new Local(Preferences.instance().getProperty("application.support.path"), "Sessions"));

    /**
     * Display donation reminder dialog
     */
    private boolean donationPrompt = true;
    private CDWindowController donationController;


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
        if(donationPrompt) {
            try {
                final License l = License.find();
                if(!l.verify()) {
                    final Calendar lastreminder = Calendar.getInstance();
                    lastreminder.setTimeInMillis(Preferences.instance().getLong("donate.reminder.date"));
                    // Display donationPrompt every n days
                    lastreminder.roll(Calendar.DAY_OF_YEAR, Preferences.instance().getInteger("donate.reminder.interval"));
                    // Display after upgrade
                    final String lastversion = Preferences.instance().getProperty("donate.reminder");
                    if(lastreminder.getTime().before(new Date(System.currentTimeMillis())) ||
                            !NSBundle.mainBundle().infoDictionary().objectForKey("Version").toString().equals(lastversion)) {
                        final int uses = Preferences.instance().getInteger("uses");
                        donationController = new CDWindowController() {
                            @Override
                            protected String getBundleName() {
                                return "Donate";
                            }

                            @Outlet
                            private NSButton neverShowDonationCheckbox;

                            public void setNeverShowDonationCheckbox(NSButton neverShowDonationCheckbox) {
                                this.neverShowDonationCheckbox = neverShowDonationCheckbox;
                                this.neverShowDonationCheckbox.setTarget(this.id());
                                this.neverShowDonationCheckbox.setState(NSCell.NSOffState);
                            }

                            @Override
                            public void awakeFromNib() {
                                this.window().setTitle(this.window().title() + " (" + uses + ")");
                                this.window().center();
                                this.window().makeKeyAndOrderFront(null);

                                super.awakeFromNib();
                            }

                            public void closeDonationSheet(final NSButton sender) {
                                this.window().close();
                                boolean never = neverShowDonationCheckbox.state() == NSCell.NSOnState;
                                if(never) {
                                    Preferences.instance().setProperty("donate.reminder",
                                            NSBundle.mainBundle().infoDictionary().objectForKey("Version").toString());
                                }
                                if(sender.tag() == CDSheetCallback.DEFAULT_OPTION) {
                                    NSWorkspace.sharedWorkspace().openURL(
                                            NSURL.URLWithString(Preferences.instance().getProperty("website.donate")));
                                }
                                // Remeber this reminder date
                                Preferences.instance().setProperty("donate.reminder.date", System.currentTimeMillis());
                                // Quit again
                                NSApplication.sharedApplication().terminate(this.id());
                            }
                        };
                        donationController.loadBundle();
                        // Cancel application termination. Dismissing the donation dialog will attempt to quit again.
                        return NSApplication.NSTerminateCancel;
                    }
                }
            }
            finally {
                // Disable until next launch
                donationPrompt = false;
            }
        }
        // Determine if there are any open connections
        for(CDBrowserController controller : CDMainController.browsers) {
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
                    final NSAlert alert = NSAlert.alert(Locale.localizedString("Quit", ""),
                            Locale.localizedString("You are connected to at least one remote site. Do you want to review open browsers?", ""),
                            Locale.localizedString("Quit Anyway", ""), //default
                            Locale.localizedString("Cancel", ""), //other
                            Locale.localizedString("Review...", ""));
                    alert.setAlertStyle(NSAlert.NSWarningAlertStyle);
                    int choice = alert.runModal(); //alternate
                    if(choice == CDSheetCallback.OTHER_OPTION) {
                        // Review if at least one window reqested to terminate later, we shall wait
                        final int result = CDBrowserController.applicationShouldTerminate(app);
                        if(NSApplication.NSTerminateNow == result) {
                            return CDTransferController.applicationShouldTerminate(app);
                        }
                        return result;
                    }
                    if(choice == CDSheetCallback.ALTERNATE_OPTION) {
                        // Cancel. Quit has been interrupted. Delete any saved sessions so far.
                        sessions.clear();
                        return NSApplication.NSTerminateCancel;
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
        return CDTransferController.applicationShouldTerminate(app);
    }

    /**
     * Quits the Rendezvous daemon and saves all preferences
     *
     * @param notification
     */
    public void applicationWillTerminate(NSNotification notification) {
        log.debug("applicationWillTerminate");

        this.invalidate();

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
    }

    /**
     * Makes a unmounted browser window the key window and brings it to the front
     *
     * @return A reference to a browser window
     */
    public static CDBrowserController newDocument() {
        return CDMainController.newDocument(false);
    }

    /**
     *
     */
    private static List<CDBrowserController> browsers
            = new ArrayList<CDBrowserController>();

    /**
     * @return
     */
    public static List<CDBrowserController> getBrowsers() {
        return browsers;
    }


    /**
     * Makes a unmounted browser window the key window and brings it to the front
     *
     * @param force If true, open a new browser regardeless of any unused browser window
     * @return A reference to a browser window
     */
    public static CDBrowserController newDocument(boolean force) {
        log.debug("newDocument");
        if(!force) {
            for(CDBrowserController controller : browsers) {
                if(!controller.hasSession()) {
                    controller.window().makeKeyAndOrderFront(null);
                    return controller;
                }
            }
        }
        final CDBrowserController controller = new CDBrowserController();
        controller.addListener(new CDWindowListener() {
            public void windowWillClose() {
                browsers.remove(controller);
            }
        });
        if(browsers.size() > 0) {
            controller.cascade();
        }
        controller.window().makeKeyAndOrderFront(null);
        browsers.add(controller);
        return controller;
    }

    /**
     * Sent by Cocoa’s built-in scripting support during execution of get or set script commands to
     * find out if the delegate can handle operations on the specified key-value key.
     *
     * @param application
     * @param key
     * @return
     */
    @Applescript
    public boolean application_delegateHandlesKey(NSApplication application, String key) {
        return key.equals("orderedBrowsers");
    }

    @Applescript
    public NSArray orderedBrowsers() {
        NSMutableArray orderedDocs = NSMutableArray.arrayWithCapacity(browsers.size());
        for(CDBrowserController browser : browsers) {
            orderedDocs.addObject(browser.proxy());
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
    public static String[] availableCharsets() {
        List<String> charsets = new Collection<String>();
        for(Charset charset : Charset.availableCharsets().values()) {
            final String name = charset.displayName();
            if(!(name.startsWith("IBM") || name.startsWith("x-"))) {
                charsets.add(name);
            }
        }
        return charsets.toArray(new String[charsets.size()]);
    }

    @Override
    protected String getBundleName() {
        return "Main";
    }
}