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
using System.Drawing;
using System.Windows.Forms;
using ch.cyberduck.core;

namespace Ch.Cyberduck.Ui.Controller
{
    internal abstract class FileController : WindowController<IPromptView>
    {
        protected readonly BrowserController BrowserController;
        private string _input = string.Empty;

        public FileController(IPromptView view, BrowserController browserController)
        {
            BrowserController = browserController;
            View = view;

            View.ValidateInput += ValidateInput;
        }

        public virtual Bitmap IconView
        {
            get { return IconCache.Instance.IconForFilename("_unknown", IconCache.IconSize.Large); }
        }

        public virtual Path Workdir
        {
            get
            {
                if (BrowserController.SelectedPaths.Count == 1)
                {
                    return BrowserController.SelectedPath.getParent();
                }
                return BrowserController.Workdir;
            }
        }

        protected string PrefilledText
        {
            get { return _input; }
            set { _input = value; }
        }

        protected Session Session
        {
            get { return BrowserController.getSession(); }
        }

        protected virtual bool ValidateInput()
        {
            string t = View.InputText.Trim();
            if (t.IndexOf('/') != -1)
            {
                return false;
            }

            if (!string.IsNullOrEmpty(t))
            {
                Path file = PathFactory.createPath(Session, Workdir.getAbsolute(),
                                                   t, AbstractPath.FILE_TYPE);
                return !file.exists();
            }
            return false;
        }

        public abstract void Callback(DialogResult result);

        public void Show()
        {
            View.InputText = PrefilledText;
            View.IconView = IconView;
            Callback(View.ShowDialog(BrowserController.View));
        }
    }
}