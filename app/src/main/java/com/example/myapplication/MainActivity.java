package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_CODE_STORAGE = 1001;
    private static final int REQUEST_CODE_OVERLAY_PERMISSION = 1002;
    private static final int UCROP_REQUEST_CODE = 69;

    private ImageView imageView;
    private Button galleryButton;
    private WindowManager windowManager;
    private ImageView statusBarImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        galleryButton = findViewById(R.id.selectImageButton);
        imageView = findViewById(R.id.imageView);
        galleryButton.setOnClickListener(v -> openGallery());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE_STORAGE);
        }

        checkOverlayPermission();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                startCrop(imageUri);
            }
        } else if (requestCode == UCROP_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(resultUri));
                    imageView.setImageBitmap(bitmap);
                    setStatusBarBackground(bitmap);
                } catch (IOException e) {
                    Log.e("MainActivity", "Error loading cropped image", e);
                }
            }
        } else if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    // Permission granted, proceed
                } else {
                    Log.d("MainActivity", "Overlay permission not granted");
                }
            }
        } else {
            Log.d("MainActivity", "Image selection cancelled or failed");
        }
    }

    private void startCrop(Uri uri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_image.jpg"));
        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(80);
        options.setToolbarColor(ContextCompat.getColor(this, R.color.white));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.white));

        UCrop uCrop = UCrop.of(uri, destinationUri);
        uCrop.withOptions(options);
        uCrop.withAspectRatio(45, 7); // You can change the aspect ratio or set other options
        uCrop.start(this);
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION);
            }
        }
    }

    private void setStatusBarBackground(Bitmap bitmap) {
        if (bitmap != null) {
            Drawable drawable = new BitmapDrawable(getResources(), bitmap);

            if (windowManager != null && statusBarImageView != null) {
                windowManager.removeView(statusBarImageView);
            }

            statusBarImageView = new ImageView(this);
            statusBarImageView.setImageDrawable(drawable);
            statusBarImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    getStatusBarHeight(),
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);

            layoutParams.gravity = Gravity.TOP;

            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager != null) {
                windowManager.addView(statusBarImageView, layoutParams);
            }
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        Log.d("MainActivity", "Status Bar Height: " + result);
        return result;
    }
}
