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
import com.apple.dnssd.*;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * @version $Id$
 */
public class Rendezvous
        implements BrowseListener, ResolveListener {

    private static Logger log = Logger.getLogger(Rendezvous.class);

    private static final String SERVICE_TYPE_SFTP = "_sftp._tcp.";
    private static final String SERVICE_TYPE_SSH = "_ssh._tcp.";
    private static final String SERVICE_TYPE_FTP = "_ftp._tcp.";
    private static final String SERVICE_TYPE_WEBDAV = "_webdav._tcp";

    private static final String[] serviceTypes = new String[]{
            SERVICE_TYPE_SFTP, SERVICE_TYPE_SSH, SERVICE_TYPE_FTP, SERVICE_TYPE_WEBDAV
    };

    private Map<String, Host> services;
    private Map<String, DNSSDService> browsers;

    private static Rendezvous instance;

    private static final Object lock = new Object();

    public static Rendezvous instance() {
        synchronized(lock) {
            if(null == instance) {
                instance = new Rendezvous();
            }
        }
        return instance;
    }

    private Rendezvous() {
        log.debug("Rendezvous");
        this.services = new HashMap<String, Host>();
        this.browsers = new HashMap<String, DNSSDService>();
    }

    /**
     * Start browsing for rendezvous servcices for all registered service types
     */
    public void init() {
        log.debug("init");
        try {
            for(int i = 0; i < serviceTypes.length; i++) {
                String protocol = serviceTypes[i];
                log.info("Adding Rendezvous service listener for " + protocol);
                this.browsers.put(protocol, DNSSD.browse(protocol, this));
            }
        }
        catch(DNSSDException e) {
            log.error(e.getMessage());
            this.quit();
        }
    }

    /**
     * Halt all service discvery browsers
     */
    public void quit() {
        for(int i = 0; i < serviceTypes.length; i++) {
            String protocol = serviceTypes[i];
            log.info("Removing Rendezvous service listener for " + protocol);
            DNSSDService service = this.browsers.get(protocol);
            if(null == service) {
                continue;
            }
            service.stop();
        }
    }

    private Set<RendezvousListener> listeners =
            Collections.synchronizedSet(new HashSet<RendezvousListener>());

    private RendezvousListener notifier = new RendezvousListener() {

        public void serviceResolved(final String servicename, final String hostname) {
            log.info("Service resolved:" + servicename);
            RendezvousListener[] l = listeners.toArray(
                    new RendezvousListener[listeners.size()]);
            for(int i = 0; i < l.length; i++) {
                l[i].serviceResolved(servicename, hostname);
            }
        }

        public void serviceLost(final String servicename) {
            log.info("Service lost:" + servicename);
            RendezvousListener[] l = listeners.toArray(
                    new RendezvousListener[listeners.size()]);
            for(int i = 0; i < l.length; i++) {
                l[i].serviceLost(servicename);
            }
        }
    };

    /**
     * Register a listener to be notified
     *
     * @param listener
     */
    public void addListener(RendezvousListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove the listener from the notification queue
     *
     * @param listener
     */
    public void removeListener(RendezvousListener listener) {
        listeners.remove(listener);
    }

    /**
     * @param identifier The full service domain name
     * @return The host this name maps to or null if none is found
     */
    public Host getServiceWithIdentifier(String identifier) {
        log.debug("getService:" + identifier);
        return services.get(identifier);
    }

    /**
     * @param displayedName The name returned by #getDisplayedName
     * @return The host this name maps to or null if none is found
     */
    public Host getServiceWithDisplayedName(String displayedName) {
        synchronized(this) {
            for(Host h : services.values()) {
                if(h.getNickname().equals(displayedName)) {
                    return h;
                }
            }
        }
        log.warn("No identifier for displayed name:" + displayedName);
        return null;
    }

    /**
     * @return The number of services found; 0 <= services < n
     */
    public int numberOfServices() {
        return services.size();
    }

    public Host getService(int index) {
        synchronized(this) {
            return services.values().toArray(new Host[services.size()])[index];
        }
    }

    /**
     * @param index
     * @return A nicely formatted informative string
     */
    public String getDisplayedName(int index) {
        if(index < this.numberOfServices()) {
            synchronized(this) {
                return services.values().toArray(new Host[services.size()])[index].getNickname();
            }
        }
        return NSBundle.localizedString("Unknown", "");
    }

    /**
     * @param identifier The full service domain name
     * @return A nicely formatted informative string
     */
    public String getDisplayedName(String identifier) {
        Host host = services.get(identifier);
        if(null == host) {
            return NSBundle.localizedString("Unknown", "");
        }
        return host.getNickname();
    }

    /**
     * @param browser
     * @param flags
     * @param ifIndex
     * @param servicename
     * @param regType
     * @param domain
     */
    public void serviceFound(DNSSDService browser, int flags, int ifIndex, String servicename,
                             String regType, String domain) {
        log.debug("serviceFound:" + servicename);
        try {
            DNSSD.resolve(flags, ifIndex, servicename, regType, domain, this);
        }
        catch(DNSSDException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * @param browser
     * @param flags
     * @param ifIndex
     * @param serviceName
     * @param regType
     * @param domain
     */
    public void serviceLost(DNSSDService browser, int flags, int ifIndex, String serviceName,
                            String regType, String domain) {
        log.debug("serviceLost:" + serviceName);
        try {
            String identifier = DNSSD.constructFullName(serviceName, regType, domain);
            notifier.serviceLost(identifier);
            synchronized(this) {
                if(null == this.services.remove(identifier)) {
                    return;
                }
            }
        }
        catch(DNSSDException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * @param resolver
     * @param errorCode
     */
    public void operationFailed(DNSSDService resolver, int errorCode) {
        log.warn("operationFailed:" + errorCode);
        resolver.stop();
    }

    /**
     * @param resolver
     * @param flags
     * @param ifIndex
     * @param fullname
     * @param hostname
     * @param port
     * @param txtRecord
     */
    public void serviceResolved(DNSSDService resolver, int flags, int ifIndex,
                                String fullname, String hostname, int port, TXTRecord txtRecord) {
        log.debug("serviceResolved:" + hostname);
        try {
            final Host host = new Host(this.getProtocol(fullname, port), hostname, port);
            host.setCredentials(new Credentials(null, null));
            synchronized(this) {
                this.services.put(fullname, host);
            }
            this.notifier.serviceResolved(fullname, hostname);
        }
        finally {
            // Note: When the desired results have been returned, the client MUST terminate
            // the resolve by calling DNSSDService.stop().
            resolver.stop();
        }
    }

    /**
     * @param serviceType
     * @return Null if no protocol can be found for the given Rendezvous service type.
     * @see "http://developer.apple.com/qa/qa2001/qa1312.html"
     */
    public Protocol getProtocol(final String serviceType, final int port) {
        if(serviceType.contains(SERVICE_TYPE_SFTP) || serviceType.contains(SERVICE_TYPE_SSH)) {
            return Protocol.SFTP;
        }
        if(serviceType.contains(SERVICE_TYPE_FTP)) {
            return Protocol.FTP;
        }
        if(serviceType.contains(SERVICE_TYPE_WEBDAV)) {
            if(Protocol.WEBDAV_SSL.getDefaultPort() == port) {
                return Protocol.WEBDAV_SSL;
            }
            return Protocol.WEBDAV;
        }
        return Protocol.getDefaultProtocol(port);
    }
}