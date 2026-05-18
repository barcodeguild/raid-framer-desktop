
# Raid Framer 2.0+
Track ArcheRage PvP Combat Metadata in Real-Time, Study and Visualize Data to Learn what Your Team Could be Doing Better to Succeed!

<img width="535" height="755" alt="image" src="https://github.com/user-attachments/assets/d8e5fbad-20e3-4285-b48e-ea5b5bf270f3" />


Watch the Gource for this Repo -> https://github.com/barcodeguild/raid-framer-desktop/raw/refs/heads/main/documentation/gource.mp4](https://github.com/barcodeguild/raid-framer-desktop/raw/refs/heads/main/documentation/gource.mp4 <-

## Overall Features
  - Tracks in-game combat events in real-time, and provides detailed analytics, metrics and visualizations.
  - Help your team get better at PvP battles. See what you're doing right, and what your team needs to improve upon.
  - Compare your performance against your teammates, and see how you individually stack-up against the raid as a whole.
  - Runs alongside the game as an overlay app. Has Lua extensions into the game APIs. One component provides the GUI, the other provides visibility into the game data. The two components work together to provide the features of the app.
  - Track damage, healing and CC with real-time metering. Separates PvP and PvE events. (See checkbox for "Separate PvP and PvE damage" in settings to enable this feature.)
  - Pretty data visualizations and graphs for the above. Optional mini-graph overlay for personal metrics. Pie charts that show RvR break-downs. (work in progress and does require some manual confirmation of automatically detected player faction status)
  - Raid management features for large raids and out-of-raid same-faction participation tracking. Considerable CPU usage occurs after 100 v 100 engagements, but large 200+ player PvP is supported.
  - Attendance copy-to-clipboard feature with minimum participation thresholds. Supports including same-faction out-of-raid players who meet the participation threshold. (Defaults to 25k PvP Damage OR 25k PvP Heals OR At least 25 CC stacks delivered to enemy players to count for attendance. These thresholds will be customizable in the future.)
  - Player class detection: Raid framer uses the timestamps of recently-casted spells, and a mapping of all castable spells to guess what skill-trees characters are playing. It's over 90% accurate. Also uses the Lua API to assert player builds more accurately over time to fill in some remaining gaps. (Note that accuracy improves with time because of this!)
  - Tabbed-detection: Optionally can close game overlays (since these are just regular windows) when the game itself closes, or when it's minimized. This is so overlays don't get in the way of other stuff on your screen. (Note: If for some reason your overlays aren't showing, make sure to try un-checking this option!)
  - Red, Blue and Pink dots indicate faction.
  - Tracks damage of the pets too (because we love the pets!).
  - Clean install and uninstall process. Newer versions can be installed over the previous and the lua addon gets copied over automatically. (it does sha256 hashing to make sure the files match)
  - Help your team grow by studying the combat logs of each battle. Know when you're being cheesed by pay-to-win gear, cheap tricks, and be able to prove it. You already know these games are cursed, let's them less cursed together friends. <3

<img width="3136" height="1475" alt="marcala_siege_022026" src="https://github.com/user-attachments/assets/f1def92f-b50f-4f43-88b8-73e916845e5e" />

<img width="752" height="620" alt="image" src="https://github.com/user-attachments/assets/241b63c0-280b-4250-ab84-8d9f051b989b" />

## Future Features Coming Soon
  - Dragon breath tracking and pet damage.
  - Once explicitly enabled in [Settings], Pressing and Holding TAB to bring up the official game schedule. The reason you have to hold tab is so this doesn't interfere with the in-game tab-targetting system. No part of this addon is designed to interfere with or automate gameplay.
  - Internationalization support targeting English, Korean, Brazilian, Russian, Chinese, German, Australian, Pirate Speak, Cat Speak, and Trash Talk.
  - Many features are in development and testing. We need to be sure that they won't affect stability of the app or the game.

## What counts as CC?
This has been a huge headache friends, let me tell you. ~ The truth is everyone wants it to work differently, so the solution ended-up being to do break-downs by debuff. You'll notice that there's a "PvP CC" column. When you see the phrase CC used throughout the APP, RF is referring to any spells or utility items that:
  - That inhibit or reduce player movement.
  - That force the player to move or physics-flings player(s) in a direction they did not intend.
  - That prevent, slow or break the casting of spells.
  - That entrap or cause the players camera to face another direction.
  - That affect the ability for a player to target another player.
  - That disable the user's weapon or the ability to use their skills, gliders, or potions.

Examples are: Silence, Distressed, Slow, Tripped, Stunned, Impaled, Frozen, Petrified, Knocked-Down, Slept, Feared, Provoked, Bubble Trap, Snare, Shackle, Overpowered, Wraiths Curse, Earthen Grip (Mud Hands), and numerous more:

  - Example 1: A friendly player casts [Stillnesss] on a clump of 7 enemy players. +7 points are attributed to that action as long as the debuff was indeed applied to all 7 players.
  - Example 2: A friendly player with [Defense] tree does the [Distressed] combo on a group of 3 enemy players. (The shield slam counts as +1, distress applied +3, provoked applied to the three targets is another +3) IE it's counting all the stacks of CC applied.
  - Example 3: A friendly player lays a [Bubble Trap] in the choke on Freed. 7 enemies get caught in the bubble. However, people can get ree-caught in the bubble, so out of those, 4 are still stuck in the bubble for another raise into the air. (+7 inital points, +4 on second raise for +11 points total) Same is true for other spells that have debuffs that can get re-applied to targets. The re-application is extra points, because it resulted in even more crowd-control time.

Finally, how can I see charms though? Yep, so where you want to go to is the "Battle Summary" overlay. It has additional metrics there about specific debuffs, that don't meet the criteria for being counted as "CC" but are still important to track. For example, charms. Charms don't meet the criteria for being counted as CC, because they don't inhibit player movement or actions, but they are still very important to track in PvP.

## Pretty Graphs
  - Visualize PvP battles in damage, heals and cc-delivered over time by player.
  - Optional mini-graph overlay for personal metrics.
  - Pie charts that show faction break-downs for [Haranya], [Nuia], and the [Pirates].
  - The idea behind the charts is to help raid leads identify what the raid is doing right, and what they could be doing better.

<img width="1181" height="183" alt="image" src="https://github.com/user-attachments/assets/7a0cb306-0fb6-4b46-8284-f722f1d08b99" />

## Custom RF Format
Raid Framer exposes its own game event logging format for ArcheAge called RF. Other devs may be interested in RF format for their own addons and tools because it's easy to implement quickly. Compared to the game's built-in logging format, the .rf format has the following improvements:
  - The format is text-based and each line of the log is written in a universal serialization format. (chronological lines of JSON)
  - All timestamps are cleansed and saved in UTC time, not the user's own local time, for privacy reasons. (This is a common practice in the software industry, and makes it easier for developers to work with the logs without having to worry about timezone conversions.)
  - Has custom message types and metadata fields that are not present in the original log format surrounding buff applications, pet ownership, world events, raid frames and presence, player classes, player class changes, duel start/ended events, player deaths (surprisingly not in the orginal logs), portal ownership, vehicle ownership, death attributions, and evnironmental damage.
  - Resolves certain key pieces of information that were missing from the logs such as who was responsible for inflicting a debuff. So instead of "X was struck by a Charmed debuff", you might see something like "Y applied a Charmed debuff to X". Another example is Library Greatclub. If someone casts Library GreatClub on the aggro holder, the default logging format doesn't show who inflicted the buff.
  - Logs are cleaned of chat effects and colors that provided no additional benefit, making them easier to parse by future addons/tools that read this new format. Also removes the need to parse and filter logs via complex regular expressions. (Could potentially reduce the compute-time footprint of working with game logs. This was as actually done to make it easier for other developers to work with the logs)

## Known Issues  / Feedback
Please give honest and constructive feedback on the project. This app has taken quite a lot of time to write. Many hours of slaving Reoky away. Including code ideas from other devs like Zilus and Xizde, with help with testing from our other friends on the East Faction. ~ Some ideas may not be possible to implement, while others will. You should post your ideas on the official ArcheRage forums thread, or open issues on this Github just so they get tracked. With all that said, here's some known issues and quirks:
  - There's a known issue with Intel integrated graphics drivers. Even if you have another dedicated card, you may have to update your Intel drivers. This app uses Skia to render the transparent windows. The hardware acceleration that Skia uses requires up-to-date graphics drivers. Please ensure that your graphics drivers are updated. Intel ARC support is still new and the drivers have to be a recent (https://www.intel.com/content/www/us/en/download-center/home.html). AMD (https://www.amd.com/en/support/download/drivers.html) and Nvidia (https://www.nvidia.com/en-us/drivers/) also.
  - Raid Framer uses a significant amount of memory. It's a Java APP with a GUI under-the-hood. If you know ArcheRage, combat.log files can get rather huge.. often 100MB+.. and that's data kept in-memory between fights. It is recommended to have at least 16GB of RAM (in total) with a high FSB/Memory clock for best performance and enough storage to save logs that on average (after a 100v100 fight will be about 100-200MB per 30 minutes) It's mostly CPU and in-memory transformations on data, and not so much graphical rendering besides the transparency, graphs and the charts (that use recycler-pattern/lazy list view rendering)
  - Why not constantly write-out data to the disk that you don't need to keep in memory, you might ask? There's definitely improvements to be made in this area friends. We could potentially do more in the way of memory offload in the future. However, this app PREFERS using RAM over DISK IO in general. In-memory transforms are fast. Some friends hard-drives are much slower. Too many disk read/writes are likely to produce lag spikes (and this happened while I was testing) as compared to working with that same data in-memory. IE if we copy 100MB to the disk during a PvP that would likely cause a lag spike. No design is final friends. Send ideas.

# Building The Code
This is a Kotlin Multiplatform project that targets Windows. It's been tested on Windows 11. It's also worked for at least one person on Windows 10. This code uses the Jetpack Compose framework for the UI. (Similar to a modern Android APP) The Lua companion does not need to be compiled or built. It's a set of scripts that gets copied to the game folder and optionally loaded by the game process. (Be sure to enable it in the character select wrench menu!) The Lua files are copied to the game folder after the first launch of Raid Framer automatically. So, you close game, install RF 2.0, open RF to finalize installation, then open the game. From them on you can open the game before or after launching RF, as the lua addon was already copied over.
  - Some people prefer to build the code themselves, so they can improve the code / see how it works / design their own addons. I tried to make this as easy as I could by providing Gradle build scripts.
  - For those familiar with Android development, this app's structure closely mimics the build process, except deploys to Windows and packages an MSI installer instead of an APK.
  - It also implements the Logcat logging format for debug logging because why not. You should be able to open the debug logger with any tools that support reading Logcat logs. (including any text editors)
  - The app's use of Realm Database has been deprecated in favor of SQLite. Everyone knows SQL. The database is written here: %USERPROFILE%\.raidframer\raidframer.db (If something goes wrong with the app, you can delete this file to reset the database. This will cause you to lose player cache and settings, but it can be useful for troubleshooting. The app will simply create a new one if it doesn't find one.)
  - The debug log is written to %USERPROFILE%\.raidframer\debug.log in Logcat format.

<img width="911" height="434" alt="image" src="https://github.com/user-attachments/assets/f00db6c6-3856-4b73-a417-3ae907f66e8c" />

<img width="1556" height="671" alt="image" src="https://github.com/user-attachments/assets/335a2c85-07ef-4dc2-88cc-d8ea344b6f7d" />

## SO ANYWAYS.. ABOUT THE BUILDING..
IntelliJ has now been consolidated into a single 'version'. There used to be Pro and Community versions. Get the latest IntelliJ (https://www.jetbrains.com/idea/download/?section=windows) and open the project with that. The IDE should recognize the Gradle build files and pick up on the project's sub-modules.

The main source is in:
* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.

The Lua Addon code is in: resources/RaidFramer (It's literally just the folder with the Lua files that gets copied to ArcheRage/Addon/RaidFramer) at runtime. This prevents the need for sharing around hacky RAR files outside of version control, and in theory should be less prone to error. The user does have to pick/confirm their ArcheRage directory though!

Final note about running the app from the IDE:
- The run configuration for the app should be detected automatically. Make sure you picked the root of the folder when opening the project and not one of the sub-folders. But if for some reason you just don't see it:
- Run Config: desktopRun -DmainClass=com.reoky.raidframer.MainKt --quiet
- Clean Build Config: clean desktopRun -DmainClass=com.reoky.raidframer.MainKt --quiet
- Package MSI Installer: composeApp:packageDistributionForCurrentOS

Note also that the debug version of the app will share the same database and settings as the release version. So if you run the debug version, it will use the same player cache and settings as the release version. This is because they are technically the same app, just with different build configurations. So be careful when running the debug version, as it can potentially mess with your settings and player cache that you use for the release version if you have both installed

# LICENCE / WARRANTY

Copyright 2026 ~ Reoky ~

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. You must provide a link to the original work if you are sharing a fork of this code. You may not use this code for evil, for hate, malice or to bully people.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
