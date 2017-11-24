package com.devicenut.pixelnutctrl;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class Main extends Application
{
    private static final String LOGNAME = "Main";

    static String devName;
    static Bluetooth ble;

    static MyPager masterPager;
    static int numFragments, pageFavorites, pageControls, pageDetails, pageCurrent;

    static Context appContext;
    @Override public void onCreate()
    {
        super.onCreate();
        appContext = getApplicationContext();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static final String TITLE_PIXELNUT       = "P!";
    static final String TITLE_ADAFRUIT       = "Adafruit";
    static final String TITLE_NONAME         = "NoName";
    static final String URL_PIXELNUT         = "http://www.pixelnutstore.com";

    static final String CMD_GET_INFO         = "?";
    static final String CMD_GET_SEGMENTS     = "?S";
    static final String CMD_GET_PATTERNS     = "?P";
    static final String CMD_GET_PLUGINS      = "?X";
    static final String CMD_BLUENAME         = "@";
    static final String CMD_BRIGHT           = "%";
    static final String CMD_DELAY            = ":";
    static final String CMD_EXTMODE          = "_";
    static final String CMD_PROPVALS         = "=";
    static final String CMD_TRIGGER          = "!";
    static final String CMD_PAUSE            = "[";
    static final String CMD_RESUME           = "]";
    static final String CMD_SEGS_ENABLE      = "#";
    static final String CMD_POP_PATTERN      = "P";
    static final String CMD_START_END        = ".";

    static final int MAXVAL_HUE              = 359;
    static final int MAXVAL_WHT              = 50;
    static final int MAXVAL_PERCENT          = 100;
    static final int MAXVAL_FORCE            = 1000;
    static final int MINVAL_DELAYRANGE       = 80;       // use this for patterns defined here, and is minimal value for custom patterns

    static final int MINLEN_SEGLEN_FORADV    = 20;      // minimum length of each segment to be able to use the advanced patterns
    static final int MINLEN_CMDSTR           = 110;     // minimum length of the command/pattern string
    static final int ADDLEN_CMDSTR_PERSEG    = 50;      // additional length of command/pattern string per additional segment,
    // otherwise will not be able to use the advanced patterns

    static final String[] basicPatternNames =
            {
                    "Solid",
                    "Waves",
                    "Blinks",
                    "Twinkles",
                    "Scanner",
                    "Spokes",
                    "Comet",
            };

    static final String[] basicPatternHelp =
            {
                    "A solid color which can be modified with the ColorHue and Whiteness properties.",

                    "This creates the effect of waves (brightness that changes up and down) that move down the strip, in a single color.\n\n" +
                    "The color and frequency of the wave can be modified with the ColorHue, Whiteness, and Count properties.",

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

    static final String[] basicPatternCmds =
            {
                    "E0 H258 Q3 T G",
                    "E10 D60 Q7 T G",
                    "E51 H232 D10 Q7 T G",
                    "E50 W80 D10 Q3 T G",
                    "E40 H120 C20 D40 Q7 T G",
                    "E30 C20 D60 Q7 T G",
                    "E20 H30 C25 D30 Q7 T G",
            };

    static final int[] basicPatternBits =
            {
                    0x83, // disable Delay control
                    0x07,
                    0x07,
                    0x03,
                    0x07,
                    0x07,
                    0x07,
            };

    static final String[] advPatternNames =
            {
                    "Rainbow Ripple",
                    "Rainbow Roll",
                    "Color Twinkles",
                    "Twinkle Comets",
                    "Comet Party",
                    "Scanner Mix",
                    "Ferris Wheel",
                    "Expanding Noise",
                    "Crazy Blinks",
                    "Bright Swells",
                    "Color Melts",
                    "Holiday",
                    "MashUp",
            };

    static final String[] advPatternHelp =
            {
                    "Color hue changes \"ripple\" down the strip. The colors move through the spectrum, and appear stationary until Triggered.\n\n" +
                    "The Force applied changes the amount of color change per pixel. At maximum Force the entire spectrum is displayed again.",

                    "Colors hue changes occur at the head and get pushed down the strip. When the end is reached they start getting cleared, creating a \"rolling\" effect.\n\n" +
                    "Triggering restarts the effect, with the amount of Force determining how fast the colors change. At the maximum Force the entire spectrum is displayed again.",

                    "This has bright white twinkling \"stars\" over a background color, which is determined by the ColorHue and Whiteness properties.\n\n" +
                    "Triggering causes the background brightness to swell up and down, with the amount of Force determining the speed of the swelling.",

                    "This has bright twinkling without a background. The ColorHue property changes the twinkling color.\n\n" +
                    "Occasional comets streak up and down and then disappear. One of the comets is red, and appears randomly every 3-6 seconds.\n\n" +
                    "The other is orange and appears only when Triggered, with the Force determining its length.",

                    "Comets pairs, one in either direction, both of which change color hue occasionally. Trigging creates new comet pairs.\n\n" +
                    "The comet color and tail lengths can be modified with the ColorHue, Whiteness, and Count properties.",

                    "Two scanners (blocks of same brightness pixels that move back and forth), with only the first one visible initially until Triggered.\n\n" +
                    "The first one changes colors on each change in direction, and the length can be modified with the Count property.\n\n" +
                    "The second one (once Triggered) moves in the opposite direction, periodically surges in speed, and is modified with ColorHue property.",

                    "Evenly spaced pixels move together around and around the strip, creating a \"Ferris Wheel\" effect.\n\n" +
                    "The spokes periodically change colors, or can be modified with the ColorHue and Whiteness properties.\n\n" +
                    "The Count property determines the number of spokes. Triggering toggles the direction of the motion.",

                    "The background is whitish noise, with the color modified by the ColorHue property.\n\n" +
                    "A Trigger causes the background to slowly and continuously expand and contract, with the Force determining the extent of the expansion.",

                    "Random colored blinking that periodically surge in the rate of blinking. The Count property determines the number of blinking changes made at once.\n\n" +
                    "Triggering changes the frequency of the blinking, with larger Forces causing faster blinking surges.",

                    "All pixels swell up and down in brightness, with random color hue and whiteness changes, or set with the ColorHue and Whiteness properties.\n\n" +
                    "Triggering changes the pace of the swelling, with larger Forces causing faster swelling.",

                    "Colors melt from one to the other, with slow and smooth transitions.\n\n" +
                    "Triggering causes a new target color to be is chosen, with larger Forces causing larger color changes.",

                    "Festive red and green twinkles, with an occasional white comet that streaks across them.\n\n." +
                    "The comet's whiteness can be modified, and Triggering creates them.",

                    "Combination of a purple scanner over a greenish twinkling background, with a red comet that is fired off every time the scanner " +
                    "bounces off the end of the strip, or when Triggered.\n\n" +
                    "The ColorHue property only affects the color of the twinkling."
            };

    static final String[] advPatternCmds =
            {
                    "E2 D20 T E101 F1000 I T G",
                    "E1 D20 F1 I T E101 F1000 I T G",
                    "E0 B50 W20 H232 D10 Q3 T E142 F250 I E50 B80 W80 D10 T G",
                    "E50 B65 W80 H50 D10 Q3 T E20 B90 C25 D30 F0 O3 T6 E20 U0 B90 H30 C45 D30 F0 I T E120 F1 I G",
                    "E20 W25 C25 D30 Q7 I T E101 F100 T E20 U0 W25 C25 D20 Q7 I T E101 F200 T G",
                    "E40 C25 D20 Q4 T E111 A0 E40 U0 V1 H270 C5 D30 Q1 I E131 F1000 O5 T5 G",
                    "E30 C20 D60 Q7 T E160 I E120 I E111 F O3 T7 G",
                    "E52 W65 C25 D60 Q1 T E150 I E120 F1000 I G",
                    "E51 C10 D60 Q4 T E112 T E131 F1 I T G",
                    "E0 B80 D10 Q3 T E111 F O10 T10 E142 F250 I T G",
                    "E0 H30 D30 T E110 F600 I T E111 A1 G",
                    "E50 B40 H0 D10 T E50 B50 H125 D15 T E20 B80 W80 H270 C15 D30 Q2 F0 I T20 O10 G",
                    "E50 V1 B65 W30 H100 D10 Q1 T E40 H270 C10 D50 T E20 C20 D15 A1 F0 I T G"
            };

    static final int[] advPatternBits =
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
                    0x12,
                    0x11,
            };

    static final int basicPatternsCount = basicPatternNames.length;
    static final int advPatternsCount = advPatternNames.length;

    // determined for android device being used
    static int pixelWidth = 0;
    static int pixelHeight = 0;
    static int pixelDensity = 0;

    // read from device during configuration
    static int curSegment = 0;                  // index from 0
    static int numPatterns = 0;                 // total number of patterns that can be chosen
    static int numSegments = 0;                 // total number of pixel segments
    static int devicePatterns = 0;              // number of custom patterns defined by device
    static int customPlugins = 0;               // number of custom plugins defined by device
    static int maxlenCmdStrs = 0;               // max length of command string that can be sent
    static int rangeDelay = MINVAL_DELAYRANGE;  // default range of delay offsets

    static final int maxNumSegs = 5;            // limited because of layout
    static int segPatterns[]        = new int[maxNumSegs];   // current pattern for each segment (index from 0)
    static int curBright[]          = new int[maxNumSegs];
    static int curDelay[]           = new int[maxNumSegs];
    static boolean segXmodeEnb[]    = new boolean[maxNumSegs];
    static int segXmodeHue[]        = new int[maxNumSegs];
    static int segXmodeWht[]        = new int[maxNumSegs];
    static int segXmodeCnt[]        = new int[maxNumSegs];
    static int segTrigForce[]       = new int[maxNumSegs];
    static int segPixels[]          = new int[maxNumSegs];
    static int segTracks[]          = new int[maxNumSegs];
    static int segLayers[]          = new int[maxNumSegs];

    // only used for multiple segments on the same physical strand:
    static int segPosStart[]        = new int[maxNumSegs];  // starting positions for each segment
    static int segPosCount[]        = new int[maxNumSegs];  // number of pixels for each segment

    static boolean doUpdate = true;             // false if device output is in pause mode
    static boolean initPatterns = false;        // true if must initialize device with patterns at startup
    static boolean multiStrands = false;        // true if device has multiple physical pixel strands
                                                // false means all segment info must be sent when changing patterns

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static final int MAXNUM_FAVORITIES = 6; // limited by layout
    static final int FAVTYPE_BASIC = 0;
    static final int FAVTYPE_ADV = 1;
    static final int FAVTYPE_STORED = 2;

    static class FavoriteInfo
    {
        static class FavPatternData
        {
            int type;
            int index;
            String values;
        }
        String name;
        FavPatternData[] data;
        int segs; // number of segments

        // used for static initialization of single segment
        FavoriteInfo(String n, int t, int p, String v)
        {
            name = n;
            data = new FavPatternData[1];
            segs = 1;
            if (!addValue(0, t, p, v)) data = null;
        }

        // used along with addValue to add multiple segments
        FavoriteInfo(String n, int c)
        {
            name = n;
            segs = c;
            data = new FavPatternData[segs];
        }

        boolean addValue(int i, int t, int p, String v)
        {
            if ((i >= segs) || !TestTypePnum(t, p))
                return false;

            data[i] = new FavPatternData();
            data[i].type = t;
            data[i].index = p;
            data[i].values = v;

            return true;
        }

        // creates instance from single string
        boolean FavoriteInfo(String s)
        {
            String[] lines = s.split("\n"); // break into lines
            if (lines.length < 2) return false;

            name = lines[0];
            segs = lines.length-1;
            data = new FavPatternData[segs];

            for (int i = 1; i < segs; ++i)
            {
                String[] strs = lines[i].split("\\s+"); // remove ALL spaces
                if (strs.length < 9) return false;

                data[i].type = Integer.parseInt(strs[0]);
                data[i].index = Integer.parseInt(strs[1]);
                if (!TestTypePnum(data[i].type, data[i].index))
                    return false;

                data[i].values = "";
                for (int j = 0; j < 6; ++j)
                    data[i].values += strs[j + 2] + " ";
            }

            return true;
        }

        // creates string from instance
        String makeString()
        {
            if (data == null) return "";

            String s = name + "\n";
            for (int i = 0; i < segs; ++i)
            {
                FavPatternData fd = data[i];
                s += fd.type + " " + fd.index + " " + fd.values;
                if (i < (segs-1)) s += "\n";
            }

            return s;
        }

        private boolean TestTypePnum(int t, int p)
        {
            switch (t)
            {
                case FAVTYPE_BASIC:
                {
                    if (p >= advPatternsCount) return false;
                    break;
                }
                case FAVTYPE_ADV:
                {
                    if (p >= basicPatternsCount) return false;
                    break;
                }
                default: return false;
            }
            return true;
        }

        String getPatternName()
        {
            return name;
        }

        int getPatternNum(int seg)
        {
            int pnum = data[seg].index;

            switch (data[seg].type)
            {
                default:
                {
                    pnum += devicePatterns;
                    break;
                }
                case FAVTYPE_ADV:
                {
                    pnum += devicePatterns + basicPatternsCount;
                    break;
                }
                case FAVTYPE_STORED:
                {
                    pnum += devicePatterns + basicPatternsCount + advPatternsCount;
                    break;
                }
            }

            return pnum;
        }

        String getPatternVals(int seg)
        {
            return data[seg].values;
        }
    }

    static final FavoriteInfo[] listFavorites = new FavoriteInfo[MAXNUM_FAVORITIES];
    static int numFavorites = 0;
    static int curFavorite = -1;

    static final FavoriteInfo defFav_Purple = new FavoriteInfo("Purple", FAVTYPE_BASIC, 0, "60 0 0 0 0 0 0");
    static final FavoriteInfo defFav_Rainbow = new FavoriteInfo(advPatternNames[0], FAVTYPE_ADV, 0, "90 0 1 0 0 0 500");

    static void AddDefaultFavorites()
    {
        listFavorites[0] = defFav_Purple;
        listFavorites[1] = defFav_Rainbow;
        numFavorites = 2;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // used to determine which patterns are allowed on which segments
    static boolean segBasicOnly[] = new boolean[maxNumSegs];
    static boolean haveBasicSegs = false;       // true if some segments too small for advanced patterns
    static boolean useAdvPatterns = true;       // false if limited flash space to receive commands
    static boolean haveAdvPatterns;             // true if current pattern selection includes advanced

    // assigned for device specific patterns
    static String[] devPatternNames_Device;
    static String[] devPatternHelp_Device;
    static String[] devPatternCmds_Device;
    static int[] devPatternBits_Device;

    // assigned for basic patterns
    static String[] listNames_Basic;
    static boolean[] listEnables_Basic;
    static int[] mapIndexToPattern_Basic;
    static int[] mapPatternToIndex_Basic;
    static String[] devPatternNames_Basic;
    static String[] devPatternHelp_Basic;
    static String[] devPatternCmds_Basic;
    static int[] devPatternBits_Basic;

    // assigned for both basic and advanced patterns
    static String[] listNames_All;
    static boolean[] listEnables_All;
    static int[] mapIndexToPattern_All;
    static int[] mapPatternToIndex_All;
    static String[] devPatternNames_All;
    static String[] devPatternHelp_All;
    static String[] devPatternCmds_All;
    static int[] devPatternBits_All;

    // set to either _Basic or _All vars when change segments
    static int[] mapIndexToPattern;
    static int[] mapPatternToIndex;
    static String[] devPatternNames;
    static String[] devPatternHelp;
    static String[] devPatternCmds;
    static int[] devPatternBits;

    static void InitVarsForDevice()
    {
        if ((numSegments == 1) || multiStrands) // not supported for logical segments
        {
            pageFavorites = 0;
            pageControls = 1;
            //FIXME pageDetails = 2;
            pageDetails = -1;
            numFragments = 2;

            AddDefaultFavorites();
        }
        else
        {
            pageFavorites = -1;
            pageControls = 0;
            //FIXME pageDetails = 1;
            pageDetails = -1;
            numFragments = 1;
        }
        pageCurrent = 0;

        if (haveBasicSegs) CreateListArrays_Basic(); // some segments use only basic patterns
        if (useAdvPatterns) CreateListArrays_Adv();  // are allowed to use advanced patterns

        curSegment = 0; // always start with first segment
    }

    private static void CreateListArrays_Basic()
    {
        Log.d(LOGNAME, "Creating Basic List Array");

        int j = 0;
        int k = 0;
        int extra = 1;
        if (devicePatterns > 0) ++extra;

        listNames_Basic = new String[numPatterns + extra];
        listEnables_Basic = new boolean[numPatterns + extra];
        mapIndexToPattern_Basic = new int[numPatterns + extra];
        mapPatternToIndex_Basic = new int[numPatterns];
        devPatternNames_Basic = new String[numPatterns];
        devPatternHelp_Basic = new String[numPatterns];
        devPatternCmds_Basic = new String[numPatterns];
        devPatternBits_Basic = new int[numPatterns];

        if (devicePatterns > 0)
        {
            listNames_Basic[j] = "Custom Patterns";
            listEnables_Basic[j] = false;
            mapIndexToPattern_Basic[j] = 0;
            ++j;

            for (int i = 0; i < devicePatterns; ++i)
            {
                Log.v(LOGNAME, "Adding custom pattern i=" + i + " j=" + j + " => " + devPatternNames_Device[i]);

                listNames_Basic[j] = devPatternNames_Device[i];
                listEnables_Basic[j] = true;
                mapIndexToPattern_Basic[j] = k;
                mapPatternToIndex_Basic[k] = j;
                devPatternNames_Basic[k] = devPatternNames_Device[i];
                devPatternHelp_Basic[k] = devPatternHelp_Device[i];
                devPatternCmds_Basic[k] = devPatternCmds_Device[i];
                devPatternBits_Basic[k] = devPatternBits_Device[i];

                ++j;
                ++k;
            }
        }

        listNames_Basic[j] = "Basic Patterns";
        listEnables_Basic[j] = false;
        mapIndexToPattern_Basic[j] = 0;
        ++j;

        for (int i = 0; i < basicPatternsCount; ++i)
        {
            Log.v(LOGNAME, "Adding basic pattern i=" + i + " j=" + j + " => " + basicPatternNames[i]);

            listNames_Basic[j] = basicPatternNames[i];
            listEnables_Basic[j] = true;
            mapIndexToPattern_Basic[j] = k;
            mapPatternToIndex_Basic[k] = j;
            devPatternNames_Basic[k] = basicPatternNames[i];
            devPatternHelp_Basic[k] = basicPatternHelp[i];
            devPatternCmds_Basic[k] = basicPatternCmds[i];
            devPatternBits_Basic[k] = basicPatternBits[i];

            ++j;
            ++k;
        }
    }

    private static void CreateListArrays_Adv()
    {
        Log.d(LOGNAME, "Creating Advanced List Array");

        int j = 0;
        int k = 0;
        int extra = 2;
        if (devicePatterns > 0) ++extra;

        listNames_All = new String[numPatterns + extra];
        listEnables_All = new boolean[numPatterns + extra];
        mapIndexToPattern_All = new int[numPatterns + extra];
        mapPatternToIndex_All = new int[numPatterns];
        devPatternNames_All = new String[numPatterns];
        devPatternHelp_All = new String[numPatterns];
        devPatternCmds_All = new String[numPatterns];
        devPatternBits_All = new int[numPatterns];

        if (devicePatterns > 0)
        {
            listNames_All[j] = "Custom Patterns";
            listEnables_All[j] = false;
            mapIndexToPattern_All[j] = 0;
            ++j;

            for (int i = 0; i < devicePatterns; ++i)
            {
                Log.v(LOGNAME, "Adding custom pattern j=" + j + " k=" + k + " => " + devPatternNames_Device[i]);

                listNames_All[j] = devPatternNames_Device[i];
                listEnables_All[j] = true;
                mapIndexToPattern_All[j] = k;
                mapPatternToIndex_All[k] = j;
                devPatternNames_All[k] = devPatternNames_Device[i];
                devPatternHelp_All[k] = devPatternHelp_Device[i];
                devPatternCmds_All[k] = devPatternCmds_Device[i];
                devPatternBits_All[k] = devPatternBits_Device[i];

                ++j;
                ++k;
            }
        }

        listNames_All[j] = "Basic Patterns";
        listEnables_All[j] = false;
        mapIndexToPattern_All[j] = 0;
        ++j;

        for (int i = 0; i < basicPatternsCount; ++i)
        {
            Log.v(LOGNAME, "Adding basic pattern j=" + j + " k=" + k + " => " + basicPatternNames[i]);

            listNames_All[j] = basicPatternNames[i];
            listEnables_All[j] = true;
            mapIndexToPattern_All[j] = k;
            mapPatternToIndex_All[k] = j;
            devPatternNames_All[k] = basicPatternNames[i];
            devPatternHelp_All[k] = basicPatternHelp[i];
            devPatternCmds_All[k] = basicPatternCmds[i];
            devPatternBits_All[k] = basicPatternBits[i];

            ++j;
            ++k;
        }

        listNames_All[j] = "Advanced Patterns";
        listEnables_All[j] = false;
        mapIndexToPattern_All[j] = 0;
        ++j;

        for (int i = 0; i < advPatternsCount; ++i)
        {
            Log.v(LOGNAME, "Adding advanced pattern j=" + j + " k=" + k + " => " + advPatternNames[i]);

            listNames_All[j] = advPatternNames[i];
            listEnables_All[j] = true;
            mapIndexToPattern_All[j] = k;
            mapPatternToIndex_All[k] = j;
            devPatternNames_All[k] = advPatternNames[i];
            devPatternHelp_All[k] = advPatternHelp[i];
            devPatternCmds_All[k] = advPatternCmds[i];
            devPatternBits_All[k] = advPatternBits[i];

            ++j;
            ++k;
        }
    }
}
