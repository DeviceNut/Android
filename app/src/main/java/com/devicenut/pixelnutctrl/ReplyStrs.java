package com.devicenut.pixelnutctrl;

import android.util.Log;

import static com.devicenut.pixelnutctrl.Main.TITLE_PIXELNUT;
import static com.devicenut.pixelnutctrl.Main.CMD_GET_PATTERNS;
import static com.devicenut.pixelnutctrl.Main.CMD_GET_PLUGINS;
import static com.devicenut.pixelnutctrl.Main.CMD_GET_SEGMENTS;
import static com.devicenut.pixelnutctrl.Main.FEATURE_INT_PATTERNS;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_FORCE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_HUE;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_PERCENT;
import static com.devicenut.pixelnutctrl.Main.MAXVAL_WHT;
import static com.devicenut.pixelnutctrl.Main.MINLEN_CMDSTR;
import static com.devicenut.pixelnutctrl.Main.MINLEN_SEGLEN_FORADV;
import static com.devicenut.pixelnutctrl.Main.advPatternsCount;
import static com.devicenut.pixelnutctrl.Main.basicPatternsCount;
import static com.devicenut.pixelnutctrl.Main.featureBits;
import static com.devicenut.pixelnutctrl.Main.maxlenAdvPatterns;
import static com.devicenut.pixelnutctrl.Main.patternBits_Device;
import static com.devicenut.pixelnutctrl.Main.patternCmds_Device;
import static com.devicenut.pixelnutctrl.Main.patternHelp_Device;
import static com.devicenut.pixelnutctrl.Main.patternNames_Device;
import static com.devicenut.pixelnutctrl.Main.haveBasicSegs;
import static com.devicenut.pixelnutctrl.Main.segBasicOnly;
import static com.devicenut.pixelnutctrl.Main.curBright;
import static com.devicenut.pixelnutctrl.Main.curDelay;
import static com.devicenut.pixelnutctrl.Main.devicePatterns;
import static com.devicenut.pixelnutctrl.Main.customPlugins;
import static com.devicenut.pixelnutctrl.Main.initPatterns;
import static com.devicenut.pixelnutctrl.Main.multiStrands;
import static com.devicenut.pixelnutctrl.Main.useAdvPatterns;
import static com.devicenut.pixelnutctrl.Main.useExtPatterns;
import static com.devicenut.pixelnutctrl.Main.maxlenCmdStrs;
import static com.devicenut.pixelnutctrl.Main.numPatterns;
import static com.devicenut.pixelnutctrl.Main.numSegments;
import static com.devicenut.pixelnutctrl.Main.segPosCount;
import static com.devicenut.pixelnutctrl.Main.segPosStart;
import static com.devicenut.pixelnutctrl.Main.rangeDelay;
import static com.devicenut.pixelnutctrl.Main.segPatterns;
import static com.devicenut.pixelnutctrl.Main.segTrigForce;
import static com.devicenut.pixelnutctrl.Main.segXmodeCnt;
import static com.devicenut.pixelnutctrl.Main.segXmodeEnb;
import static com.devicenut.pixelnutctrl.Main.segXmodeHue;
import static com.devicenut.pixelnutctrl.Main.segXmodeWht;
import static com.devicenut.pixelnutctrl.Main.segLayers;
import static com.devicenut.pixelnutctrl.Main.segPixels;
import static com.devicenut.pixelnutctrl.Main.segTracks;

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
        setPercentage = false;
        didFinishReading = false;
    }

    private void CheckSegVals(int i)
    {
        if (!CheckValue(segPatterns[i],  0, numPatterns-1))  segPatterns[i] = 0;
        if (!CheckValue(segXmodeHue[i],  0, MAXVAL_HUE))     segXmodeHue[i] = 0;
        if (!CheckValue(segXmodeWht[i],  0, MAXVAL_WHT))     segXmodeWht[i] = MAXVAL_WHT;
        if (!CheckValue(segXmodeCnt[i],  0, MAXVAL_PERCENT)) segXmodeCnt[i] = 0;
        if (!CheckValue(segTrigForce[i], 0, MAXVAL_FORCE))   segTrigForce[i] = MAXVAL_FORCE >> 1;
    }

    private void CheckForExtendedCommands()
    {
        getSegments = (numSegments > 1);
        getPatterns = (devicePatterns > 0);
        getPlugins = (customPlugins > 0);
        setPercentage = (getSegments || getPatterns || getPlugins);
    }

    int Next(String reply)
    {
        Log.v(LOGNAME, "ReplyState=" + replyState + " OptionLines=" + optionLines);

        if (replyFail)
        {
            Log.w(LOGNAME, "Have already failed");
            return 0;
        }
        else if (didFinishReading)
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
                if (multiStrands)
                {
                    String[] strs = reply.split("\\s+"); // remove ALL spaces
                    for (int i = 0, j = 0; (i + 1) < strs.length; ++j)
                    {
                        if (i >= (3 * segPixels.length)) break; // prevent overrun

                        int val1 = Integer.parseInt(strs[i++]);
                        int val2 = Integer.parseInt(strs[i++]);
                        int val3 = Integer.parseInt(strs[i++]);
                        Log.d(LOGNAME, ">> Segment Info " + j + ": " + val1 + ":" + val2 + ":" + val3);

                        segPixels[j] = val1;
                        segLayers[j] = val2;
                        segTracks[j] = val3;
                        segBasicOnly[j] = false;

                        if (!CheckValue(segPixels[j], 1, 0) ||
                            !CheckValue(segLayers[j], 2, 0) ||
                            !CheckValue(segTracks[j], 1, 0))
                        {
                            replyFail = true;
                            break;
                        }

                        if (strs.length == 3) // only first set of values has been sent
                        {
                            while (++j < numSegments)
                            {
                                segPixels[j] = val1;
                                segLayers[j] = val2;
                                segTracks[j] = val3;
                                segBasicOnly[j] = false;
                            }
                            break;
                        }
                    }
                }
                else
                {
                    String[] strs = reply.split("\\s+"); // remove ALL spaces
                    for (int i = 0, j = 0; (i + 1) < strs.length; ++j)
                    {
                        if (i >= (2 * segPosStart.length)) break; // prevent overrun

                        int val1 = Integer.parseInt(strs[i++]);
                        int val2 = Integer.parseInt(strs[i++]);
                        Log.d(LOGNAME, ">> Segment Extents " + j + ": " + val1 + ":" + val2);

                        segPosStart[j] = val1;
                        segPosCount[j] = val2;

                        // if can use advanced patterns, but a given segment is very short,
                        // then only allow basic patterns on that particular segment
                        if (useAdvPatterns && (val2 < MINLEN_SEGLEN_FORADV))
                        {
                            Log.w(LOGNAME, "No advanced patterns for segment=" + j);
                            segBasicOnly[j] = true;
                            haveBasicSegs = true;
                        }
                        else segBasicOnly[j] = false;
                    }
                }
            }
            else if (replyState <= numSegments+1)
            {
                int segindex = replyState-2;
                Log.v(LOGNAME, "SegValues[" + segindex + "]: " + reply);

                String[] strs = reply.split("\\s+"); // remove ALL spaces
                if (strs.length >= 8)
                {
                    curBright[   segindex] = Integer.parseInt(strs[0]);
                    curDelay[    segindex] = (byte)Integer.parseInt(strs[1]);
                    segPatterns[ segindex] = Integer.parseInt(strs[2]);
                    segXmodeEnb[ segindex] = strs[3].charAt(0) != '0';
                    segXmodeHue[ segindex] = Integer.parseInt(strs[4]) + (Integer.parseInt(strs[5]) << 8);
                    segXmodeWht[ segindex] = Integer.parseInt(strs[6]);
                    segXmodeCnt[ segindex] = Integer.parseInt(strs[7]);
                    segTrigForce[segindex] = Integer.parseInt(strs[8]) + (Integer.parseInt(strs[9]) << 8);

                    if (segPatterns[segindex] > 0) segPatterns[segindex] -= 1; // device patterns start at 1

                    if (!CheckValue(curDelay[segindex], -rangeDelay, rangeDelay))
                    {
                        curDelay[segindex] = 0;
                        Log.v(LOGNAME, "Adjusting delay=" + curDelay[segindex]);
                    }

                    if (!CheckValue(curBright[segindex], 0, MAXVAL_PERCENT))
                    {
                        curBright[segindex] = MAXVAL_PERCENT;
                        Log.v(LOGNAME, "Adjusting bright=" + curBright[segindex]);
                    }

                    Log.d(LOGNAME, ">> Bright=" + curBright[segindex] + " Delay=" + curDelay[segindex]);
                    Log.d(LOGNAME, ">> Pattern=" + segPatterns[segindex] + " Mode=" + segXmodeEnb[segindex] + " Force=" + segTrigForce[segindex]);
                    Log.d(LOGNAME, ">> Hue=" + segXmodeHue[segindex] + " White=" + segXmodeWht[segindex] + " Count=" + segXmodeCnt[segindex]);

                    CheckSegVals(segindex);
                }
                else replyFail = true;
            }
            else replyFail = true;

            if (!replyFail)
            {
                if (--optionLines == 0) getSegments = false; // finished with segments
                else ++replyState;
            }
        }
        else if (getPatterns)
        {
            int index = (replyState-1)/3;
            if (index < devicePatterns)
            {
                int line = ((replyState-1) % 3);

                     if (line == 0) patternNames_Device[index] = reply;
                else if (line == 1) patternHelp_Device[index] = reply.replace('\t', '\n');
                else
                {
                    patternCmds_Device[index] = reply;
                    patternBits_Device[index] = 0;

                    boolean haveforce = false;
                    String[] strs = reply.split("\\s+"); // remove ALL spaces

                    for (int i = 0; i < strs.length; ++i)
                    {
                        if (strs[i].length() <= 0) continue; // shouldn't happen?

                        if ((strs[i].charAt(0) == 'Q') && (strs[i].length() > 1))
                        {
                            int val = Integer.parseInt(strs[i].substring(1));
                            patternBits_Device[index] |= val;
                        }
                        else if ((strs[i].charAt(0) == 'F') && (strs[i].length() > 1) && (strs[i].charAt(1) != '0')) // ignore zero-force setting
                            haveforce = true;

                        else if (strs[i].charAt(0) == 'I')
                        {
                            patternBits_Device[index] |= 0x10;
                            if (haveforce) patternBits_Device[index] |= 0x20;
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
        else switch(replyState+1)
        {
            case 1: // first line: title string and line count
            {
                String[] strs = reply.split("\\s+"); // remove ALL spaces
                if (strs.length >= 2)
                {
                    if (strs[0].contains(TITLE_PIXELNUT))
                    {
                        optionLines = Integer.parseInt(strs[1]);
                        Log.v(LOGNAME, ">> Option lines = " + optionLines);

                        if (optionLines < 2)
                        {
                            Log.e(LOGNAME, "Expected at least 2 option lines");
                            replyFail = true;
                        }
                    }
                    else
                    {
                        Log.e(LOGNAME, "Unexpected title: " + reply);
                        replyFail = true;
                    }

                }
                else
                {
                    Log.e(LOGNAME, "Expected 2 parameters on line 1");
                    replyFail = true;
                }

                if (!replyFail)
                {
                    progressPercent = 0;
                    progressPcentInc = 101/optionLines;
                    Log.v(LOGNAME, "ProgressPercentageInc=" + (int)progressPcentInc);

                    ++replyState;
                    --optionLines;
                }
                break;
            }
            case 2: // second line: 6 device settings for all configurations
            {
                String[] strs = reply.split("\\s+"); // remove ALL spaces
                if (strs.length >= 6)
                {
                    numSegments     = Integer.parseInt(strs[0]);
                    segPatterns[0]  = Integer.parseInt(strs[1]);
                    devicePatterns  = Integer.parseInt(strs[2]);
                    featureBits     = Integer.parseInt(strs[3]);
                    customPlugins   = Integer.parseInt(strs[4]);
                    maxlenCmdStrs   = Integer.parseInt(strs[5]);

                    if (numSegments < 0)
                    {
                        multiStrands = false;
                        numSegments = -numSegments;

                        if (devicePatterns > 0)
                        {
                            Log.e(LOGNAME, "Cannot have device patterns AND logical segments");
                            replyFail = true;
                        }
                    }
                    else if (numSegments > 1) multiStrands = true;

                    if (maxlenCmdStrs < MINLEN_CMDSTR)
                    {
                        Log.e(LOGNAME, "Cmd/Pattern string is too short: len=" + maxlenCmdStrs);
                        replyFail = true;
                    }
                    else if (!replyFail)
                    {
                        if (segPatterns[0] > 0)
                        {
                            segPatterns[0] -= 1; // device patterns start at 1
                            initPatterns = false;
                        }
                        else initPatterns = true; // trigger sending initial pattern to device

                        if (segPatterns[0] >= numPatterns)
                        {
                            segPatterns[0] = 0;
                            Log.v(LOGNAME, "Resetting current pattern=0");
                        }

                        if ((featureBits & FEATURE_INT_PATTERNS) != 0)
                        {
                            // can only use internal patterns
                            useExtPatterns = false;
                            useAdvPatterns = false;
                            numPatterns = 0;

                            haveBasicSegs = true;
                            for (int i = 0; i < numSegments; ++i)
                                segBasicOnly[i] = true;
                        }
                        else if (((numSegments > 1) && !multiStrands && (maxlenCmdStrs < (maxlenAdvPatterns * numSegments))) ||
                                 (maxlenCmdStrs < (maxlenAdvPatterns * numSegments)))
                        {
                            // if command/pattern string not long enough then must use only basic patterns
                            useExtPatterns = true;
                            useAdvPatterns = false;
                            numPatterns = basicPatternsCount;

                            haveBasicSegs = true;
                            for (int i = 0; i < numSegments; ++i)
                                segBasicOnly[i] = true;
                        }
                        else
                        {
                            useExtPatterns = true;
                            useAdvPatterns = true;
                            numPatterns = basicPatternsCount + advPatternsCount;
                        }
                        numPatterns += devicePatterns;

                        if (numSegments < 1) numSegments = 1;
                        if (customPlugins < 0) customPlugins = 0;

                        Log.d(LOGNAME, ">> Segments=" + numSegments + ((numSegments > 1) ? (multiStrands ? " (physical)" : " (logical)") : ""));
                        Log.d(LOGNAME, ">> CmdStrLen=" + maxlenCmdStrs);
                        Log.d(LOGNAME, ">> CurPattern=" + segPatterns[0] + " DoInit=" + initPatterns);
                        Log.d(LOGNAME, ">> DevicePatterns=" + devicePatterns + " Advanced=" + useAdvPatterns);
                        Log.d(LOGNAME, ">> Total patterns=" + numPatterns);
                        Log.d(LOGNAME, ">> CustomPlugins=" + customPlugins);
                        Log.v(LOGNAME, ">> Features=0x" + Integer.toHexString(featureBits));

                        if (devicePatterns > 0)
                        {
                            patternNames_Device = new String[devicePatterns];
                            patternHelp_Device = new String[devicePatterns];
                            patternCmds_Device = new String[devicePatterns];
                            patternBits_Device = new int[devicePatterns];
                        }
                    }
                }
                else
                {
                    Log.e(LOGNAME, "Expected 6 parameters on line 2");
                    replyFail = true;
                }

                if (!replyFail)
                {
                    progressPcentInc = 101/(optionLines+1);
                    Log.v(LOGNAME, "ProgressPercentageInc=" + (int)progressPcentInc);

                    ++replyState;
                    --optionLines;
                    if (optionLines == 0) CheckForExtendedCommands();
                }
                break;
            }
            case 3: // third line: 5 more settings (if not multiple physical segments)
            {
                String[] strs = reply.split("\\s+"); // remove ALL spaces
                if (strs.length >= 5)
                {
                    segPixels[0] = Integer.parseInt(strs[0]);
                    segLayers[0] = Integer.parseInt(strs[1]);
                    segTracks[0] = Integer.parseInt(strs[2]);
                    curBright[0] = Integer.parseInt(strs[3]);
                    curDelay[0]  = Integer.parseInt(strs[4]);

                    if (!CheckValue(segPixels[0], 1, 0) ||
                        !CheckValue(segLayers[0], 2, 0) ||
                        !CheckValue(segTracks[0], 1, 0))
                    {
                        Log.e(LOGNAME, "Unexpected values on line 3");
                        replyFail = true;
                    }
                    else
                    {
                        if (!CheckValue(curDelay[0], -rangeDelay, rangeDelay))
                        {
                            curDelay[0] = 0;
                            Log.v(LOGNAME, "Adjusting delay=" + curDelay[0]);
                        }

                        if (!CheckValue(curBright[0], 0, MAXVAL_PERCENT))
                        {
                            curBright[0] = MAXVAL_PERCENT;
                            Log.v(LOGNAME, "Adjusting bright=" + curBright[0]);
                        }

                        Log.d(LOGNAME, ">> Pixels=" + segPixels[0] + " Layers=" + segLayers[0] + " Tracks=" + segTracks[0]);
                        Log.d(LOGNAME, ">> Bright=" + curBright[0] + " Delay=" + curDelay[0]);
                    }
                }
                else
                {
                    Log.e(LOGNAME, "Expected 5 parameters on line 3");
                    replyFail = true;
                }

                if (!replyFail)
                {
                    ++replyState;
                    --optionLines;
                    if (optionLines == 0) CheckForExtendedCommands();
                }
                break;
            }
            case 4: // fourth line: 5 extern mode values (if not multiple segments)
            {
                String[] strs = reply.split("\\s+"); // remove ALL spaces
                if (strs.length >= 5)
                {
                    segXmodeEnb[0] = (Integer.parseInt(strs[0]) != 0);
                    segXmodeHue[0] = Integer.parseInt(strs[1]);
                    segXmodeWht[0] = Integer.parseInt(strs[2]);
                    segXmodeCnt[0] = Integer.parseInt(strs[3]);
                    segTrigForce[0] = Integer.parseInt(strs[4]);

                    Log.d(LOGNAME, ">> Enable=" + segXmodeEnb[0] + " Hue=" + segXmodeHue[0] + " White=" + segXmodeWht[0]);
                    Log.d(LOGNAME, ">> Count=" + segXmodeCnt[0] + " Force=" + segTrigForce[0]);

                    CheckSegVals(0);
                }
                else
                {
                    Log.e(LOGNAME, "Expected 5 parameters on line 4");
                    replyFail = true;
                }

                if (!replyFail)
                {
                    ++replyState;
                    --optionLines;
                    if (optionLines == 0) CheckForExtendedCommands();
                }
                break;
            }
            default: // ignore for forward compatibility
            {
                int line = replyState + 1;
                Log.w(LOGNAME, "Line=" + line + " Reply=" + reply);

                --optionLines;
                if (optionLines == 0) CheckForExtendedCommands();
                break;
            }
        }

        if (replyFail)
        {
            Log.e(LOGNAME, "Read failed: state=" + replyState);
            return -1;
        }
        else if ((replyState <= 1) || (optionLines != 0))
        {
            Log.v(LOGNAME, "Post progress...");
            return 1; // post progress
        }

        boolean moreinfo = false;

        if (setPercentage)
        {
            // use 101 to insure the progress bar fills up entirely
            progressPcentInc = 101.0 / ((getSegments ? (numSegments+1) : 0) + (devicePatterns *3) + (customPlugins*2));
            progressPercent = -progressPcentInc; // incremented first, so start at 0
            setPercentage = false;

            Log.v(LOGNAME, "ProgressPercentageInc=" + (int)progressPcentInc);
        }

        if (getSegments)
        {
            sendCmdStr = CMD_GET_SEGMENTS;
            optionLines = numSegments+1;
            moreinfo = true;
        }
        else if (getPatterns)
        {
            sendCmdStr = CMD_GET_PATTERNS;
            optionLines = devicePatterns *3;
            moreinfo = true;
        }
        else if (getPlugins)
        {
            sendCmdStr = CMD_GET_PLUGINS;
            optionLines = customPlugins*2;
            moreinfo = true;
        }

        if (moreinfo)
        {
            replyState = 1;
            Log.v(LOGNAME, "Send new command...");
            return 2; // send new command
        }

        didFinishReading = true;
        Log.v(LOGNAME, "Finished !!!");
        return 3; // finished - goto Controls activity
    }

    private boolean CheckValue(int val, int min, int max)
    {
        if (val < min) return false;
        if ((0 < max) && (max < val)) return false;
        return true;
    }
}
