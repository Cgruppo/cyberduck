package ch.cyberduck.core;

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

import ch.cyberduck.core.serializer.HostReaderFactory;
import ch.cyberduck.core.serializer.HostWriterFactory;
import ch.cyberduck.core.serializer.Reader;

import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

/**
 * @version $Id$
 */
public class HistoryCollection extends AbstractHostCollection {
    private static Logger log = Logger.getLogger(HistoryCollection.class);

    private static HistoryCollection HISTORY_COLLECTION = new HistoryCollection(
            LocalFactory.createLocal(Preferences.instance().getProperty("application.support.path"), "History")
    );

    /**
     * @return
     */
    public static HistoryCollection defaultCollection() {
        return HISTORY_COLLECTION;
    }

    private Local folder;

    /**
     * Reading bookmarks from this folder
     *
     * @param f Parent directory to look for bookmarks
     */
    public HistoryCollection(Local f) {
        this.folder = f;
        this.folder.mkdir(true);
    }

    public void open() {
        this.folder.open();
    }

    /**
     * @param bookmark
     * @return
     */
    public Local getFile(Host bookmark) {
        return LocalFactory.createLocal(folder, bookmark.getNickname() + ".duck");
    }

    /**
     * Does not allow duplicate entries.
     *
     * @param row
     * @param bookmark
     */
    @Override
    public void add(int row, Host bookmark) {
        HostWriterFactory.instance().write(bookmark, this.getFile(bookmark));
        if(!this.contains(bookmark)) {
            super.add(row, bookmark);
        }
        else {
            this.sort();
        }
    }

    /**
     * Does not allow duplicate entries.
     *
     * @param bookmark
     * @return
     */
    @Override
    public boolean add(Host bookmark) {
        HostWriterFactory.instance().write(bookmark, this.getFile(bookmark));
        if(!this.contains(bookmark)) {
            super.add(bookmark);
        }
        else {
            this.sort();
        }
        return true;
    }

    /**
     * @param row
     * @return the element that was removed from the list.
     */
    @Override
    public Host remove(int row) {
        this.getFile(this.get(row)).delete(false);
        return super.remove(row);
    }

    @Override
    public boolean remove(Object item) {
        if(this.contains(item)) {
            this.getFile(this.get(this.indexOf(item))).delete(false);
        }
        return super.remove(item);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        for(Object next : c) {
            if(this.contains(next)) {
                this.getFile(this.get(this.indexOf(next))).delete(false);
            }
        }
        return super.removeAll(c);
    }

    @Override
    public void load() {
        log.info("Reloading:" + folder);
        final AttributedList<Local> bookmarks = folder.children(
                new PathFilter<Local>() {
                    public boolean accept(Local file) {
                        return file.getName().endsWith(".duck");
                    }
                }
        );
        final Reader<Host> reader = HostReaderFactory.instance();
        for(Local next : bookmarks) {
            super.add(reader.read(next));
        }
        this.sort();
    }

    @Override
    protected void sort() {
        Collections.sort(this, new Comparator<Host>() {
            public int compare(Host o1, Host o2) {
                Local f1 = getFile(o1);
                Local f2 = getFile(o2);
                if(f1.attributes().getModificationDate() < f2.attributes().getModificationDate()) {
                    return 1;
                }
                if(f1.attributes().getModificationDate() > f2.attributes().getModificationDate()) {
                    return -1;
                }
                return 0;
            }
        });
    }

    @Override
    public void clear() {
        log.debug("Removing all bookmarks from:" + folder);
        for(Host next : this) {
            this.getFile(next).delete(false);
        }
        super.clear();
    }

    @Override
    public boolean allowsAdd() {
        return false;
    }

    @Override
    public boolean allowsEdit() {
        return false;
    }
}