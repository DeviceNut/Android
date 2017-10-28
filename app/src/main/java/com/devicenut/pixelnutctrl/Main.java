package com.devicenut.pixelnutctrl;

import android.app.Application;

public class Main extends Application
{
    public static final String TITLE_PIXELNUT = "P!";
    public static final String URL_PIXELNUT = "http://www.pixelnutstore.com";

    public static final String CMD_GET_INFO     = "?";
    public static final String CMD_GET_SEGMENTS = "?S";
    public static final String CMD_GET_PATTERNS = "?P";
    public static final String CMD_GET_PLUGINS  = "?X";
    public static final String CMD_BLUENAME     = "@";
    public static final String CMD_BRIGHT       = "%";
    public static final String CMD_DELAY        = ":";
    public static final String CMD_EXTMODE      = "_";
    public static final String CMD_PROPVALS     = "=";
    public static final String CMD_TRIGGER      = "!";
    public static final String CMD_PAUSE        = "[";
    public static final String CMD_RESUME       = "]";
    public static final String CMD_SEGS_ENABLE  = "#";
    public static final String CMD_POP_PATTERN  = "P";
    public static final String CMD_START_END    = ".";

    public static final int MAXVAL_HUE          = 359;
    public static final int MAXVAL_WHT          = 50;
    public static final int MAXVAL_PERCENT      = 100;
    public static final int MAXVAL_FORCE        = 1000;
    public static final int MINVAL_DELAYRANGE   = 80;       // use this for patterns defined here, and is minimal value for custom patterns

    public static final int MINLEN_SEGLEN_FORADV = 20;      // minimum length of each segment to be able to use the advanced patterns
    public static final int MINLEN_CMDSTR        = 110;     // minimum length of the command/pattern string
    public static final int ADDLEN_CMDSTR_PERSEG = 50;      // additional length of command/pattern string per additional segment,
                                                            // otherwise will not be able to use the advanced patterns

    public static final String[] basicPatternHelp =
            {
                    "A solid color which can be modified with the ColorHue and Whiteness properties.",

                    "A color that ripples down the strip, and can be modified with the ColorHue and Whiteness properties.",

                    "A color that rolls down the strip, and can be modified with the ColorHue and Whiteness properties.",

                    "This creates the effect of waves (brightness that changes up and down) that move down the strip, in a single color.\n\n" +
                    "The color and frequency of the wave can be modified with the ColorHue, Whiteness, and Count properties.",

                    "Creates what seems like noise (randomly set pixels with a random brightness), using the color selected with the ColorHue and Whiteness properties.\n\n" +
                    "The Count property determines how many pixels are set at once.",

                    "Randomly blinks pixels on/off with equal brightness, in the color selected with the ColorHue and Whiteness properties.\n\n" +
                    "The Count property determines how many pixels blink at once.",

                    "Creates the effect of twinkling (rising/falling brightness that disappear in a random fashion).\n\n" +
                    "The color of the twinkling can be modified with the ColorHue and Whiteness properties.",

                    "Creates the effect of scanning (blocks of the same brightness that move back and forth from one end to the other).\n\n" +
                    "The color and length of the block can be modified with the ColorHue and Count properties.",

                    "Evenly spaced pixels move in unison, like spokes in a wheel. The color can be modified with the ColorHue and Whiteness properties.\n\n" +
                    "The number of spokes is determined by the Count property, with larger counts creating more spokes.",

                    "Creates the effect of a comet (strip of light which is bright at the head, and gets progressively dimmer towards the end).\n\n" +
                    "Both the color and length of the tail can be modified with the ColorHue, Whiteness, and Count properties.",
            };

    public static final String[] basicPatternCmds =
            {
                    "E0 H270 Q3 T G",
                    "E2 H135 D40 Q3 T G",
                    "E1 H100 D40 Q3 T G",
                    "E10 D60 Q7 T G",
                    "E52 W30 D10 Q3 T G",
                    "E51 H232 D10 Q3 T G",
                    "E50 W80 D10 Q3 T G",
                    "E40 H120 C20 D40 Q7 T G",
                    "E30 C20 D60 Q7 T G",
                    "E20 H30 C25 D30 Q7 T G",
            };

    public static final String[] basicPatternNames =
            {
                    "Solid",
                    "Ripple",
                    "Roller",
                    "Waves",
                    "Noisy",
                    "Blinks",
                    "Twinkles",
                    "Scanner",
                    "Spokes",
                    "Comet",
            };

    public static final int[] basicPatternBits =
            {
                    0x03,
                    0x03,
                    0x03,
                    0x07,
                    0x07,
                    0x07,
                    0x03,
                    0x07,
                    0x07,
                    0x07,
            };

    public static final String[] advPatternHelp =
            {
                    "Color hue changes \"ripple\" down the strip. The colors move through the spectrum, and appear stationary until Triggered.\n\n" +
                    "The Force applied changes the amount of color change per pixel. At maximum Force the entire spectrum is displayed again.",

                    "Colors hue changes occur at the head and get pushed down the strip. When the end is reached they start getting cleared, creating a \"rolling\" effect.\n\n" +
                    "Triggering restarts the effect, with the amount of Force determining how fast the colors change. At the maximum Force the entire spectrum is displayed again.",

                    "This has bright white twinkling \"stars\" over a background color, which is determined by the ColorHue and Brightness properties.\n\n" +
                    "Triggering causes the background brightness to swell up and down, with the amount of Force determining the speed of the swelling.",

                    "This has bright twinkling without a background. The ColorHue property changes the twinkling color.\n\n" +
                    "Occasional comets streak up and down and then disappear. One of the comets is red, and appears randomly every 3-6 seconds.\n\n" +
                    "The other is orange and appears only when Triggered, with the Force determining its length.",

                    "Comets pairs, one in either direction, both of which change color hue occasionally. Trigging causes new comets to be added.\n\n" +
                    "The comet color and tail lengths can be modified with the ColorHue, Whiteness, and Count properties.",

                    "Two scanners (blocks of same brightness pixels that move back and forth), with only the first one visible initially until Triggered.\n\n" +
                    "The first one changes colors on each change in direction, and the length can be modified with the Count property.\n\n" +
                    "The second one (once Triggered) moves in the opposite direction, periodically surges in speed, and is modified with ColorHue property.",

                    "Evenly spaced pixels move together around and around the strip, creating a \"Ferris Wheel\" effect.\n\n" +
                    "The spokes periodically change colors, or can be modified with the ColorHue and Whiteness properties.\n\n" +
                    "The Count property determines the number of spokes. Triggering toggles the direction of the motion." ,

                    "The background is whitish noise, with the color modified by the ColorHue property.\n\n" +
                    "A Trigger causes the background to slowly and continuously expand and contract, with the Force determining the extent of the expansion.",

                    "Random colored blinking that periodically surge in the rate of blinking. The Count property determines the number of blinking changes made at once.\n\n" +
                    "Triggering changes the frequency of the blinking, with larger Forces causing faster blinking surges.",

                    "All pixels swell up and down in brightness, with random color hue and whiteness changes, or set with the ColorHue and Whiteness properties.\n\n" +
                    "Triggering changes the pace of the swelling, with larger Forces causing faster swelling.",

                    "All pixels move through color hue and whiteness transitions that are slow and smooth.\n\n" +
                    "A new color is chosen every time the previous target color has been reached, or when Triggered, " +
                    "with the Force determining how large the color changes are.\n\n" +
                    "The time it takes to reach a new color is proportional to the size of the color change.",

                    "Combination of a purple scanner over a greenish twinkling background, with a red comet that is fired off every time the scanner " +
                    "bounces off the end of the strip, or when Triggered.\n\n" +
                    "The ColorHue property only affects the color of the twinkling."
            };

    public static final String[] advPatternCmds =
            {
                    "E2 D20 T E101 F1000 I T G",
                    "E1 D20 F1 I T E101 F1000 I T G",
                    "E0 B50 W20 H232 D10 Q3 T E142 D10 F250 I E50 B80 W80 D10 T G",
                    "E50 B65 W80 H50 D10 Q3 T E20 B90 C25 D30 F0 O3 T6 E20 U0 B90 H30 C45 D30 F0 I T E120 F1 I G",
                    "E20 W25 C25 D30 Q7 I T E101 F100 T E20 U0 W25 C25 D20 Q7 I T E101 F200 T G",
                    "E40 C25 D20 Q4 T E111 A0 E40 U0 V1 H270 C5 D30 Q1 I E131 F1000 O5 T5 G",
                    "E30 C20 D60 Q7 T E160 I E120 I E111 F O3 T7 G",
                    "E52 C25 W65 D20 Q1 T E150 D60 I E120 F1000 I G",
                    "E51 C10 D60 Q4 T E112 T E131 F1 I T G",
                    "E0 B80 D10 Q3 T E111 F O10 T10 E142 F250 I T G",
                    "E0 H30 D30 T E110 F600 I T E111 A1 G",
                    "E50 V1 B65 W30 H100 D10 Q1 T E40 H270 C10 D50 T E20 D15 C20 A1 F0 I T G"
            };

    public static final String[] advPatternNames =
            {
                    "Rainbow Ripple",
                    "Rainbow Roll",
                    "Color Twinkles",
                    "Twinkle Comets",
                    "Dueling Comets",
                    "Dueling Scanners",
                    "Ferris Wheel",
                    "Expanding Noise",
                    "Blink Surges",
                    "Bright Swells",
                    "Color Smooth",
                    "MashUp",
            };

    public static final int[] advPatternBits =
            {
                    0x30,
                    0x30,
                    0x33,
                    0x33,
                    0x17,
                    0x15,
                    0x17,
                    0x31,
                    0x34,
                    0x33,
                    0x30,
                    0x11,
            };

    public static final int basicPatternsCount = basicPatternNames.length;
    public static final int advPatternsCount = advPatternNames.length;
    public static int stdPatternsCount;

    public static String[] devPatternNames;
    public static String[] devPatternHelp;
    public static String[] devPatternCmds;
    public static int[] devPatternBits;

    public static int pixelWidth = 0;
    public static int pixelLength = 0;
    public static int pixelDensity = 0;

    public static int curSegment        = 0;    // index from 0
    public static int numPatterns       = 0;    // total number of patterns that can be chosen
    public static int numSegments       = 0;    // total number of pixel segments
    public static int customPatterns    = 0;    // number of custom patterns defined by device
    public static int customPlugins     = 0;    // number of custom plugins defined by device
    public static int maxlenCmdStrs     = 0;    // max length of command string that can be sent
    public static int rangeDelay        = MINVAL_DELAYRANGE; // default range of delay offsets

    public static boolean useAdvPatterns = true;    // false for small segments and/or limited flash space
    public static boolean editPatterns = true;      // false if device has fixed patterns that cannot be changed
    public static boolean initPatterns = false;     // true if must initialize device with patterns at startup
    public static boolean multiStrands = false;     // true if device has multiple physical pixel strands
                                                    // false means all segment info must be sent when changing patterns

    // limited to 5 segments
    public static int curDelay[]        = { 0,0,0,0,0 }; // delay in msecs
    public static int curBright[]       = { 0,0,0,0,0 }; // maximum brightness
    public static boolean segXmodeEnb[] = { false,false,false,false,false };
    public static int segXmodeHue[]     = { 0,0,0,0,0 };
    public static int segXmodeWht[]     = { 0,0,0,0,0 };
    public static int segXmodeCnt[]     = { 0,0,0,0,0 };
    public static int segTrigForce[]    = { 0,0,0,0,0 };
    public static int segPatterns[]     = { 0,0,0,0,0 }; // index from 0
    public static int segPixels[]       = { 0,0,0,0,0 }; // number of pixels
    public static int segLayers[]       = { 0,0,0,0,0 }; // number of layers
    public static int segTracks[]       = { 0,0,0,0,0 }; // number of tracks
    // only used for multiple segments on the same physical strand:
    public static int segPosStart[]     = { 0,0,0,0,0 }; // starting positions for each segment
    public static int segPosCount[]     = { 0,0,0,0,0 }; // number of pixels for each segment

    public static String devName;
    public static Bluetooth ble; // = new Bluetooth();
}
