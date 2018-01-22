package com.devicenut.pixelnutctrl;

import android.util.Log;

import static android.os.Process.setThreadPriority;
import static com.devicenut.pixelnutctrl.Main.cmdPauseEnable;
import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;

import static com.devicenut.pixelnutctrl.Main.CMD_SEQ_END;
import static com.devicenut.pixelnutctrl.Main.MAXLEN_BLE_CHUNK;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.devIsBLE;
import static com.devicenut.pixelnutctrl.Main.isConnected;
import static com.devicenut.pixelnutctrl.Main.msgWriteEnable;
import static com.devicenut.pixelnutctrl.Main.msgWriteQueue;
import static com.devicenut.pixelnutctrl.Main.wifi;
import static com.devicenut.pixelnutctrl.Main.SleepMsecs;

class MsgQueue extends Thread
{
    private static final String LOGNAME = "MsgQueue";

    private void BleSendCommand(String str)
    {
        //Log.v(LOGNAME, "CmdPause=" + (cmdPauseEnable ? "on" : "off"));
        if (cmdPauseEnable) SleepMsecs(300);
        Log.d(LOGNAME, "Send command: \"" + str + "\"");
        ble.WriteString(str);
    }

    public void run()
    {
        Log.i(LOGNAME, "Running thread...");
        setThreadPriority(-10);

        StringBuilder bigstr = new StringBuilder(1000);
        String[] writeChunks = null;
        int nextChunk = 0;
        boolean doChunks = false;
        boolean endofseq = false;

        while (isConnected)
        {
            if (msgWriteEnable) // else wait for previous command to complete
            {
                if (devIsBLE && doChunks)
                {
                    while ((nextChunk < writeChunks.length))
                    {
                        String chunk = writeChunks[nextChunk];
                        Log.v(LOGNAME, "Chunk str=\"" + chunk + "\"");

                        if (chunk.length() >= MAXLEN_BLE_CHUNK) // cannot support chunks that are too large
                            throw new NullPointerException("Chunk too large: " + chunk);

                        // +1 for space AND newline separator
                        if ((bigstr.length() + chunk.length() + 2) >= MAXLEN_BLE_CHUNK)
                            break;

                        bigstr.append(chunk);
                        bigstr.append(" ");
                        ++nextChunk;
                    }

                    Log.v(LOGNAME, "Sending chunk: \"" + bigstr.toString() + "\"");
                    bigstr.append("\n"); // MUST be terminated

                    msgWriteEnable = false;
                    BleSendCommand(bigstr.toString());
                    bigstr.delete(0, bigstr.length());

                    doChunks = (nextChunk < writeChunks.length);
                    continue; // wait for write to complete
                }

                String cmdstr = null;
                if (!endofseq)
                {
                    cmdstr = msgWriteQueue.get();
                    if ((cmdstr != null) && cmdstr.equals(CMD_SEQ_END))
                        cmdstr = null;
                }
                // else have already seen EOS while peeking ahead
                else endofseq = false; // reset for next round

                if (cmdstr != null)
                {
                    Log.v(LOGNAME, ">Get command: \"" + cmdstr + "\"");
                    while(true) // coalesce control commands (starts with non-alphanumeric)
                    {
                        String nextcmd = msgWriteQueue.peek();
                        if (nextcmd == null) break;

                        if (nextcmd.equals(CMD_SEQ_END)) // check if have another sequence
                        {
                            msgWriteQueue.get(); // skip this and peek ahead
                            nextcmd = msgWriteQueue.peek();
                            if (nextcmd == null)
                            {
                                endofseq = true; // must remember this
                                break; // process command in cmdstr
                            }
                            // else see if nextcmd can be coalesced
                        }
                        // else see if nextcmd can be coalesced

                        Log.v(LOGNAME, "Next command: \"" + nextcmd + "\"");
                        if (isLetter(cmdstr.charAt(0)) || isDigit(cmdstr.charAt(0)) ||
                                !nextcmd.substring(0, 1).equals(cmdstr.substring(0, 1)))
                            break;

                        Log.v(LOGNAME, "Skipping=\"" + cmdstr + "\" (\"" + nextcmd + "\")");
                        cmdstr = msgWriteQueue.get();
                    }
                    Log.v(LOGNAME, "Have command: \"" + cmdstr + "\"");
                }

                // cmdstr==null means have an end-of-sequence now

                if (devIsBLE)
                {
                    boolean dosend = false;

                    if (cmdstr != null)
                    {
                        int cmdlen = cmdstr.length() + 1; // +1 for line termination

                        if (cmdlen >= MAXLEN_BLE_CHUNK)
                        {
                            writeChunks = cmdstr.split("\\s+"); // remove ALL spaces
                            nextChunk = 0;
                            doChunks = true;

                            dosend = true; // first send string so far, then handle chunks
                        }
                        else if ((bigstr.length() + cmdlen) >= MAXLEN_BLE_CHUNK)
                            dosend = true; // send command string so far
                    }
                    else dosend = true; // flush commands - end of sequence

                    if (dosend && (bigstr.length() > 0))
                    {
                        msgWriteEnable = false;
                        BleSendCommand(bigstr.toString());
                        bigstr.delete(0, bigstr.length());
                    }

                    if ((cmdstr != null) && !doChunks) // build up largest string possible
                    {
                        Log.v(LOGNAME, ">Add command: \"" + cmdstr + "\"");
                        bigstr.append(cmdstr);
                        bigstr.append("\n"); // MUST be terminated
                        // Note: do *not* have to have space at end of these
                    }
                }
                else
                {
                    if ((cmdstr == null) && (bigstr.length() > 0)) // send entire sequence at once for wifi
                    {
                        msgWriteEnable = false;
                        String str = bigstr.toString();
                        Log.d(LOGNAME, "Send command: \"" + str + "\"");
                        wifi.WriteString(str);
                        bigstr.delete(0, str.length());
                    }
                    else if (cmdstr != null)
                    {
                        Log.v(LOGNAME, ">Add command: \"" + cmdstr + "\"");
                        bigstr.append(cmdstr);
                        bigstr.append("\n");
                    }
                }
            }
            else yield(); // give scheduler notice while waiting for write to complete
        }
    }
}
