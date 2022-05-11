package com.example.employee;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationService extends Service {
//    FirebaseFirestore fstore = FirebaseFirestore.getInstance();
    FirebaseDatabase rootnode;
    DatabaseReference reference;
    List<Double> latloc=new ArrayList<>();
    List<Double> lonloc=new ArrayList<>();
    List<Double> speedloc =new ArrayList<>();
    TextView tv_lat,tv_lon;
    Integer cnt=0;

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null && locationResult.getLastLocation() != null) {
                double latitude = locationResult.getLastLocation().getLatitude();
                double longitude = locationResult.getLastLocation().getLongitude();
                double speed=locationResult.getLastLocation().getSpeed();


                latloc.add(latitude);
                lonloc.add(longitude);
                speedloc.add(speed);

                Log.d("Location Updates ", latitude + " " + longitude);

                SharedPreferences sharedPreferences=getSharedPreferences("GPSTracker",MODE_PRIVATE);


                String phnno=sharedPreferences.getString("Phone No.","");

                rootnode=FirebaseDatabase.getInstance();
                reference=rootnode.getReference(phnno);
                reference.child("Latitude").setValue(latloc);
                reference.child("Longitude").setValue(lonloc);
                reference.child("Speed").setValue(speedloc);
                cnt++;
                DocumentReference documentReference= FirebaseFirestore.getInstance().collection("Locations").document(phnno);
                Map<String,Object> locations=new HashMap<>();
                locations.put("latitude",latloc);
                locations.put("longitude",lonloc);
                locations.put("speed",speedloc);
                locations.put("count",cnt);


                documentReference.set(locations).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //do nothing
                    }
                });



            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startLocationService() {
        String channelId = "location_notification_channel";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(),
                channelId
        );
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Location Service");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setContentText("Running");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelId,
                        "Location Service",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationChannel.setDescription("This channel is used by location service");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(30000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        startForeground(Constants.LOCATION_SERVICE_ID,builder.build());

    }
    private void stopLocationService(){
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent!=null)
        {
            String action=intent.getAction();
            if(action!=null)
            {
                if(action.equals(Constants.ACTION_START_LOCATION_SERVICE))
                {
                    startLocationService();
                }
                else if(action.equals(Constants.ACTION_STOP_LOCATION_SERVICE))
                {
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

}
