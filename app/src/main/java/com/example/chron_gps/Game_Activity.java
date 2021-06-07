package com.example.chron_gps;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Game_Activity extends AppCompatActivity implements SensorEventListener {

    Button btn_ready;
    private ImageView imagePlayPause;
    private TextView textCurrentTime, textTotalDuration;
    private SeekBar playerSeekBar;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();


    //Gia to StepCounter
    private TextView textViewStepDetector;
    private SensorManager sensorManager;
    private Sensor  mStepDetector;
    int stepDetect = 0;
    // Telos StepCounter



    //Gia location
    public static final int DEFAULT_UPDATE_INTERVAL = 5;
    public static final int FAST_UPDATE_INTERVAL = 3;
    private static final int PERMISSIONS_FINE_LOCATION = 99;
    private static String FILE_GAME ;
    ArrayList<String> Table_Game = new ArrayList<String>();
    int game_count = 0;

    //Gia fakelo
    String currentTime;

    //



    //
// references to the UI elements

    TextView  tv_sensor, tv_updates;

    Switch sw_locationupdates_game, sw_gps_game;

    // variable to remember if we are tracking location or not.
    boolean updateOn = false;
    // current location
    Location currentLocation;

    //Location request is a config file for all setting related to FusedLocationClient.
    LocationRequest locationRequest;

    LocationCallback locationCallBack;

    // Google's API for location services. The majority of the app functions using this class.
    FusedLocationProviderClient fusedLocationProviderClient;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);


        //Gia to StepCounter
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        textViewStepDetector = findViewById(R.id.textViewStepDetector);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) !=null) {
            mStepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        }
    // Telos tou StepCounter



        //gps???
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        sw_gps_game = findViewById(R.id.sw_gps_game);
        sw_locationupdates_game = findViewById(R.id.sw_locationsupdates_game);

        locationRequest = new LocationRequest();

        //how often does the default location check occur?
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);

        //how often does the location check occur when set to the most frequent update?
        locationRequest.setFastestInterval(1000 * FAST_UPDATE_INTERVAL);

        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //event that is triggered whenever the update interval is met.
        locationCallBack = new LocationCallback() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                // save the location
                updateUIValues(locationResult.getLastLocation());
            }
        };


        sw_gps_game.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps_game.isChecked()) {
                    //most accurate - use GPS
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensors");
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WIFI");
                }

            }
        });

        sw_locationupdates_game.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationupdates_game.isChecked()) {
                    // turn on location tracking
                    startLocationUpdates();
                } else {
                    //turn of location tracking
                    stopLocationUpdates();
                }
            }
        });

        updateGPS();

        //

        imagePlayPause = findViewById(R.id.imagePlayPause);
        textCurrentTime =  findViewById(R.id.textCurrentTime);
        textTotalDuration = findViewById(R.id.textTotalDuration);
        playerSeekBar = findViewById(R.id.playerSeekBar);
        mediaPlayer = new MediaPlayer();
        btn_ready = findViewById(R.id.btn_ready);

        playerSeekBar.setMax(100);

        btn_ready.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Game_Activity.this, MainActivity.class );
                mediaPlayer.stop();
                stopLocationUpdates();
                startActivity(intent);
            }
        });

        imagePlayPause.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    handler.removeCallbacks(updater);
                    mediaPlayer.pause();
                    imagePlayPause.setImageResource(R.drawable.ic_play_game);
                } else {
                    mediaPlayer.start();
                    imagePlayPause.setImageResource(R.drawable.ic_pause_game);
                    updateSeekBar();
                }
                save(v);
            }


        });

        prepareMediaPlayer();

        // An to vgalw ayto tote o allos den mporei na proxwraei to kommati klp sto seekBar kai elitoyrgei kanonika o ypoloipo
        playerSeekBar.setOnTouchListener(new View.OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                SeekBar seekBar = (SeekBar) view;
                int playsPosition =  (mediaPlayer.getDuration() / 100) * seekBar.getProgress();
                mediaPlayer.seekTo(playsPosition);
                textCurrentTime.setText(milliSecondToTimer(mediaPlayer.getCurrentPosition()));
                return false;
            }
        });

    }

    private void  prepareMediaPlayer(){
        try {
            mediaPlayer.setDataSource("https://firebasestorage.googleapis.com/v0/b/music-97497.appspot.com/o/Tash%20Sultana%20-%20Gemini%20(Official%20Audio).mp3?alt=media&token=477d6f43-817b-4e89-bfb2-93eae32b9d2a");
            mediaPlayer.prepare();
            textTotalDuration.setText(milliSecondToTimer(mediaPlayer.getDuration()));

        } catch (Exception exception){
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Runnable updater = new Runnable() {
        @Override
        public void run() {
            updateSeekBar();
            long currenDuration = mediaPlayer.getDuration();
            textCurrentTime.setText(milliSecondToTimer(currenDuration));
        }
    };

    private void  updateSeekBar(){
        if (mediaPlayer.isPlaying()) {
            playerSeekBar.setProgress((int) (((float) mediaPlayer.getCurrentPosition() / mediaPlayer.getDuration()) * 100));
            handler.postDelayed(updater, 1000);
        }
    }

    private String milliSecondToTimer(long milliSeconds){
        String timerString = "";
        String secondString;

        int hours = (int) (milliSeconds / (1000 *60 * 60));
        int minutes = (int) (milliSeconds % (1000 * 60 * 60)) / (1000 * 60);
        int seconds = (int) ((milliSeconds % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        if (hours > 0) {
            timerString = hours + ":";
        }
        if (seconds < 10){
            secondString = "0" + seconds;
        } else {
            secondString = "" + seconds;
        }

        timerString = timerString + minutes + ":" + secondString;
        return timerString;

    }

    //gia gps
    private void stopLocationUpdates() {
        tv_updates.setText("Location is NOT being tracked");

        tv_sensor.setText("Not tracking location");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

    private void startLocationUpdates() {

        tv_updates.setText("Location is being tracked");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions

            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                }
                else {
                    Toast.makeText(this, "This app requires permission to be granted in order to work properly", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void updateGPS() {
        //get permissions from the user to track GPS
        //get the current location from the fused client
        //update the UI - i.e. set all properties in their associated text view items.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(Game_Activity.this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            //user provided the permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onSuccess(Location location) {
                    //we got permissions.Put the values of location. XXX into the UI components.
                    if (location != null)
                    {
                        updateUIValues(location);
                    }
                    currentLocation = location;



                    //updateUIValues(location);
                }
            });

        }
        else {
            // permissions not granted yet.

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }


    }




    @RequiresApi(api = Build.VERSION_CODES.O)
    private void updateUIValues(Location location) {
        // update all of the text view objects with a ne Location.


        String Latitude = String.valueOf(location.getLatitude());
        String Longtitude = String.valueOf(location.getLongitude());
        String Altitude = String.valueOf(location.getAltitude());
        String Accuracy = String.valueOf(location.getAccuracy());
        String Speed = String.valueOf(location.getSpeed());
        FileOutputStream foss = null;
        currentTime= new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        game_count += 1;
        Table_Game.clear();
        Table_Game.add("Enhmerwsh no:"+ game_count+ " "+"Number Of steps: "+stepDetect+" " + "Lat:" + Latitude + " " + "Long:" + Longtitude + " " + "Alt:" + Altitude + " " + "Acc:" + Accuracy + " " + "Speed:" + Speed+ " Time: "+ currentTime);
        //Table_Game.add("Enhmerwsh no:");





        try {

            FILE_GAME = LocalDate.now().toString()+"Game";
            //FILE_GAME = "Game.txt";
            foss = openFileOutput(FILE_GAME, MODE_APPEND);

            for (int i=0; i< Table_Game.size(); i++)
                foss.write(((Table_Game.get(i)+"\n").getBytes()));
            //foss.write((Table.get(Update_pointer).getBytes()));
            //}
            //else {
            //foss.write((Table.get(0).getBytes()));


            //}


            Toast.makeText(this, "Saved to" + getFilesDir() + "/" + FILE_GAME, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (foss != null){
                try {
                    foss.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void save(View v) {

        FileOutputStream fos = null;
        game_count += 1;
        currentTime= new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        Table_Game.add("Song no:"+game_count+" -------------------------------------------------------" + " Time: "+ currentTime);




        // plhrofories hmera
        try {
            FILE_GAME = LocalDate.now().toString()+"Game";

            //FILE_NAME2 = LocalDate.now().toString()+"Errors.txt";
            fos = openFileOutput(FILE_GAME, MODE_APPEND);
            for (int i=0; i< Table_Game.size(); i++)
                fos.write(((Table_Game.get(i)+"\n").getBytes()));


            //fos.write(text.getBytes());





            Toast.makeText(this, "Saved to" + getFilesDir() + "/" + FILE_GAME, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    // Gia to StepCounter Sinarthseis---------------------------------------------------------------------------------

    @Override
    public void onSensorChanged(SensorEvent event) {
        //if (event.sensor == mStepCounter){
        //stepCount = (int) event.values[0];
        //textViewStepCounter.setText(String.valueOf(stepCount));
        //}
        //else
        if (event.sensor == mStepDetector){
            stepDetect = (int) (stepDetect + event.values[0]);
            textViewStepDetector.setText(String.valueOf(stepDetect));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        //if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null)
        //sensorManager.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) !=null)
            sensorManager.registerListener(this, mStepDetector, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null)
            sensorManager.unregisterListener(this, mStepDetector);
    }

    // Telos Sinartisewn tou StepCounter -------------------------------------------------------------


}