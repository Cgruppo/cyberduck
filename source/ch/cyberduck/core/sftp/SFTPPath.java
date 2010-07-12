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

import ch.cyberduck.core.*;
import ch.cyberduck.core.i18n.Locale;
import ch.cyberduck.core.io.BandwidthThrottle;
import ch.cyberduck.core.io.IOResumeException;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.sftp.SFTPException;
import ch.ethz.ssh2.sftp.SFTPv3DirectoryEntry;
import ch.ethz.ssh2.sftp.SFTPv3FileAttributes;
import ch.ethz.ssh2.sftp.SFTPv3FileHandle;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.List;

/**
 * @version $Id$
 */
public class SFTPPath extends Path {
    private static Logger log = Logger.getLogger(SFTPPath.class);

    public static class Factory extends PathFactory<SFTPSession> {
        @Override
        protected Path create(SFTPSession session, String path, int type) {
            return new SFTPPath(session, path, type);
        }

        @Override
        protected Path create(SFTPSession session, String parent, String name, int type) {
            return new SFTPPath(session, parent, name, type);
        }

        @Override
        protected Path create(SFTPSession session, Path path, Local file) {
            return new SFTPPath(session, path, file);
        }

        @Override
        protected <T> Path create(SFTPSession session, T dict) {
            return new SFTPPath(session, dict);
        }
    }

    private final SFTPSession session;

    private SFTPPath(SFTPSession s, String parent, String name, int type) {
        super(parent, name, type);
        this.session = s;
    }

    private SFTPPath(SFTPSession s, String path, int type) {
        super(path, type);
        this.session = s;
    }

    private SFTPPath(SFTPSession s, Path parent, Local file) {
        super(parent, file);
        this.session = s;
    }

    private <T> SFTPPath(SFTPSession s, T dict) {
        super(dict);
        this.session = s;
    }

    @Override
    public SFTPSession getSession() {
        return session;
    }

    @Override
    public AttributedList<Path> list() {
        final AttributedList<Path> childs = new AttributedList<Path>();
        try {
            this.getSession().check();
            this.getSession().message(MessageFormat.format(Locale.localizedString("Listing directory {0}", "Status"),
                    this.getName()));

            List<SFTPv3DirectoryEntry> children = this.getSession().sftp().ls(this.getAbsolute());
            for(SFTPv3DirectoryEntry f : children) {
                if(!f.filename.equals(".") && !f.filename.equals("..")) {
                    Path p = new SFTPPath(this.getSession(), this.getAbsolute(),
                            f.filename, f.attributes.isDirectory() ? Path.DIRECTORY_TYPE : Path.FILE_TYPE);
                    p.setParent(this);
                    if(null != f.attributes.uid) {
                        p.attributes().setOwner(f.attributes.uid.toString());
                    }
                    if(null != f.attributes.gid) {
                        p.attributes().setGroup(f.attributes.gid.toString());
                    }
                    if(null != f.attributes.size) {
                        p.attributes().setSize(f.attributes.size);
                    }
                    if(null != f.attributes.mtime) {
                        p.attributes().setModificationDate(Long.parseLong(f.attributes.mtime.toString()) * 1000L);
                    }
                    if(null != f.attributes.atime) {
                        p.attributes().setAccessedDate(Long.parseLong(f.attributes.atime.toString()) * 1000L);
                    }
                    if(f.attributes.isSymlink()) {
                        try {
                            String target = this.getSession().sftp().readLink(p.getAbsolute());
                            if(!target.startsWith("/")) {
                                target = Path.normalize(this.getAbsolute() + Path.DELIMITER + target);
                            }
                            p.setSymlinkTarget(target);
                            SFTPv3FileAttributes attr = this.getSession().sftp().stat(target);
                            if(attr.isDirectory()) {
                                p.attributes().setType(Path.SYMBOLIC_LINK_TYPE | Path.DIRECTORY_TYPE);
                            }
                            else if(attr.isRegularFile()) {
                                p.attributes().setType(Path.SYMBOLIC_LINK_TYPE | Path.FILE_TYPE);
                            }
                        }
                        catch(IOException e) {
                            log.warn("Cannot read symbolic link target of " + p.getAbsolute() + ":" + e.getMessage());
                            p.attributes().setType(Path.SYMBOLIC_LINK_TYPE | Path.FILE_TYPE);
                        }
                    }
                    String perm = f.attributes.getOctalPermissions();
                    if(null != perm) {
                        p.attributes().setPermission(new Permission(Integer.parseInt(perm.substring(perm.length() - 3))));
                    }
                    childs.add(p);
                }
            }
            this.getSession().setWorkdir(this);
        }
        catch(IOException e) {
            childs.attributes().setReadable(false);
            this.error("Listing directory failed", e);
        }
        return childs;
    }

    @Override
    public void mkdir() {
        if(this.attributes().isDirectory()) {
            try {
                this.getSession().check();
                this.getSession().message(MessageFormat.format(Locale.localizedString("Making directory {0}", "Status"),
                        this.getName()));

                this.getSession().sftp().mkdir(this.getAbsolute(),
                        new Permission(Preferences.instance().getInteger("queue.upload.permissions.folder.default")).getOctalNumber());
            }
            catch(IOException e) {
                this.error("Cannot create folder", e);
            }
        }
    }

    @Override
    public void rename(AbstractPath renamed) {
        try {
            this.getSession().check();
            this.getSession().message(MessageFormat.format(Locale.localizedString("Renaming {0} to {1}", "Status"),
                    this.getName(), renamed));

            if(renamed.exists()) {
                renamed.delete();
            }
            this.getSession().sftp().mv(this.getAbsolute(), renamed.getAbsolute());
            this.setPath(renamed.getAbsolute());
        }
        catch(IOException e) {
            if(this.attributes().isFile()) {
                this.error("Cannot rename file", e);
            }
            if(this.attributes().isDirectory()) {
                this.error("Cannot rename folder", e);
            }
        }
    }

    @Override
    public void delete() {
        try {
            this.getSession().check();
            if(this.attributes().isFile() || this.attributes().isSymbolicLink()) {
                this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                        this.getName()));

                this.getSession().sftp().rm(this.getAbsolute());
            }
            else if(this.attributes().isDirectory()) {
                for(AbstractPath child : this.childs()) {
                    if(!this.getSession().isConnected()) {
                        break;
                    }
                    child.delete();
                }
                this.getSession().message(MessageFormat.format(Locale.localizedString("Deleting {0}", "Status"),
                        this.getName()));

                this.getSession().sftp().rmdir(this.getAbsolute());
            }
        }
        catch(IOException e) {
            if(this.attributes().isFile()) {
                this.error("Cannot delete file", e);
            }
            if(this.attributes().isDirectory()) {
                this.error("Cannot delete folder", e);
            }
        }
    }

    @Override
    public void readSize() {
        if(this.attributes().isFile()) {
            try {
                this.getSession().check();
                SFTPv3FileAttributes attr = this.getSession().sftp().stat(this.getAbsolute());
                this.getSession().message(MessageFormat.format(Locale.localizedString("Getting size of {0}", "Status"),
                        this.getName()));

                this.attributes().setSize(attr.size);
            }
            catch(IOException e) {
                this.error("Cannot read file attributes", e);
            }
        }
    }

    @Override
    public void readTimestamp() {
        if(this.attributes().isFile()) {
            SFTPv3FileHandle handle = null;
            try {
                this.getSession().check();
                this.getSession().message(MessageFormat.format(Locale.localizedString("Getting timestamp of {0}", "Status"),
                        this.getName()));

                handle = this.getSession().sftp().openFileRO(this.getAbsolute());
                SFTPv3FileAttributes attr = this.getSession().sftp().fstat(handle);
                this.attributes().setModificationDate(Long.parseLong(attr.mtime.toString()) * 1000L);
                this.getSession().sftp().closeFile(handle);
            }
            catch(IOException e) {
                this.error("Cannot read file attributes", e);
            }
            finally {
                if(handle != null) {
                    try {
                        this.getSession().sftp().closeFile(handle);
                    }
                    catch(IOException e) {
                        ;
                    }
                }
            }
        }
    }

    @Override
    public void readUnixPermission() {
        if(this.attributes().isFile()) {
            SFTPv3FileHandle handle = null;
            try {
                this.getSession().check();
                this.getSession().message(MessageFormat.format(Locale.localizedString("Getting permission of {0}", "Status"),
                        this.getName()));

                handle = this.getSession().sftp().openFileRO(this.getAbsolute());
                SFTPv3FileAttributes attr = this.getSession().sftp().fstat(handle);
                String perm = attr.getOctalPermissions();
                try {
                    this.attributes().setPermission(new Permission(Integer.parseInt(perm.substring(perm.length() - 3))));
                }
                catch(NumberFormatException e) {
                    log.error(e.getMessage());
                }
                this.getSession().sftp().closeFile(handle);
            }
            catch(IOException e) {
                this.error("Cannot read file attributes", e);
            }
            finally {
                if(handle != null) {
                    try {
                        this.getSession().sftp().closeFile(handle);
                    }
                    catch(IOException e) {
                        ;
                    }
                }
            }
        }
    }

    @Override
    public void writeOwner(String owner, boolean recursive) {
        try {
            this.getSession().check();
            this.getSession().message(MessageFormat.format(Locale.localizedString("Changing owner of {0} to {1}", "Status"),
                    this.getName(), owner));

            SFTPv3FileAttributes attr = new SFTPv3FileAttributes();
            attr.uid = new Integer(owner);
            this.getSession().sftp().setstat(this.getAbsolute(), attr);
            if(this.attributes().isDirectory()) {
                if(recursive) {
                    for(AbstractPath child : this.childs()) {
                        if(!this.getSession().isConnected()) {
                            break;
                        }
                        ((Path) child).writeOwner(owner, recursive);
                    }
                }
            }
        }
        catch(IOException e) {
            this.error("Cannot change owner", e);
        }
    }

    @Override
    public void writeGroup(String group, boolean recursive) {
        try {
            this.getSession().check();
            this.getSession().message(MessageFormat.format(Locale.localizedString("Changing group of {0} to {1}", "Status"),
                    this.getName(), group));

            SFTPv3FileAttributes attr = new SFTPv3FileAttributes();
            attr.gid = new Integer(group);
            this.getSession().sftp().setstat(this.getAbsolute(), attr);
            if(this.attributes().isDirectory()) {
                if(recursive) {
                    for(AbstractPath child : this.childs()) {
                        if(!this.getSession().isConnected()) {
                            break;
                        }
                        ((Path) child).writeGroup(group, recursive);
                    }
                }
            }
        }
        catch(IOException e) {
            this.error("Cannot change group", e);
        }
    }

    @Override
    public void writeUnixPermission(Permission perm, boolean recursive) {
        try {
            this.writePermissionsImpl(perm, recursive);
        }
        catch(IOException e) {
            this.error("Cannot change permissions", e);
        }
    }

    private void writePermissionsImpl(Permission perm, boolean recursive) throws IOException {
        this.getSession().check();
        this.getSession().message(MessageFormat.format(Locale.localizedString("Changing permission of {0} to {1}", "Status"),
                this.getName(), perm.getOctalString()));

        SFTPv3FileAttributes attr = new SFTPv3FileAttributes();
        if(recursive && this.attributes().isFile()) {
            // Do not write executable bit for files if not already set when recursively updating directory.
            // See #1787
            Permission modified = new Permission(perm);
            if(!this.attributes().getPermission().getOwnerPermissions()[Permission.EXECUTE]) {
                modified.getOwnerPermissions()[Permission.EXECUTE] = false;
            }
            if(!this.attributes().getPermission().getGroupPermissions()[Permission.EXECUTE]) {
                modified.getGroupPermissions()[Permission.EXECUTE] = false;
            }
            if(!this.attributes().getPermission().getOtherPermissions()[Permission.EXECUTE]) {
                modified.getOtherPermissions()[Permission.EXECUTE] = false;
            }
            attr.permissions = modified.getOctalNumber();
        }
        else {
            attr.permissions = perm.getOctalNumber();
        }
        this.getSession().sftp().setstat(getAbsolute(), attr);
        this.attributes().setPermission(perm);
        if(this.attributes().isDirectory()) {
            if(recursive) {
                for(AbstractPath child : this.childs()) {
                    if(!this.getSession().isConnected()) {
                        break;
                    }
                    ((SFTPPath) child).writePermissionsImpl(perm, recursive);
                }
            }
        }
    }

    @Override
    public void writeTimestamp(long millis) {
        if(this.attributes().isFile()) {
            try {
                this.writeModificationDateImpl(millis);
            }
            catch(IOException e) {
                this.error("Cannot change timestamp", e);
            }
        }
    }

    private void writeModificationDateImpl(long modified) throws IOException {
        if(this.attributes().isFile()) {
            this.getSession().message(MessageFormat.format(Locale.localizedString("Changing timestamp of {0} to {1}", "Status"),
                    this.getName(), modified));
            SFTPv3FileAttributes attrs = new SFTPv3FileAttributes();
            int t = (int) (modified / 1000);
            // We must both set the accessed and modified time
            // See AttribFlags.SSH_FILEXFER_ATTR_V3_ACMODTIME
            attrs.atime = t;
            attrs.mtime = t;
            this.getSession().sftp().setstat(this.getAbsolute(), attrs);
        }
    }

    @Override
    protected void download(BandwidthThrottle throttle, StreamListener listener, final boolean check) {
        InputStream in = null;
        OutputStream out = null;
        try {
            if(check) {
                this.getSession().check();
            }
            if(attributes().isFile()) {
                if(Preferences.instance().getProperty("ssh.transfer").equals(Protocol.SFTP.getIdentifier())) {
                    SFTPv3FileHandle handle = this.getSession().sftp().openFileRO(this.getAbsolute());
                    in = new SFTPInputStream(handle);
                    if(status().isResume()) {
                        log.info("Skipping " + status().getCurrent() + " bytes");
                        final long skipped = in.skip(status().getCurrent());
                        if(skipped < status().getCurrent()) {
                            throw new IOResumeException("Skipped " + skipped + " bytes instead of " + this.status().getCurrent());
                        }
                    }
                }
                if(Preferences.instance().getProperty("ssh.transfer").equals(Protocol.SCP.getIdentifier())) {
                    SCPClient scp = this.getSession().openScp();
                    scp.setCharset(this.getSession().getEncoding());
                    in = scp.get(this.getAbsolute());
                }
                out = this.getLocal().getOutputStream(this.status().isResume());
                this.download(in, out, throttle, listener);
            }
        }
        catch(IOException e) {
            this.error("Download failed", e);
        }
        finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    @Override
    protected void upload(BandwidthThrottle throttle, StreamListener listener, final boolean check) {
        InputStream in = null;
        OutputStream out = null;
        SFTPv3FileHandle handle = null;
        try {
            if(attributes().isFile()) {
                if(check) {
                    this.getSession().check();
                }
                in = this.getLocal().getInputStream();
                if(Preferences.instance().getProperty("ssh.transfer").equals(Protocol.SFTP.getIdentifier())) {
                    if(status().isResume() && this.exists()) {
                        handle = this.getSession().sftp().openFileRWAppend(this.getAbsolute());
                    }
                    else {
                        handle = this.getSession().sftp().createFileTruncate(this.getAbsolute());
                    }
                    // We do set the permissions here as otherwise we might have an empty mask for
                    // interrupted file transfers
                    try {
                        final String octal = this.attributes().getPermission().getOctalString();
                        log.info("Updating permissions:" + octal);
                        SFTPv3FileAttributes attr = new SFTPv3FileAttributes();
                        attr.permissions = this.attributes().getPermission().getOctalNumber();
                        this.getSession().sftp().fsetstat(handle, attr);
                    }
                    catch(SFTPException e) {
                        // We might not be able to change the attributes if we are
                        // not the owner of the file; but then we still want to proceed as we
                        // might have group write privileges
                        log.warn(e.getMessage());
                    }
                    out = new SFTPOutputStream(handle);
                    if(status().isResume()) {
                        long skipped = ((SFTPOutputStream) out).skip(status().getCurrent());
                        log.info("Skipping " + skipped + " bytes");
                        if(skipped < this.status().getCurrent()) {
                            throw new IOResumeException("Skipped " + skipped + " bytes instead of " + this.status().getCurrent());
                        }
                    }
                }
                else if(Preferences.instance().getProperty("ssh.transfer").equals(Protocol.SCP.getIdentifier())) {
                    SCPClient scp = this.getSession().openScp();
                    scp.setCharset(this.getSession().getEncoding());
                    out = scp.put(this.getName(), this.getLocal().attributes().getSize(),
                            this.getParent().getAbsolute(),
                            "0" + this.attributes().getPermission().getOctalString());
                }

                this.upload(out, in, throttle, listener);

                if(Preferences.instance().getBoolean("queue.upload.preserveDate")) {
                    try {
                        this.writeModificationDateImpl(this.getLocal().attributes().getModificationDate());
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
        catch(IOException e) {
            this.error("Upload failed", e);
        }
        finally {
            try {
                if(handle != null) {
                    this.getSession().sftp().closeFile(handle);
                }
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(out);
            }
            catch(IOException e) {
                log.error(e.getMessage());
            }
        }
    }
}