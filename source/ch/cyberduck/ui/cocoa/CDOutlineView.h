/*
 *  Copyright (c) 2008 David Kocher. All rights reserved.
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

#import <Cocoa/Cocoa.h>
#import "QuickLook.h"
#define QLPreviewPanel NSClassFromString(@"QLPreviewPanel")

@interface CDOutlineView : NSOutlineView {

@private
	BOOL mBrowserWasDoubleClicked;
	int mBrowserEditingColumn;
	int mBrowserEditingRow;

	NSMutableString *select_string;
	NSTimer *select_timer;
	NSTimer *autoexpand_timer;

	BOOL quickLookAvailable;
}

- (void)handleBrowserClick:(id)sender;

@end