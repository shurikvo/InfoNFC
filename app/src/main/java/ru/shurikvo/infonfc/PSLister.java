package ru.shurikvo.infonfc;

import android.nfc.tech.IsoDep;

import ru.shurikvo.apdu.ApduMaster;
import ru.shurikvo.utils.ByteMatter;
import ru.shurikvo.utils.SingleTag;
import ru.shurikvo.utils.TLVParser;

public class PSLister {
    //public String message = "";

    private ApduMaster apdu = null;
    private final ByteMatter byt = new ByteMatter();

    private String getAppInfo(String aid, String name) {
        int RC;
        String sCmd, sResp;
        StringBuilder sb = new StringBuilder();

        sCmd = String.format("00A40400%02X%s",aid.length()/2,aid);
        sResp = apdu.sendApdu(sCmd);
        sb.append(apdu.message);
        if(!apdu.SW.equals("9000"))
            return sb.toString();

        sb.append("Найдено приложение: ").append(name).append('\n');
        if(sResp.length() < 10) {
            sb.append(" - не персонализировано").append('\n');
            return sb.toString();
        }
        sb.append('\n');

        TLVParser tlv = new TLVParser();
        byte[] bData = byt.toByteArray(sResp);
        RC = tlv.parse(bData,0,bData.length);
        if(RC < 0) {
            sb.append(tlv.Message).append('\n');
            return sb.toString();
        }

        for(int i = 0; i < tlv.TagList.size(); i++) {
            SingleTag sit = tlv.TagList.get(i);
            sb.append(byt.toHexString(sit.TagName))
                    .append(" ").append(byt.toHexString(sit.TagLength))
                    .append(" ").append(byt.toHexString(sit.TagValue))
                    .append('\n');
        }

        return sb.toString();
    }

    public String getPSList(IsoDep iso) {
        int RC;
        StringBuilder sb = new StringBuilder();
        apdu = new ApduMaster();

        RC = apdu.connect(iso);
        sb.append(apdu.message);
        if(RC < 0) {
            return sb.toString();
        }

        sb.append(getAppInfo("A0000000031010","Visa"));
        sb.append(getAppInfo("A0000000041010","MasterCard"));
        sb.append(getAppInfo("A0000006581010","МИР"));

        apdu.close();
        return sb.toString();
    }
}
