package com.example.currentplacedetailsonmap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

public class Restarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Intent i = new Intent(context, GetDeviceLocationService.class);
        i.putExtra("IP", extras.getString("IP"));
        i.putExtra("PORT", extras.getInt("PORT"));
        i.putExtra("myId", extras.getInt("myId"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }
}
