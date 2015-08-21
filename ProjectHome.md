### General Description ###
Generates scrambles for 2x2x2, 3x3x3, 4x4x4, 5x5x5, 6x6x6, and 7x7x7 puzzle cubes (Rubik's style 6 axial 3D mechanical puzzles), as well as scrambles for the Megaminx, Pyraminx, Square One, and UFO. It provides an inspection countdown and stopwatch. The provides running statistics of the best, average, and standard deviation of the solve times. Each puzzle will keep it's own running log of statistics.

This application (v1.7) uses the JavaScript scramble algorithms in the [scramblers.zip](http://www.worldcubeassociation.org/regulations/scrambles.zip) hosted by the World Cube Association (WCA). (_This project is not affiliated with the WCA_). Credit for the scramble algorithms goes to [Jaap Scherphuis](http://www.jaapsch.net/puzzles/), [Syoji Takamatsu](http://reddragon.nce.buttobi.net/) (Pyraminx), [Lucas Garron](http://garron.us/) (Pyraminx), Michael Gottlieb (Pyraminx) [Tom van der Zanden](http://www.tomvanderzanden.nl) (2x2x2), and Clément Gallet. I have made reasonable attempts to contact the authors listed above to request and obtain permission to use their work in this project.

For some of the authors no contact information could be found. If you are one of the authors and do not want your work to be used in this non-commercial open-source project please contact me and I will remove it.

It should be noted that while the code I have written is licensed under Apache 2, and is free to use for both commercial and non-commercial endeavors, however the scramble code remains intellectual property of their respective owners and should not be used commercially without their consent.

**I would like to acknowledge and express my personal gratitude to these Puzzle Gurus for their known or perhaps unknown contributions to this project and the puzzle community at large.**

This project is now considered stable and the feature set is considered closed to avoid [feature creep](http://en.wikipedia.org/wiki/Feature_creep). I will of course fix any bugs or stability problems that might arise. If you would like to add something, by all means go for it. I am open to answering any questions you might have about the code (assuming I can remember what I did). I am open to adding "Locales" if anyone wants to translate the English text into the [Android Locales](http://developer.android.com/sdk/android-2.0.html#locs)

### System Requirements ###
This application should run on Android 1.5, 1.6, and 2.0 on small, normal, and large screens. The caveat being the only physical device I have tested it on is my Motorola Droid.

Feedback is welcome and appreciated (rogerlew@gmail.com).


### QRcode to marketplace ###
[![](http://android-cubetimer.googlecode.com/files/qrcode.png)](http://android-cubetimer.googlecode.com/files/cubetimer_v1.7.apk)

### Instructions ###
  * Tap to start countdown
  * Tap to stop timer
  * App generates new scramble when stopped. (The 2x2x2 scrambler is a little slow.)
  * Long pause on screen to clear records for the current puzzle, or all records can be   cleared through the options menu.
  * Use ringer volume controls to adjust beep volume. Beep can been toggled on and off via the options menu.
  * To change the puzzle select "Set Puzzle" under the options menu.
  * The inspection time is applied across all puzzles. Set your preferred inspection time through the options menu.
  * App will remember your records and inspection time automatically.
  * Make sure you exit the timer when you are done, it is configured to keep the screen on.
  * Happy cubing!
### Revision History (v1.0.1 and v1.0.2 are pre google code) ###
**v1.7**
  * Incorporated WCA JavaScript scramblers using WebView objects and Android's addJavaScriptInterface method
  * Added necessary code for additional puzzles (selection, record handling, etc.)
  * Cube timer now supports 9 puzzles!
  * Added help page
  * Added option to turn beep on and off

**v1.6.5**
  * Scrambler code improved to prevent sequences like U D Ui

**v1.6.4**
  * Prompts user if they want to save time if time is
    1. less than 8 seconds
    1. outside of +/- 2 standard deviations of the running average

**v1.6.3.1**
  * License changed to Apache 2
  * Code refactored, no functionality changed

**v1.6.3**
  * App will beep after countdown (time 00:00.00)
  * Beep volume is set by ringer volume controls using the Audio Manager
  * Beep was obtained from the public domain [link to beep](http://www.partnersinrhyme.com/soundfx/PDsoundfx/PDsoundfx_sounds/beep_sounds/beep_beep-pure_wav.shtml)

**v1.6.2**
  * Now uses scale independent layout units (sp)
  * Compatibility tested with small, normal, and large screens
  * Minimum compatibility as tested is now at SDK 3

**v1.6.1**
  * Will no longer log negative times
  * Moved text around a little, still need to optimize for different size screens

**v1.6**
  * Removed spinner and button, user can now use the entire view to start / stop / reset times
  * wrote a recursive filter for computing running mean, standard deviation, minimum, maximum, and count
  * filter based on the method described here: http://www.johndcook.com/standard_deviation.html

**v1.5**
  * Displays best and average times
  * Saves best and average times and the inspection time using Get Preferences

**v1.0.2**
  * Fixed scramble bug where last two moves could undo themselves
  * App no longer resets when the slider is opened

**v1.0.1**
  * Fix to stay in portrait mode

### Screen Shots of v1.7 ###
![http://android-cubetimer.googlecode.com/files/screenshot_v1.7_0.png](http://android-cubetimer.googlecode.com/files/screenshot_v1.7_0.png)

![http://android-cubetimer.googlecode.com/files/screenshot_v1.7_1.png](http://android-cubetimer.googlecode.com/files/screenshot_v1.7_1.png)

![http://android-cubetimer.googlecode.com/files/screenshot_v1.7_2.png](http://android-cubetimer.googlecode.com/files/screenshot_v1.7_2.png)

![http://android-cubetimer.googlecode.com/files/screenshot_v1.7_3.png](http://android-cubetimer.googlecode.com/files/screenshot_v1.7_3.png)

![http://android-cubetimer.googlecode.com/files/screenshot_v1.7_4.png](http://android-cubetimer.googlecode.com/files/screenshot_v1.7_4.png)

### Screen Shots of v1.6.1 ###
![http://android-cubetimer.googlecode.com/files/screenshot_v1.6_0.png](http://android-cubetimer.googlecode.com/files/screenshot_v1.6_0.png)

![http://android-cubetimer.googlecode.com/files/screenshot_v1.6_1.png](http://android-cubetimer.googlecode.com/files/screenshot_v1.6_1.png)


### Screen Shots of v1.5 (Deprecated) ###
![http://android-cubetimer.googlecode.com/files/screenshot2.png](http://android-cubetimer.googlecode.com/files/screenshot2.png)

![http://android-cubetimer.googlecode.com/files/screenshot.png](http://android-cubetimer.googlecode.com/files/screenshot.png)