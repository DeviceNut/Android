package com.devicenut.pixelnutctrl;

import android.util.Log;

import static com.devicenut.pixelnutctrl.Main.DEVSTAT_FAILED;
import static com.devicenut.pixelnutctrl.Main.MAXLEN_BLE_CHUNK;
import static com.devicenut.pixelnutctrl.Main.ble;
import static com.devicenut.pixelnutctrl.Main.devIsBLE;
import static com.devicenut.pixelnutctrl.Main.isConnected;
import static com.devicenut.pixelnutctrl.Main.msgWriteEnable;
import static com.devicenut.pixelnutctrl.Main.msgWriteQueue;
import static com.devicenut.pixelnutctrl.Main.wifi;
import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;

class MsgQueue extends Thread
{
    private static final String LOGNAME = "MsgQueue";

    private static String[] writeChunks = null;
    private static int nextChunk = 0;

    //public void MsgQueue() {}

    public void run()
    {
        Log.i(LOGNAME, "Running thread...");

        while (isConnected)
        {
            if (msgWriteEnable) // wait for previous command to complete
            {
                if (devIsBLE && ((writeChunks != null) && (nextChunk < writeChunks.length)))
                {
                    String str = null;
                    int len = 0;
                    while ((nextChunk < writeChunks.length))
                    {
                        String chunk = writeChunks[nextChunk++];
                        int chlen = chunk.length();

                        if (chlen >= MAXLEN_BLE_CHUNK) // cannot support chunks that are too large
                            throw new NullPointerException("Chunk too large: " + chunk);

                        // +1 for space/newline separator
                        if ((len + chlen + 1) >= MAXLEN_BLE_CHUNK) break;

                        if (len > 0) str += " ";
                        len += chlen + 1;
                        str += chunk;
                    }

                    Log.v(LOGNAME, "Sending chunk: \"" + str + "\"");
                    str += "\n"; // MUST have line termination
                    ble.WriteString(str);
                }
                else
                {
                    String cmdstr = msgWriteQueue.get();
                    if (cmdstr != null)
                    {
                        Log.v(LOGNAME, "Next command: \"" + cmdstr + "\"");
                        while (true) // coalesce control commands (starts with non-alphanumeric)
                        {
                            String nextcmd = msgWriteQueue.peek();
                            if ((nextcmd == null) ||
                                    isLetter(cmdstr.charAt(0)) || isDigit(cmdstr.charAt(0)) ||
                                    !nextcmd.substring(0, 1).equals(cmdstr.substring(0, 1)))
                                break;

                            Log.v(LOGNAME, "Skipping=\"" + cmdstr + "\" (\"" + nextcmd + "\")");
                            cmdstr = msgWriteQueue.get();
                        }
                        Log.d(LOGNAME, "Send command: \"" + cmdstr + "\"");

                        if (devIsBLE)
                        {
                            if (cmdstr.length()+1 >= MAXLEN_BLE_CHUNK)
                            {
                                writeChunks = cmdstr.split("\\s+"); // remove ALL spaces
                                nextChunk = 0;
                                continue; // read from chunk array
                            }
                            else ble.WriteString(cmdstr + "\n");
                        }
                        else wifi.WriteString(cmdstr);
                    }
                }
            }

            yield(); // give scheduler notice we're not busy
        }
    }
}
