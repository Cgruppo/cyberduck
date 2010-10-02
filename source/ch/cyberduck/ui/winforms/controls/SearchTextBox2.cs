﻿using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Drawing;
using System.Data;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace Ch.Cyberduck.ui.winforms.controls
{
    [DefaultEvent("TextChanged")]
    public partial class SearchTextBox2 : UserControl
    {
        public delegate void TextChange(object sender, EventArgs e);

        private readonly Font _defaultFont;
        private readonly Font _placeHolderFont;

        private ContextMenuStrip _contextMenu;

        private string _currentText = String.Empty;
        private string _placeHolderText = "Suchen";

        public SearchTextBox2()
        {
            _defaultFont = SystemFonts.MessageBoxFont;
            Font = _defaultFont;
            _placeHolderFont = new Font(Font, FontStyle.Italic);

            InitializeComponent();

            SuspendLayout();

            Paint += UserControl1_Paint;
            Resize += UserControl1_Resize;

            textBox.Multiline = false;
            textBox.BorderStyle = BorderStyle.None;
            textBox.Enter += textBox_Enter;
            textBox.Leave += textBox_Leave;
            textBox.Click += textBox_Click;
            textBox.KeyDown += textBox_KeyDown;
            textBox.TextChanged += textBox_TextChanged;

            EnabledChanged += SearchTextBox_EnabledChanged;

            xPictureBox.Image = ResourcesBundle.search_inactive;
            xPictureBox.Click += xPictureBox_Click;
            xPictureBox.Visible = true;

            keyStrokeTimer.Interval = 250;
            keyStrokeTimer.Tick += keyStrokeTimer_Tick;

            Controls.Add(textBox);
            Controls.Add(xPictureBox);

            ResumeLayout(false);

            textBox.ForeColor = Color.Gray;
            textBox.Font = _placeHolderFont;
            textBox.Text = _placeHolderText;
        }

        [Description("The placeholder is displayed when the field is empty and does not have the focus."),
         Category("SearchTextBox"), DefaultValue("Suchen")]
        public string PlaceHolderText
        {
            get { return _placeHolderText; }
            set
            {
                _placeHolderText = value;
                textBox.ForeColor = Color.Gray;
                textBox.Font = _placeHolderFont;
                textBox.Text = _placeHolderText;
            }
        }

        [Description("Assign a context menu to set search options"), Category("SearchTextBox")]
        public ContextMenuStrip OptionsMenu
        {
            get { return _contextMenu; }
            set
            {
                _contextMenu = value;
                //_cmPictureBox.Visible = _contextMenu != null;
            }
        }

        public new Font Font
        {
            get { return textBox.Font; }
            set { textBox.Font = value; }
        }

        public new string Text
        {
            get { return textBox.Text != _placeHolderText ? textBox.Text : String.Empty; }
            set
            {
                if (!textBox.Focused && String.IsNullOrEmpty(value))
                {
                    textBox.Text = _placeHolderText;
                    textBox.ForeColor = Color.Gray;
                    textBox.Font = _placeHolderFont;

                }
                else
                {
                    textBox.Text = value;
                    textBox.ForeColor = Color.Black;
                    textBox.Font = _defaultFont;
                }
            }
        }

        [Description(
            "If greater than 0, waits this amount of miliseconds after a key has been pressed before raising the TextChanged event."
            ), Category("SearchTextBox"), DefaultValue(250)]
        public int KeyStrokeDelay
        {
            get { return keyStrokeTimer.Interval; }
            set { keyStrokeTimer.Interval = value; }
        }

        public new event TextChange TextChanged;

        private void cmPictureBox_MouseClick(object sender, MouseEventArgs e)
        {
            if (_contextMenu != null)
            {
                Point loc = Parent.PointToScreen(Location);
                _contextMenu.Show(loc.X + 3, loc.Y + Height);
            }
        }

        private void SearchTextBox_EnabledChanged(object sender, EventArgs e)
        {
            if (!Enabled)
            {
                BackColor = Color.LightGray;
                textBox.BackColor = Color.LightGray;
            }
            else
            {
                BackColor = Color.White;
                textBox.BackColor = Color.White;
            }
        }

        /// <summary>
        /// Occurs when the Text property value changes.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void textBox_TextChanged(object sender, EventArgs e)
        {
            if (keyStrokeTimer.Enabled)
                keyStrokeTimer.Stop();
            if (textBox.Text == String.Empty || textBox.Text == _placeHolderText)
            {
                if (_currentText != textBox.Text && textBox.Text == String.Empty && TextChanged != null)
                {
                    _currentText = textBox.Text;
                    TextChanged(this, e);
                }
                //xPictureBox.Visible = false;
                xPictureBox.Image = ResourcesBundle.search_inactive;
            }
            else
            {
                xPictureBox.Image = ResourcesBundle.search_active;
                //xPictureBox.Visible = true;
                if (_currentText != textBox.Text)
                {
                    _currentText = textBox.Text;
                    keyStrokeTimer.Start();
                }
            }
        }

        private void keyStrokeTimer_Tick(object sender, EventArgs e)
        {
            if (textBox.Text != _placeHolderText && TextChanged != null)
                TextChanged(this, e);
            keyStrokeTimer.Stop();
        }


        private void xPictureBox_Click(object sender, EventArgs e)
        {
            textBox.Text = String.Empty;
            textBox.Focus();
        }

        private void textBox_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.KeyCode == Keys.Escape)
            {
                textBox.Text = String.Empty;
            }
        }

        private void textBox_Click(object sender, EventArgs e)
        {
            textBox.SelectAll();
        }

        private void textBox_Enter(object sender, EventArgs e)
        {
            if (textBox.Text == _placeHolderText)
            {
                textBox.ForeColor = Color.Black;
                textBox.Font = _defaultFont;
                textBox.Text = string.Empty;
            }
            else
            {
                textBox.SelectAll();
            }
        }

        private void textBox_Leave(object sender, EventArgs e)
        {
            if (textBox.Text == string.Empty)
            {
                textBox.ForeColor = Color.Gray;
                textBox.Text = _placeHolderText;
                textBox.Font = _placeHolderFont;
            }
        }

        private void UserControl1_Resize(object sender, EventArgs e)
        {
            int offset = _contextMenu != null ? 10 : 0;
            textBox.Size = new Size(Width - 22, Height - 2);
            //textBox.Size = new Size(Width - 43 - offset, Height - 6);
            //textBox.Location = new Point(23 + offset, 2);
            textBox.Location = new Point(3, (Height / 2) - (textBox.Size.Height / 2) + 1);

            xPictureBox.Size = new Size(18, Height - 2);
            xPictureBox.SizeMode = PictureBoxSizeMode.CenterImage;
            xPictureBox.Location = new Point(Width - 19, 1);
            //xPictureBox.Location = new Point(Width - 18, (Height - 15)/2);
            xPictureBox.BackColor = textBox.BackColor;
        }

        private void UserControl1_Paint(object sender, PaintEventArgs e)
        {
            e.Graphics.FillRectangle(new SolidBrush(textBox.BackColor), 1, 1, ClientRectangle.Width - 2,
                                     ClientRectangle.Height - 2);
            //e.Graphics.DrawImage(Resources.searchg, 3, (Height - 18)/2, 18, 18);            
            ControlPaint.DrawBorder(e.Graphics, ClientRectangle, Color.Gray, ButtonBorderStyle.Solid);
        }

        public void select(int start, int length)
        {
            textBox.Select(start, length);
        }
    }
}
