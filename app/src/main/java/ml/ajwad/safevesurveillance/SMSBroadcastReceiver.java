package ml.ajwad.safevesurveillance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.SmsMessage;


public class SMSBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
    {
        // Get Bundle object contained in the SMS intent passed in
        Bundle bundle = intent.getExtras();
        SmsMessage[] shortMessage;
        String sms_str ="";

        if (bundle != null)
        {
            // Get the SMS message
            Object[] pdus = (Object[]) bundle.get("pdus");
            assert pdus != null;
            shortMessage = new SmsMessage[pdus.length];
            for (int i=0; i<shortMessage.length; i++){
                shortMessage[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                String messageBody = shortMessage[i].getMessageBody();
                sms_str += messageBody;
                if(sms_str.startsWith("<SafEveAlert>")) {
                    sms_str += ("\n"+ shortMessage[i].getMessageBody());
                    Intent smsIntent = new Intent("Inbox");
                    smsIntent.putExtra("message", sms_str);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(smsIntent);
                }
            }
        }
    }
}

