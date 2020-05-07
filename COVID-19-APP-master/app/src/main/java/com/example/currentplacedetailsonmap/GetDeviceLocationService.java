package com.example.currentplacedetailsonmap;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;

public class GetDeviceLocationService extends Service {


    private String IP;
    private int PORT;
    private int myId;

    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;

    private Bundle bundle;

    private Timer timer;
    private TimerTask timerTask;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O){
            startMyOwnForeground();
        } else {
            startForeground(1, new Notification());
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFCATION_CHANNEL_ID = "example.permanence";
        String channelName = "COVID-19 Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFCATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFCATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("COVID-19 Running in Background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Bundle extras = intent.getExtras();
        IP = extras.getString("IP");
        PORT = extras.getInt("PORT");
        myId = extras.getInt("myId");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        startTimer();

        return START_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopTimerTask();

        Intent broadcastIntent = new Intent();
        broadcastIntent.putExtra("myId", myId);
        broadcastIntent.putExtra("IP", IP);
        broadcastIntent.putExtra("PORT", PORT);
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }

    public void startTimer(){
        timer = new Timer();
        timerTask = new TimerTask(){
            public void run(){
                getDeviceLocation();
            }
        };
        timer.schedule(timerTask, 0, (long)(1000 * 0.25 * 60));
    }

    public void stopTimerTask(){
        if (timer != null){
            timer.cancel();
            timer = null;
        }
    }

    private void sendLocationToServer(Location loc){
        try {
            Socket clientSocket = new Socket(IP, PORT);
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

            JSONObject obj = new JSONObject();
            obj.put("type", "insert");
            obj.put("id", myId);
            obj.put("lat", loc.getLatitude());
            obj.put("lon", loc.getLongitude());
            String msg = obj.toString();
            out.writeUTF(msg);
            out.flush();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        mLastKnownLocation = task.getResult();
                        if (mLastKnownLocation != null) {
                            // Send location to server
                            sendLocationToServer(mLastKnownLocation);
                        }
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }
}
