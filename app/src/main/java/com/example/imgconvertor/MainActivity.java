package com.example.imgconvertor;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_TAKE_PHOTO = 1;

    private String currentPhotoPath;
    private ImageView imageView;
    private Button btConvet;
    private TextView tvJpegPath;
    private TextView tvPngPath;
    private String pathtoPng;
    private Disposable subscribe;
    private AlertDialog cancelDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        imageView = findViewById(R.id.imageView);
        btConvet = findViewById(R.id.bt_convert);
        tvJpegPath = findViewById(R.id.tv_jpeg_path);
        tvPngPath = findViewById(R.id.tv_png_path);
        btConvet.setOnClickListener(view -> subscribe = Completable.create(emitter -> {
            convertImg();
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> createCancelDialog())
                .doOnComplete(() -> cancelDialog.dismiss())
                .doOnDispose(() -> tvPngPath.setText("Операция отменена"))
                .subscribe(() -> tvPngPath.setText(pathtoPng)));

        findViewById(R.id.bt_create).setOnClickListener(view -> dispatchTakePictureIntent());
    }

    private void createCancelDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        cancelDialog = dialog.setTitle("Конвертация")
                .setMessage("Выполяется...")
                .setNegativeButton("Cancel", (dialogInterface, i) -> subscribe.dispose())
                .setOnCancelListener(dialogInterface -> subscribe.dispose())
                .create();
        cancelDialog.show();

    }

    private void convertImg() {

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        String tpath = currentPhotoPath.replace(".jpg", ".png");
        pathtoPng = tpath.replace("JPEG", "PNG");

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(pathtoPng);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            setPic();
            btConvet.setEnabled(true);
        }
    }


    private void setPic() {
        // Get the dimensions of the View
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        //bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        imageView.setImageBitmap(bitmap);
        tvJpegPath.setText(currentPhotoPath);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
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
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (subscribe != null)
            subscribe.dispose();
    }
}