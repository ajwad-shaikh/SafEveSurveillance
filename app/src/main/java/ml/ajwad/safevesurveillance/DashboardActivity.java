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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).
                registerReceiver(receiver, new IntentFilter("Inbox"));
        super.onResume();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.requireNonNull(intent.getAction()).equalsIgnoreCase("Inbox")) {
                final String message = intent.getStringExtra("message");
                StringBuilder smsBuilder = new StringBuilder();
                String strbody = message;
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                smsBuilder.append(". \t ");
                smsBuilder.append("\n  \t\t   Timestamp: "+sdf.format(Calendar.getInstance(Locale.getDefault())) + " \t ");
                smsBuilder.append("\n"+strbody);
                String final1 = smsBuilder.toString();
                Integer hashFinal = strbody.hashCode();
                listItems.add(final1);
                adapter.notifyDataSetChanged();
                String contentText = "";
                String devPlace = "Device ID : ";
                String latPlace = "Latitude : ";
                String longPlace = "Longitude : ";
                String endPlace = "</SafEveAlert>";
                try {
                    Integer deviceID = Integer.parseInt(message.substring
                            (message.indexOf(devPlace) + devPlace.length(),
                                    message.indexOf(latPlace)).replaceAll("[^\\d.]", ""));
                    contentText += "SafEve Device Number " + deviceID + " around you reported distress. Report immediately.";
                    Float dLat = Float.valueOf(message.substring
                            (message.indexOf(latPlace) + latPlace.length(),
                                    message.indexOf(longPlace)).replaceAll("[^\\d.]", ""));
                    Float dLong = Float.valueOf(message.substring
                            (message.indexOf(longPlace) + longPlace.length(),
                                    message.indexOf(endPlace)).replaceAll("[^\\d.]", ""));
                    mDatabase.child("device").child(deviceID.toString()).child("d_lat").setValue(dLat);
                    mDatabase.child("device").child(deviceID.toString()).child("d_long").setValue(dLong);
                    mDatabase.child("device").child(deviceID.toString()).child("enforcer_assigned").setValue(1);
                }catch(Exception e) {
                    contentText += "A SafEve Device around you reported distress. Report immediately.";
                }

                mBuilder.setSmallIcon(R.drawable.logo);
                mBuilder.setContentTitle("New SafEve Distress Reported!");
                mBuilder.setContentText(contentText);
                mBuilder.setAutoCancel(true);
                Intent resultIntent = new Intent(DashboardActivity.this, DashboardActivity.class);
                stackBuilder.addParentStack(DashboardActivity.class);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                assert mNotificationManager != null;
                mNotificationManager.notify(0, mBuilder.build());
            }
        }
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
                    lookForSMS();
                } else {
                    Log.d(TAG, "firebase auth failed");
                }
            }
        });
    }

    private void lookForSMS(){
        final String SMS_URI_INBOX = "content://sms/inbox";
        StringBuilder smsBuilder = new StringBuilder();
        int msgCount = 0;
        try {
            Uri uri = Uri.parse(SMS_URI_INBOX);
            String[] projection = new String[]{"_id", "address", "person", "body", "date", "type"};
            //String tquery = "address='"+ tref + "'";
            Cursor cur = getContentResolver().query(uri, projection, null, null, "date desc");
            if (cur.moveToLast()) {
                int index_Body = cur.getColumnIndex("body");
                int index_Date = cur.getColumnIndex("date");
                do {
                    smsBuilder = new StringBuilder();
                    String strbody = cur.getString(index_Body);
                    long longDate = cur.getLong(index_Date);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                    Date resultDate = new Date(longDate);
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
                    }catch(Exception e) {
                        continue;
                    }
                    smsBuilder.append(msgCount++ + ". \t ");
                    smsBuilder.append("\n  \t\t   Timestamp: "+sdf.format(resultDate) + " \t ");
                    smsBuilder.append("\n"+strbody);
                    String final1 = smsBuilder.toString();
                    Integer hashFinal = strbody.hashCode();
                    listItems.add(final1);
                    adapter.notifyDataSetChanged();
                    //Log.d("Address", strAddress);
                } while (cur.moveToPrevious());
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
