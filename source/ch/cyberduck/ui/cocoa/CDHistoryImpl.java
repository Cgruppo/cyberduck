package ch.cyberduck.ui.cocoa;

/*
 *  Copyright (c) 2003 David Kocher. All rights reserved.
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

import ch.cyberduck.core.History;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Preferences;

import com.apple.cocoa.foundation.*;

import java.io.File;

import org.apache.log4j.Logger;

/**
 * @version $Id$
 */
public class CDHistoryImpl extends History {
	private static Logger log = Logger.getLogger(CDHistoryImpl.class);

	private static CDHistoryImpl instance;

	private static final File HISTORY_FILE = new File(NSPathUtilities.stringByExpandingTildeInPath("~/Library/Application Support/Cyberduck/History.plist"));

	static {
		HISTORY_FILE.getParentFile().mkdir();
	}

	public static CDHistoryImpl instance() {
		if (null == instance) {
			instance = new CDHistoryImpl();
		}
		return instance;
	}

	public void load() {
		this.load(HISTORY_FILE);
	}

	/**
	 * Deserialize all the bookmarks saved previously in the users's application support directory
	 */
	public void load(java.io.File f) {
		log.debug("load");
		if (f.exists()) {
			log.info("Found Bookmarks file: " + f.toString());
			NSData plistData = new NSData(f);
			String[] errorString = new String[]{null};
			Object propertyListFromXMLData =
			    NSPropertyListSerialization.propertyListFromData(plistData,
			        NSPropertyListSerialization.PropertyListImmutable,
			        new int[]{NSPropertyListSerialization.PropertyListXMLFormat},
			        errorString);
			if (errorString[0] != null)
				log.error("Problem reading bookmark file: " + errorString[0]);
			else
				log.info("Successfully read Bookmarks: " + propertyListFromXMLData);
			if (propertyListFromXMLData instanceof NSArray) {
				NSArray entries = (NSArray) propertyListFromXMLData;
				java.util.Enumeration i = entries.objectEnumerator();
				Object element;
				while (i.hasMoreElements()) {
					element = i.nextElement();
					if (element instanceof NSDictionary) { //new since 2.1
						this.addItem(new Host((NSDictionary) element));
					}
					if (element instanceof String) { //backward compatibilty <= 2.1beta5 (deprecated)
						try {
							this.addItem(new Host((String) element));
						}
						catch (java.net.MalformedURLException e) {
							log.error("Bookmark has invalid URL: " + e.getMessage());
						}
					}
				}
			}
		}
	}

	public void save() {
		this.save(HISTORY_FILE);
	}

	/**
	 * Saves this collection of bookmarks in to a file to the users's application support directory
	 * in a plist xml format
	 */
	public void save(java.io.File f) {
		log.debug("save");
		if (Preferences.instance().getProperty("favorites.save").equals("true")) {
			try {
				NSMutableArray list = new NSMutableArray();
				java.util.Iterator i = this.iterator();
				while (i.hasNext()) {
					Host bookmark = (Host) i.next();
					list.addObject(bookmark.getAsDictionary());
				}
				NSMutableData collection = new NSMutableData();
				String[] errorString = new String[]{null};
				collection.appendData(NSPropertyListSerialization.dataFromPropertyList(
				    list,
				    NSPropertyListSerialization.PropertyListXMLFormat,
				    errorString)
				);
				if (errorString[0] != null)
					log.error("Problem writing bookmark file: " + errorString[0]);

				if (collection.writeToURL(f.toURL(), true))
					log.info("Bookmarks sucessfully saved to :" + f.toString());
				else
					log.error("Error saving Bookmarks to :" + f.toString());
			}
			catch (java.net.MalformedURLException e) {
				log.error(e.getMessage());
			}
		}
	}
}