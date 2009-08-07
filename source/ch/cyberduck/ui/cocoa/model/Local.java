package ch.cyberduck.ui.cocoa.model;

/*
 *  Copyright (c) 2009 David Kocher. All rights reserved.
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
import ch.cyberduck.core.threading.DefaultMainAction;
import ch.cyberduck.ui.cocoa.CDController;
import ch.cyberduck.ui.cocoa.application.NSWorkspace;
import ch.cyberduck.ui.cocoa.foundation.*;

import org.apache.log4j.Logger;
import org.rococoa.Rococoa;

/**
 * @version $Id:$
 */
public class Local extends ch.cyberduck.core.Local {
    private static Logger log = Logger.getLogger(Local.class);

    public Local(ch.cyberduck.core.Local parent, String name) {
        super(parent, name);
    }

    public Local(String parent, String name) {
        super(parent, name);
    }

    public Local(String path) {
        super(path);
    }

    public Local(java.io.File path) {
        super(path);
    }

    @Override
    public void setPath(String name) {
        super.setPath(stringByExpandingTildeInPath(name));
    }

    @Override
    protected void init() {
        if(!loadNative()) {
            return;
        }
//        FileForker forker = new MacOSXForker();
//        forker.usePathname(new Pathname(_impl.getAbsoluteFile()));
//        if(forker.isAlias()) {
//            try {
//                this.setPath(forker.makeResolved().getPath());
//            }
//            catch(IOException e) {
//                log.error("Error resolving alias:" + e.getMessage());
//            }
//        }
        super.init();
    }

    /**
     * @return Path relative to the home directory denoted with a tilde.
     */
    @Override
    public String getAbbreviatedPath() {
        return stringByAbbreviatingWithTildeInPath(this.getAbsolute());
    }

    private static CDController c = new CDController() {
    };

    private static boolean JNI_LOADED = false;

    private static boolean loadNative() {
        if(!JNI_LOADED) {
            JNI_LOADED = Native.load("Local");
        }
        return JNI_LOADED;
    }

    @Override
    protected native String kind(String extension);

    @Override
    public Permission getPermission() {
        try {
            NSDictionary fileAttributes = NSFileManager.defaultManager().fileAttributes(
                    _impl.getAbsolutePath());
            if(null == fileAttributes) {
                log.error("No such file:" + getAbsolute());
                return null;
            }
            NSObject object = fileAttributes.objectForKey(NSFileManager.NSFilePosixPermissions);
            if(null == object) {
                log.error("No such file:" + getAbsolute());
                return null;
            }
            NSNumber posix = Rococoa.cast(object, NSNumber.class);
            String posixString = Integer.toString(posix.intValue() & 0177777, 8);
            return new Permission(Integer.parseInt(posixString.substring(posixString.length() - 3)));
        }
        catch(NumberFormatException e) {
            return Permission.EMPTY;
        }
    }

    @Override
    public void writePermissions(final Permission perm, final boolean recursive) {
        c.invoke(new DefaultMainAction() {
            public void run() {
                boolean success = NSFileManager.defaultManager().changeFileAttributes(
                        NSDictionary.dictionaryWithObjectsForKeys(
                                NSArray.arrayWithObject(NSNumber.numberWithInt(perm.getOctalNumber())),
                                NSArray.arrayWithObject(NSFileManager.NSFilePosixPermissions)),
                        _impl.getAbsolutePath());
                if(!success) {
                    log.error("File attribute changed failed:" + getAbsolute());
                }
                if(attributes.isDirectory() && recursive) {
                    for(AbstractPath child : childs()) {
                        child.writePermissions(perm, recursive);
                    }
                }
            }
        });
    }

    @Override
    public void writeModificationDate(final long millis) {
        c.invoke(new DefaultMainAction() {
            public void run() {
                boolean success = NSFileManager.defaultManager().changeFileAttributes(
                        NSDictionary.dictionaryWithObjectsForKeys(
                                NSArray.arrayWithObject(NSDate.dateWithTimeIntervalSince1970(millis / 1000)),
                                NSArray.arrayWithObject(NSFileManager.NSFileModificationDate)),
                        _impl.getAbsolutePath());
                if(!success) {
                    log.error("File attribute changed failed:" + getAbsolute());
                }
            }
        });
    }

    @Override
    public long getCreationDate() {
        final NSDictionary fileAttributes = NSFileManager.defaultManager().fileAttributes(_impl.getAbsolutePath());
        // If flag is true and path is a symbolic link, the attributes of the linked-to file are returned;
        // if the link points to a nonexistent file, this method returns null. If flag is false,
        // the attributes of the symbolic link are returned.
        if(null == fileAttributes) {
            log.error("No such file:" + getAbsolute());
            return -1;
        }
        NSObject date = fileAttributes.objectForKey(NSFileManager.NSFileCreationDate);
        if(null == date) {
            // Returns an entry’s value given its key, or null if no value is associated with key.
            log.error("No such file:" + getAbsolute());
            return -1;
        }
        return (long) (Rococoa.cast(date, NSDate.class).timeIntervalSince1970() * 1000);
    }

    /**
     * NSWorkspace.RecycleOperation
     */
    @Override
    public void trash() {
        if(this.exists()) {
            final ch.cyberduck.core.Local file = this;
            c.invoke(new DefaultMainAction() {
                public void run() {
                    log.debug("Move " + file + " to Trash");
                    if(!NSWorkspace.sharedWorkspace().performFileOperation(
                            NSWorkspace.RecycleOperation,
                            file.getParent().getAbsolute(), "",
                            NSArray.arrayWithObject(file.getName()))) {
                        log.warn("Failed to move " + file.getAbsolute() + " to Trash");
                    }
                }
            });
        }
    }

    /**
     * @param originUrl The URL of the resource originally hosting the quarantined item, from the user's point of
     *                  view. For web downloads, this property is the URL of the web page on which the user initiated
     *                  the download. For attachments, this property is the URL of the resource to which the quarantined
     *                  item was attached (e.g. the email message, calendar event, etc.). The origin URL may be a file URL
     *                  for local resources, or a custom URL to which the quarantining application will respond when asked
     *                  to open it. The quarantining application should respond by displaying the resource to the user.
     *                  Note: The origin URL should not be set to the data URL, or the quarantining application may start
     *                  downloading the file again if the user choses to view the origin URL while resolving a quarantine
     *                  warning.
     * @param dataUrl   The URL from which the data for the quarantined item data was
     *                  actaully streamed or downloaded, if available
     */
    @Override
    public void setQuarantine(final String originUrl, final String dataUrl) {
        if(!loadNative()) {
            return;
        }
        this.setQuarantine(this.getAbsolute(), originUrl, dataUrl);
    }

    /**
     * UKXattrMetadataStore
     *
     * @param path
     * @param originUrl
     * @param dataUrl
     */
    private native void setQuarantine(String path, String originUrl, String dataUrl);

    /**
     * Set the kMDItemWhereFroms on the file.
     *
     * @param dataUrl
     */
    @Override
    public void setWhereFrom(final String dataUrl) {
        if(!loadNative()) {
            return;
        }
        this.setWhereFrom(this.getAbsolute(), dataUrl);
    }

    /**
     * Set the kMDItemWhereFroms on the file.
     *
     * @param path
     * @param dataUrl
     */
    private native void setWhereFrom(String path, String dataUrl);

    /**
     * Update the custom icon for the file in the Finder
     *
     * @param progress An integer from -1 and 9. If -1 is passed,
     *                 the resource fork with the custom icon is removed from the file.
     */
    @Override
    public void setIcon(final int progress) {
        if(progress > 9 || progress < -1) {
            log.warn("Local#setIcon:" + progress);
            return;
        }
        if(Preferences.instance().getBoolean("queue.download.updateIcon")) {
            if(!loadNative()) {
                return;
            }
            final String path = this.getAbsolute();
            c.invoke(new DefaultMainAction() {
                public void run() {
                    if(-1 == progress) {
                        removeResourceFork();
                    }
                    else {
                        setIconFromFile(path, "download" + progress + ".icns");
                    }
                }
            });
        }
        // Disabled because of #221
        // NSWorkspace.sharedWorkspace().noteFileSystemChanged(this.getAbsolute());
    }

    /**
     * Removes the resource fork from the file alltogether
     */
    private void removeResourceFork() {
        this.removeCustomIcon();
//        try {
//            FileForker forker = new MacOSXForker();
//            forker.usePathname(new Pathname(_impl.getAbsoluteFile()));
//            forker.makeForkOutputStream(true, false).close();
//        }
//        catch(IOException e) {
//            log.error("Failed to remove resource fork from file:" + e.getMessage());
//        }
    }

    /**
     * @param icon the absolute path to the image file to use as an icon
     */
    private native void setIconFromFile(String path, String icon);

    private void removeCustomIcon() {
        this.removeCustomIcon(this.getAbsolute());
    }

    private native void removeCustomIcon(String path);

    private static String stringByAbbreviatingWithTildeInPath(String string) {
        return NSString.stringByAbbreviatingWithTildeInPath(string);
    }

    private static String stringByExpandingTildeInPath(String string) {
        return NSString.stringByExpandingTildeInPath(string);
    }

    @Override
    protected native String applicationForExtension(String extension);

    @Override
    public String toString() {
        return this.toURL();
    }

    @Override
    public String toURL() {
        return Local.stringByAbbreviatingWithTildeInPath(this.getAbsolute());
    }
}