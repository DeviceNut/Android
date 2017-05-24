package com.devicenut.pixelnutctrl;

import android.app.Application;

public class Main extends Application
{
    public static final String TITLE_PIXELNUT = "PixelNut!";
    public static final String URL_PIXELNUT = "http://www.pixelnutstore.com";

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

    public static String devName;
}
