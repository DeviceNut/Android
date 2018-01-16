package com.devicenut.pixelnutctrl;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class Main extends Application
{
    private static final String LOGNAME = "Main";

    static Wifi wifi;
    static Bluetooth ble;
    static int deviceID;
    static String devName;
    static boolean devIsBLE;
    static boolean blePresentAndEnabled = false;
    static boolean wifiPresentAndEnabled = false;

    static MsgQueue msgThread;
    static final PCQueue<String> msgWriteQueue = new PCQueue<>(50);
    static volatile boolean msgWriteEnable = true;
    static volatile boolean cmdPauseEnable = true;

    static boolean doRefreshCache = false;

    static MyPager masterPager;
    static int numFragments, pageFavorites, pageControls, pageDetails, pageCurrent;

    static int maxlenAdvPatterns;
    static Context appContext;

    @Override public void onCreate()
    {
        super.onCreate();
        appContext = getApplicationContext();

        maxlenAdvPatterns = 0;
        for (int i = 0; i < advPatternCmds.length; ++i)
        {
            if (maxlenAdvPatterns < advPatternCmds[i].length())
                maxlenAdvPatterns = advPatternCmds[i].length();
        }
    }

    // only works on background threads
    static void SleepMsecs(int msecs)
    {
        //noinspection EmptyCatchBlock
        try { Thread.sleep(msecs); }
        catch (Exception ignored) {}
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static final int DEVSTAT_SUCCESS        =  0;
    static final int DEVSTAT_DISCONNECTED   = -1;
    static final int DEVSTAT_FAILED         = -2;

    static final int MAXLEN_BLE_CHUNK       = 20; // max chars that can be sent at once with BLE implementation

    static final String TITLE_PIXELNUT       = "P!";
    static final String TITLE_ADAFRUIT       = "Adafruit";
    static final String TITLE_NONAME         = "NoName";
    static final String URL_PIXELNUT         = "http://www.pixelnut.io";

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
    static final String CMD_POP_PATTERN      = "P "; // always used before pattern string, needs to be separated
    static final String CMD_START_END        = ".";
    static final String CMD_SEQ_END          = "\n";

    static final int MAXVAL_HUE              = 359;
    static final int MAXVAL_WHT              = 50;
    static final int MAXVAL_PERCENT          = 100;
    static final int MAXVAL_FORCE            = 1000;
    static final int MINVAL_DELAYRANGE       = 80;      // use this for patterns defined here, and is minimal value for custom patterns

    static final int MINLEN_CMDSTR           = 100;     // minimum length of the command/pattern string
    static final int MINLEN_SEGLEN_FORADV    = 20;      // minimum length of each segment to be able to use the advanced patterns

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
                    "E50 B60 H0 D10 T E50 B70 H125 D15 T E20 V1 B90 W80 H270 C25 D30 Q2 F0 I T20 O10 G",
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

    static boolean isConnected = false;         // shared by Devices and Master

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

    static int featureBits = 0;                 // bits that enable extended features
    static final int FEATURE_INT_PATTERNS = 0x01;   // set if cannot use external patterns
    static final int FEATURE_BASIC_PATTERNS = 0x02; // set if cannot use advanced patterns

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

    static boolean createViewFavs = false;      // true when views have been created, false when it's deleted
    static boolean createViewCtrls = false;
    static boolean helpActive = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    static final int MAXNUM_FAVORITIES = 6; // limited by layout
    static final int FAVTYPE_DEVICE = 0;
    static final int FAVTYPE_BASIC = 1;
    static final int FAVTYPE_ADV = 2;
    static final int FAVTYPE_STORED = 3;  // TODO: not implemented
    static final int NUM_FAVSTR_VALS = 7; // number of values in vals string (bright, delay, auto/manual, color, white, count, trigger)

    static class FavoriteInfo
    {
        private static final String LOGNAME = "FavInfo";

        static class FavPatternData
        {
            int type;
            int index;
            String values;
        }

        String name;
        FavPatternData[] data;
        int segs; // number of segments
        boolean builtin;

        // used for static initialization of single segment
        FavoriteInfo(String n, int t, int i, String v)
        {
            name = n;
            segs = 1;
            builtin = true;
            data = new FavPatternData[1];
            data[0] = new FavPatternData();
            data[0].type = t;
            data[0].index = i;
            data[0].values = v;
        }

        // used along with addValue to add multiple segments
        FavoriteInfo(String n, int c)
        {
            name = n;
            segs = c;
            data = new FavPatternData[segs];
            builtin = false;
        }

        boolean addValue(int s, int p, String v)
        {
            if (s >= segs)
            {
                Log.w(LOGNAME, "AddValue: invalid seg=" + s);
                data = null;
                return false; // invalid segment number
            }

            int t, i;

            if ((p >= (devicePatterns + basicPatternsCount)) &&
                (p <  (devicePatterns + basicPatternsCount + advPatternsCount)))
            {
                t = FAVTYPE_ADV;
                i = p - (devicePatterns + basicPatternsCount);
            }
            else if ((p >= devicePatterns) &&
                     (p <  (devicePatterns + basicPatternsCount)))
            {
                t = FAVTYPE_BASIC;
                i = p - devicePatterns;
            }
            else if ((p >= 0) || (p < devicePatterns))
            {
                t = FAVTYPE_DEVICE;
                i = p;
            }
            else
            {
                Log.w(LOGNAME, "AddValue: invalid pnum=" + p);
                data = null;
                return false; // invalid pattern number
            }

            data[s] = new FavPatternData();
            data[s].type = t;
            data[s].index = i;
            data[s].values = v;

            return true;
        }

        // creates instance from single string
        FavoriteInfo(String s)
        {
            data = null;
            if (s == null) return;
            String[] lines = s.split("\n"); // break into lines
            if (lines.length < 2) return;

            name = lines[0];
            segs = lines.length-1;
            data = new FavPatternData[segs];
            builtin = false;

            for (int i = 0; i < segs; ++i)
            {
                String[] strs = lines[i+1].split("\\s+"); // remove ALL spaces
                if (strs.length < NUM_FAVSTR_VALS + 2) // name, type, then vals
                {
                    Log.w(LOGNAME, "CreateInstance: strlen=" + strs.length);
                    data = null;
                    return;
                }

                data[i] = new FavPatternData();
                data[i].type = Integer.parseInt(strs[0]);
                data[i].index = Integer.parseInt(strs[1]);
                if (!TestTypePnum(data[i].type, data[i].index))
                {
                    Log.w(LOGNAME, "CreateInstance: invalid type.index=" + data[i].type + "." + data[i].index);
                    data = null;
                    return;
                }

                data[i].values = "";
                for (int j = 0; j < NUM_FAVSTR_VALS; ++j)
                    data[i].values += strs[j + 2] + " ";
            }
        }

        // creates string from instance
        String makeString(String n)
        {
            if (data == null) return "";

            String s = (n == null) ? name : n;
            s += "\n";
            for (int i = 0; i < segs; ++i)
            {
                s += data[i].type + " " + data[i].index + " " + data[i].values;
                if (i < (segs-1)) s += "\n";
            }

            return s;
        }

        private boolean TestTypePnum(int t, int i)
        {
            if (i < 0) return false;
            switch (t)
            {
                case FAVTYPE_DEVICE:
                {
                    if (i >= devicePatterns) return false;
                    break;
                }
                case FAVTYPE_BASIC:
                {
                    if (i >= basicPatternsCount) return false;
                    break;
                }
                case FAVTYPE_ADV:
                {
                    if (i >= advPatternsCount) return false;
                    break;
                }
                default: return false;
            }
            return true;
        }

        int getSegmentCount() { return segs; }
        String getPatternName() { return name; }

        int getPatternNum(int seg)
        {
            if (data == null) return 0;
            if (seg >= segs) seg = segs-1;

            int pnum = data[seg].index;

            switch (data[seg].type)
            {
                default:
                {
                    pnum = 0;
                    break;
                }
                case FAVTYPE_DEVICE:
                {
                    break;
                }
                case FAVTYPE_BASIC:
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
            if (data == null) return "";
            if (seg >= segs) seg = segs-1;
            return data[seg].values;
        }

        boolean userCreated() { return !builtin; }
    }

    static final FavoriteInfo[] listFavorites = new FavoriteInfo[MAXNUM_FAVORITIES];
    static int numFavorites = 0;
    static int curFavorite = -1;

    static final FavoriteInfo defFav_Purple = new FavoriteInfo("Purple", FAVTYPE_BASIC, 0, "60 0 0 0 0 0 0");
    static final FavoriteInfo defFav_Rainbow = new FavoriteInfo("Rainbow", FAVTYPE_ADV, 0, "90 0 0 0 0 0 500");
    static final FavoriteInfo defFav_Holiday = new FavoriteInfo("Christmas", FAVTYPE_ADV, 11, "100 -20 11 0 50 0 1000");

    static void AddDefaultFavorites()
    {
        listFavorites[0] = defFav_Purple;
        numFavorites = 1;

        if (useAdvPatterns)
        {
            listFavorites[1] = defFav_Rainbow;
            listFavorites[2] = defFav_Holiday;
            numFavorites += 2;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // used to determine which patterns are allowed on which segments
    static boolean segBasicOnly[] = new boolean[maxNumSegs];
    static boolean haveBasicSegs = false;       // true if some segments too small for advanced patterns
    static boolean useAdvPatterns = true;       // false if limited flash space to receive commands
    static boolean useExtPatterns;              // false if can use only device internal patterns

    // assigned for device specific patterns
    static String[] patternNames_Device;
    static String[] patternHelp_Device;
    static String[] patternCmds_Device;
    static int[] patternBits_Device;

    // assigned for basic patterns
    static String[] listNames_Basic;
    static boolean[] listEnables_Basic;
    static int[] mapIndexToPattern_Basic;
    static int[] mapPatternToIndex_Basic;
    static String[] patternNames_Basic;
    static String[] patternHelp_Basic;
    static String[] patternCmds_Basic;
    static int[] patternBits_Basic;

    // assigned for both basic and advanced patterns
    static String[] listNames_All;
    static boolean[] listEnables_All;
    static int[] mapIndexToPattern_All;
    static int[] mapPatternToIndex_All;
    static String[] patternNames_All;
    static String[] patternHelp_All;
    static String[] patternCmds_All;
    static int[] patternBits_All;

    // set to either _Basic or _All vars when change segments
    static int[] mapIndexToPattern;
    static int[] mapPatternToIndex;
    static String[] patternNames;
    static String[] patternHelp;
    static String[] patternCmds;
    static int[] patternBits;

    static void InitVarsForDevice()
    {
        if (useExtPatterns)
        {
            pageFavorites = 0;
            pageControls = 1;
            pageDetails = -1; // FIXME =2
            numFragments = 2;

            AddDefaultFavorites();
        }
        else
        {
            pageFavorites = -1;
            pageControls = 0;
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
        int n = devicePatterns;

        if (useExtPatterns) n += basicPatternsCount;
        else --extra;

        listNames_Basic = new String[n + extra];
        listEnables_Basic = new boolean[n + extra];
        mapIndexToPattern_Basic = new int[n + extra];
        mapPatternToIndex_Basic = new int[n];
        patternNames_Basic = new String[n];
        patternHelp_Basic = new String[n];
        patternCmds_Basic = new String[n];
        patternBits_Basic = new int[n];

        if (devicePatterns > 0)
        {
            listNames_Basic[j] = "Custom Patterns";
            listEnables_Basic[j] = false;
            mapIndexToPattern_Basic[j] = 0;
            ++j;

            for (int i = 0; i < devicePatterns; ++i)
            {
                Log.v(LOGNAME, "Adding custom pattern i=" + i + " j=" + j + " => " + patternNames_Device[i]);

                listNames_Basic[j] = patternNames_Device[i];
                listEnables_Basic[j] = true;
                mapIndexToPattern_Basic[j] = k;
                mapPatternToIndex_Basic[k] = j;
                patternNames_Basic[k] = patternNames_Device[i];
                patternHelp_Basic[k] = patternHelp_Device[i];
                patternCmds_Basic[k] = patternCmds_Device[i];
                patternBits_Basic[k] = patternBits_Device[i];

                ++j;
                ++k;
            }
        }

        if (useExtPatterns)
        {
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
                patternNames_Basic[k] = basicPatternNames[i];
                patternHelp_Basic[k] = basicPatternHelp[i];
                patternCmds_Basic[k] = basicPatternCmds[i];
                patternBits_Basic[k] = basicPatternBits[i];

                ++j;
                ++k;
            }
        }
    }

    private static void CreateListArrays_Adv()
    {
        Log.d(LOGNAME, "Creating Advanced List Array");

        int j = 0;
        int k = 0;
        int extra = 2;
        if (devicePatterns > 0) ++extra;
        int n = devicePatterns + basicPatternsCount + advPatternsCount;

        listNames_All = new String[n + extra];
        listEnables_All = new boolean[n + extra];
        mapIndexToPattern_All = new int[n + extra];
        mapPatternToIndex_All = new int[n];
        patternNames_All = new String[n];
        patternHelp_All = new String[n];
        patternCmds_All = new String[n];
        patternBits_All = new int[n];

        if (devicePatterns > 0)
        {
            listNames_All[j] = "Custom Patterns";
            listEnables_All[j] = false;
            mapIndexToPattern_All[j] = 0;
            ++j;

            for (int i = 0; i < devicePatterns; ++i)
            {
                Log.v(LOGNAME, "Adding custom pattern j=" + j + " k=" + k + " => " + patternNames_Device[i]);

                listNames_All[j] = patternNames_Device[i];
                listEnables_All[j] = true;
                mapIndexToPattern_All[j] = k;
                mapPatternToIndex_All[k] = j;
                patternNames_All[k] = patternNames_Device[i];
                patternHelp_All[k] = patternHelp_Device[i];
                patternCmds_All[k] = patternCmds_Device[i];
                patternBits_All[k] = patternBits_Device[i];

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
            patternNames_All[k] = basicPatternNames[i];
            patternHelp_All[k] = basicPatternHelp[i];
            patternCmds_All[k] = basicPatternCmds[i];
            patternBits_All[k] = basicPatternBits[i];

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
            patternNames_All[k] = advPatternNames[i];
            patternHelp_All[k] = advPatternHelp[i];
            patternCmds_All[k] = advPatternCmds[i];
            patternBits_All[k] = advPatternBits[i];

            ++j;
            ++k;
        }
    }
}
