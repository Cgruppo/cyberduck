package ch.cyberduck.core;

/*
 * Copyright (c) 2002-2009 David Kocher. All rights reserved.
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

import ch.cyberduck.core.i18n.Locale;

import java.util.Iterator;

/**
 * @version $Id$
 */
public class RendezvousCollection extends AbstractHostCollection {

    private static RendezvousCollection RENDEZVOUS_COLLECTION
            = new RendezvousCollection();

    private RendezvousCollection() {
        RendezvousFactory.instance().addListener(new RendezvousListener() {
            public void serviceResolved(String identifier, Host host) {
                RendezvousCollection.this.collectionItemAdded(host);
            }

            public void serviceLost(String servicename) {
                RendezvousCollection.this.collectionItemRemoved(null);
            }
        });
    }

    /**
     * @return
     */
    public static RendezvousCollection defaultCollection() {
        return RENDEZVOUS_COLLECTION;
    }

    @Override
    public String getName() {
        return Locale.localizedString("Bonjour");
    }

    @Override
    public Host get(int row) {
        return RendezvousFactory.instance().getService(row);
    }

    @Override
    public int size() {
        return RendezvousFactory.instance().numberOfServices();
    }

    @Override
    public Host remove(int row) {
        return null;
    }

    @Override
    public Object[] toArray() {
        Host[] content = new Host[this.size()];
        int i = 0;
        for(Host host : this) {
            content[i] = host;
        }
        return content;
    }

    @Override
    public Iterator<Host> iterator() {
        return RendezvousFactory.instance().iterator();
    }

    @Override
    public boolean allowsAdd() {
        return false;
    }

    @Override
    public boolean allowsDelete() {
        return false;
    }

    @Override
    public boolean allowsEdit() {
        return false;
    }
}
