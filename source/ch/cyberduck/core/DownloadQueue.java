package ch.cyberduck.core;

/*
 *  Copyright (c) 2004 David Kocher. All rights reserved.
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
import com.apple.cocoa.foundation.NSMutableDictionary;

import java.util.Iterator;
import java.util.List;

import ch.cyberduck.ui.cocoa.growl.Growl;

/**
 * @version $Id$
 */
public class DownloadQueue extends Queue {

	public DownloadQueue() {
		super();
	}
	
	public DownloadQueue(Path root) {
		super(root);
	}
	
	public NSMutableDictionary getAsDictionary() {
		NSMutableDictionary dict = super.getAsDictionary();
		dict.setObjectForKey(String.valueOf(Queue.KIND_DOWNLOAD), "Kind");
		return dict;
	}

	public void callObservers(Object arg) {
		super.callObservers(arg);
		if(arg instanceof Message) {
			Message msg = (Message)arg;
			if(msg.getTitle().equals(Message.QUEUE_STOP)) {
				if(this.isComplete()) {
					Growl.instance().notify(NSBundle.localizedString("Download complete",
																	 "Growl Notification"),
											this.getName());
				}
			}
		}
	}
	
	protected List getChilds(List childs, Path p) {
		childs.add(p);
		if(p.attributes.isDirectory() && !p.attributes.isSymbolicLink()) {
			p.attributes.setSize(0);
			for(Iterator i = p.list(false, true).iterator(); i.hasNext();) {
				Path child = (Path)i.next();
				child.setLocal(new Local(p.getLocal(), child.getName()));
				this.getChilds(childs, child);
			}
		}
		return childs;
	}

	protected void reset() {
		this.size = 0;
		for(Iterator iter = this.getJobs().iterator(); iter.hasNext();) {
			this.size += ((Path)iter.next()).attributes.getSize();
		}
	}

	protected void process(Path p) {
		p.download();
	}
}