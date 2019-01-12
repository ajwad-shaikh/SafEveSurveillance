package ml.ajwad.safevesurveillance;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.ContentValues.TAG;

public class DashboardActivity extends Activity {

    ArrayList<String> listItems = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    int clickCounter = 1;
    private DatabaseReference mDatabase;
    private ListView mListView;
    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
    TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        adapter = new ArrayAdapter<String>(this,
                R.layout.list_row,
                listItems);
        if (checkAndRequestPermissions()) {
            mListView = (ListView) findViewById(R.id.listSMS);
            mListView.setAdapter(adapter);
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        LocalBroadcastManager.getInstance(this).
                registerReceiver(receiver, new IntentFilter("Inbox"));

        loginToFirebase();

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase("Inbox")) {
                final String message = intent.getStringExtra("message");
                listItems.add(0, clickCounter++ + " " + message);
                adapter.notifyDataSetChanged();
                mBuilder.setSmallIcon(R.drawable.logo);
                mBuilder.setContentTitle("New SafEve Distress Received!");
                mBuilder.setContentText(message);
                mBuilder.setAutoCancel(true);
                Intent resultIntent = new Intent(DashboardActivity.this, DashboardActivity.class);
                stackBuilder.addParentStack(DashboardActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(0, mBuilder.build());
            }
        }

        final String SMS_URI_INBOX = "content://sms/inbox";
        final String SMS_URI_ALL = "content://sms/";
        StringBuilder smsBuilder = new StringBuilder();
        int msgCount = 0;
    };

    private  boolean checkAndRequestPermissions() {
        int permissionSendMessage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS);

        int receiveSMS = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECEIVE_SMS);

        int readSMS = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_SMS);
        List<String> listPermissionsNeeded = new ArrayList<>();

        if (receiveSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECEIVE_MMS);
        }
        if (readSMS != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.READ_SMS);
        }
        if (permissionSendMessage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.SEND_SMS);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]),
                    REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void loginToFirebase() {
        // Authenticate with Firebase, and request location updates
        String email = getString(R.string.firebase_email);
        String password = getString(R.string.firebase_password);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>(){
            @Override
            public void onComplete(Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "firebase auth success");
                    lookforSMS();
                } else {
                    Log.d(TAG, "firebase auth failed");
                }
            }
        });
    }

    private void lookforSMS(){
        final String SMS_URI_INBOX = "content://sms/inbox";
        final String SMS_URI_ALL = "content://sms/";
        StringBuilder smsBuilder = new StringBuilder();
        int msgCount = 0;
        try {
            Uri uri = Uri.parse(SMS_URI_INBOX);
            String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};
            //String tquery = "address='"+ tref + "'";
            Cursor cur = getContentResolver().query(uri, projection, null, null, "date desc");
            if (cur.moveToFirst()) {
                int index_Address = cur.getColumnIndex("address");
                int index_Person = cur.getColumnIndex("person");
                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");
                int index_Type = cur.getColumnIndex("type");
                do {
                    smsBuilder = new StringBuilder();
                    String strAddress = cur.getString(index_Address);
                    int intPerson = cur.getInt(index_Person);
                    String strbody = cur.getString(index_Body);
                    long longDate = cur.getLong(index_Date);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                    Date resultDate = new Date(longDate);
                    int int_Type = cur.getInt(index_Type);
                    if(!strbody.startsWith("<SafEveAlert>"))
                        continue;
                    String devPlace = "Device ID : ";
                    String latPlace = "Latitude : ";
                    String longPlace = "Longitude : ";
                    String endPlace = "</SafEveAlert>";
                    try {
                        Integer deviceID = Integer.parseInt(strbody.substring
                                (strbody.indexOf(devPlace) + devPlace.length(),
                                        strbody.indexOf(latPlace)).replaceAll("[^\\d.]", ""));
                        Float dLat = Float.valueOf(strbody.substring
                                (strbody.indexOf(latPlace) + latPlace.length(),
                                        strbody.indexOf(longPlace)).replaceAll("[^\\d.]", ""));
                        Float dLong = Float.valueOf(strbody.substring
                                (strbody.indexOf(longPlace) + longPlace.length(),
                                        strbody.indexOf(endPlace)).replaceAll("[^\\d.]", ""));

                        mDatabase.child("device").child(deviceID.toString()).child("d_lat").setValue(dLat);
                        mDatabase.child("device").child(deviceID.toString()).child("d_long").setValue(dLong);
                        mDatabase.child("device").child(deviceID.toString()).child("enforcer_assigned").setValue(1);
                    }catch(Exception e){
                        continue;
                    }



                    smsBuilder.append(msgCount++ + ". \t ");
                    //smsBuilder.append("[ ");
                    smsBuilder.append("\n  \t\t   Timestamp: "+sdf.format(resultDate) + " \t ");
                    smsBuilder.append("\n"+strbody);
                    //smsBuilder.append(int_Type);
                    //smsBuilder.append(" ]\n\n");
                    String final1 = smsBuilder.toString();
                    Integer hashFinal = strbody.hashCode();
                    listItems.add(final1);
                    adapter.notifyDataSetChanged();
                    //Log.d("Address", strAddress);
                } while (cur.moveToNext());
                //clickCounter = msgCount;
                if (!cur.isClosed()) {
                    cur.close();
                    cur = null;
                }
            } else {
                smsBuilder.append("no result!");
            }
        } catch (SQLiteException ex) {
            Log.d("SQLiteException", ex.getMessage());
        }
    }

}
