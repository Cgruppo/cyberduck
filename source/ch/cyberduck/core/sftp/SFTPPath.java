package ch.cyberduck.core.sftp;

/*
 *  Copyright (c) 2007 David Kocher. All rights reserved.
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

import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.io.SFTPInputStream;
import ch.ethz.ssh2.io.SFTPOutputStream;
import ch.ethz.ssh2.sftp.SFTPException;
import ch.ethz.ssh2.sftp.SFTPv3DirectoryEntry;
import ch.ethz.ssh2.sftp.SFTPv3FileAttributes;
import ch.ethz.ssh2.sftp.SFTPv3FileHandle;

import ch.cyberduck.core.*;
import ch.cyberduck.core.io.BandwidthThrottle;

import com.apple.cocoa.foundation.NSBundle;
import com.apple.cocoa.foundation.NSDictionary;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;

/**
 * @version $Id$
 */
public class SFTPPath extends Path {
    private static Logger log = Logger.getLogger(SFTPPath.class);

    static {
        PathFactory.addFactory(Session.SFTP, new Factory());
    }

    private static class Factory extends PathFactory {
        protected Path create(Session session) {
            return new SFTPPath((SFTPSession) session);
        }

        protected Path create(Session session, String path) {
            return new SFTPPath((SFTPSession) session, path);
        }

        protected Path create(Session session, String parent, String name) {
            return new SFTPPath((SFTPSession) session, parent, name);
        }

        protected Path create(Session session, String path, Local file) {
            return new SFTPPath((SFTPSession) session, path, file);
        }

        protected Path create(Session session, NSDictionary dict) {
            return new SFTPPath((SFTPSession) session, dict);
        }
    }

    private final SFTPSession session;

    private SFTPPath(SFTPSession s) {
        this.session = s;
    }

    private SFTPPath(SFTPSession s, String parent, String name) {
        super(parent, name);
        this.session = s;
    }

    private SFTPPath(SFTPSession s, String path) {
        super(path);
        this.session = s;
    }

    private SFTPPath(SFTPSession s, String parent, Local file) {
        super(parent, file);
        this.session = s;
    }

    private SFTPPath(SFTPSession s, NSDictionary dict) {
        super(dict);
        this.session = s;
    }

    public Session getSession() {
        return this.session;
    }

    public AttributedList list(final ListParseListener listener) {
        synchronized(session) {
            AttributedList childs = new AttributedList() {
                public boolean add(Object object) {
                    boolean result = super.add(object);
                    listener.parsed(this);
                    return result;
                }
            };
            try {
                session.check();
                session.message(NSBundle.localizedString("Listing directory", "Status", "") + " " + this.getAbsolute());
                List children = session.sftp().ls(this.getAbsolute());
                Iterator i = children.iterator();
                while(i.hasNext()) {
                    SFTPv3DirectoryEntry f = (SFTPv3DirectoryEntry) i.next();
                    if(!f.filename.equals(".") && !f.filename.equals("..")) {
                        Path p = PathFactory.createPath(session, this.getAbsolute(), f.filename);
                        p.setParent(this);
                        p.attributes.setOwner(f.attributes.uid.toString());
                        p.attributes.setGroup(f.attributes.gid.toString());
                        p.attributes.setSize(f.attributes.size.doubleValue());
                        p.attributes.setModificationDate(Long.parseLong(f.attributes.mtime.toString()) * 1000L);
                        p.attributes.setAccessedDate(Long.parseLong(f.attributes.atime.toString()) * 1000L);
                        if(f.attributes.isSymlink()) {
                            try {
                                String target = session.sftp().readLink(p.getAbsolute());
                                if(!target.startsWith("/")) {
                                    target = Path.normalize(this.getAbsolute() + Path.DELIMITER + target);
                                }
                                p.setSymbolicLinkPath(target);
                                SFTPv3FileAttributes attr = session.sftp().stat(target);
                                if(attr.isDirectory()) {
                                    p.attributes.setType(Path.SYMBOLIC_LINK_TYPE | Path.DIRECTORY_TYPE);
                                }
                                else if(attr.isRegularFile()) {
                                    p.attributes.setType(Path.SYMBOLIC_LINK_TYPE | Path.FILE_TYPE);
                                }
                            }
                            catch(IOException e) {
                                log.warn("Cannot read symbolic link target of "+p.getAbsolute()+":"+e.getMessage());
                                p.attributes.setType(Path.SYMBOLIC_LINK_TYPE | Path.FILE_TYPE);
                            }
                        }
                        else if(f.attributes.isDirectory()) {
                            p.attributes.setType(Path.DIRECTORY_TYPE);
                        }
                        else if(f.attributes.isRegularFile()) {
                            p.attributes.setType(Path.FILE_TYPE);
                        }
                        String perm = f.attributes.getOctalPermissions();
                        p.attributes.setPermission(new Permission(Integer.parseInt(perm.substring(perm.length()-3))));
                        p.status.setSkipped(this.status.isSkipped());
                        childs.add(p);
                    }
                }
            }
            catch(SFTPException e) {
                childs.attributes().setReadable(false);
                this.error("Listing directory failed", e);
            }
            catch(IOException e) {
                this.error("Connection failed", e);
                session.interrupt();
            }
            finally {
                session.fireActivityStoppedEvent();
            }
            return childs;
        }
    }

    public void cwdir() throws IOException {
        // We don't need this as we always work with absolute paths
    }

    public void mkdir(boolean recursive) {
        synchronized(session) {
            log.debug("mkdir:" + this.getName());
            try {
                if(recursive) {
                    if(!this.getParent().exists()) {
                        this.getParent().mkdir(recursive);
                    }
                }
                session.check();
                session.message(NSBundle.localizedString("Make directory", "Status", "") + " " + this.getName());
                Permission perm = new Permission(Preferences.instance().getInteger("queue.upload.permissions.folder.default"));
                session.sftp().mkdir(this.getAbsolute(), new Integer(perm.getOctalNumber()).intValue());
                this.cache().put(this, new AttributedList());
                this.getParent().invalidate();
            }
            catch(SFTPException e) {
                this.error("Cannot create folder", e);
            }
            catch(IOException e) {
                this.error("Connection failed", e);
                session.interrupt();
            }
            finally {
                session.fireActivityStoppedEvent();
            }
        }
    }

    public void rename(String filename) {
        synchronized(session) {
            try {
                session.check();
                session.message(NSBundle.localizedString("Renaming to", "Status", "") + " " + filename + " (" + this.getName() + ")");
                session.sftp().mv(this.getAbsolute(), filename);
                this.getParent().invalidate();
                this.setPath(filename);
                this.getParent().invalidate();
            }
            catch(SFTPException e) {
                if(this.attributes.isFile()) {
                    this.error("Cannot rename file", e);
                }
                if(this.attributes.isDirectory()) {
                    this.error("Cannot rename folder", e);
                }
            }
            catch(IOException e) {
                this.error("Connection failed", e);
                session.interrupt();
            }
            finally {
                session.fireActivityStoppedEvent();
            }
        }
    }

    public void delete() {
        synchronized(session) {
            log.debug("delete:" + this.toString());
            try {
                session.check();
                if(this.attributes.isFile() || this.attributes.isSymbolicLink()) {
                    session.message(NSBundle.localizedString("Deleting", "Status", "") + " " + this.getName());
                    session.sftp().rm(this.getAbsolute());
                }
                else if(this.attributes.isDirectory()) {
                    for(Iterator iter = this.childs().iterator(); iter.hasNext();) {
                        if(!session.isConnected()) {
                            break;
                        }
                        ((AbstractPath) iter.next()).delete();
                    }
                    session.message(NSBundle.localizedString("Deleting", "Status", "") + " " + this.getName());
                    session.sftp().rmdir(this.getAbsolute());
                }
                this.getParent().invalidate();
            }
            catch(SFTPException e) {
                if(this.attributes.isFile()) {
                    this.error("Cannot delete file", e);
                }
                if(this.attributes.isDirectory()) {
                    this.error("Cannot delete folder", e);
                }
            }
            catch(IOException e) {
                this.error("Connection failed", e);
                session.interrupt();
            }
            finally {
                session.fireActivityStoppedEvent();
            }
        }
    }

    public void readSize() {
        synchronized(session) {
            if(this.attributes.isFile()) {
                SFTPv3FileHandle handle = null;
                try {
                    session.check();
                    handle = session.sftp().openFileRO(this.getAbsolute());
                    SFTPv3FileAttributes attr = session.sftp().fstat(handle);
                    session.message(NSBundle.localizedString("Getting size of", "Status", "") + " " + this.getName());
                    this.attributes.setSize(attr.size.doubleValue());
                    session.sftp().closeFile(handle);
                }
                catch(SFTPException e) {
                    this.error("Cannot read file attributes", e);
                }
                catch(IOException e) {
                    this.error("Connection failed", e);
                    session.interrupt();
                }
                finally {
                    if(handle != null) {
                        try {
                            session.sftp().closeFile(handle);
                        }
                        catch(IOException e) {
                            ;
                        }
                    }
                    session.fireActivityStoppedEvent();
                }
            }
        }
    }

    public void readTimestamp() {
        synchronized(session) {
            if(this.attributes.isFile()) {
                SFTPv3FileHandle handle = null;
                try {
                    session.check();
                    session.message(NSBundle.localizedString("Getting timestamp of", "Status", "") + " " + this.getName());
                    handle = session.sftp().openFileRO(this.getAbsolute());
                    SFTPv3FileAttributes attr = session.sftp().fstat(handle);
                    this.attributes.setModificationDate(Long.parseLong(attr.mtime.toString()) * 1000L);
                    session.sftp().closeFile(handle);
                }
                catch(SFTPException e) {
                    this.error("Cannot read file attributes", e);
                }
                catch(IOException e) {
                    this.error("Connection failed", e);
                    session.interrupt();
                }
                finally {
                    if(handle != null) {
                        try {
                            session.sftp().closeFile(handle);
                        }
                        catch(IOException e) {
                            ;
                        }
                    }
                    session.fireActivityStoppedEvent();
                }
            }
        }
    }

    public void readPermission() {
        synchronized(session) {
            if(this.attributes.isFile()) {
                SFTPv3FileHandle handle = null;
                try {
                    session.check();
                    session.message(NSBundle.localizedString("Getting permission of", "Status", "") + " " + this.getName());
                    handle = session.sftp().openFileRO(this.getAbsolute());
                    SFTPv3FileAttributes attr = session.sftp().fstat(handle);
                    String perm = attr.getOctalPermissions();
                    try {
                        this.attributes.setPermission(new Permission(Integer.parseInt(perm.substring(perm.length()-3))));
                    }
                    catch(NumberFormatException e) {
                        this.attributes.setPermission(Permission.EMPTY);
                    }
                    session.sftp().closeFile(handle);
                }
                catch(SFTPException e) {
                    this.error("Cannot read file attributes", e);
                }
                catch(IOException e) {
                    this.error("Connection failed", e);
                    session.interrupt();
                }
                finally {
                    if(handle != null) {
                        try {
                            session.sftp().closeFile(handle);
                        }
                        catch(IOException e) {
                            ;
                        }
                    }
                    session.fireActivityStoppedEvent();
                }
            }
        }
    }

    public void writeOwner(String owner, boolean recursive) {
        synchronized(session) {
            log.debug("changeOwner");
            try {
                session.check();
                session.message(NSBundle.localizedString("Changing owner to", "Status", "")
                        + " " + owner + " (" + this.getName() + ")");
                SFTPv3FileAttributes attr = new SFTPv3FileAttributes();
                attr.uid = new Integer(owner);
                session.sftp().setstat(this.getAbsolute(), attr);
                if(this.attributes.isDirectory()) {
                    if(recursive) {
                        for(Iterator iter = this.childs().iterator(); iter.hasNext();) {
                            if(!session.isConnected()) {
                                break;
                            }
                            ((Path) iter.next()).writeOwner(owner, recursive);
                        }
                    }
                }
                this.getParent().invalidate();
            }
            catch(NumberFormatException e) {
                this.error("Cannot change owner", e);
            }
            catch(SFTPException e) {
                this.error("Cannot change owner", e);
            }
            catch(IOException e) {
                this.error("Connection failed", e);
                session.interrupt();
            }
            finally {
                session.fireActivityStoppedEvent();
            }
        }
    }

    public void writeGroup(String group, boolean recursive) {
        synchronized(session) {
            log.debug("changeGroup");
            try {
                session.check();
                session.message(NSBundle.localizedString("Changing group to", "Status", "")
                        + " " + group + " (" + this.getName() + ")");
                SFTPv3FileAttributes attr = new SFTPv3FileAttributes();
                attr.gid = new Integer(group);
                session.sftp().setstat(this.getAbsolute(), attr);
                if(this.attributes.isDirectory()) {
                    if(recursive) {
                        for(Iterator iter = this.childs().iterator(); iter.hasNext();) {
                            if(!session.isConnected()) {
                                break;
                            }
                            ((Path) iter.next()).writeGroup(group, recursive);
                        }
                    }
                }
                this.getParent().invalidate();
            }
            catch(NumberFormatException e) {
                this.error("Cannot change group", e);
            }
            catch(SFTPException e) {
                this.error("Cannot change group", e);
            }
            catch(IOException e) {
                this.error("Connection failed", e);
                session.interrupt();
            }
            finally {
                session.fireActivityStoppedEvent();
            }
        }
    }

    public void writePermissions(Permission perm, boolean recursive) {
        synchronized(session) {
            log.debug("changePermissions");
            try {
                session.check();
                session.message(NSBundle.localizedString("Changing permission to", "Status", "")
                        + " " + perm.getOctalString() + " (" + this.getName() + ")");
                SFTPv3FileAttributes attr = new SFTPv3FileAttributes();
                attr.permissions = new Integer(perm.getOctalNumber());
                session.sftp().setstat(this.getAbsolute(), attr);
                if(this.attributes.isDirectory()) {
                    if(recursive) {
                        for(Iterator iter = this.childs().iterator(); iter.hasNext();) {
                            if(!session.isConnected()) {
                                break;
                            }
                            ((AbstractPath) iter.next()).writePermissions(perm, recursive);
                        }
                    }
                }
                this.getParent().invalidate();
            }
            catch(SFTPException e) {
                this.error("Cannot change permissions", e);
            }
            catch(IOException e) {
                this.error("Connection failed", e);
                session.interrupt();
            }
            finally {
                session.fireActivityStoppedEvent();
            }
        }
    }

    public void writeModificationDate(long millis) {
        ;
    }

    public void download(BandwidthThrottle throttle, StreamListener listener) {
        synchronized(session) {
            log.debug("download:" + this.toString());
            InputStream in = null;
            OutputStream out = null;
            try {
                status.reset();
                if(this.attributes.isDirectory()) {
                    this.getLocal().mkdir(true);
                    status.setComplete(true);
                }
                if(this.attributes.isFile()) {
                    session.check(
                            Preferences.instance().getProperty("ssh.transfer").equals(Session.SFTP)
                    );
                    out = new Local.OutputStream(this.getLocal(), status.isResume());
                    this.getLocal().touch();
                    if(Preferences.instance().getProperty("ssh.transfer").equals(Session.SFTP)) {
                        SFTPv3FileHandle handle = session.sftp().openFileRO(this.getAbsolute());
                        in = new SFTPInputStream(handle);
                        if(status.isResume()) {
                            status.setCurrent((long) this.getLocal().attributes.getSize());
                            long skipped = in.skip(status.getCurrent());
                            log.info("Skipping " + skipped + " bytes");
                            if(skipped < this.status.getCurrent()) {
                                throw new IOResumeException("Skipped " + skipped + " bytes instead of " + this.status.getCurrent());
                            }
                        }
                    }
                    if(Preferences.instance().getProperty("ssh.transfer").equals(Session.SCP)) {
                        SCPClient scp = session.openScp();
                        scp.setCharset(session.getEncoding());
                        in = scp.get(this.getAbsolute());
                    }
                    this.download(in, out, throttle, listener);
                }
                if(Preferences.instance().getBoolean("queue.download.changePermissions")) {
                    log.info("Updating permissions");
                    Permission perm = null;
                    if(Preferences.instance().getBoolean("queue.download.permissions.useDefault")
                            && this.attributes.isFile()) {
                        perm = new Permission(
                                Preferences.instance().getInteger("queue.download.permissions.file.default")
                        );
                    }
                    else {
                        perm = this.attributes.getPermission();
                    }
                    if(null != perm) {
                        if(this.attributes.isDirectory()) {
                            perm.getOwnerPermissions()[Permission.WRITE] = true;
                            perm.getOwnerPermissions()[Permission.EXECUTE] = true;
                        }
                        this.getLocal().writePermissions(perm, false);
                    }
                }
                if(Preferences.instance().getBoolean("queue.download.preserveDate")) {
                    log.info("Updating timestamp");
                    if(-1 == this.attributes.getModificationDate()) {
                        this.readTimestamp();
                    }
                    if(this.attributes.getModificationDate() != -1) {
                        long timestamp = this.attributes.getModificationDate();
                        this.getLocal().writeModificationDate(timestamp/*, this.getHost().getTimezone()*/);
                    }
                }
            }
            catch(SFTPException e) {
                this.error("Download failed", e);
            }
            catch(IOException e) {
                this.error("Connection failed", e);
                session.interrupt();
            }
            finally {
                try {
                    if(in != null) {
                        in.close();
                        in = null;
                    }
                    if(out != null) {
                        out.close();
                        out = null;
                    }
                }
                catch(IOException e) {
                    log.error(e.getMessage());
                }
                finally {
                    session.fireActivityStoppedEvent();
                }
            }
        }
    }

    public void upload(BandwidthThrottle throttle, StreamListener listener) {
        synchronized(session) {
            log.debug("upload:" + this.toString());
            InputStream in = null;
            OutputStream out = null;
            SFTPv3FileHandle handle = null;
            try {
                status.reset();
                if(this.attributes.isDirectory()) {
                    this.mkdir();
                    status.setComplete(true);
                }
                if(attributes.isFile()) {
                    session.check(
                            Preferences.instance().getProperty("ssh.transfer").equals(Session.SFTP)
                    );
                    in = new Local.InputStream(this.getLocal());
                    if(Preferences.instance().getProperty("ssh.transfer").equals(Session.SFTP)) {
                        if(status.isResume() && this.exists()) {
                            handle = session.sftp().openFileRWAppend(this.getAbsolute());
                        }
                        else {
                            handle = session.sftp().createFileTruncate(this.getAbsolute());
                        }
                    }
                    // We do set the permissions here as otherwise we might have an empty mask for
                    // interrupted file transfers
                    Permission p = attributes.getPermission();
                    if(Preferences.instance().getBoolean("queue.upload.changePermissions")) {
                        if(null == p) {
                            if(Preferences.instance().getBoolean("queue.upload.permissions.useDefault")) {
                                if(this.attributes.isFile()) {
                                    p = new Permission(
                                            Preferences.instance().getInteger("queue.upload.permissions.file.default"));
                                }
                                if(this.attributes.isDirectory()) {
                                    p = new Permission(
                                            Preferences.instance().getInteger("queue.upload.permissions.folder.default"));
                                }
                            }
                            else {
                                p = this.getLocal().attributes.getPermission();
                            }
                        }
                        if(null != p) {
                            if(Preferences.instance().getProperty("ssh.transfer").equals(Session.SFTP)) {
                                try {
                                    log.info("Updating permissions:"+p.getOctalString());
                                    SFTPv3FileAttributes attr = new SFTPv3FileAttributes();
                                    attr.permissions = new Integer(p.getOctalNumber());
                                    session.sftp().fsetstat(handle, attr);
                                }
                                catch(SFTPException e) {
                                    // We might not be able to change the attributes if we are
                                    // not the owner of the file; but then we still want to proceed as we
                                    // might have group write privileges
                                    log.warn(e.getMessage());
                                }
                            }
                        }
                    }
                    if(Preferences.instance().getProperty("ssh.transfer").equals(Session.SFTP)) {
                        if(status.isResume()) {
                            status.setCurrent(
                                    session.sftp().stat(this.getAbsolute()).size.intValue());
                        }
                    }
                    if(Preferences.instance().getProperty("ssh.transfer").equals(Session.SFTP)) {
                        out = new SFTPOutputStream(handle);
                        if(status.isResume()) {
                            long skipped = ((SFTPOutputStream)out).skip(status.getCurrent());
                            log.info("Skipping " + skipped + " bytes");
                            if(skipped < this.status.getCurrent()) {
                                throw new IOResumeException("Skipped " + skipped + " bytes instead of " + this.status.getCurrent());
                            }
                        }
                    }
                    if(Preferences.instance().getProperty("ssh.transfer").equals(Session.SCP)) {
                        SCPClient scp = session.openScp();
                        scp.setCharset(session.getEncoding());
                        out = scp.put(this.getName(), (long)this.getLocal().attributes.getSize(),
                                this.getParent().getAbsolute(),
                                "0"+p.getOctalString());
                    }
                    this.upload(out, in, throttle, listener);
                }
                if(Preferences.instance().getProperty("ssh.transfer").equals(Session.SFTP)) {
                    if(Preferences.instance().getBoolean("queue.upload.preserveDate")) {
                        if(this.attributes.isFile()) {
                            log.info("Updating timestamp");
                            SFTPv3FileAttributes attrs = new SFTPv3FileAttributes();
                            int t = (int) (this.getLocal().attributes.getModificationDate() / 1000);
                            // We must both set the accessed and modified time
                            // See AttribFlags.SSH_FILEXFER_ATTR_V3_ACMODTIME
                            attrs.atime = new Integer(t);
                            attrs.mtime = new Integer(t);
                            try {
                                if(null == handle) {
                                    if(this.attributes.isFile()) {
                                        handle = session.sftp().openFileRW(this.getAbsolute());
                                    }
//                            if(this.attributes.isDirectory()) {
//                                handle = session.sftp().openDirectory(this.getAbsolute());
//                            }
                                }
                                session.sftp().fsetstat(handle, attrs);
                            }
                            catch(SFTPException e) {
                                // We might not be able to change the attributes if we are
                                // not the owner of the file; but then we still want to proceed as we
                                // might have group write privileges
                                log.warn(e.getMessage());
                            }
                        }
                    }
                }
                this.getParent().invalidate();
            }
            catch(SFTPException e) {
                this.error("Upload failed", e);
            }
            catch(IOException e) {
                this.error("Connection failed", e);
                session.interrupt();
            }
            finally {
                try {
                    if(handle != null) {
                        session.sftp().closeFile(handle);
                    }
                    if(in != null) {
                        in.close();
                        in = null;
                    }
                    if(out != null) {
                        out.close();
                        out = null;
                    }
                }
                catch(IOException e) {
                    log.error(e.getMessage());
                }
                finally {
                    session.fireActivityStoppedEvent();
                }
            }
        }
    }
}
