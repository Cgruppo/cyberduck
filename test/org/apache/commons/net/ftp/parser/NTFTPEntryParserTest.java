/*
 * Copyright 2001-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.net.ftp.parser;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import ch.cyberduck.core.ftp.FTPParserFactory;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileEntryParser;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Calendar;

public class NTFTPEntryParserTest extends TestCase
{

    private static final String[] samples =
            {
                    "05-26-95  10:57AM               143712 $LDR$",
                    "05-20-97  03:31PM                  681 .bash_history",
                    "12-05-96  05:03PM       <DIR>          absoft2",
                    "11-14-97  04:21PM                  953 AUDITOR3.INI",
                    "05-22-97  08:08AM                  828 AUTOEXEC.BAK",
                    "01-22-98  01:52PM                  795 AUTOEXEC.BAT",
                    "05-13-97  01:46PM                  828 AUTOEXEC.DOS",
                    "12-03-96  06:38AM                  403 AUTOTOOL.LOG",
                    "12-03-96  06:38AM       <DIR>          123xyz",
                    "01-20-97  03:48PM       <DIR>          bin",
                    "05-26-1995  10:57AM               143712 $LDR$",
                    "07-10-07  07:32PM                69610 Algemene Leveringsvoorwaarden *******.pdf",
                    "07-11-07  12:52AM       <DIR>          aspnet_client",
                    "07-10-07  07:30PM       <DIR>          auth",
                    "07-03-07  01:55PM       <DIR>          cgi-bin",
                    "07-10-07  07:32PM                  428 global.asa",
                    "07-03-07  01:55PM       <DIR>          icon",
                    "07-10-07  07:29PM       <DIR>          img",
                    "07-10-07  07:32PM       <DIR>          include",
                    "07-10-07  07:32PM                 3384 index.html",
                    "07-10-07  07:32PM       <DIR>          js",
                    "07-10-07  07:37PM       <DIR>          kandidaten",
                    "07-10-07  07:32PM       <DIR>          lib",
                    "07-10-07  07:37PM       <DIR>          opdrachtgevers",
                    "07-10-07  07:32PM                 1309 stijl1.css",
                    "07-10-07  07:32PM       <DIR>          style",
                    "07-15-07  02:40PM       <DIR>          temp",
                    "07-10-07  07:32PM       <DIR>          vacatures"
    };

    /**
     * @see junit.framework.TestCase#TestCase(String)
     */
    public NTFTPEntryParserTest (String name)
    {
        super(name);
    }

    private FTPFileEntryParser parser;
    private SimpleDateFormat df;

    public void setUp() throws Exception
    {
        this.parser = new FTPParserFactory().createFileEntryParser("WINDOWS");
        this.df = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US);
    }

    public void testParse() throws Exception
    {
        for(int i = 0; i < samples.length; i++) {
            assertNotNull(samples[i], parser.parseFTPEntry(samples[i]));
        }
    }

    public void testParseFieldsOnDirectory() throws Exception
    {
        FTPFile parsed = parser.parseFTPEntry("12-05-96  05:03PM       <DIR>          absoft2");
        assertNotNull("Could not parse entry.", parsed);
        assertEquals("Thu Dec 05 17:03:00 1996",
                     df.format(parsed.getTimestamp().getTime()));
        assertTrue("Should have been a directory.",
                   parsed.isDirectory());
        assertEquals("absoft2", parsed.getName());
//        assertEquals(-1, (int)parsed.getSize());

        parsed = parser.parseFTPEntry(
                "12-03-96  06:38AM       <DIR>          123456");
        assertNotNull("Could not parse entry.", parsed);
        assertTrue("Should have been a directory.",
                parsed.isDirectory());
        assertEquals("123456", parsed.getName());
//        assertEquals(-1, (int)parsed.getSize());
    }

    public void testParseFieldsOnFile() throws Exception
    {
        FTPFile parsed = parser.parseFTPEntry(
                "05-22-97  12:08AM                  5000000000 AUTOEXEC.BAK");
        assertNotNull("Could not parse entry.", parsed);
        assertEquals("Thu May 22 00:08:00 1997",
                df.format(parsed.getTimestamp().getTime()));
        assertTrue("Should have been a file.",
                   parsed.isFile());
        assertEquals("AUTOEXEC.BAK", parsed.getName());
        assertEquals(5000000000l, parsed.getSize());
    }

    public void testDirectoryBeginningWithNumber() throws Exception
    {
        FTPFile parsed = parser.parseFTPEntry("12-03-96  06:38AM       <DIR>          123xyz");
        assertNotNull(parsed);
        assertEquals("name", "123xyz", parsed.getName());
    }

    public void testDirectoryBeginningWithNumberFollowedBySpaces() throws Exception
    {
        FTPFile parsed = parser.parseFTPEntry(
                "12-03-96  06:38AM       <DIR>          123 xyz");
        assertNotNull(parsed);
        assertEquals("name", "123 xyz", parsed.getName());
        parsed = parser.parseFTPEntry(
                "12-03-96  06:38AM       <DIR>          123 abc xyz");
        assertNotNull(parsed);
        assertEquals("name", "123 abc xyz", parsed.getName());
    }

    public void testElectic() throws Exception
    {
        FTPFile parsed = parser.parseFTPEntry(
                "09-04-06  11:28AM                  149 gearkommandon with spaces.txt");
        assertNotNull(parsed);
        assertEquals(parsed.getName(), "gearkommandon with spaces.txt");
        assertTrue(parsed.getType() == FTPFile.FILE_TYPE);
        assertTrue(parsed.getTimestamp().get(Calendar.MONTH) == Calendar.SEPTEMBER);
        assertTrue(parsed.getTimestamp().get(Calendar.DAY_OF_MONTH) == 4);
        assertTrue(parsed.getTimestamp().get(Calendar.YEAR) == 2006);
    }

    public static Test suite()
    {
        return new TestSuite(NTFTPEntryParserTest.class);
    }
}
