package com.example.employee;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignInActivity extends AppCompatActivity {
    TextInputEditText name, regno, phnno;
    TextInputLayout tilphno;
    Button Login;
    LocationRequest locationRequest;
    public static final int REQUEST_CHECK_SETTING = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);
        name = findViewById(R.id.name);
        regno = findViewById(R.id.regno);
        phnno = findViewById(R.id.phone);
        tilphno = findViewById(R.id.TILphone);


        AlertDialog alertDialog=new AlertDialog.Builder(SignInActivity.this)
                .setTitle("Important")
                .setMessage("Make sure the location is on!")
                .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .create();

        alertDialog.show();

        Login = findViewById(R.id.logIn);
        Login.setEnabled(false);

        Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (phnno.getText().toString().length() < 10) {
                    tilphno.setError(" ");
                    tilphno.setBoxStrokeErrorColor(ColorStateList.valueOf(Color.parseColor("#FF0000")));
                    phnno.requestFocus();
                    return;
                }
                SharedPreferences sharedPreferences=getSharedPreferences("GPSTracker",MODE_PRIVATE);
                SharedPreferences.Editor editor=sharedPreferences.edit();

                editor.putString("Phone No.",phnno.getText().toString());
                editor.commit();

                Intent intent = new Intent(SignInActivity.this, OTPActivity.class);
                intent.putExtra("phone", phnno.getText().toString());
                startActivity(intent);
                finish();
            }
        });

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(getApplicationContext())
                .checkLocationSettings(builder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response = task.getResult(ApiException.class);
                    Toast.makeText(SignInActivity.this, "GPS is On", Toast.LENGTH_SHORT).show();
                    Login.setEnabled(true);
                } catch (ApiException e) {
                    switch ((e.getStatusCode())) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            try {
                                resolvableApiException.startResolutionForResult(SignInActivity.this, REQUEST_CHECK_SETTING);
                            } catch (IntentSender.SendIntentException sendIntentException) {
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            break;
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_CHECK_SETTING)
        {
            switch (resultCode)
            {
                case Activity.RESULT_OK:
                    Toast.makeText(this,"GPS turned on",Toast.LENGTH_SHORT).show();
                    Login.setEnabled(true);
                    break;

                case Activity.RESULT_CANCELED:
                    Login.setEnabled(false);
                    Toast.makeText(this,"GPS is required to be turned on",Toast.LENGTH_SHORT).show();
            }
        }
    }
}