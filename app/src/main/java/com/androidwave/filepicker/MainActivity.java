package com.androidwave.filepicker;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.androidwave.filepicker.utils.FileCompressor;
import com.androidwave.filepicker.utils.Global;
import com.androidwave.filepicker.utils.MySingleton;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "mainActivty";
    private static String uploadUrl = "http://194.186.110.74:8501/v1/models/default:predict";
    static final int REQUEST_TAKE_PHOTO = 1;
    static final int REQUEST_GALLERY_PHOTO = 2;
    private static String b64 = "b64";
    private static String key = "key";
    private static String image_bytes = "image_bytes";
    private static String instances = "instances";
    private static String encodedString = "";
    File mPhotoFile;
    FileCompressor mCompressor;
    Button btnClick, btnUpload;
    ImageView imvPic;
    Bitmap photo = null;
    JSONObject object = new JSONObject();
    List<String> coordinateList  = null;

    ViewGroup container;


    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCompressor = new FileCompressor(this);

        initUI();
        showSelectImage();
        showUploadData();
    }


    private void initUI(){
        btnUpload = (Button)findViewById(R.id.btnUpload);
        btnClick = (Button)findViewById(R.id.btnClick);
        imvPic = (ImageView)findViewById(R.id.imvPic);
        container = (ViewGroup) findViewById(R.id.container);
    }


    private void showSelectImage(){
        btnClick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
    }


    private void showUploadData(){
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected()){
                    Log.d(TAG, "internet is connected");
                    sendAndRequestResponse();
                }else {
                    Toast.makeText(MainActivity.this, "Internet is not connected", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }



    // upload base64 String and receive data using API
    public void sendAndRequestResponse(){

        Log.i(TAG, object.toString());
        if (object.toString().isEmpty() || object.toString().equals("")){
            Toast.makeText(MainActivity.this, "Please capture picture", Toast.LENGTH_SHORT).show();
            return;
        }
        ProgressDialog dialog = ProgressDialog.show(MainActivity.this, "",
                "Uploading. Please wait...", true);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                uploadUrl,
                object,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        dialog.hide();
                        List<String> responseList = new ArrayList<String>();
                        try {
                            JSONArray predictions = response.getJSONArray("predictions");
                            for (int i = 0; i < predictions.length(); i++){
                                JSONObject predictionObj = predictions.getJSONObject(i);
//                               Log.d(TAG, "sendAndRequestResponse : " + predictionObj.toString());
                                JSONArray detectionArray = predictionObj.getJSONArray("detection_boxes");
                                Log.d(TAG, "sendAndRequestResponse" + detectionArray.toString());
                                for (int j = 0; j < detectionArray.length(); j++){
                                    coordinateList = new ArrayList<String>();
                                    coordinateList.add( detectionArray.getString(i));
                                    Log.d(TAG, "sendAndRequestResponse " + coordinateList.toString());
                                    int x = coordinateList.indexOf(j);
//                                   Log.d(TAG, "sendAndRequestResponse : x :" + x);
//                                   displayImageView(0.2,0.5,0.7,0.8);
                                    Toast.makeText(MainActivity.this, coordinateList.toString(), Toast.LENGTH_SHORT).show();
                                    displayImageView(Math.random(),Math.random(), Math.random(), Math.random());
                                }
                            }
                        }catch (JSONException e){
                            e.printStackTrace();
                            dialog.hide();
                        }
                    }
                },
                new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        dialog.hide();
                        Toast.makeText(MainActivity.this, "Response Failed, Try again", Toast.LENGTH_LONG).show();
                        displayImageView(0.2,0.5,0.7,0.8);
                        if (error.getMessage() != null) {
                            Log.i(TAG, error.getMessage());
                        } else {
                            Log.i(TAG, "fail");
                        }

                    }
                }
        );

        // Add JsonObjectRequest to the RequestQueue
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }


    /**
     * Alert dialog for capture or select from galley
     */
    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setItems(items, (dialog, item) -> {
            if (items[item].equals("Take Photo")) {
                requestStoragePermission(true);
            } else if (items[item].equals("Choose from Library")) {
                requestStoragePermission(false);
            } else if (items[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    /**
     * Capture image from camera
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                // Error occurred while creating the File
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);

                mPhotoFile = photoFile;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

            }
        }
    }


    /**
     * Select image fro gallery
     */
    private void dispatchGalleryIntent() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhoto.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(pickPhoto, REQUEST_GALLERY_PHOTO);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
//                Bitmap photo = (Bitmap) data.getExtras().get("data");
                try {
                    mPhotoFile = mCompressor.compressToFile(mPhotoFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (requestCode == REQUEST_GALLERY_PHOTO) {
                Uri selectedImage = data.getData();
                try {
                    mPhotoFile = mCompressor.compressToFile(new File(getRealPathFromUri(selectedImage)));
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            Glide.with(MainActivity.this).load(mPhotoFile).apply(new RequestOptions().placeholder(R.drawable.profile_pic_place_holder)).into(imvPic);

            // convert file to bitmap
            String filePath = mPhotoFile.getPath();
            photo = BitmapFactory.decodeFile(filePath);
            encodedString = encodeImageToString(photo);
            Log.d(TAG, "onActivityResult" + encodedString.toString());
            convertToJson(encodedString);
        }
    }


    private void convertToJson(String encodedString){
        JSONObject bitmpaObjet = new JSONObject();
        JSONObject object1 = new JSONObject();
        JSONArray array1 = new JSONArray();
        try {
            bitmpaObjet.put(b64, encodedString);
            object1.put(image_bytes, bitmpaObjet);
            object1.put(key, "12");
            array1.put(object1);
            object.put(instances, array1);
            Log.d(TAG, "convertToJson" + object.toString());
        }catch (JSONException e){
            e.printStackTrace();
        }
    }


    private String encodeImageToString(Bitmap bitmap){
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        int quality = 100; //100: compress nothing
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bao);

        if(bitmap != null){//important! prevent out of memory
            bitmap.recycle();
            bitmap = null;
        }

        byte [] ba = bao.toByteArray();
        String encodedImage = Base64.encodeToString(ba, Base64.DEFAULT);
        return encodedImage;
    }


    /**
     * Requesting multiple permissions (storage and camera) at once
     * This uses multiple permission model from dexter
     * On permanent denial opens settings dialog
     */
    private void requestStoragePermission(boolean isCamera) {
        Dexter.withActivity(this).withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            if (isCamera) {
                                dispatchTakePictureIntent();
                            } else {
                                dispatchGalleryIntent();
                            }
                        }
                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // show alert dialog navigating to Settings
                            showSettingsDialog();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).withErrorListener(error -> Toast.makeText(getApplicationContext(), "Error occurred! ", Toast.LENGTH_SHORT).show())
                .onSameThread()
                .check();
    }


    /**
     * Showing Alert Dialog with Settings option
     * Navigates user to app settings
     * NOTE: Keep proper title and message depending on your app
     */
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", (dialog, which) -> {
            dialog.cancel();
            openSettings();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();

    }

    // navigating user to app settings
    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }


    /**
     * Create file with current timestamp name
     *
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String mFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File mFile = File.createTempFile(mFileName, ".jpg", storageDir);
        return mFile;
    }


    /**
     * Get real file path from URI
     *
     * @param contentUri
     * @return
     */
    public String getRealPathFromUri(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = getContentResolver().query(contentUri, proj, null, null, null);
            assert cursor != null;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }

        return false;
    }


//    private void displayImageView(double ys, double xs, double ye, double xe) {
//
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        int height = displayMetrics.heightPixels;
//        int width = displayMetrics.widthPixels;
//
//        //LinearLayOut Setup
//        LinearLayout linearLayout= new LinearLayout(this);
//        linearLayout.setOrientation(LinearLayout.VERTICAL);
//
//        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.MATCH_PARENT);
//        lp.setMargins((int)(width * ys), (int)(height * xs), (int)(width * ye), (int)(height * xe));
//        linearLayout.setLayoutParams(lp);
//
//        //ImageView Setup
//        ImageView imageView = new ImageView(this);
//
//
//        //setting image resource
//        imageView.setImageBitmap(photo);
//
//        //setting image position
//        imageView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT));
//
//        //adding view to layout
//        linearLayout.addView(imageView);
//        //make visible to program
//        setContentView(linearLayout);
//    }


    private void displayImageView(double ys, double xs, double ye, double xe) {

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

        int marginLeft = (int) (width * xs);
        int marginTop = (int) (height * ys);
        int marginRight = (int) (width * (1 - xe));
        int marginBottom = (int) (height * (1 - ye));

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        lp.setMargins(marginLeft, marginTop, marginRight, marginBottom);

        View imageView = LayoutInflater.from(MainActivity.this).inflate(R.layout.block_image, container, false);

        container.addView(imageView, lp);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.finish:
                finish();

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
