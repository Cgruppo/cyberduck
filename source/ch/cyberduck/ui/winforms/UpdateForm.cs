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
using System;
using System.ComponentModel;
using System.Drawing;
using System.Windows.Forms;
using ch.cyberduck.core;
using ch.cyberduck.core.i18n;
using Ch.Cyberduck.ui.controller;
using Ch.Cyberduck.Ui.Controller;
using Ch.Cyberduck.Ui.Winforms;
using wyDay.Controls;

namespace Ch.Cyberduck.ui.winforms
{
    public partial class UpdateForm : BaseForm, IUpdateView
    {
        public UpdateForm()
        {
            InitializeComponent();

            Closing += delegate {
                               updater.Cancel();
                           };

            ConfigureUpdater();

            pictureBox.Image = IconCache.Instance.IconForName("cyberduck", 64);
            newVersionAvailableLabel.Text = Locale.localizedString("A new version of %@ is available!").Replace("%@",
                                                                                                                Preferences
                                                                                                                    .
                                                                                                                    instance
                                                                                                                    ().
                                                                                                                    getProperty
                                                                                                                    ("application.name"));

            //force handle creation to make the updater work
            IntPtr intPtr = Handle;
            OnLoad(new EventArgs());            
        }

        public override string[] BundleNames
        {
            get { return new[] {"Sparkle"}; }
        }

        public void CheckForUpdates(bool background)
        {
            if (!background)
            {
                UpdateStatusLabel("Looking for newer versions of Cyberduck.", false);
                tableLayoutPanel.RowStyles[7].SizeType = SizeType.AutoSize;
                SetStatusChecking(true);
                updater.ForceCheckForUpdate(true);
                Show();
            }
            else
            {
                updater.ForceCheckForUpdate(true);
            }
        }

        public bool AboutToInstallUpdate
        {
            get { return updater.UpdateStepOn == UpdateStepOn.UpdateReadyToInstall; }
        }

        private void ConfigureUpdater()
        {
            //updater.DaysBetweenChecks = 500;
            //updater.WaitBeforeCheckSecs = 1000000;

            updater.ContainerForm = this;
            updater.KeepHidden = true;
            updater.Visible = false;
            updater.UpdateType = UpdateType.DoNothing;
            updater.UpdateAvailable += delegate
                                           {
                                               laterButton.Visible = true;
                                               installButton.Text = Locale.localizedString("Install and Relaunch",
                                                                                           "Sparkle");

                                               tableLayoutPanel.RowStyles[7].SizeType = SizeType.Percent;
                                               tableLayoutPanel.RowStyles[7].Height = 100;

                                               string currentVersion =
                                                   Preferences.instance().getProperty("application.version");
                                               string newVersion = updater.Version;

                                               //todo lookup string needs to be changed as soon as the Sparkle.string files are available
                                               versionLabel.Text = Locale.localizedString(
                                                   "%1$@ %2$@ is now available (you have %3$@). Would you like to download it now?")
                                                   .Replace("%1$@",
                                                            Preferences.instance
                                                                ().getProperty
                                                                ("application.name"))
                                                   .Replace("%2$@", newVersion).Replace("%3$@", currentVersion);

                                               SetStatusChecking(false);
                                               statusLabel.Text = "Update available";
                                               changesTextBox.Text = updater.Changes.Replace("\n", "\r\n");
                                               //installButton.Enabled = true;
                                               Show();
                                           };

            // error cases
            updater.CheckingFailed +=
                delegate(object sender, FailArgs args) { UpdateStatusLabel(args.ErrorTitle, true); };

            updater.UpdateFailed +=
                (sender, args) =>
                UpdateStatusLabel(args.ErrorTitle, true);

            updater.DownloadingOrExtractingFailed +=
                (sender, args) =>
                UpdateStatusLabel(args.ErrorTitle, true);
            // end error cases

            updater.ProgressChanged += delegate(object sender, int progress) { progressBar.Value = progress; };

            updater.BeforeDownloading += (sender, args) =>
                                             {
                                                 UpdateStatusLabel("Downloading new version.", false);
                                                 progressBar.Style = ProgressBarStyle.Continuous;
                                                 progressBar.Value = 0;
                                                 progressBar.Visible = true;
                                             };

            updater.UpToDate += delegate
                                    {
                                        progressBar.Visible = false;
                                        UpdateStatusLabel(Locale.localizedString("You're up to date!", "Sparkle"), false);
                                    };

            updater.ReadyToBeInstalled += delegate
                                              {
                                                  progressBar.Visible = false;
                                                  statusLabel.Text = "Installing new version.";
                                                  updater.InstallNow();
                                              };

            updater.BeforeChecking += delegate
                                          {
                                              laterButton.Visible = false;
                                              installButton.Text = Locale.localizedString("OK");
                                              progressBar.Style = ProgressBarStyle.Marquee;
                                              progressBar.Visible = true;
                                          };
        }

        private void UpdateStatusLabel(String status, bool error)
        {
            if (error)
            {
                progressBar.Visible = false;
                laterButton.Visible = false;
                installButton.Text = Locale.localizedString("Cancel");
            }
            statusLabel.Visible = true;
            statusLabel.ForeColor = error ? Color.Red : Color.FromKnownColor(KnownColor.ControlText);
            statusLabel.Text = error ? "Error: " + status : status;
        }

        public void SetStatusChecking(bool checking)
        {
            statusLabel.Visible = checking;
            progressBar.Visible = checking;
            newVersionAvailableLabel.Visible = !checking;
            versionLabel.Visible = !checking;
            releaseNotesLabel.Visible = !checking;
            changesTextBox.Visible = !checking;
        }

        private void donateButton_Click(object sender, EventArgs e)
        {
            if (updater.UpdateStepOn == UpdateStepOn.UpdateAvailable)
            {
                updater.InstallNow();
                laterButton.Enabled = false;
                installButton.Enabled = false;
            }
            else
            {
                Close();
            }
        }

        private void laterButton_Click(object sender, EventArgs e)
        {
            Close();
        }
    }
}