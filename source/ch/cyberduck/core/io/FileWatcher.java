package ch.cyberduck.core.io;

/*
 *  Copyright (c) 2006 David Kocher. All rights reserved.
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

import ch.cyberduck.core.Local;
import ch.cyberduck.ui.cocoa.CDController;

import org.apache.log4j.Logger;

import java.io.IOException;

public class FileWatcher extends CDController {
    private static Logger log = Logger.getLogger(FileWatcher.class);

//    private FileMonitor monitor = FileMonitor.getInstance();

    private static FileWatcher instance = null;

    private FileWatcher() {
        //
    }

    private static final Object lock = new Object();

    public static FileWatcher instance() {
        synchronized(lock) {
            if(null == instance) {
                instance = new FileWatcher();
            }
            return instance;
        }
    }

    public void watch(final Local file, final FileWatcherListener listener) throws IOException {
//        monitor.addWatch(new File(file.getAbsolute()));
//        monitor.addFileListener(new FileMonitor.FileListener() {
//
//            @Override
//            public void fileChanged(FileMonitor.FileEvent e) {
//                if(e.getType() == FileMonitor.FILE_MODIFIED) {
//                    listener.fileWritten(new Local(e.getFile()));
//                }
//                if(e.getType() == FileMonitor.FILE_DELETED) {
//                    listener.fileDeleted(new Local(e.getFile()));
//                    monitor.unwatch(e.getFile());
//                }
//                if(e.getType() == FileMonitor.FILE_RENAMED) {
//                    listener.fileRenamed(new Local(e.getFile()));
//                }
//            }
//        });
    }
}
