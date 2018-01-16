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
        Log.v(LOGNAME, "CmdPause=" + (cmdPauseEnable ? "on" : "off"));
        if (cmdPauseEnable) SleepMsecs(300);
        ble.WriteString(str);
    }

    public void run()
    {
        Log.i(LOGNAME, "Running thread...");
        setThreadPriority(-10);

        StringBuilder strSequence = new StringBuilder(1000);
        String[] writeChunks = null;
        int nextChunk = 0;

        while (isConnected)
        {
            if (msgWriteEnable) // else wait for previous command to complete
            {
                if (devIsBLE && ((writeChunks != null) && (nextChunk < writeChunks.length)))
                {
                    StringBuilder str = new StringBuilder(MAXLEN_BLE_CHUNK);

                    while ((nextChunk < writeChunks.length))
                    {
                        String chunk = writeChunks[nextChunk];
                        Log.v(LOGNAME, "Chunk str=\"" + chunk + "\"");

                        if (chunk.length() >= MAXLEN_BLE_CHUNK) // cannot support chunks that are too large
                            throw new NullPointerException("Chunk too large: " + chunk);

                        // +1 for space or newline separator
                        if ((str.length() + chunk.length() + 1) >= MAXLEN_BLE_CHUNK)
                            break;

                        if (str.length() > 0) str.append(" ");
                        str.append(chunk);
                        ++nextChunk;
                    }

                    Log.v(LOGNAME, "Sending chunk: \"" + str + "\"");
                    str.append("\n"); // MUST be terminated

                    msgWriteEnable = false;
                    BleSendCommand(str.toString());
                }
                else
                {
                    String cmdstr = msgWriteQueue.get();

                    if (cmdstr != null)
                    {
                        if (cmdstr.equals(CMD_SEQ_END))
                        {
                            msgWriteQueue.get();
                            cmdstr = null;
                        }
                        else
                        {
                            Log.v(LOGNAME, ">Get command: \"" + cmdstr + "\"");
                            while (true) // coalesce control commands (starts with non-alphanumeric)
                            {
                                String nextcmd = msgWriteQueue.peek();
                                if ((nextcmd == null) || nextcmd.equals(CMD_SEQ_END))
                                    break;

                                Log.v(LOGNAME, "Next command: \"" + nextcmd + "\"");
                                if (isLetter(cmdstr.charAt(0)) || isDigit(cmdstr.charAt(0)) ||
                                        !nextcmd.substring(0, 1).equals(cmdstr.substring(0, 1)))
                                    break;

                                Log.v(LOGNAME, "Skipping=\"" + cmdstr + "\" (\"" + nextcmd + "\")");
                                cmdstr = msgWriteQueue.get();
                            }
                            Log.d(LOGNAME, "Send command: \"" + cmdstr + "\"");
                        }

                        if (devIsBLE)
                        {
                            if (cmdstr == null) continue; // ignore

                            else if (cmdstr.length()+1 >= MAXLEN_BLE_CHUNK)
                            {
                                writeChunks = cmdstr.split("\\s+"); // remove ALL spaces
                                nextChunk = 0;
                                continue; // read from chunk array
                            }
                            else
                            {
                                msgWriteEnable = false;
                                BleSendCommand(cmdstr + "\n"); // MUST be terminated
                            }
                        }
                        else
                        {
                            if (cmdstr == null) // send entire sequence at once for wifi
                            {
                                msgWriteEnable = false;
                                wifi.WriteString(strSequence.toString());
                                strSequence.delete(0, strSequence.length());
                            }
                            else
                            {
                                strSequence.append(cmdstr);
                                strSequence.append("\n");
                                continue; // keep draining queue
                            }
                        }
                    }
                }
            }

            yield(); // give scheduler notice we're not busy
        }
    }
}
