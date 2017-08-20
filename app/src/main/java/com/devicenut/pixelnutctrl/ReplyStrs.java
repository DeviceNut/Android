package com.devicenut.pixelnutctrl;

import android.content.Intent;
import android.util.Log;

import static com.devicenut.pixelnutctrl.Main.MAXVAL_HUE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PERCENT;
import static com.devicenut.pixelnutctrl.Main.TITLE_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.advPatternBits;
import static com.devicenut.pixelnutctrl.Main.advPatternCmds;
import static com.devicenut.pixelnutctrl.Main.advPatternHelp;
import static com.devicenut.pixelnutctrl.Main.advPatternNames;
import static com.devicenut.pixelnutctrl.Main.advPatternsCount;
import static com.devicenut.pixelnutctrl.Main.basicPatternBits;
import static com.devicenut.pixelnutctrl.Main.basicPatternCmds;
import static com.devicenut.pixelnutctrl.Main.basicPatternHelp;
import static com.devicenut.pixelnutctrl.Main.basicPatternNames;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.countLayers;
import static com.devicenut.pixelnutctrl.Main.countPixels;
import static com.devicenut.pixelnutctrl.Main.countTracks;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.curPattern;
import static com.devicenut.pixelnutctrl.Main.customPatterns;
import static com.devicenut.pixelnutctrl.Main.customPlugins;
import static com.devicenut.pixelnutctrl.Main.devPatternBits;
import static com.devicenut.pixelnutctrl.Main.devPatternCmds;
import static com.devicenut.pixelnutctrl.Main.devPatternHelp;
import static com.devicenut.pixelnutctrl.Main.devPatternNames;
import static com.devicenut.pixelnutctrl.Main.editPatterns;
import static com.devicenut.pixelnutctrl.Main.maxlenCmdStrs;
import static com.devicenut.pixelnutctrl.Main.numPatterns;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.posSegCount;
import static com.devicenut.pixelnutctrl.Main.posSegStart;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.stdPatternsCount;
import static com.devicenut.pixelnutctrl.Main.xmodeEnabled;
import static com.devicenut.pixelnutctrl.Main.xmodeHue;
import static com.devicenut.pixelnutctrl.Main.xmodePixCnt;
import static com.devicenut.pixelnutctrl.Main.xmodeWhite;

class ReplyStrs
{
    private final String LOGNAME = "ReplyStrs";

    private int replyState;
    private int optionLines;
    private boolean replyFail;
    private boolean didFinishReading;

    private boolean setPercentage;
    private boolean getSegments;
    private boolean getPatterns;
    private boolean getPlugins;

    double progressPercent;
    double progressPcentInc;
    String sendCmdStr;

    ReplyStrs()
    {
        replyState = 0;
        replyFail = false;
        didFinishReading = false;
    }

    int Next(String reply)
    {
        if (replyFail || didFinishReading)
        {
            Log.e(LOGNAME, "Reply after finish: " + reply);
            replyFail = true;
            return 0;
        }
        else if ((replyState > 1) && (optionLines <= 0))
        {
            Log.w(LOGNAME, "Unexpected reply: " + reply);
            replyFail = true;
        }
        else if (getSegments)
        {
            if (replyState == 1)
            {
                String[] strs = reply.split(" ");
                for (int i = 0, j = 0; i < strs.length; ++j)
                {
                    int val1 = Integer.parseInt(strs[i++]);
                    int val2 = Integer.parseInt(strs[i++]);
                    Log.v(LOGNAME, "Segment " + i + ": " + val2 + "." + val2);

                    if ((val1 < 0) || (val1 >= countPixels-1) ||
                            (val2 < 0) || (val2 >= (countPixels-val1)))
                    {
                        replyFail = true;
                        break;
                    }

                    posSegStart[j] = val1;
                    posSegCount[j] = val2;

                    if (i >= 12) break; // only support for 6 segments, just ignore if more
                }

                getSegments = false; // only single line to read
                replyState = 2; // trigger completion
                optionLines = 0;
            }
            else replyFail = true;
        }
        else if (getPatterns)
        {
            int index = (replyState-1)/3;
            if (index < customPatterns)
            {
                int line = ((replyState-1) % 3);

                     if (line == 0) devPatternNames[index] = new String(reply);
                else if (line == 1) devPatternHelp[index] = new String(reply);
                else
                {
                    devPatternCmds[index] = new String(reply);
                    devPatternBits[index] = 0;

                    boolean haveforce = false;
                    String[] strs = reply.split("\\s+");

                    for (int i = 0; i < strs.length; ++i)
                    {
                        if (strs[i].length() <= 0) continue; // shouldn't happen?

                        if ((strs[i].charAt(0) == 'Q') && (strs[i].length() > 1))
                        {
                            int val = Integer.parseInt(strs[i].substring(1));
                            devPatternBits[index] |= val;
                        }
                        else if (strs[i].charAt(0) == 'F') haveforce = true;
                        else if (strs[i].charAt(0) == 'I')
                        {
                            devPatternBits[index] |= 0x10;
                            if (haveforce) devPatternBits[index] |= 0x20;
                        }
                    }
                }

                if (--optionLines == 0) getPatterns = false; // finished with patterns

                else ++replyState;
            }
            else replyFail = true;
        }
        else if (getPlugins)
        {
            throw new NullPointerException("Custom Plugins Not Supported Yet");
        }
        else switch(replyState)
            {
                case 0: // first line: title
                {
                    if (reply.contains(TITLE_PIXELNUT)) ++replyState;
                    else Log.w(LOGNAME, "Expected title: " + reply);
                    progressPercent = 0;
                    progressPcentInc = 15;
                    break;
                }
                case 1: // second line: # of additional lines
                {
                    String[] strs = reply.split(" ");
                    optionLines = Integer.parseInt(reply);
                    if ((strs.length == 1) && (optionLines >= 3))
                        ++replyState;

                    else replyFail = true;

                    progressPcentInc = 100/optionLines;
                    break;
                }
                case 2: // additional line 1: 4 device constants
                {
                    String[] strs = reply.split(" ");
                    if (strs.length == 4)
                    {
                        countPixels = Integer.parseInt(strs[0]);
                        countLayers = Integer.parseInt(strs[1]);
                        countTracks = Integer.parseInt(strs[2]);
                        rangeDelay = Integer.parseInt(strs[3]);

                        Log.d(LOGNAME, "Constants: Pixels=" + countPixels + " Layers=" + countLayers + " Tracks=" + countTracks + " RangeDelay=" + rangeDelay);

                        if (!CheckValue(countPixels, 1, 0) ||
                                !CheckValue(countLayers, 2, 0) ||
                                !CheckValue(countTracks, 1, 0))
                            replyFail = true;
                    }
                    else replyFail = true;

                    if (!replyFail)
                    {
                        ++replyState;
                        --optionLines;
                    }
                    break;
                }
                case 3: // additional line 2: 4 extern mode values
                {
                    String[] strs = reply.split(" ");
                    if (strs.length == 4)
                    {
                        xmodeEnabled = (Integer.parseInt(strs[0]) != 0);
                        xmodeHue = Integer.parseInt(strs[1]);
                        xmodeWhite = Integer.parseInt(strs[2]);
                        xmodePixCnt = Integer.parseInt(strs[3]);

                        Log.d(LOGNAME, "Externs: Enable=" + xmodeEnabled + " Hue=" + xmodeHue + " White=" + xmodeWhite + " PixCnt=" + xmodePixCnt);

                        if (!CheckValue(xmodeHue, 0, MAXVAL_HUE) ||
                                !CheckValue(xmodeWhite, 0, MAXVAL_PERCENT) ||
                                !CheckValue(xmodePixCnt, 0, MAXVAL_PERCENT))
                            replyFail = true;
                    }
                    else replyFail = true;

                    if (!replyFail)
                    {
                        ++replyState;
                        --optionLines;
                    }
                    break;
                }
                case 4: // additional line 3: 3 current settings
                {
                    String[] strs = reply.split(" ");
                    if (strs.length == 3)
                    {
                        curPattern = Integer.parseInt(strs[0]);
                        curDelay = Integer.parseInt(strs[1]);
                        curBright = Integer.parseInt(strs[2]);

                        Log.d(LOGNAME, "Current: Pattern=" + curPattern + " Delay=" + curDelay + " Bright=" + curBright);

                        if (curPattern == 0) curPattern = 1; // ok to reset to default
                        if (CheckValue(curBright, 0, MAXVAL_PERCENT))
                        {
                            // allow for bad current delay value
                            if (curDelay < -rangeDelay) curDelay = -rangeDelay;
                            else if (curDelay > rangeDelay) curDelay = rangeDelay;
                        }
                        else replyFail = true;
                    }
                    else replyFail = true;

                    if (!replyFail)
                    {
                        ++replyState;
                        --optionLines;
                    }
                    break;
                }
                case 5: // additional line 4: 4 current settings
                {
                    String[] strs = reply.split(" ");
                    if (strs.length == 4)
                    {
                        numSegments = Integer.parseInt(strs[0]);
                        customPatterns = Integer.parseInt(strs[1]);
                        customPlugins = Integer.parseInt(strs[2]);
                        maxlenCmdStrs = Integer.parseInt(strs[3]);
                        Log.d(LOGNAME, "Xinfo: Segments=" + numSegments + " XPatterns=" + customPatterns + " XPlugins=" + customPlugins + " MaxCmdStr=" + maxlenCmdStrs);

                        if ((numSegments < 1)    ||
                            (customPlugins < 0)  ||
                            (maxlenCmdStrs < 80))
                            replyFail = true;

                        else if (customPatterns > 0) // indicates fixed internal device patterns
                        {
                            //customPatterns = -customPatterns;
                            editPatterns = false;
                            stdPatternsCount = 0; // prevent using patterns defined here
                        }
                        else stdPatternsCount = basicPatternsCount + advPatternsCount;

                        if (!replyFail)
                        {
                            // range check the pattern and reset if out of bounds, but don't fail
                            if (!CheckValue(curPattern, 1, numPatterns)) curPattern = 1;

                            getSegments = (numSegments > 1);
                            getPatterns = (customPatterns > 0);
                            getPlugins = (customPlugins > 0);

                            numPatterns = customPatterns + stdPatternsCount;
                            Log.v(LOGNAME, "Total patterns=" + numPatterns);

                            if (!getPatterns)
                            {
                                devPatternNames = new String[numPatterns];
                                devPatternHelp  = new String[numPatterns];
                                devPatternCmds  = new String[numPatterns];
                                devPatternBits  = new int[numPatterns];
                            }

                            setPercentage = (getSegments || getPatterns || getPlugins);

                            ++replyState;
                            --optionLines;
                        }
                    }
                    else replyFail = true;

                    break;
                }
                default: // ignore for forward compatibility
                {
                    Log.w(LOGNAME, "Unknown settings: " + reply);
                    --optionLines;
                    break;
                }
            }

        if (replyFail)
        {
            Log.e(LOGNAME, "Read failed: state=" + replyState);
            return -1;
        }
        else if ((replyState <= 1) || (optionLines != 0))
            return 1; // post progress

        boolean moreinfo = false;

        if (setPercentage)
        {
            progressPcentInc = 100.0 / ((numSegments-1) + (customPatterns*2) + (customPlugins*2));
            progressPercent = progressPcentInc;
            setPercentage = false;

            Log.v(LOGNAME, "ProgressPercentageInc=" + (int)progressPcentInc);
        }

        if (getSegments)
        {
            sendCmdStr = "?S";
            moreinfo = true;
            optionLines = 1;
        }
        else if (getPatterns)
        {
            sendCmdStr = "?P";
            moreinfo = true;
            optionLines = customPatterns*3;

            devPatternNames = new String[numPatterns];
            devPatternHelp  = new String[numPatterns];
            devPatternCmds  = new String[numPatterns];
            devPatternBits  = new int[numPatterns];
        }
        else if (getPlugins)
        {
            sendCmdStr = "?X";
            moreinfo = true;
            optionLines = customPlugins*2;
        }

        if (moreinfo)
        {
            replyState = 1;
            return 2; // send new command
        }

        if (stdPatternsCount > 0)
        {
            int i;
            for (i = 0; i < basicPatternsCount; ++i)
            {
                devPatternNames[i] = basicPatternNames[i];
                devPatternHelp[ i] = basicPatternHelp[i];
                devPatternCmds[ i] = basicPatternCmds[i];
                devPatternBits[ i] = basicPatternBits[i];
            }

            int j = i;
            for (i = 0; i < advPatternsCount; ++i)
            {
                devPatternNames[i+j] = advPatternNames[i];
                devPatternHelp[ i+j] = advPatternHelp[i];
                devPatternCmds[ i+j] = advPatternCmds[i];
                devPatternBits[ i+j] = advPatternBits[i];
            }
        }

        didFinishReading = true;
        return 3; // finished - goto Controls activity
    }

    private boolean CheckValue(int val, int min, int max)
    {
        if (val < min) return false;
        if ((0 < max) && (max < val)) return false;
        return true;
    }
}
