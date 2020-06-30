package com.example.getlocation;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    public static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager mLocationManager;

    private LocationRequest mLocationRequest;
    private com.google.android.gms.location.LocationListener listener;
    private long UPDATE_INTERVAL = 2 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */
    private LocationManager locationManager;

    private double latitude = -1;
    private double longitude = -1;

    private ArrayList<Point> points = new ArrayList<>();
    private ListView listView;
    ArrayAdapter<String> adapter;
    String[] coordPoints;
    DatabaseHelper databaseHelper;
    Cursor userCursor;
    SQLiteDatabase db;
    EditText nameSet;
    Spinner sets;
    ArrayList<String> listSets;
    TextView length, nodes, time;
    int[] minDist;
    private boolean permissionGranted;
    private static final int REQUEST_PERMISSION_WRITE = 1001;
    private static String FILE_NAME = "content.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        checkLocation(); //check whether location service is enable or not in your  phone
        listView = findViewById(R.id.listview);
        nameSet = findViewById(R.id.name_set);
        sets = findViewById(R.id.sets);
        length = findViewById(R.id.length);
        nodes = findViewById(R.id.nodes);
        time = findViewById(R.id.time);

        databaseHelper = new DatabaseHelper(getApplicationContext());
        listSets = new ArrayList<>();
        listSets.add("choose sets");
        getDataForSpinner();
        loadDataToSpinner();
    }

    public void onClick(View v) {
        switch (v.getId()){
            case R.id.add_point:
                if(latitude != -1 && longitude != -1) {
                    points.add(new Point(latitude, longitude));
                    loadDataToListView();
                }
                break;
            case R.id.clear_list:
                points.clear();
                loadDataToListView();
                break;
            case R.id.save_set:
                Toast.makeText(this, "save_set", Toast.LENGTH_SHORT).show();
                Log.d("mytag", "save_set");
                for (int i = 0; i < points.size(); i++) {
                    db = databaseHelper.getReadableDatabase();
                    databaseHelper.addNode(db, nameSet.getText().toString(), String.valueOf(points.get(i).getX()), String.valueOf(points.get(i).getY()));
                }
                listSets.add(nameSet.getText().toString());
                loadDataToSpinner();
                nameSet.setText("");
                break;
            case R.id.select:
                if(!sets.getSelectedItem().toString().equals("choose sets")) {
                    db = databaseHelper.getReadableDatabase();
                    String item = sets.getSelectedItem().toString();
                    userCursor = db.rawQuery("select _id, name, latitude, longitude from " + DatabaseHelper.TABLE + " WHERE name = '" + item + "';", null);
                    Log.d("mytag", userCursor.getCount() + "");
                    points.clear();
                    while(userCursor.moveToNext()){
                        String lat = userCursor.getString(userCursor.getColumnIndex(DatabaseHelper.COLUMN_LATITUDE));
                        String lon = userCursor.getString(userCursor.getColumnIndex(DatabaseHelper.COLUMN_LONGITUDE));
                        points.add(new Point(Double.parseDouble(lat), Double.parseDouble(lon)));
                        Log.d("mytag", new Point(Double.parseDouble(lat), Double.parseDouble(lon)).toString());
                    }
                    loadDataToListView();
                }
                break;
            case R.id.delete:
                db = databaseHelper.getReadableDatabase();
                String item = sets.getSelectedItem().toString();
                db.delete(DatabaseHelper.TABLE, "name = '" + item + "'", null);
                Toast.makeText(this, "Record '" + item + "' was deleted", Toast.LENGTH_SHORT).show();
                getDataForSpinner();
                loadDataToSpinner();
                break;
            case R.id.start:
                AntAlgorithm algorithm = new AntAlgorithm(points.size(), points);
                algorithm.start();
                length.setText(String.valueOf(AntAlgorithm.Lmin));
                nodes.setText(Arrays.toString(AntAlgorithm.Tmin));
                time.setText(AntAlgorithm.leadTime);
                minDist = AntAlgorithm.Tmin;
                break;
            case R.id.save_csv:
                    if(minDist != null && !permissionGranted) {
                    FileOutputStream fos = null;
                    try {
                        String line = Arrays.toString(minDist).replace("[", "").replace("]", "");
                        line = line.replace(" ", "");
                        FILE_NAME = sets.getSelectedItem().toString() + ".csv";
                        fos = new FileOutputStream(getExternalPath());
                        Toast.makeText(this, new File(String.valueOf(Environment.getExternalStorageDirectory()))+ "", Toast.LENGTH_SHORT).show();
                        fos.write(line.getBytes());
                    }
                    catch(IOException ex) {
                        Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    finally{
                        try{
                            if(fos!=null)
                                fos.close();
                        }
                        catch(IOException ex){

                            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }


                    Toast.makeText(this, "File '" + sets.getSelectedItem().toString() +"' saved successfully", Toast.LENGTH_SHORT).show();
                }

        }
    }

    private File getExternalPath() {
        return(new File(Environment.getExternalStorageDirectory(), FILE_NAME));
    }

    // проверяем, доступно ли внешнее хранилище для чтения и записи
    public boolean isExternalStorageWriteable(){
        String state = Environment.getExternalStorageState();
        return  Environment.MEDIA_MOUNTED.equals(state);
    }
    // проверяем, доступно ли внешнее хранилище хотя бы только для чтения
    public boolean isExternalStorageReadable(){
        String state = Environment.getExternalStorageState();
        return  (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }

    private boolean checkPermissions(){

        if(!isExternalStorageReadable() || !isExternalStorageWriteable()){
            Toast.makeText(this, "Внешнее хранилище не доступно", Toast.LENGTH_LONG).show();
            return false;
        }
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_WRITE);
            return false;
        }
        return true;
    }

    public void loadDataToListView(){
        coordPoints = new String[points.size()];
        String line;
        for (int i = 0; i < points.size(); i++) {
            line = (i + 1) + " точка " + points.get(i).getX() + ", " + points.get(i).getY();
            coordPoints[i] = line;
        }
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, coordPoints);
        listView.setAdapter(adapter);
    }

    public void loadDataToSpinner(){
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listSets);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sets.setAdapter(spinnerAdapter);
    }

    public void getDataForSpinner(){
        db = databaseHelper.getReadableDatabase();
        Cursor names =  db.rawQuery("select * from "+ DatabaseHelper.TABLE, null);
        listSets.clear();
        listSets.add("choose sets");
        while(names.moveToNext()){
            String name = names.getString(names.getColumnIndex(DatabaseHelper.COLUMN_NAME));
            if(!listSets.contains(name)){
                listSets.add(name);
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        startLocationUpdates();

        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if(mLocation == null){
            startLocationUpdates();
        }
        if (mLocation != null) {

            // mLatitudeTextView.setText(String.valueOf(mLocation.getLatitude()));
            //mLongitudeTextView.setText(String.valueOf(mLocation.getLongitude()));
        } else {
            Toast.makeText(this, "Location not Detected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this);
        Log.d("reque", "--->>>>");
    }

    @Override
    public void onLocationChanged(Location location) {

        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        // You can now create a LatLng Object for use with maps
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
    }

    private boolean checkLocation() {
        if(!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch (requestCode){
            case REQUEST_PERMISSION_WRITE:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    permissionGranted = true;
                    Toast.makeText(this, "Разрешения получены", Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(this, "Необходимо дать разрешения", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

}