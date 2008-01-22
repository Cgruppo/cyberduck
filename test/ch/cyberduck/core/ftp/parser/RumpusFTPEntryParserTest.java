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

import org.apache.commons.net.ftp.FTPFileEntryParser;
import org.apache.commons.net.ftp.FTPFile;

import java.util.Calendar;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.framework.TestCase;

/**
 * @version $Id:$
 */
public class RumpusFTPEntryParserTest extends TestCase {

    public RumpusFTPEntryParserTest(String name) {
        super(name);
    }

    private FTPFileEntryParser parser;


    public void setUp() throws Exception {
        this.parser = new FTPParserFactory().createFileEntryParser("MACOS");
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testParse() throws Exception {
        FTPFile parsed = null;

        parsed = parser.parseFTPEntry(
                "drwxr-xr-x               folder        0 Oct 18 13:02 Akrilik"
        );
        assertNotNull(parsed);
        assertEquals(parsed.getName(), "Akrilik");
        assertTrue(parsed.getType() == FTPFile.DIRECTORY_TYPE);
        assertTrue(parsed.getTimestamp().get(Calendar.MONTH) == Calendar.OCTOBER);
        assertTrue(parsed.getTimestamp().get(Calendar.DAY_OF_MONTH) == 18);
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION));

        parsed = parser.parseFTPEntry(
                "drwxrwxrwx               folder        0 Oct 11 14:53 Uploads"
        );
        assertNotNull(parsed);
        assertEquals(parsed.getName(), "Uploads");
        assertTrue(parsed.getType() == FTPFile.DIRECTORY_TYPE);
        assertTrue(parsed.getTimestamp().get(Calendar.MONTH) == Calendar.OCTOBER);
        assertTrue(parsed.getTimestamp().get(Calendar.DAY_OF_MONTH) == 11);
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION));

        parsed = parser.parseFTPEntry(
                "-rw-r--r--        0      589878   589878 Oct 15 13:03 WebDAV SS.bmp"
        );
        assertNotNull(parsed);
        assertEquals(parsed.getName(), "WebDAV SS.bmp");
        assertTrue(parsed.getType() == FTPFile.FILE_TYPE);
        assertTrue(parsed.getTimestamp().get(Calendar.MONTH) == Calendar.OCTOBER);
        assertTrue(parsed.getTimestamp().get(Calendar.DAY_OF_MONTH) == 15);
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION));
        assertTrue(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION));
        assertFalse(parsed.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION));
    }

    public void testUnknownSystIdentifier() throws Exception {
        this.parser = new FTPParserFactory().createFileEntryParser("Digital Domain FTP");

        FTPFile parsed = null;
        parsed = parser.parseFTPEntry(
                "drwxrwxrwx               folder        0 Jan 19 20:36 Mastered 1644"
        );
        assertNotNull(parsed);
        assertEquals(parsed.getName(), "Mastered 1644");
        assertTrue(parsed.getType() == FTPFile.DIRECTORY_TYPE);

        parsed = parser.parseFTPEntry(
                "-rwxrwxrwx        0   208143684 208143684 Jan 14 02:13 Dhannya dhannya.rar"
        );
        assertNotNull(parsed);
        assertEquals(parsed.getName(), "Dhannya dhannya.rar");
        assertTrue(parsed.getType() == FTPFile.FILE_TYPE);

        parsed = parser.parseFTPEntry(
                "drwxr-xr-x               folder        0 Jan 14 16:04 Probeordner");
        assertNotNull(parsed);
        assertEquals(parsed.getName(), "Probeordner");
        assertTrue(parsed.getType() == FTPFile.DIRECTORY_TYPE);
    }

    public static Test suite() {
        return new TestSuite(RumpusFTPEntryParserTest.class);
    }
}