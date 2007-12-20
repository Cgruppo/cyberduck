package ch.cyberduck.core.ftp.parser;

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

import ch.cyberduck.core.ftp.FTPParserFactory;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileEntryParser;

import java.util.Calendar;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @version $Id: TrellixFTPEntryParserTest.java 2042 2006-04-23 01:39:32Z dkocher $
 */
public class WebstarFTPEntryParserTest extends TestCase {

    public WebstarFTPEntryParserTest(String name) {
        super(name);
    }

    private FTPFileEntryParser parser;


    public void setUp() throws Exception {
        this.parser = new FTPParserFactory().createFileEntryParser("MACOS WebSTAR FTP");
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testParse() throws Exception {
        FTPFile parsed = null;

        parsed = parser.parseFTPEntry(
                "-rwx------          17      332      640 Dec 20 08:54 file 1"
        );
        assertNotNull(parsed);
        assertEquals(parsed.getName(), "file 1");
        assertTrue(parsed.getType() == FTPFile.FILE_TYPE);
        assertEquals(640, parsed.getSize());

        parsed = parser.parseFTPEntry(
                "drwx------             folder          2 Dec 20 08:55 folder1"
        );
        assertNotNull(parsed);
        assertEquals(parsed.getName(), "folder1");
        assertTrue(parsed.getType() == FTPFile.DIRECTORY_TYPE);
        assertTrue(parsed.getTimestamp().get(Calendar.MONTH) == Calendar.DECEMBER);
        assertTrue(parsed.getTimestamp().get(Calendar.DAY_OF_MONTH) == 20);
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION));
    }

    public static Test suite() {
        return new TestSuite(WebstarFTPEntryParserTest.class);
    }
}