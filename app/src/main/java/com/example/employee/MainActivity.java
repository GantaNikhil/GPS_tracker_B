package com.example.employee;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity  {
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    private Chronometer chronometer;
    private long pauseOffset;
    private boolean running;
    private Button start, pause, resume, reset;
    SwitchCompat sw_gps;
    TextView text_check,tv_lat,tv_lon,tv_speed;

    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;


    public static final int REQUEST_CODE_LOCATION_PERMISSION=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        start = findViewById(R.id.startTime);
        pause = findViewById(R.id.pauseTime);
        resume = findViewById(R.id.resumeTime);
        reset = findViewById(R.id.resetTime);
        sw_gps = findViewById(R.id.sw_gps);
        text_check = findViewById(R.id.text_check);

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_speed = findViewById(R.id.tv_speed);

        chronometer = findViewById(R.id.chronometer);
        chronometer.setBase(SystemClock.elapsedRealtime());

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StartTime(view);
                start.setVisibility(View.GONE);
                pause.setVisibility(View.VISIBLE);
                reset.setVisibility(View.VISIBLE);
            }
        });
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PauseTime(view);
                pause.setVisibility(View.GONE);
                resume.setVisibility(View.VISIBLE);
            }
        });
        resume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ResumeTime(view);
                resume.setVisibility(View.GONE);
                pause.setVisibility(View.VISIBLE);
            }
        });
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ResetTime(view);
                reset.setVisibility(View.GONE);
                start.setVisibility(View.VISIBLE);
                resume.setVisibility(View.GONE);
                pause.setVisibility(View.GONE);
            }
        });

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_gps.isChecked()) {
                    text_check.setText("GPS");
//                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                } else {
                    text_check.setText("WiFi/Towers");
//                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                }
            }
        });

//        UpdateGPS();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==REQUEST_CODE_LOCATION_PERMISSION && grantResults.length>0)
        {
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                startLocationService();
            }
            else
                Toast.makeText(MainActivity.this,"Permissions denied",Toast.LENGTH_SHORT).show();
        }
    }


    public void StartTime(View v) {
//        chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
//        if (!running) {
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            running = true;
//        }
        if(ContextCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION);

        }
        else
            startLocationService();
    }

    public void ResumeTime(View v) {
        if (!running) {
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            running = true;
        }
        startLocationService();
    }

    public void PauseTime(View v) {
        if (running) {
            chronometer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
            running = false;
        }
        stopLocationService();
    }

    public void ResetTime(View v) {
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
        stopLocationService();
    }

    private boolean isLocationServiceRunning()
        {
        ActivityManager activityManager =(ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if(activityManager!=null)
        {
            for(ActivityManager.RunningServiceInfo serviceInfo : activityManager.getRunningServices(Integer.MAX_VALUE))
            {
                if(LocationService.class.getName().equals(serviceInfo.service.getClassName()))
                {
                    if(serviceInfo.foreground)
                        return true;
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService(){
        if(!isLocationServiceRunning())
        {
            Intent intent=new Intent(getApplicationContext(),LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this,"Location started",Toast.LENGTH_SHORT).show();

            UpdateGPS();

        }
    }
    private void stopLocationService(){
        if(isLocationServiceRunning())
        {
            Intent intent=new Intent(getApplicationContext(),LocationService.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this,"Location stopped",Toast.LENGTH_SHORT).show();

        }
    } private void UpdateGPS() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    UpdateUIvalues(location);
                }
            });
        }
    }

    private void UpdateUIvalues(Location location) {
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        if(location.hasSpeed())
        {
            tv_speed.setText(String.valueOf(location.getSpeed()));
        }
        else
            tv_speed.setText("-");

    }

}