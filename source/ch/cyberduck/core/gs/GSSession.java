package ch.cyberduck.core.gs;

/*
 * Copyright (c) 2002-2010 David Kocher. All rights reserved.
 *
 * http://cyberduck.ch/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * Bug fixes, suggestions and comments should be sent to:
 * dkocher@cyberduck.ch
 */

import ch.cyberduck.core.Host;
import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.SessionFactory;
import ch.cyberduck.core.cloud.Distribution;
import ch.cyberduck.core.s3.S3Session;

import org.jets3t.service.CloudFrontServiceException;

/**
 * Google Storage for Developers is a new service for developers to store and
 * access data in Google's cloud. It offers developers direct access to Google's
 * scalable storage and networking infrastructure as well as powerful authentication
 * and data sharing mechanisms.
 *
 * @version $Id$
 */
public class GSSession extends S3Session {

    static {
        SessionFactory.addFactory(Protocol.GOOGLESTORAGE, new Factory());
    }

    public static class Factory extends SessionFactory {
        @Override
        protected Session create(Host h) {
            return new GSSession(h);
        }
    }

    protected GSSession(Host h) {
        super(h);
    }

    @Override
    protected void configure() {
        super.configure();
        configuration.setProperty("s3service.disable-dns-buckets", String.valueOf(true));
    }

    /**
     * Not supported
     *
     * @param bucket Name of the container
     * @param method
     * @return
     * @throws CloudFrontServiceException
     */
    @Override
    public org.jets3t.service.model.cloudfront.Distribution[] listDistributions(String bucket, Distribution.Method method) throws CloudFrontServiceException {
        return new org.jets3t.service.model.cloudfront.Distribution[]{};
    }


    /**
     * Not supported
     *
     * @param container
     * @param method
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public Distribution readDistribution(String container, Distribution.Method method) {
        throw new UnsupportedOperationException();
    }

    /**
     * Not supported
     *
     * @param enabled
     * @param container
     * @param method
     * @param cnames
     * @param logging
     * @throws UnsupportedOperationException
     */
    @Override
    public void writeDistribution(final boolean enabled, String container, Distribution.Method method, final String[] cnames, boolean logging) {
        throw new UnsupportedOperationException();
    }
}