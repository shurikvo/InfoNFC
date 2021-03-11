package ru.shurikvo.infonfc;

import android.nfc.tech.IsoDep;

import java.nio.charset.StandardCharsets;

import ru.shurikvo.apdu.ApduMaster;
import ru.shurikvo.utils.ByteMatter;
import ru.shurikvo.utils.SingleTag;
import ru.shurikvo.utils.TLVParser;

public class PSLister {
    public boolean isError = false;

    private ApduMaster apdu = null;
    private final ByteMatter byt = new ByteMatter();
    private String sPAN = "", sHolderName = "", sExDate = "";

    public String getCardInfo(IsoDep iso) {
        int RC;
        String sCmd, sResp, sTitle = "";
        isError = false;
        StringBuilder sb = new StringBuilder();
        apdu = new ApduMaster();

        RC = apdu.connect(iso);
        sb.append(apdu.message);
        if(RC < 0) {
            isError = true;
            return sb.toString();
        }

        sCmd = "80CA9F7F00";
        sResp = apdu.sendApdu(sCmd);
        sb.append(apdu.message);
        if(!apdu.SW.equals("9000"))
            return sb.toString();

        CardTypeParser ctp = new CardTypeParser();
        sb.append("Тип чипа: ").append(ctp.parseCPLC(sResp)).append('\n');

        apdu.close();
        return sb.toString();
    }

    public String getPSList(IsoDep iso) {
        int RC;
        isError = false;
        StringBuilder sb = new StringBuilder();
        apdu = new ApduMaster();

        RC = apdu.connect(iso);
        sb.append(apdu.message);
        if(RC < 0) {
            isError = true;
            return sb.toString();
        }

        sb.append(getAppInfo("A0000000031010","Visa"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A0000000041010","MasterCard"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A0000006581010", "МИР 1010"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A0000006582010", "МИР 2010"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A0000000041090", "MChip Transport"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A0000000032010", "VISA Electron"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A000000003101001", "VISA Credit"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A000000003101002", "VISA Debit"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A0000000043060", "Maestro"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A0000000651010", "JCB"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }
        sb.append(getAppInfo("A000000333010101", "China Pay"));
        if(isError) {
            apdu.close();
            return sb.toString();
        }

        apdu.close();
        return sb.toString();
    }

    private String getAppInfo(String aid, String name) {
        int RC;
        String sCmd, sResp, sB = "";
        StringBuilder sb = new StringBuilder();

        sCmd = String.format("00A40400%02X%s",aid.length()/2,aid);
        sResp = apdu.sendApdu(sCmd);
        sb.append(apdu.message);
        if(apdu.isError) {
            this.isError = apdu.isError;
            return sb.toString();
        }
        if(!apdu.SW.equals("9000"))
            return sb.toString();

        sb.append("Найдено приложение: ").append(name);
        if(sResp.length() < 10) {
            sb.append(" - не персонализировано").append('\n');
            return sb.toString();
        } else {
            sb.append(" - персонализировано").append('\n');
        }
        sb.append('\n');

        TLVParser tlv = new TLVParser();
        byte[] bData = byt.toByteArray(sResp);
        RC = tlv.parse(bData,0,bData.length);
        if(RC < 0) {
            sb.append(tlv.Message).append('\n');
            isError = true;
            return sb.toString();
        }

        for (int i = 0; i < tlv.TagList.size(); i++) {
            SingleTag sit = tlv.TagList.get(i);
            if(sit.TagName[0] == 0x50 && sit.TagSize > 0) {
                sB = new String(sit.TagValue, StandardCharsets.US_ASCII);
                sb.append("Заголовок: ").append(sB).append('\n');
            }
            if(sit.TagName[0] == 0x84 && sit.TagSize > 0) {
                sB = byt.toHexString(sit.TagValue);
                sb.append("AID: ").append(sB).append('\n');
            }
            sb.append(byt.toHexString(sit.TagName)).append(" ").append(byt.toHexString(sit.TagLength));
            if(sit.TagSize > 0)
                sb.append(" ").append(byt.toHexString(sit.TagValue));
            sb.append('\n');
        }

        sPAN = "";
        sHolderName = "";
        sExDate = "";

        sb.append(checkRecord(3, 1));
        if(isError)
            return sb.toString();
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(1, 2));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(2, 2));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(2, 3));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(1, 1));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(1, 3));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(2, 1));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(3, 2));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(3, 3));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(4, 1));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(4, 2));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(4, 3));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(5, 1));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(5, 2));
            if(isError)
                return sb.toString();
        }
        if(sPAN.length() == 0 || sHolderName.length() == 0 || sExDate.length() == 0) {
            sb.append(checkRecord(5, 3));
            if(isError)
                return sb.toString();
        }

        sb.append("PAN: ").append(sPAN).append('\n');
        sb.append("Срок действия: ").append(sExDate).append('\n');
        sb.append("Владедец: ").append(sHolderName).append('\n');
        return sb.toString();
    }

    private String checkRecord(int nFile, int nRecord) {
        int RC;
        String sCmd, sResp, sB = "";
        StringBuilder sb = new StringBuilder();

        sCmd = String.format("00B2%02X%02X00",nRecord,(nFile << 3) | 0x04);
        sResp = apdu.sendApdu(sCmd);
        sb.append(apdu.message);
        if(apdu.isError) {
            this.isError = apdu.isError;
            return sb.toString();
        }
        if(apdu.SW.equals("9000")) {
            TLVParser tlv = new TLVParser();
            byte[] bData = byt.toByteArray(sResp);
            RC = tlv.parse(bData,0,bData.length);
            if(RC < 0) {
                sb.append(tlv.Message).append('\n');
                isError = true;
                return sb.toString();
            }
            for (int i = 0; i < tlv.TagList.size(); i++) {
                SingleTag sit = tlv.TagList.get(i);
                if(sit.TagSize == 0)
                    continue;
                sB = byt.toHexString(sit.TagName);
                if(sB.equals("5A")) {
                    sPAN = byt.toHexString(sit.TagValue);
                }
                if(sB.equals("5F20")) {
                    sHolderName = byt.toHexString(sit.TagValue);
                    String[] sP = sHolderName.split("/");
                    sHolderName = "";
                    if(sP != null) {
                        if (sP.length > 1)
                            sHolderName = sP[1] + " ";
                        if (sP.length > 0)
                            sHolderName += sP[0];
                    }
                }
                if(sB.equals("5F24")) {
                    sB = byt.toHexString(sit.TagValue);
                    if(sB.length() >= 6)
                        sExDate = String.format("%s/%s/%s",sB.substring(4,6),sB.substring(2,4),sB.substring(0,2));
                    else
                        sExDate = sB;
                }
            }
        }
        return sb.toString();
    }
}
