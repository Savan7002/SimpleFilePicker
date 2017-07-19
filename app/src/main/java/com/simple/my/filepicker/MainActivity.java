package com.simple.my.filepicker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ImagePickerDialog.OnCompleteListener {

    /**
     * Request code for file picker
     */
    private final static int FILE_PICK_REQUEST_CODE = 100;
    /**
     * Request code for file access permission
     */
    public final static int FILE_READ_PERMISSION_REQUEST_CODE = 101;
    private TextView tvFileName;
    private ImageView ivSelectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button btnPickFile = (Button) findViewById(R.id.activity_main_btn_pick_file);
        final Button btnPickImage = (Button) findViewById(R.id.activity_main_btn_pick_image);
        tvFileName = (TextView) findViewById(R.id.activity_main_tv_file_name);
        ivSelectedImage = (ImageView) findViewById(R.id.activity_main_iv_selected_file);

        btnPickFile.setOnClickListener(this);
        btnPickImage.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.activity_main_btn_pick_file:
                final String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                if (checkForPermission(this, permission)) {
                    openFilePicker();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{permission}, FILE_READ_PERMISSION_REQUEST_CODE);
                    }
                }
                break;
            case R.id.activity_main_btn_pick_image:
                final ImagePickerDialog imagePickerDialog = new ImagePickerDialog();
                imagePickerDialog.show(getFragmentManager(), this.getClass().getSimpleName());
                break;
        }
    }

    /**
     * Called to check permission(In Android M and above versions only)
     *
     * @param permission, which we need to pass
     * @return true, if permission is granted else false
     */
    public static boolean checkForPermission(final Context context, final String permission) {
        int result = ContextCompat.checkSelfPermission(context, permission);
        //If permission is granted then it returns 0 as result
        return result == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case FILE_READ_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker();
                }
                break;
        }
    }

    /**
     * Open picker for file selection
     */
    private void openFilePicker() {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        // For Samsung devices
        //final Intent intent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        //intent.putExtra("CONTENT_TYPE", "*/*");
        //intent.addCategory(Intent.CATEGORY_DEFAULT);

        startActivityForResult(intent, FILE_PICK_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            String path = data.getDataString();
            path = GetFilePath.getPath(this, Uri.parse(path));
            if (path != null) {
                final File file = new File(path);
                if (file.length() > 0) {
                    Toast.makeText(this, String.format("File %s selected", file.getName()), Toast.LENGTH_LONG).show();
                    tvFileName.setText(file.getName());
                } else {
                    Toast.makeText(this, "File is corrupt or not proper.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Please select file from local storage.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onComplete(String path) {
        Glide.with(MainActivity.this).load(new File(path)).into(ivSelectedImage);
    }
}
