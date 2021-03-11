package ru.shurikvo.infonfc;

import java.util.ArrayList;

public class CardTypeParser {
    private final ArrayList<CardTypeItem> typeList = new ArrayList<CardTypeItem>();

    public String parseCPLC(String sData) {
        String result = "Не распознан";

        if(sData.length() < 50)
            return result;

        for(int i = 0; i < typeList.size(); ++i) {
            CardTypeItem cit = typeList.get(i);
            String crit = sData.substring(cit.start,cit.start+cit.value.length());
            if(!crit.equals(cit.value))
                continue;
            result = cit.text;
        }
        return result;
    }

    public CardTypeParser() {
        typeList.add(new CardTypeItem(10,"5021","KONA 10"));
        typeList.add(new CardTypeItem(10,"5015","KONA 12"));
        typeList.add(new CardTypeItem(10,"5055","KONA 132"));
        typeList.add(new CardTypeItem(10,"5049","KONA 151s"));
        typeList.add(new CardTypeItem(10,"258C","KONA 23s"));
        typeList.add(new CardTypeItem(10,"2599","KONA 20"));
        typeList.add(new CardTypeItem(10,"010A","KONA 25"));
        typeList.add(new CardTypeItem(10,"020A","KONA 28"));
        typeList.add(new CardTypeItem(10,"5037","KONA 121"));
        typeList.add(new CardTypeItem(10,"5066","KONA 122SPS"));
        typeList.add(new CardTypeItem(10,"5040","KONA 101"));
        typeList.add(new CardTypeItem(10,"0208","KONA 231S"));
        typeList.add(new CardTypeItem(10,"5052","KONA 150"));
        typeList.add(new CardTypeItem(10,"5022","KONA 111"));
        typeList.add(new CardTypeItem(10,"010C","KONA 26"));
        typeList.add(new CardTypeItem(10,"5219","KONA 14S"));
        typeList.add(new CardTypeItem(10,"190A","KONA 251"));
        typeList.add(new CardTypeItem(10,"190C","KONA 261"));
        typeList.add(new CardTypeItem(10,"6C13","KONA2 D1040"));
        typeList.add(new CardTypeItem(10,"6C14","KONA2 D1080"));
        typeList.add(new CardTypeItem(10,"6B64","KONA2 D1081"));
        typeList.add(new CardTypeItem(10,"D32182","KONA2 D1321"));
        typeList.add(new CardTypeItem(10,"D32171","NXP P71"));
        typeList.add(new CardTypeItem(10,"D35A","KONA2 D2350"));
        typeList.add(new CardTypeItem(10,"190F","KONA2 C2304"));
        typeList.add(new CardTypeItem(10,"1610","KONA2 C2320"));
        typeList.add(new CardTypeItem(10,"190E","KONA2 C2200s"));
        typeList.add(new CardTypeItem(10,"6B67","KONA2 D1025"));
        typeList.add(new CardTypeItem(10,"990281","Secora 16/80"));
    }
}
