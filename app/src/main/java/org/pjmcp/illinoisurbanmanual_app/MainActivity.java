package org.pjmcp.illinoisurbanmanual_app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public static final int CAMERA_PERM_CODE = 101;
    public static final int CAMERA_REQUEST_CODE = 102;
    public static final int GALLERY_REQUEST_CODE = 105;
    private static final int PERMISSIONS_FINE_LOCATION = 55;
    ImageView selectedImage;
    Button cameraBtn, galleryBtn, siltFenceBtn, responseButton;
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


    @Override
    //Inital call upon opening
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        //Assign listener to new button
        siltFenceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Start second activity
                Intent intent = new Intent(MainActivity.this, ChooseBMP.class);
                startActivity(intent);
            }
        });

        //confiure new method for callback. Whenever event interval is met
        locationCallBack = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //save the location
                updateUIValues(locationResult.getLastLocation());
            }
        };


        //initate location request
        //set time interval to every 30 seconds for quickness
        responseButton = findViewById(R.id.responseButton);


        responseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Response.class);
                startActivity(intent);
            }
        });

//        //Click listener for switch elements
//        sw_gps.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (sw_gps.isChecked()) {
//                    //use gps for most accurate
//                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//                } else {
//                    //use blanaced accuracy between accuracy and power (WIFI)
//                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
//                }
//            }
//        });
//        sw_locationsupdates.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (sw_locationsupdates.isChecked()) {
//                    //turn on location tracking
//                    startLocationUpdates();
//                } else {
//                    //turn off tracking
//                    stopLocationUpdates();
//                }
//            }
//        });
//        //call the upddate GPS after seetting everyhting
//        updateGPS();

        final TextView myClickableUrl = (TextView) findViewById(R.id.TextView1);
        myClickableUrl.setText("Click my web site: www.illinoisurbanmanual.org");
        Linkify.addLinks(myClickableUrl, Linkify.WEB_URLS);
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
        updateGPS();


    }


    private void updateGPS(){
        //update GPS to revise location after getting permission from the fused client
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        //permissions
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //user provided the permission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    //permission recieved. Put the values of location.
                    updateUIValues(location);

                }
            });
        }
        else {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //if correct version of operationg system
                requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }
    //retrieve location characteristics
    private void updateUIValues(Location location) {

        //update all the text view objects with new location UI values
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));


    }

    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA}, CAMERA_PERM_CODE);
        }else {
            dispatchTakePictureIntent();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateGPS();
                }
                else {
                    Toast.makeText(this, "This app reuqires permissions", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
        if (requestCode == CAMERA_PERM_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
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
                uploadImageToFirebase(f.getName(),contentUri);
            }

        }

        if (requestCode == GALLERY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp + "." + getFileExt(contentUri);
                Log.d("tag", "onActivityResult: Gallery Image Uri:  " + imageFileName);
                selectedImage.setImageURI(contentUri);

                uploadImageToFirebase(imageFileName,contentUri);
            }

        }

    }

    private void uploadImageToFirebase(String name, Uri contentUri) {
        //connect app to firebase storage
        //Speicify path where to save image to upload images selected by user
        StorageReference image = storageReference.child("images/" + name);
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
                Toast.makeText(MainActivity.this, "Image is uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "failed to upload", Toast.LENGTH_SHORT).show();
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


//        Double lat = tv_lat.setText(String.valueOf(LocationRequest.getLatitude()));
//        tv_lon.setText(String.valueOf(location.getLongitude()));
//        //Add lat long category to photo
//        String lat = "Latitude";
//        String lon = "Longitude";
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


}

///Credit attributed to SmallAcademy for photo/camera --> firebase methodology