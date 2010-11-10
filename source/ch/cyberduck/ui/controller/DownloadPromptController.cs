﻿// 
// Copyright (c) 2010 Yves Langisch. All rights reserved.
// http://cyberduck.ch/
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
// 
// Bug fixes, suggestions and comments should be sent to:
// yves@cyberduck.ch
// 
using ch.cyberduck.core;

namespace Ch.Cyberduck.Ui.Controller
{
    internal class DownloadPromptController : TransferPromptController
    {
        public DownloadPromptController(WindowController parent, Transfer transfer) : base(parent, transfer)
        {
            ;
        }

        protected override string TransferName
        {
            get { return "Download"; }
        }

        public override TransferAction prompt()
        {
            TransferPromptModel = new DownloadPromptModel(this, Transfer);
            return base.prompt();
        }
    }
}