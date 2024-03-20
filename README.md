# .:Raid Framer:.

**Raid Framer** is an open-source overlay add-on for ArcheRage. (Not to be confused with ArcheAge) It features damage tracking, debuff monitoring and seamless integration. It's designed as a front-end to the game's built-in logging system, allowing you to read log data in a visual manner without endless scrolling through the in-game menus to do the same.

My hope is that this tool will bring something to the table in-terms of damage trackers. It draws in inspiration from other damage trackers written by Xizde, Oturan, and others.

# Getting the Add-on

There are two ways to get **Raid Framer**. The first way is to download the source-code and build it yourself using Intellij and the included Gradle wrapper from the command-line. **Intellij Community Edition** is a free IDE for working with Java and Kotlin projects. You can get that here: https://www.jetbrains.com/idea/download

The second way (with understanding that most players don't want a hassle) there's also a releases section to this Github that has pre-built versions of **Raid Framer**. Included is a setup program that automatically installs the runtime environment, resources and executable. It will place a shortcut on your desktop that you can use to launch the overlay, and you can quit it at any time from the system tray menu or by hitting the X on the combat overlay.

## Some Notable Features

This overlay can automatically hide itself when you tab out of the game so that it's not covering other windows. It also has some experimental functionality to do the same for obstructed in-game windows (like the faction menu the play's inventory) It does this based on what color is behind the overlay and also via *garbage text* recognition. The overlay is transparent so the colors of what's behind it bleed through the transparency and this allows us to detect the inventory color. The actual images themselves after being run through recognition are discarded.

- Re-settable damage and heals counter.
- Independent damage logs for each player that scroll from the top.
- Click player names to see current cast bar, status, and debuffs.
- Shrink debuffs window to show only debuffs to save UI space.
- Window positions are saved locally. The config entries are saved inside a Realm file. You can open these with **Realm Studio** from **MongoDB** if you want.
- **Shift + Click** to drag overlays to any position on the screen you'd like. **Grab the edges** of overlays to resize them. The option in **Settings** lets you lock this so you don't accidentally your entire overlays.
- Combat log seeking finds your "Combat.log" file automatically so you don't have to manually enter it. (Although you do have to select the correct one from the list.)
- Debuff icons on the buff bar.
- Retribution Tracking the flashes names of players using the defense skill and are either healing or dealing damage simultaneously.


More features in the future. Please provide me feedback friends so I can code stuff for us.

If you add a feature to the code, please consider making a pull request! I will be watching for feature requests, and listening to people's ideas about what to add!

## Warranty

This software is open-source, it doesn't really come with any warranty or guarantee of any kind, but you are welcome to try it friends! All I can say is I do try to make it good, even though I'm sure there will be bugs. Please report those. ~


## LICENSE

Copyright 2004 ~ Reoky ~

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

