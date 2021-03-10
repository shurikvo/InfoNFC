package ru.shurikvo.utils;

import java.io.*;
import java.util.*;
import java.text.*;

public class TLVParser {
	private static final String ClassName = "TLVParser";
	private int LenLen = 0;
	private ByteMatter byt = new ByteMatter();

	public int TagLengthMax = 2;
	public byte[] NativeContainers = new byte[0];
	public String Message = "";
	public ArrayList<SingleTag> TagList = new ArrayList<SingleTag>();

	public int parse(byte[] bRaw, int nOff, int nRootLength) {
		int i, RC, nTagLength, nTagValueLength, nGotLength, nRestLength;
		boolean isContainer;
		byte[] bRootTag;
		SingleTag tag = null;
		
		{ // Check and Prepare
			Message = "";

			if (nOff > (bRaw.length - 2)) {
				Message = "Offset is wrong";
				return -1;
			}
			if (nRootLength < nOff + 2) {
				Message = "Declared length is wrong";
				return -1;
			}
			bRootTag = Arrays.copyOfRange(bRaw,nOff,nOff+nRootLength);
		}
//System.out.println("> "+byt.toHexString(bRootTag));
		nOff = 0;
		while ((nRootLength - nOff) >= 2) {
			{ // Tag
				tag = new SingleTag();
				
//byte[] bbb = Arrays.copyOfRange(bRootTag,nOff,bRootTag.length);System.out.println("# "+byt.toHexString(bbb));
				isContainer = IsContainer(bRootTag, nOff);
				nTagLength = TagLength(bRootTag, nOff);
//System.out.println("Off: "+nOff+" TagLength: "+nTagLength+" RootLength: "+nRootLength);
				if (nTagLength <= 0)
					return nTagLength;
				if ((nOff + nTagLength) > (nRootLength - 1)) {
					Message = "Wrong tag length";
					return -1;
				}
//System.out.println("isContainer: "+isContainer+" TagLength: "+nTagLength);
				tag.TagName = Arrays.copyOfRange(bRootTag,nOff,nOff+nTagLength);
				nOff += nTagLength;
//System.out.println(">-T "+byt.toHexString(tag.TagName));
				
				nTagValueLength = DataLength(bRootTag, nOff);
				if (nTagValueLength < 0)
					return nTagValueLength;
				if ((nOff + LenLen + nTagValueLength) > nRootLength) {
					Message = "Wrong tag value length";
					return -1;
				}
//System.out.println("TagValueLength: "+nTagValueLength+" LenLen: "+LenLen);
				tag.TagSize = nTagValueLength;
				tag.TagLength = Arrays.copyOfRange(bRootTag,nOff,nOff+LenLen);
				nOff += LenLen;
//System.out.println(">-L "+byt.toHexString(tag.TagLength));
				if (nTagValueLength > 0)
					tag.TagValue = Arrays.copyOfRange(bRootTag,nOff,nOff+nTagValueLength);
				else
					tag.TagValue = new byte[0];
//System.out.println(">-V "+byt.toHexString(tag.TagValue));
				TagList.add(tag);
			}
			if (isContainer) {
				RC = parse(tag.TagValue, 0, tag.TagValue.length);
				if (RC < 0)
					return RC;
			}
			nOff += nTagValueLength;
		}
		return TagList.size();
	}

	public void clearParser() {
		TagList = new ArrayList<SingleTag>();
	}

	private int DataLength(byte[] bRootTag, int nOff) {
		int i, Len = 0;

		LenLen = 0;
		if ((bRootTag[nOff] & 0x80) != 0 && (bRootTag[nOff] & 0x70) != 0) {
			Message = "Wrong length format";
			return -1;
		}
		if ((bRootTag[nOff] & 0x80) == 0) {
			LenLen = 1;
			return bRootTag[nOff];
		} else
			LenLen = (bRootTag[nOff] & 0x0F) + 1;
		if(LenLen > 3) {
			LenLen = 0;
			Message = "Length more than 3 bytes";
			return -1;
		}
		for (i = 1; i < LenLen; ++i){
			Len = (Len << 8) + (bRootTag[nOff + i] & 0xFF);
		}
		return Len;
	}

	private int TagLength(byte[] bRootTag, int nOff) {
		int i, L = 1;

		if ((bRootTag[nOff] & 0x1F) != 0x1F)
			return L;
		L++;
		for (i = 1; i < 5; ++i) {
			if ((bRootTag[nOff + i] & 0x80) != 0)
				L++;
			else
				break;
		}
		if (L <= TagLengthMax)
			return L;
		else {
			Message = "Too long tag";
			return -1;
		}
	}

	private boolean IsContainer(byte[] bRootTag, int nOff) {
		int i;
		
		if ((bRootTag[nOff] & 0x20) != 0)
			return true;
		else {
			for(i = 0; i < NativeContainers.length; ++i)
				if (bRootTag[nOff] == NativeContainers[i])
					return true;
			return false;
		}
	}
}