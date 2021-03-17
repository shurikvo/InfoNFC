package ru.shurikvo.infonfc;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;

import ru.shurikvo.apdu.ApduMaster;
import ru.shurikvo.utils.ByteMatter;

public class ScrollingActivity extends AppCompatActivity {

    private final static String TAG = "nfc_test";
    private static final String KEY_LOG = "LOG";

    private String messageInfo;
    private ByteMatter byt = new ByteMatter();

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private AlertDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String subject = "NFC Info "+android.text.format.DateFormat.format("yyMMddHHmmss", new java.util.Date()).toString();
                Intent email = new Intent(Intent.ACTION_SEND);
                email.putExtra(Intent.EXTRA_EMAIL, new String[]{ "shurikvo@gmail.com" });
                email.putExtra(Intent.EXTRA_SUBJECT, subject);
                email.putExtra(Intent.EXTRA_TEXT, messageInfo);
                email.setType("message/rfc822");
                startActivity(Intent.createChooser(email, "Choose an Email client :"));
            }
        });

        FloatingActionButton fin = (FloatingActionButton) findViewById(R.id.fin);
        fin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessage(R.string.about, R.string.about_text);
            }
        });

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            showMessage(R.string.error, R.string.no_nfc);
            finish();
            return;
        }
        pendingIntent = PendingIntent.getActivity(this,0,
                new Intent(this,this.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);

        if (savedInstanceState != null) {
            messageInfo = savedInstanceState.getString(KEY_LOG, "");
            showInfo();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_LOG, messageInfo);
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Onpause stop listening
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        messageInfo = "========== New Intent:\n";
        super.onNewIntent(intent);
        setIntent(intent);
        resolveIntent(intent);
    }

    private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.nfc_disabled);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
        return;
    }

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            assert tag != null;
            messageInfo += "---------- Tag:\n" + tag.toString();
            byte[] payload = detectTagData(tag).getBytes();
            showInfo();
        }
    }

    private String detectTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        messageInfo += "\n---------- Tag Data:";
        sb.append("UID (hex): ").append(toHex(id).toUpperCase().replace(" ","")).append('\n');
        sb.append("UID (reversed hex): ").append(toReversedHex(id).toUpperCase().replace(" ","")).append('\n');
        sb.append("UID (dec): ").append(toDec(id)).append('\n');
        sb.append("UID (reversed dec): ").append(toReversedDec(id)).append('\n');

        String prefix = "android.nfc.tech.";
        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.substring(prefix.length()));
            sb.append(", ");
        }

        sb.delete(sb.length() - 2, sb.length());

        for (String tech : tag.getTechList()) {
            sb.append("\n[" + tech + "]");
            if (tech.equals(MifareClassic.class.getName())) {
                sb.append('\n');
                String type = "Unknown";

                try {
                    MifareClassic mifareTag = MifareClassic.get(tag);

                    switch (mifareTag.getType()) {
                        case MifareClassic.TYPE_CLASSIC:
                            type = "Classic";
                            break;
                        case MifareClassic.TYPE_PLUS:
                            type = "Plus";
                            break;
                        case MifareClassic.TYPE_PRO:
                            type = "Pro";
                            break;
                        case MifareClassic.TYPE_UNKNOWN:
                            type = "Unknown";
                            break;
                    }
                    sb.append("Mifare Classic type: ");
                    sb.append(type);
                    sb.append('\n');

                    sb.append("Mifare size: ");
                    sb.append(mifareTag.getSize() + " bytes");
                    sb.append('\n');

                    sb.append("Mifare sectors: ");
                    sb.append(mifareTag.getSectorCount());
                    sb.append('\n');

                    sb.append("Mifare blocks: ");
                    sb.append(mifareTag.getBlockCount());

                    int nBlk = 0;
                    byte[] bKey = {(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF,(byte)0xFF};
                    mifareTag.connect();
                    for(int nSec = 0; nSec < 40; ++nSec) {
                        if (mifareTag.authenticateSectorWithKeyA(nSec,bKey)) {
                            sb.append("Sector ").append(nSec).append(": Auth OK").append('\n');
                            int nB = mifareTag.getBlockCountInSector(nSec);
                            for(int i = 0; i < nB; ++i) {
                                byte[] bData = mifareTag.readBlock(nBlk);
                                sb.append(byt.toHexString(bData)).append('\n');
                                nBlk++;
                            }
                        } else {
                            sb.append("Sector ").append(nSec).append(": Auth Error").append('\n');
                        }
                    }
                    mifareTag.close();


                } catch (Exception e) {
                    sb.append("Mifare classic error: " + e.getMessage());
                }
            }

            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                sb.append("Mifare Ultralight type: ");
                sb.append(type);
            }

            if (tech.equals(NfcA.class.getName())) {
                sb.append('\n');
                NfcA nfcATag = NfcA.get(tag);

                byte[] bAtqa = nfcATag.getAtqa();
                sb.append("ATQA: ").append(toHex(bAtqa).toUpperCase()).append('\n');

                short nSak = nfcATag.getSak();
                sb.append(String.format("SAK: %02X", nSak));
            }

            if (tech.equals(NdefFormatable.class.getName())) {
                sb.append('\n');
                NdefFormatable ndefFormatableTag = NdefFormatable.get(tag);

                sb.append("Connected: "+ndefFormatableTag.isConnected()).append('\n');
            }

            if (tech.equals(IsoDep.class.getName())) {
                int RC;
                sb.append('\n');

                PSLister pli = new PSLister();
                sb.append(pli.getCardInfo(IsoDep.get(tag)));
                sb.append(pli.getPSList(IsoDep.get(tag)));
            }
        }
        Log.v("test",sb.toString());
        messageInfo += "\n" + sb.toString();
        return sb.toString();
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    private long toDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    private long toReversedDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = bytes.length - 1; i >= 0; --i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }
    //----------------------------------------------------------------------------------------------
    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
    }

    public void showInfo() {
        TextView messageText = (TextView) findViewById(R.id.messageText);
        messageText.setText(messageInfo);
    }
    //----------------------------------------------------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            showMessage(R.string.app_name , R.string.action_settings);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}