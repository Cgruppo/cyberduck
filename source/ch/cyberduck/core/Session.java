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

import ch.cyberduck.ui.LoginController;
import ch.cyberduck.ui.cocoa.growl.Growl;
import ch.cyberduck.ui.cocoa.threading.BackgroundException;

import com.apple.cocoa.foundation.NSBundle;
import com.apple.cocoa.foundation.NSObject;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

/**
 * @version $Id$
 */
public abstract class Session extends NSObject {

    private static Logger log = Logger.getLogger(Session.class);

    public static final String SFTP = "sftp";
    public static final String FTP = "ftp";
    public static final String FTP_TLS = "ftps";

    public static final String FTP_STRING = NSBundle.localizedString("FTP (File Transfer Protocol)", "");
    public static final String FTP_TLS_STRING = NSBundle.localizedString("FTP-SSL (FTP over TLS/SSL)", "");
    public static final String SFTP_STRING = NSBundle.localizedString("SFTP (SSH Secure File Transfer)", "");

    private String identification;

    /**
     * Default port for ftp
     */
    public static final int FTP_PORT = 21;

    /**
     * Default port for ssh
     */
    public static final int SSH_PORT = 22;

    /**
     * Encapsulating all the information of the remote host
     */
    protected Host host = null;

    public Object clone() {
        return SessionFactory.createSession((Host) this.host.clone());
    }

    protected Session(Host h) {
        this.host = h;
    }

    /**
     * @return The remote host identification such as the response to the SYST command in FTP
     */
    public String getIdentification() {
        return this.identification;
    }

    /**
     * @param id
     */
    public void setIdentification(String id) {
        this.identification = id;
    }

    /**
     *
     */
    private Resolver resolver;

    /**
     * Assert that the connection to the remote host is still alive. Open connection if needed.
     *
     * @throws IOException The connection to the remote host failed.
     */
    public void check() throws IOException {
        this.fireActivityStartedEvent();
        try {
            if(!this.isConnected()) {
                // if not connected anymore, reconnect the session
                this.connect();
            }
            else {
                try {
                    // Send a 'no operation command' to make sure the session is alive
                    this.noop();
                }
                catch(IOException e) {
                    // The session has timed out since from the server side
                    // Close the underlying socket first
                    this.interrupt();
                    if(e instanceof SocketException) {
                        if(e.getMessage().equals("Connection reset")) {
                            this.connect();
                            return;
                        }
                        // Do not try to reconnect, because this exception is
                        // thrown when the socket is interrupted by the user asynchroneously
                        throw e;
                    }
                    // Try to reconnect once more
                    this.connect();
                }
            }
        }
        catch(SocketException e) {
            this.interrupt();
            if(e.getMessage().equals("Connection reset")) {
                this.connect();
                return;
            }
            // Do not try to reconnect, because this exception is
            // thrown when the socket is interrupted by the user asynchroneously
            throw e;
        }
        catch(SocketTimeoutException e) {
            // Signals that a timeout has occurred on a socket read or accept
            this.interrupt();
            // Try to reconnect once more
            this.connect();
        }
    }

    /**
     * @return true if the control channel is either tunneled using TLS or SSH
     */
    public abstract boolean isSecure();

    /**
     *
     * @return
     */
    public abstract String getSecurityInformation();

    protected abstract void connect() throws IOException, LoginCanceledException;

    protected LoginController loginController;

    public void setLoginController(LoginController loginController) {
        this.loginController = loginController;
    }

    protected abstract void login() throws IOException, LoginCanceledException;

    /**
     * Connect to the remote host and mount the home directory
     *
     * @return null if we fail, the mounted directory if we succeed
     */
    public Path mount() {
        synchronized(this) {
            this.message(NSBundle.localizedString("Mounting", "Status", "") + " " + host.getHostname() + "...");
            try {
                this.check();
                if(!this.isConnected()) {
                    return null;
                }
                Path home;
                if(host.hasReasonableDefaultPath()) {
                    home = PathFactory.createPath(this, host.getDefaultPath());
                    home.attributes.setType(Path.DIRECTORY_TYPE);
                    if(!home.list().attributes().isReadable()) {
                        // the default path does not exist or is not readable due to permission issues
                        home = workdir();
                    }
                }
                else {
                    home = workdir();
                }
                Growl.instance().notify(
                        NSBundle.localizedString("Connection opened", "Growl", "Growl Notification"),
                        host.getHostname());
                return home;
            }
            catch(IOException e) {
                this.error(null, "Connection failed", e, host.getHostname());
                this.interrupt();
            }
            return null;
        }
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
     *
     * @return The custom character encoding specified by the host
     * of this session or the default encoding if not specified
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
     *
     * @return The maximum number of concurrent connections allowed or -1 if no limit is set
     */
    public int getMaxConnections() {
        if(null == host.getMaxConnections()) {
            return Preferences.instance().getInteger("connection.host.max");
        }
        return host.getMaxConnections().intValue();
    }

    /**
     * @return The current working directory (pwd) or null if it cannot be retrieved for whatever reason
     * @throws ConnectionCanceledException If the underlying connection has already been closed before
     */
    protected abstract Path workdir() throws ConnectionCanceledException;

    /**
     * Send a no operation command
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

    /**
     * 
     * @param command
     */
    public abstract void sendCommand(String command) throws IOException;

    /**
     * @return boolean True if the session has not yet been closed.
     */
    public abstract boolean isConnected();

    private Timer keepAliveTimer = null;

    private Vector listeners = new Vector();

    public void addConnectionListener(ConnectionListener listener) {
        listeners.add(listener);
    }

    public void removeConnectionListener(ConnectionListener listener) {
        listeners.remove(listener);
    }

    protected void fireConnectionWillOpenEvent() throws ResolveCanceledException, UnknownHostException {
        log.debug("connectionWillOpen");
        ConnectionListener[] l = (ConnectionListener[]) listeners.toArray(
                new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].connectionWillOpen();
        }
        this.resolver = new Resolver(this.host.getHostname());
        this.message(NSBundle.localizedString("Resolving", "Status", "") + " " + host.getHostname() + "...");
        // Try to resolve the hostname first
        this.resolver.resolve();
        // The IP address could successfully be determined
    }

    protected void fireConnectionDidOpenEvent() {
        log.debug("connectionDidOpen");
        if(Preferences.instance().getBoolean("connection.keepalive")) {
            this.keepAliveTimer = new Timer();
            this.keepAliveTimer.scheduleAtFixedRate(new KeepAliveTask(),
                    Preferences.instance().getInteger("connection.keepalive.interval"),
                    Preferences.instance().getInteger("connection.keepalive.interval"));
        }

        ConnectionListener[] l = (ConnectionListener[]) listeners.toArray(
                new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].connectionDidOpen();
        }
    }

    protected void fireConnectionWillCloseEvent() {
        log.debug("connectionWillClose");
        this.message(NSBundle.localizedString("Disconnecting...", "Status", ""));
        ConnectionListener[] l = (ConnectionListener[]) listeners.toArray(
                new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].connectionWillClose();
        }
    }

    protected void fireConnectionDidCloseEvent() {
        log.debug("connectionDidClose");
        if(this.keepAliveTimer != null) {
            this.keepAliveTimer.cancel();
        }
        this.message(NSBundle.localizedString("Disconnected", "Status", ""));
        ConnectionListener[] l = (ConnectionListener[]) listeners.toArray(
                new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].connectionDidClose();
        }
    }

    /**
     * Must always be used in pair with #activityStopped
     */
    public void fireActivityStartedEvent() {
        log.debug("activityStarted");
        ConnectionListener[] l = (ConnectionListener[]) listeners.toArray(
                new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].activityStarted();
        }
    }

    /**
     * Must always be used in pair with #activityStarted
     */
    public void fireActivityStoppedEvent() {
        log.debug("activityStopped");
        ConnectionListener[] l = (ConnectionListener[]) listeners.toArray(
                new ConnectionListener[listeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].activityStopped();
        }
    }

    private Vector transcriptListeners = new Vector();

    public void addTranscriptListener(TranscriptListener listener) {
        transcriptListeners.add(listener);
    }

    public void removeTranscriptListener(TranscriptListener listener) {
        transcriptListeners.remove(listener);
    }

    /**
     * Log the message to all subscribed transcript listeners
     * @see TranscriptListener
     * @param message
     */
    protected void log(final String message) {
        log.info(message);
        TranscriptListener[] l = (TranscriptListener[]) transcriptListeners.toArray(
                new TranscriptListener[transcriptListeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].log(message);
        }
    }

    private Vector progressListeners = new Vector();

    public void addProgressListener(ProgressListener listener) {
        progressListeners.add(listener);
    }

    public void removeProgressListener(ProgressListener listener) {
        progressListeners.remove(listener);
    }

    /**
     * @param message
     * @param title If not null, then send the message to Growl
     */
    public void message(final String message, String title) {
        log.info(message);
        ProgressListener[] l = (ProgressListener[]) progressListeners.toArray(
                new ProgressListener[progressListeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].message(message);
        }
        if(title != null) {
            Growl.instance().notify(NSBundle.localizedString(message, "Growl"), title);
        }
    }

    public void message(final String message) {
        this.message(message, null);
    }

    private Vector errorListeners = new Vector();

    public void addErrorListener(ErrorListener listener) {
        errorListeners.add(listener);
    }

    public void removeErrorListener(ErrorListener listener) {
        errorListeners.remove(listener);
    }

    public void error(Path path, String message, Exception e) {
        this.error(path, message, e, null);
    }

    /**
     *
     * @param path
     * @param message
     * @param e
     * @param title If not null, send the error to Growl
     */
    public void error(Path path, String message, Exception e, String title) {
        log.info(e.getMessage());
        BackgroundException failure = new BackgroundException(this, path, message, e);
        ErrorListener[] l = (ErrorListener[]) errorListeners.toArray(
                new ErrorListener[errorListeners.size()]);
        for(int i = 0; i < l.length; i++) {
            l[i].error(failure);
        }
        if(title != null) {
            Growl.instance().notify(NSBundle.localizedString(message, "Growl"), title);
        }
    }

    /**
     * A task to send no operation commands
     */
    private class KeepAliveTask extends TimerTask {
        public void run() {
            try {
                Session.this.noop();
            }
            catch(IOException e) {
                Session.this.interrupt();
                this.cancel();
            }
        }
    }

    private List backHistory = new ArrayList();

    private List forwardHistory = new ArrayList();

    /**
     * @param p
     */
    public void addPathToHistory(Path p) {
        if(backHistory.size() > 0) {
            if(p.equals(backHistory.get(backHistory.size() - 1))) {
                return;
            }
        }
        this.backHistory.add(p);
    }

    /**
     * Moves the returned path to the forward cache
     *
     * @return The previously browsed path or null if there is none
     */
    public Path getPreviousPath() {
        int size = this.backHistory.size();
        if(size > 1) {
            this.forwardHistory.add(this.backHistory.get(size - 1));
            Path p = (Path) this.backHistory.get(size - 2);
            //delete the fetched path - otherwise we produce a loop
            this.backHistory.remove(size - 1);
            this.backHistory.remove(size - 2);
            return p;
        }
        else if(1 == size) {
            this.forwardHistory.add(this.backHistory.get(size - 1));
            return (Path) this.backHistory.get(size - 1);
        }
        return null;
    }

    public Path getForwardPath() {
        int size = this.forwardHistory.size();
        if(size > 0) {
            Path p = (Path) this.forwardHistory.get(size - 1);
            this.forwardHistory.remove(size - 1);
            return p;
        }
        return null;
    }


    public Path[] getBackHistory() {
        return (Path[]) this.backHistory.toArray(new Path[this.backHistory.size()]);
    }

    public Path[] getForwardHistory() {
        return (Path[]) this.forwardHistory.toArray(new Path[this.forwardHistory.size()]);
    }

    /**
     *
     */
    private Cache cache = new Cache();

    /**
     *
     * @return The directory listing cache
     */
    public Cache cache() {
        return this.cache;
    }

    /**
     *
     * @param other
     * @return
     */
    public boolean equals(Object other) {
        if (null == other) {
            return false;
        }
        if (other instanceof Session) {
            return this.getHost().getHostname().equals(((Session)other).getHost().getHostname())
                    && this.getHost().getProtocol().equals(((Session)other).getHost().getProtocol());
        }
        return false;
    }

    protected void finalize() throws java.lang.Throwable {
        log.debug("finalize:" + super.toString());
        super.finalize();
    }
}
