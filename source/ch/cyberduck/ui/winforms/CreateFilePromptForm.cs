﻿// 
// Copyright (c) 2010-2011 Yves Langisch. All rights reserved.
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
using ch.cyberduck.core.i18n;
using Ch.Cyberduck.Ui.Controller;

namespace Ch.Cyberduck.Ui.Winforms
{
    public partial class CreateFilePromptForm : PromptForm, ICreateFilePromptView
    {
        public CreateFilePromptForm()
        {
            InitializeComponent();

            Text = Locale.localizedString("Create new file", "File");
            Button cancelBtn = new Button
                                   {
                                       AutoSize = true,
                                       Size = new Size(75, okButton.Size.Height),
                                       TabIndex = 5,
                                       Text = Locale.localizedString("Cancel"),
                                       UseVisualStyleBackColor = true,
                                       Anchor = AnchorStyles.Bottom | AnchorStyles.Left
                                   };
            tableLayoutPanel.Controls.Add(cancelBtn, 1, 2);

            label.Text = Locale.localizedString("Enter the name for the new file:", "File");
            okButton.Text = Locale.localizedString("Create", "File");

            // cancelButton is the 'Edit' button now
            cancelButton.DialogResult = DialogResult.Yes;
            cancelButton.Text = Locale.localizedString("Edit", "File");

            CancelButton = cancelBtn;
        }
    }
}