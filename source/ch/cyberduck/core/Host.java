package ch.cyberduck.core;
/*
 *  Copyright (c) 2002 David Kocher. All rights reserved.
 *  http://icu.unizh.ch/~dkocher/
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

import java.io.IOException;
import ch.cyberduck.core.http.HTTPSession;
import ch.cyberduck.core.sftp.SFTPSession;
import ch.cyberduck.core.ftp.FTPSession;
import ch.cyberduck.core.http.HTTPSession;
import ch.cyberduck.core.Path;
import ch.cyberduck.ui.ObserverList;
import com.sshtools.j2ssh.transport.HostKeyVerification;
import java.util.Observable;
import java.util.Observer;
import java.util.List;
import org.apache.log4j.Logger;

public class Host extends Observable {
    private static Logger log = Logger.getLogger(Host.class);

    public Login login;
    private String protocol = Preferences.instance().getProperty("connection.protocol.default");
    private int port = Integer.parseInt(Preferences.instance().getProperty("connection.port.default"));
    private String name;
    private String workdir = Preferences.instance().getProperty("connection.path.default");
    private HostKeyVerification hostKeyVerification;
    private transient Session session;

    public Host(String protocol, String name, int port, String workdir, Login login) {
        this.protocol = protocol != null ? protocol : this.protocol;
        this.port = port != -1 ? port : this.port;
	//@todo extract protocol:// if accidentially added
        this.name = name;
	this.workdir = workdir != null ? workdir : this.workdir;
        this.login = login != null ? login : this.login;
	ObserverList.instance().registerObservable(this);
	log.debug(this.toString());
    }

    public Host(String protocol, String name, int port, Login login) {
	this(protocol, name, port, null, login);
    }

    public void callObservers(Object arg) {
        log.debug("callObservers:"+arg.toString());
	log.debug(this.countObservers()+" observers known.");
        long start = System.currentTimeMillis();
	this.setChanged();
//	if(arg instanceof Path)
	    //@todothis.workdir = (Path)arg;
	this.notifyObservers(arg);
        long end = System.currentTimeMillis();
	log.debug((end - start) + " ms");
    }
    

    public void openSession() {
        log.debug("openSession");
	if(null == session) {
	    if(this.getProtocol().equalsIgnoreCase(Session.HTTP)) {
		this.session = new HTTPSession(this);
	    }
	    //  @todo      if(this.getProtocol().equalsIgnoreCase(Session.HTTPS)) {
     //            return new HTTPSession(this);
     //        }
	    if(this.getProtocol().equalsIgnoreCase(Session.FTP)) {
		this.session = new FTPSession(this);
	    }
	    if(this.getProtocol().equalsIgnoreCase(Session.SFTP)) {
		this.session = new SFTPSession(this);
	    }
	}
        this.session.connect();
	this.callObservers(new Message(Message.OPEN));
    }
    
    public boolean hasValidSession() {
	return session != null && session.isConnected();//@todo use check() without reconnecting 
    }

    public void closeSession() {
        log.debug("closeSession");
	if(session != null) {
	    this.session.close();
	    this.session = null;
	}
	this.callObservers(new Message(Message.CLOSE));
    }

    public void recycle() {
        log.debug("recycle");
	this.closeSession();
	this.openSession();
    }
    
//    public boolean isConnected() {
//	return this.getSession().isConnected();
  //  }    

    public Login getLogin() {
	return this.login;
    }
    
    public String getName() {
	return this.name;
    }

    public String getWorkdir() {
	return this.workdir;
    }

    public int getPort() {
	return this.port;
    }

//@todo SSHHost, FTPHost, HTTPHost, etc
    //ssh specific
    public void setHostKeyVerification(HostKeyVerification h) {
	this.hostKeyVerification = h;
    }

    public HostKeyVerification getHostKeyVerification() {
	return this.hostKeyVerification;
    }

    
    public String getProtocol() {
	return this.protocol;
    }

    /**
	* @return The IP address of the remote host if available
     */
    public String getIp() {
        //if we call getByName(null) InetAddress would return localhost
        if(this.name == null)
            return "Unknown host";
        try {
            return java.net.InetAddress.getByName(name).toString();
        }
        catch(java.net.UnknownHostException e) {
            return "Unknown host";
        }
    }
    
    public String toString() {
	return("Host:"+protocol+","+name+","+port+","+workdir+","+login);
    }
}
