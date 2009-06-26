package ch.cyberduck.core;

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

import com.apple.cocoa.foundation.NSBundle;

import ch.cyberduck.ui.cocoa.threading.BackgroundException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @version $Id$
 */
public abstract class Session {
    private static Logger log = Logger.getLogger(Session.class);

    /**
     * Encapsulating all the information of the remote host
     */
    protected Host host;

    /**
     *
     */
    protected Path workdir;

    protected Session(Host h) {
        this.host = h;
    }

    /**
     * @return The remote host identification such as the response to the SYST command in FTP
     */
    public String getIdentification() {
        try {
            return this.host.getIp();
        }
        catch(UnknownHostException e) {
            return this.host.getHostname();
        }
    }

    private final String ua = NSBundle.mainBundle().objectForInfoDictionaryKey("CFBundleName") + "/"
            + Preferences.instance().getProperty("version");

    public String getUserAgent() {
        return ua;
    }

    /**
     * Used for the hostname resolution in the background
     */
    private Resolver resolver;

    /**
     * Assert that the connection to the remote host is still alive.
     * Open connection if needed.
     *
     * @throws IOException The connection to the remote host failed.
     */
    public void check() throws IOException {
        try {
            try {
                if(!this.isConnected()) {
                    // If not connected anymore, reconnect the session
                    this.connect();
                }
                else {
                    // The session is still supposed to be connected
                    try {
                        // Send a 'no operation command' to make sure the session is alive
                        this.noop();
                    }
                    catch(IOException e) {
                        // Close the underlying socket first
                        this.interrupt();
                        // Try to reconnect once more
                        this.connect();
                    }
                }
            }
            catch(SocketException e) {
                if(e.getMessage().equals("Software caused connection abort")) {
                    // Do not report as failed if socket opening interrupted
                    log.warn("Supressed socket exception:" + e.getMessage());
                    throw new ConnectionCanceledException();
                }
                if(e.getMessage().equals("Socket closed")) {
                    // Do not report as failed if socket opening interrupted
                    log.warn("Supressed socket exception:" + e.getMessage());
                    throw new ConnectionCanceledException();
                }
                throw e;
            }
            catch(SSLHandshakeException e) {
                log.error("SSL Handshake failed: " + e.getMessage());
                if(e.getCause() instanceof sun.security.validator.ValidatorException) {
                    throw e;
                }
                // Most probably caused by user dismissing ceritifcate. No trusted certificate found.
                throw new ConnectionCanceledException(e.getMessage());
            }
        }
        catch(IOException e) {
            this.interrupt();
            this.error(null, "Connection failed", e);
            throw e;
        }
    }

    /**
     * @return The timeout in milliseconds
     */
    protected int timeout() {
        return (int) Preferences.instance().getDouble("connection.timeout.seconds") * 1000;
    }

    /**
     * @return true if the control channel is either tunneled using TLS or SSH
     */
    public boolean isSecure() {
        if(this.isConnected()) {
            return this.host.getProtocol().isSecure();
        }
        return false;
    }

    /**
     * Opens the TCP connection to the server
     *
     * @throws IOException
     * @throws LoginCanceledException
     */
    protected abstract void connect() throws IOException, ConnectionCanceledException, LoginCanceledException;

    protected LoginController login;

    /**
     * Sets the callback to ask for login credentials
     *
     * @param loginController
     * @see #login
     */
    public void setLoginController(LoginController loginController) {
        this.login = loginController;
    }

    protected void login() throws IOException {
        login.check(host);

        final Credentials credentials = host.getCredentials();
        this.message(MessageFormat.format(NSBundle.localizedString("Authenticating as {0}", "Status", ""),
                credentials.getUsername()));
        this.login(credentials);

        if(!this.isConnected()) {
            throw new ConnectionCanceledException();
        }

        login.success(host);
    }

    /**
     * Send the authentication credentials to the server. The connection must be opened first.
     *
     * @throws IOException
     * @throws LoginCanceledException
     * @see #connect
     */
    protected abstract void login(Credentials credentials) throws IOException;

    /**
     *
     * @return
     */
    public Path mount() {
        try {
            if(StringUtils.isNotBlank(host.getDefaultPath())) {
                return this.mount(host.getDefaultPath());
            }
            return this.mount(null);
        }
        catch(IOException e) {
            this.interrupt();
        }
        return null;
    }

    /**
     * Connect to the remote host and mount the home directory
     *
     * @param directory
     * @return null if we fail, the mounted working directory if we succeed
     */
    protected Path mount(String directory) throws IOException {
        this.message(MessageFormat.format(NSBundle.localizedString("Mounting {0}", "Status", ""),
                host.getHostname()));
        this.check();
        if(!this.isConnected()) {
            return null;
        }
        Path home;
        if(directory != null) {
            if(directory.startsWith(Path.DELIMITER) || directory.equals(this.workdir().getName())) {
                home = PathFactory.createPath(this, directory,
                        directory.equals(Path.DELIMITER) ? Path.VOLUME_TYPE | Path.DIRECTORY_TYPE : Path.DIRECTORY_TYPE);
            }
            else if(directory.startsWith(Path.HOME)) {
                // relative path to the home directory
                home = PathFactory.createPath(this,
                        this.workdir().getAbsolute(), directory.substring(1), Path.DIRECTORY_TYPE);
            }
            else {
                // relative path
                home = PathFactory.createPath(this,
                        this.workdir().getAbsolute(), directory, Path.DIRECTORY_TYPE);
            }
            if(!home.childs().attributes().isReadable()) {
                // the default path does not exist or is not readable due to permission issues
                home = this.workdir();
            }
        }
        else {
            home = this.workdir();
        }
        return home;
    }

    /**
     * Close the connecion to the remote host.
     * The protocol specific implementation has to  be coded in the subclasses.
     *
     * @see Host
     */
    public abstract void close();

    /**
     * @return the host this session connects to
     */
    public Host getHost() {
        return this.host;
    }

    /**
     * @return The custom character encoding specified by the host
     *         of this session or the default encoding if not specified
     * @see Preferences
     * @see Host
     */
    public String getEncoding() {
        if(null == this.host.getEncoding()) {
            return Preferences.instance().getProperty("browser.charset.encoding");
        }
        return this.host.getEncoding();
    }

    /**
     * @return The maximum number of concurrent connections allowed or -1 if no limit is set
     */
    public int getMaxConnections() {
        if(null == host.getMaxConnections()) {
            return Preferences.instance().getInteger("connection.host.max");
        }
        return host.getMaxConnections();
    }

    /**
     * @return The current working directory (pwd) or null if it cannot be retrieved for whatever reason
     * @throws ConnectionCanceledException If the underlying connection has already been closed before
     */
    public abstract Path workdir() throws IOException;

    /**
     * Send a 'no operation' command
     *
     * @throws IOException
     */
    protected abstract void noop() throws IOException;

    /**
     * Interrupt any running operation asynchroneously by closing the underlying socket.
     * Close the underlying socket regardless of its state; will throw a socket exception
     * on the thread owning the socket
     */
    public void interrupt() {
        if(null == this.resolver) {
            return;
        }
        this.resolver.cancel();
    }

    public boolean isSendCommandSupported() {
        return false;
    }

    /**
     * Sends an arbitrary command to the server
     *
     * @param command
     */
    public abstract void sendCommand(String command) throws IOException;

    /**
     * @return False
     */
    public boolean isArchiveSupported() {
        return false;
    }

    /**
     * Create ompressed archive.
     *
     * @param archive
     */
    public void archive(final Archive archive, final List<Path> files) {
        try {
            this.check();

            this.sendCommand(archive.getCompressCommand(files));

            // The directory listing is no more current
            for(Path file : files) {
                file.getParent().invalidate();
            }
        }
        catch(IOException e) {
            this.error(null, "Cannot create archive", e);
        }
    }

    /**
     * @return False
     */
    public boolean isUnarchiveSupported() {
        return false;
    }

    /**
     * Unpack compressed archive
     *
     * @param archive
     */
    public void unarchive(final Archive archive, Path file) {
        try {
            this.check();

            this.sendCommand(archive.getDecompressCommand(file));

            // The directory listing is no more current
            file.getParent().invalidate();
        }
        catch(IOException e) {
            this.error(null, "Cannot expand archive", e);
        }
    }

    /**
     * @return boolean True if the session has not yet been closed.
     */
    public abstract boolean isConnected();

    /**
     * If a connection attempt is currently being made.
     *
     * @return
     */
    public boolean isOpening() {
        return resolver != null;
    }

    private Set<ConnectionListener> listeners
            = Collections.synchronizedSet(new HashSet<ConnectionListener>());

    public void addConnectionListener(ConnectionListener listener) {
        listeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all connection listeners that an attempt is made to open this session
     *
     * @throws ResolveCanceledException      If the name resolution has been canceled by the user
     * @throws java.net.UnknownHostException If the name resolution failed
     * @see ConnectionListener
     */
    protected void fireConnectionWillOpenEvent() throws ResolveCanceledException, UnknownHostException {
        log.debug("connectionWillOpen");
        ConnectionListener[] l = listeners.toArray(new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].connectionWillOpen();
        }
        // Configuring proxy if any
        Proxy.configure(this.host.getHostname());
        this.resolver = new Resolver(this.host.getHostname(true));
        this.message(MessageFormat.format(NSBundle.localizedString("Resolving {0}", "Status", ""),
                host.getHostname()));

        // Try to resolve the hostname first
        this.resolver.resolve();
        // The IP address could successfully be determined
    }

    /**
     * Starts the <code>KeepAliveTask</code> if <code>connection.keepalive</code> is true
     * Notifies all connection listeners that the connection has been opened successfully
     *
     * @see ConnectionListener
     */
    protected void fireConnectionDidOpenEvent() {
        log.debug("connectionDidOpen");
        this.resolver = null;

        ConnectionListener[] l = listeners.toArray(new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].connectionDidOpen();
        }
    }

    /**
     * Notifes all connection listeners that a connection is about to be closed
     *
     * @see ConnectionListener
     */
    protected void fireConnectionWillCloseEvent() {
        log.debug("connectionWillClose");
        this.message(MessageFormat.format(NSBundle.localizedString("Disconnecting {0}", "Status", ""),
                this.getHost().getHostname()));
        ConnectionListener[] l = listeners.toArray(new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].connectionWillClose();
        }
    }

    /**
     * Notifes all connection listeners that a connection has been closed
     *
     * @see ConnectionListener
     */
    protected void fireConnectionDidCloseEvent() {
        log.debug("connectionDidClose");

        this.resolver = null;
        this.workdir = null;

        ConnectionListener[] l = listeners.toArray(new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].connectionDidClose();
        }
    }

    private Set<TranscriptListener> transcriptListeners
            = Collections.synchronizedSet(new HashSet<TranscriptListener>());

    public void addTranscriptListener(TranscriptListener listener) {
        transcriptListeners.add(listener);
    }

    public void removeTranscriptListener(TranscriptListener listener) {
        transcriptListeners.remove(listener);
    }

    /**
     * Log the message to all subscribed transcript listeners
     *
     * @param message
     * @see TranscriptListener
     */
    public void log(boolean request, final String message) {
        log.info(message);
        for(TranscriptListener listener : transcriptListeners) {
            listener.log(request, message);
        }
    }

    private Set<ProgressListener> progressListeners
            = Collections.synchronizedSet(new HashSet<ProgressListener>());

    public void addProgressListener(ProgressListener listener) {
        progressListeners.add(listener);
    }

    public void removeProgressListener(ProgressListener listener) {
        progressListeners.remove(listener);
    }

    /**
     * Notifies all progress listeners
     *
     * @param message The message to be displayed in a status field
     * @see ProgressListener
     */
    public void message(final String message) {
        log.info(message);
        for(ProgressListener listener : progressListeners) {
            listener.message(message);
        }
    }

    private Set<ErrorListener> errorListeners
            = Collections.synchronizedSet(new HashSet<ErrorListener>());

    public void addErrorListener(ErrorListener listener) {
        errorListeners.add(listener);
    }

    public void removeErrorListener(ErrorListener listener) {
        errorListeners.remove(listener);
    }

    /**
     * Notifies all error listeners of this error without sending this error to Growl
     *
     * @param path    The path related to this error
     * @param message The error message to be displayed in the alert sheet
     * @param e       The cause of the error
     */
    public void error(Path path, String message, Throwable e) {
        final BackgroundException failure = new BackgroundException(this, path, message, e);
        this.message(failure.getMessage());
        for(ErrorListener listener : errorListeners) {
            listener.error(failure);
        }
    }

    /**
     * Caching files listings of previously visited directories
     */
    private Cache<Path> cache = new Cache<Path>();

    /**
     * @return The directory listing cache
     */
    public Cache<Path> cache() {
        return this.cache;
    }

    /**
     * @param other
     * @return true if the other session denotes the same hostname and protocol
     */
    public boolean equals(Object other) {
        if(null == other) {
            return false;
        }
        if(other instanceof Session) {
            return this.getHost().getHostname().equals(((Session) other).getHost().getHostname())
                    && this.getHost().getProtocol().equals(((Session) other).getHost().getProtocol());
        }
        return false;
    }

    protected void finalize() throws java.lang.Throwable {
        log.debug("finalize:" + super.toString());
        super.finalize();
    }
}