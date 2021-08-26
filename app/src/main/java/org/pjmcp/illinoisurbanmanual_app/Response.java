package org.pjmcp.illinoisurbanmanual_app;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.*;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Response extends AppCompatActivity {
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;
    private static final int PERMISSIONS_FINE_LOCATION = 55;
    ImageView selectedImage;
    Button cameraBtn, galleryBtn, siltFenceBtn;
    String currentPhotoPath;

    //storage reference as variable
    StorageReference storageReference;

    //Text views temporarily have text views
    TextView tv_lat, tv_lon;
    Switch sw_locationsupdates, sw_gps;
    //variable to remmeber if tracking location is on
    boolean updateOn = false;
    //new class for location provider API for location services
    FusedLocationProviderClient fusedLocationProviderClient;
    //location request config file to influence fusedprovider
    LocationRequest locationRequest;
    //Constant for manipulating updating frequency
    public static final int DEFAULT_UPDATE_INTERVAL = 30;

    //location callback declaration
    LocationCallback locationCallBack;

    //ADD MAP//////////////
    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient client;
    ImageView imageView;
    private GoogleMap mMap;
///////////////////////////////////////


    @Override
    //Inital call upon opening
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.response);

        selectedImage = findViewById(R.id.displayImageView);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        //Initialize storage reference
        storageReference = FirebaseStorage.getInstance().getReference();
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationsupdates = findViewById(R.id.sw_locationsupdates);

        //set all properties of location request
        locationRequest = new LocationRequest();
        //slower frequency interval
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);
        //faster frequency interval
        locationRequest.setFastestInterval(100 * DEFAULT_UPDATE_INTERVAL);

        //specify the priority for power- accuracy balance
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        //Button to specify the upload
        siltFenceBtn = (Button) findViewById(R.id.contributeBtn);

        //assign variable
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        //Initialize fused location
        client = LocationServices.getFusedLocationProviderClient(this);

        //Assign listener to new button
        siltFenceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Start second activity
                Intent intent = new Intent(Response.this, ChooseBMP.class);
                startActivity(intent);
            }
        });

        //confiure new method for callback. Whenever event interval is met
        locationCallBack = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

            }
        };


        //initate location request
        //set time interval to every 30 seconds for quickness


        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askCameraPermissions();
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);
            }
        });

        //Click listener for switch elements
        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_gps.isChecked()) {
                    //use gps for most accurate
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                } else {
                    //use blanaced accuracy between accuracy and power (WIFI)
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                }
            }
        });
        sw_locationsupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_locationsupdates.isChecked()) {
                    //turn on location tracking
                    startLocationUpdates();
                } else {
                    //turn off tracking
                    stopLocationUpdates();
                }
            }
        });
        //call the upddate GPS after seetting everyhting

        //check permissions
        if (ActivityCompat.checkSelfPermission(Response.this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            //When permission granted
            //Call method
            getCurrentLocation();
        }else {
            //When permission denied
            //Request permissiono
            ActivityCompat.requestPermissions(Response.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 44);
        }
    } //end onCreate Method


    private void stopLocationUpdates() {
        Toast.makeText(this, "location is not being tracked", Toast.LENGTH_SHORT).show();
        tv_lon.setText("Location is not being tracked");
        tv_lat.setText("Location is not being tracked");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
    }

    private void startLocationUpdates() {
        Toast.makeText(this, "location is being tracked", Toast.LENGTH_SHORT).show();
        //method requiring 3 parameters asking for location request based on frequency and accuracy
        //permission check
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
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);



    }






    private void askCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        } else {
            dispatchTakePictureIntent();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 44) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //WhenPermission granted
                //Call method
                getCurrentLocation();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                File f = new File(currentPhotoPath);
                selectedImage.setImageURI(Uri.fromFile(f));
                Log.d("tag", "ABsolute Url of Image is " + Uri.fromFile(f));

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                this.sendBroadcast(mediaScanIntent);

                //upload data to firebase with two parameters (filename and file location)
                uploadImageToFirebase(f.getName(), contentUri);
            }

        }

        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "." + getFileExt(contentUri);
                Log.d("tag", "onActivityResult: Gallery Image Uri:  " + imageFileName);
                selectedImage.setImageURI(contentUri);

                uploadImageToFirebase(imageFileName, contentUri);
            }

        }

    }

    private void uploadImageToFirebase(String name, Uri contentUri) {
        //connect app to firebase storage
        //Speicify path where to save image to upload images selected by user
        StorageReference image = storageReference.child("images/response/" + name);
        image.putFile(contentUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //get url of image upon success
                image.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag", "onSucees: Uplaoaded image URI is " + uri.toString());
                    }

                });
                //when upload is successful display a success image
                Toast.makeText(Response.this, "Image is uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(Response.this, "failed to upload", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //

    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";



        String latitude = "Latitude";
        String longitude = "Longitude";


//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.IUM",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private void getCurrentLocation() {
        //Initalize task location
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
        Task<Location> task = client.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    //sysnc map
                    supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(@NonNull GoogleMap googleMap) {
                            //INitalize lat lng
                            LatLng latLng = new LatLng(location.getLatitude(),
                                    location.getLongitude());
                            //create marker options
                            MarkerOptions options = new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.fromResource(R.raw.iconlocation2))
                                    .title("This is your location");
                            //ZoomMap
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
                            //Add Marker on map
                            googleMap.addMarker(options);

                            mMap = googleMap;
                            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);


                            //////////////Add GEOJSON/////////////////////
                            try{
                                GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.photorequests, getApplicationContext());


                                GeoJsonPointStyle pointStyle = layer.getDefaultPointStyle();

                                GeoJsonLayer problemLocations = new GeoJsonLayer(mMap,R.raw.photopolygons, getApplicationContext());
                                GeoJsonPolygonStyle polyStyle = problemLocations.getDefaultPolygonStyle();
                                polyStyle.setStrokeColor(Color.RED);
                                polyStyle.setFillColor(Color.RED);

                                GeoJsonLayer projectBoundaries = new GeoJsonLayer(mMap, R.raw.projectboundary, getApplicationContext());
                                GeoJsonPolygonStyle polyStyle2 = projectBoundaries.getDefaultPolygonStyle();
                                polyStyle.setFillColor(Color.TRANSPARENT);
//                                pointStyle.setStrokeWidth(3);

//                                pointStyle.setPointRadius(6);
//                                pointStyle.setFillOpacity(0.6);
                                problemLocations.addLayerToMap();
                                projectBoundaries.addLayerToMap();
//                                layer.addLayerToMap();

                            } catch (IOException e) {

                            } catch (JSONException e) {

                            }
                        }
                    });
                }
            }
        });
    }




    }


