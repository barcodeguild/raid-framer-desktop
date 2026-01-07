
# Raid Framer 2.0 Features

## Overall Features
  - Game Overlay with a Lua Companion Addon : One component provides the GUI, the other provides real-time data from the game.
  - Damage, Heal and CC metering. (This is like.. the main feature..)
  - Pretty data visualizations for the above.
  - Raid detection, management, and metrics. Up-to 200 vs 200 battles supported. Factions are determined via subtractive logic. However, some participants may need to be manually placed until a Lua/game API is opened for reading faction status. Essentially the APP is just using subtractive logic and correlation for now.
  - Attendence copy feature with minimum participation thresholds. Supports up to 200 players. Basically just looks for a minimum participation of at least 25k PvP Damage OR 25k PvP Heals OR At least 25 CC stacks delivered to enemy players to count for attendence. (We might need to tweak this in the future.) Basically just works by caching nearby players, substracting those from hosile factions, subtractinng those from personal filters, and looking for the minimum performance criteria to be counted. (Which there has to be a threshold because otherwise it would count anyone in the area whether they were there for the raid or not.)
  - Build/spec detection: Raid framer uses the timestamps of recently casted spells, and a mapping of spells to guess what skill-trees characters are playing. It's over 90% accurate, but we might switch to the Lua API that does the same in the future.
  - Support for separating PvE and PvP damage via a (local-only) cache of players that the addon discovers over time. (Note that accuracy improves with time because of this!)
  - Tabbed-detection: Will close game overlays (since these are just regular windows) when the game itself closes, or when it's minimized. This is so overlays don't get in the way of other stuff on your screen.
  - Once explicitly enabled in [Settings], Pressing and Holding TAB to bring up the official game schedule. The reason you have to hold tab is so this doesn't interfere with the in-game tab-targetting system. No part of this addon is designed to interfere with or automate gameplay.
  - Internationalization support targetting English, Korean, Brazilian, Russian, Chinese, German, Australian, and Pirate

## What counts as CC?
This has been a huge headache friends, let me tell you. ~ The truth is everyone wants it to work differently, so the solution ended-up being to do breakdowns by skill. However, you'll notice that there is still a "PvP CC" column. When you see the phrase CC used throughout the APP, RF is referring to any spells or utility items that:
  - Inhibit or reduce player movement.
  - Prevent or slow the casting of spells, or confine the casting in a specific direction.
  - That entrap or cause the players camera to face another direction.
  - That affect the ability for a player to target another player.
  - That force the player to move or physics fling players in a direction they did not intend to move.
  - That disable the user's weapon or ability to use their skills, gliders, or potions.

Examples are: Silence, Distressed, Slow, Tripped, Stunned, Impaled, Frozen, Petrified, Knocked-Down, Slept, Feared, Provoked, Bubble Trap, Snare, Shackle, Overpowered, Wraiths Curse, Earthen Grip (Mud Hands), and numerous more:

  - Example 1: A friendly CC Mage casts a Stillnesss on a clump of 7 enemy players. +7 points are attributed to that action as long as the debuff was indeed applied to all 7 players.
  - Example 2: A friendly Melee with Defense tree does the distress combo on a group of 3 enemy players. (The shield slam counts as +1, distress applied +3, provoked applied to the three targets is another +3) IE it's counting all the stacks of CC applied.
  - Example 3: A friendly Mage lays a Bubble Trap in the choke on Freed. 7 enemies get caught in the bubble. However, people can get ree-caught in the bubble, so out of those, 4 are still stuck in the bubble for another raise into the aie. (+7 inital points, +4 on second raise for +11 points total) Same is true for other spells that have debuffs that can get re-applied to targets. The re-application is extra points, because it resulted in even more crowd-control time.

Finally, how can I see charms though? Yep, so where you want to go to is the "Battle Summary" overlay. It has additional metrics there and includes charmed debuff filtering specifically, including also a "charms by raid" pie chart.

## Pretty Graphs
  - Visualize PvP battles in damage, heals and cc-deliever over time by player.
  - Optional mini-graph overlay for personal metrics.
  - Pie charts that show RvR break-downs. (work in progress and does require some manual confirmation of player faction status)
  - The idea behind the charts is to help raid leads idetify what the raid is doing right, and what they need to improve upon as a group to get better at applying their stratedgy.
  - Reference comparison allows for comparing the performance of individual players against the rest of the raid as a reference. (still not working)

## Custom RF Format
  - Similar concept to the ArcheRage combat.log file format with improvements that make the format more useful/useable for addons, and for sharing with others.
  - All timestamps are cleansed and saved in UTC time, not the user's own local time, for privacy reasons.
  - RF formatted logs can be exported in "time slices" to scope the log to a specific battle or duel.
  - This format includes data and fields that the original log format was missing. For example, which character was responsible for applying a debuff to another character, that the original format does not have. So instead of "X was struck by a Charmed debuff", you might see something like "Y applied a Charmed debuff to X".
  - Separatation of PvE versus PvP damage, allowing you to export PvP-only battle slices.
  - Logs are cleaned of chat effects and colors that provided no additional benefit, making them easier to parse by future addons/tools that read this new format.
  - Logs are saved in a universal serialization format (chronological lines of JSON with web-oriented typing) that removes the need to parse them with complex regular expressions, which should hopefully reduce the compute-time footprint of working with game logs.

## Known Issues  / Feedback
Please give honest and constructive feedback on the project. This app has taken quite a lot time to write. Many hours of slaving Reoky away. Including code ideas from other devs like Zilus and Xizde, with help with testing from our other friends on the East Faction. ~ Some ideas may not be possible to implement, while others will. You should post your ideas on the offical AR forums thread, or open issues on this Github just so they get tracked. With all that said, here's some known issues and quirks:
  - There's a known issue with Intel integrated graphics drivers. Even if you have a dedicated card, if you also have an Intel Graphics chip, you may have to update your drivers. This app uses Skia to render the transparent windows. The hardware acceleration that Skia uses requires up-to-date graphics drivers. If Raid Framer simply does not open, please ensure that your graphics drivers are updated. Intel ARC drivers have to be a recent version (https://www.intel.com/content/www/us/en/download-center/home.html). AMD (https://www.amd.com/en/support/download/drivers.html) and Nvidia (https://www.nvidia.com/en-us/drivers/) also.
  - Raid Framer uses a significant amount of memory. It's a Java APP with a GUI under-the-hood. If you know ArcheRage, combat.log files can get rather huge.. 100MB.. and that's data that has to be kept in-memory between fights. It is recommended to have at least 16GB of RAM (in total), and a good amount of hard drive space too! Why not constantly write-out data to the disk that you don't need to keep in memory, you might ask? There's definitely improvements to be made in this area friends. We could potentially do more in the way of memory offload in the future. However, this app does prefer to use memory over disk IO anyways, and this is because disk read/writes are more likely to produce lag spikes, than working with that same data in-memory. IE if we copy 100MB to the disk during a PvP that would likely cause a lag spike, while allocating 100MB in memory probably wouldn't, or at least, wouldn't be *as bad*. Open to suggestions in this area. No design is final friends.

# Building The Code
This is a Kotlin Multiplatform project targeting Desktop. It uses the Jetpack Compose framework for the UI. The Lua companion does not need to be compiled or built. Instead, the Lua files are copied to the game folder when you install Raid Framer, as the installation process is designed to be as simple as possible. Of course, some people prefer to build the code themselves, so they can improve the code / see how it works / design their own addons. I tried to make this as easy as I could by providing the gradle build scripts, and a wrapper around the code that builds everything. For those familar with Android developement, this structure closely mimicks a known build process, and also implements the Logcat logging format because why not. It's just so this project is in alignment with the offical Google + Jetbrains JetPack compose way of doing things, because what I discovered is the framework has been developed rapidly, and new projects can go out-of-sync quickly if they are not maintained.

## SO ANYWAYS.. ABOUT THE BUILDING..
InteliJ has now been consolidated into a single 'version'. There used to be Pro and Community versions. All you need is to get the latest Intelij (https://www.jetbrains.com/idea/download/?section=windows) and open the project with that. The IDE should recognize the Gradle build files and pick up on the project's sub-modules.

The main source is in:
* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.

The Lua Addon code is in: resources/RaidFramer (It's literally just the folder with the Lua files that gets copied to ArcheRage/Addon/RaidFramer) at runtime. This prevents the need for sharing around hacky RAR files outside of version control, and in theory should be less prone to error. The user does have to pick/confirm their ArcheRage directory though!

# LICENCE / WARRANTY
This is an addon for my favorite game. I can't provide any guarantee or warranty about it's operation, or its safety. However, I will say that I did try to make a splendid addon for us! I ask that you don't use this code for evil, for hate, malice or to bully people, and that if you makes forks of it, to at least say that it's a fork of the original by providing a link. This isn't a 'service' nor does it have a public backend.

Copyright 2026 ~ Reoky ~

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.