package ru.shurikvo.apdu;

import android.nfc.tech.IsoDep;
import java.io.IOException;
import ru.shurikvo.utils.ByteMatter;

public class ApduMaster {
    public boolean isConnected = false, isError = false;
    public String SW = "", message = "";

    private IsoDep isoDepTag;
    private final ByteMatter byt = new ByteMatter();

    public int connect(IsoDep iso) {
        int RC;
        isError = false;
        StringBuilder sb = new StringBuilder();
        isoDepTag = iso;

        try {
            isoDepTag.setTimeout(5000);
            isoDepTag.connect();
            isConnected = isoDepTag.isConnected();
            if(isConnected) {
                sb.append("Карта подключена").append('\n');
                RC = 0;
            }
            else {
                sb.append("Карта не подключена").append('\n');
                RC = -1;
            }
        } catch(IOException e) {
            sb.append("Exception: ").append(e.getMessage()).append('\n');
            message = sb.toString();
            isError = true;
            return -1;
        }
        message = sb.toString();
        return RC;
    }

    public void close() {
        isError = false;
        StringBuilder sb = new StringBuilder();

        try {
            isoDepTag.close();
            isConnected = isoDepTag.isConnected();
            sb.append("Карта отключена").append('\n');
        } catch(IOException e) {
            sb.append("Exception: ").append(e.getMessage()).append('\n');
            isError = true;
        }
        message = sb.toString();
    }

    public String sendApdu(String sCmd) {
        byte[] bAPDU, result;
        String sResp = "";
        isError = false;
        StringBuilder sb = new StringBuilder();
        SW = "";

        sb.append(">> ").append(sCmd).append('\n');
        bAPDU = byt.toByteArray(sCmd);
        try {
            result = isoDepTag.transceive(bAPDU);
            sResp = byt.toHexString(result);
            if(sResp.length() >= 4) {
                SW = sResp.substring(sResp.length() - 4);
                sResp = sResp.substring(0, sResp.length() - 4);
            }
            sb.append("<< ").append(sResp).append(" ").append(SW).append('\n');
        } catch(IOException e) {
            sb.append("Exception: ").append(e.getMessage()).append('\n');
            isError = true;
        }

        message = sb.toString();
        return sResp;
    }
}
