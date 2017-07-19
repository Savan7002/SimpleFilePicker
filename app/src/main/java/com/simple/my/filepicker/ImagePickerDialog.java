package com.simple.my.filepicker;

import android.Manifest;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.io.File;
import java.io.IOException;

public class ImagePickerDialog extends DialogFragment implements View.OnClickListener {

    private static final int REQUEST_PERMISSION_GALLERY = 0;
    private static final int REQUEST_PERMISSION_CAMERA_GALLERY = 10;
    private static final int REQUEST_PERMISSION_CAMERA = 1;
    private static final int REQUEST_PIC_FROM_GALLERY = 101;
    private static final int REQUEST_PIC_FROM_CAMERA = 102;
    private static final int REQUEST_CROP_PIC = 103;
    private String selectedFilePath;

    public static interface OnCompleteListener {
        public abstract void onComplete(String path);
    }

    private OnCompleteListener mListener;

    public static boolean checkForPermission(final Context context, final String permission) {
        int result = ContextCompat.checkSelfPermission(context, permission);
        //If permission is granted then it returns 0 as result
        return result == PackageManager.PERMISSION_GRANTED;
    }

    // make sure the Activity implemented it
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.mListener = (OnCompleteListener) activity;
        } catch (final ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnCompleteListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_NoActionBar);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_image_picker, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeComponent(view);
    }

    /**
     * Initialize components
     *
     * @param view
     */
    private void initializeComponent(View view) {
        getDialog().getWindow().setBackgroundDrawableResource(R.color.background);
        final RelativeLayout rlParent = (RelativeLayout) view.findViewById(R.id.dialog_image_picker_rl_parent);
        final Button tvGallery = (Button) view.findViewById(R.id.dialog_image_picker_btn_gallery);
        final Button tvCamera = (Button) view.findViewById(R.id.dialog_image_picker_btn_camera);
        final Button tvCancel = (Button) view.findViewById(R.id.dialog_image_picker_btn_cancel);
        final LinearLayout llAnimateLayout = (LinearLayout) view.findViewById(R.id.dialog_image_picker_ll_animetlayout);

        final Animation animation = AnimationUtils.loadAnimation(getActivity(), R.anim.bottom_up);
        llAnimateLayout.startAnimation(animation);
        llAnimateLayout.setVisibility(View.VISIBLE);

        rlParent.setOnClickListener(this);
        tvGallery.setOnClickListener(this);
        tvCamera.setOnClickListener(this);
        tvCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        final String writeStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        final String cameraAccess = Manifest.permission.CAMERA;
        switch (v.getId()) {
            case R.id.dialog_image_picker_btn_gallery:
                if (checkForPermission(getActivity(), writeStorage)) {
                    openGallery();
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{writeStorage}, REQUEST_PERMISSION_GALLERY);
                    }
                }
                break;
            case R.id.dialog_image_picker_btn_camera:
                if (checkForPermission(getActivity(), cameraAccess) && checkForPermission(getActivity(), writeStorage)) {
                    openCamera();
                } else {
                    if (!checkForPermission(getActivity(), cameraAccess)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{cameraAccess}, REQUEST_PERMISSION_CAMERA);
                        }
                    } else if (!checkForPermission(getActivity(), writeStorage)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{writeStorage}, REQUEST_PERMISSION_CAMERA_GALLERY);
                        }
                    }
                }
                break;
            case R.id.dialog_image_picker_btn_cancel:
                getDialog().dismiss();
                break;
            case R.id.dialog_image_picker_rl_parent:
                getDialog().dismiss();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION_GALLERY:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery();
                } else {
                    //Utils.showSnackBar(getView(), getString(R.string.err_msg_permission_required_for_furthur_process), true, getActivity());
                }
                break;
            case REQUEST_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    final String writeStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
                    if (!checkForPermission(getActivity(), writeStorage)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{writeStorage}, REQUEST_PERMISSION_CAMERA_GALLERY);
                        }
                    } else {
                        openCamera();
                    }
                } else {
                    //Utils.showSnackBar(getView(), getString(R.string.err_msg_permission_required_for_furthur_process), true, getActivity());
                }
                break;
            case REQUEST_PERMISSION_CAMERA_GALLERY:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    //Utils.showSnackBar(getView(), getString(R.string.err_msg_permission_required_for_furthur_process), true, getActivity());
                }
                break;
        }
    }

    /**
     * Open camera for capture image
     */
    private void openCamera() {
        try {
            final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + getString(R.string.app_name));
            if (!file.exists()) {
                file.mkdir();
            }
            final File cameraFile = new File(file.getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg");
            cameraFile.createNewFile();
            selectedFilePath = cameraFile.getAbsolutePath();
            final Intent intentPicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri imageUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                imageUri = FileProvider.getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + ".provider", cameraFile);
            } else {
                imageUri = Uri.fromFile(cameraFile);
            }
            intentPicture.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intentPicture, REQUEST_PIC_FROM_CAMERA);
        } catch (IOException | ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open gallery for select image
     */
    private void openGallery() {
        final Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_PIC_FROM_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PIC_FROM_GALLERY:
                    final Uri selectedImage = data.getData();
                    selectedFilePath = GetFilePath.getPath(getActivity(), selectedImage);
                    //final String[] fileFormats = getResources().getStringArray(R.array.allowed_file_format);
                    //final ArrayList<String> formats = new ArrayList<String>(Arrays.asList(fileFormats));
                    if (!TextUtils.isEmpty(selectedFilePath) && new File(selectedFilePath).exists() /*&& formats.contains(selectedFilePath.substring(selectedFilePath.lastIndexOf(".") + 1).toLowerCase())*/) {
                        //performCrop();

                        mListener.onComplete(selectedFilePath);
                        getDialog().dismiss();
                    }
                    break;
                case REQUEST_PIC_FROM_CAMERA:
                    if (!TextUtils.isEmpty(selectedFilePath) && new File(selectedFilePath).exists()) {
                        //performCrop();
                        mListener.onComplete(selectedFilePath);
                        getDialog().dismiss();
                    }
                    break;
                case REQUEST_CROP_PIC:
                    final Uri imageUri = data.getData();
                    selectedFilePath = GetFilePath.getPath(getActivity(), imageUri);

                    mListener.onComplete(selectedFilePath);
                    getDialog().dismiss();

                    break;
            }
        }
    }

    /**
     * Perform crop operation on selected image
     */
    private void performCrop() {
        try {
            final Intent cropIntent = new Intent("com.android.camera.action.CROP");
            final File cropedFile = new File(getActivity().getExternalCacheDir() + File.separator + "cropfile.jpg");
            if (cropedFile.exists()) {
                cropedFile.delete();
            }
            cropedFile.createNewFile();
            Uri cropImageUri;
            Uri dataImageUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cropImageUri = FileProvider.getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + ".provider", cropedFile);
                dataImageUri = FileProvider.getUriForFile(getActivity(), getActivity().getApplicationContext().getPackageName() + ".provider", new File(selectedFilePath));
            } else {
                cropImageUri = Uri.fromFile(cropedFile);
                dataImageUri = Uri.fromFile(new File(selectedFilePath));
            }
            cropIntent.setDataAndType(dataImageUri, "image");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 1);
            cropIntent.putExtra("aspectY", 1);
            cropIntent.putExtra("outputX", 200);
            cropIntent.putExtra("outputY", 200);
            cropIntent.putExtra("return-data", false);
            cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, cropImageUri);
            startActivityForResult(cropIntent, REQUEST_CROP_PIC);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
