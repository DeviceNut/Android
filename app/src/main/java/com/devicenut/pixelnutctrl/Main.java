package com.devicenut.pixelnutctrl;

import android.app.Application;

public class Main extends Application
{
    public static final String TITLE_PIXELNUT = "PixelNut!";
    public static final String URL_PIXELNUT = "http://www.pixelnutstore.com";

    public static final String CMD_GETINFO2     = "??";
    public static final String CMD_GETINFO      = "?";
    public static final String CMD_BLUENAME     = "@";
    public static final String CMD_BRIGHT       = "%";
    public static final String CMD_DELAY        = ":";
    public static final String CMD_EXTMODE      = "_";
    public static final String CMD_PROPVALS     = "=";
    public static final String CMD_TRIGGER      = "!";
    public static final String CMD_PAUSE        = "[";
    public static final String CMD_RESUME       = "]";

    public static final int MAXVAL_PATTERN      = 13;
    public static final int MAXVAL_HUE          = 359;
    public static final int MAXVAL_PERCENT      = 100;

    public static final String[] patternNames =
            {
                "Rainbow Wipe     ",
                "Rainbow Roll     ",
                "Light Waves      ",
                "Blue Twinkle     ",
                "Twinkle Comets   ",
                "Dueling Comets   ",
                "Dueling Scanners ",
                "Ferris Wheel     ",
                "White Noise      ",
                "Bright Blinks    ",
                "Bright Swells    ",
                "Color Smooth     ",
                "All Together     ",
            };

    public static final String[] patternStrs =
            {
                "E2 D10 T E101 F1000 I T G",
                "E1 D10 I T E101 F1000 I T G",
                "E10 D60 Q7 T E101 I T E120 F250 I T G",
                "E0 B50 W20 H232 Q3 T E140 D10 F250 I E50 B80 W80 D10 T G",
                "E50 B65 W80 H50 Q3 T E20 L0 B90 C15 D30 O3 T6 E20 U0 B90 H30 C25 D30 I T E120 I G",
                "E20 W25 C15 D30 Q7 I T E101 F100 I T E20 U0 W25 C15 D20 Q7 I T E101 F200 I T G",
                "E40 C10 D20 Q4 F300 T E111 A0 E40 U0 H270 C5 D30 Q1 I E131 F1000 O5 T5 G",
                "E30 C40 D80 Q7 T E160 I E120 I E111 F O3 T7 G",
                "E52 W65 D20 Q5 T E150 D80 T E120 F1000 I T G",
                "E0 B50 W10 Q3 E140 D10 F250 I E51 D10 T E112 T G",
                "E0 B80 T E140 F250 Q7 I T E111 F I O10 T10 G",
                "E0 H30 D30 Q7 T E110 F600 I T E111 A1 G",
                "E50 B65 W30 H50 Q1 T V1 E40 H270 C10 D50 T E20 L0 V1 D15 C20 A1 I G",
            };

    public static int countPixels       = 0;
    public static int countLayers       = 0;
    public static int countTracks       = 0;
    public static int rangeDelay        = 80;
    public static int curPattern        = 0;
    public static int curDelay          = 0;
    public static int curBright         = 0;
    public static boolean xmodeEnabled  = false;
    public static int xmodeHue          = 0;
    public static int xmodeWhite        = 0;
    public static int xmodePixCnt       = 0;

    // extended info:
    public static int internalPatterns  = 0;    // number of internal patterns: 0 if none
    public static int maxlenSendStrs    = 0;    // max length of command string to send
    public static int maxlenEEPROM      = 0;    // max length of internal EEPROM to store pattern(s)

    public static String devName;
}
