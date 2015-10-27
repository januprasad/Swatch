package com.jenuine.materialpainter;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.jenuine.materialpainter.view.PaletteView;
import com.novoda.notils.caster.Views;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class PainterActivity extends ActionBarActivity {

    private static final int READ_REQUEST_CODE = 42;
    private static final int ANIMATION_START_DELAY = 300;
    private static final int ANIMATION_DURATION = 400;
    private static final float TENSION = 1.f;

    private View root;
    private TextView startingText;
    private PaletteView paletteView;
    private ImageButton selectImage;
    private ImageView imageView;
    private Toolbar toolbar;

    private int fabHideTranslationY;
    private int toolbarHideTranslationY;

    private boolean viewsVisible = false;
    private boolean isImageSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painter);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.painter_prompt);
        fabHideTranslationY = 2 * getResources().getDimensionPixelOffset(R.dimen.fab_min_size);
        toolbarHideTranslationY = -2 * getResources().getDimensionPixelOffset(R.dimen.toolbar_min_size);

        initViews();
        setListeners();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();

                showImage(uri);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_share && isImageSelected) {
            paletteView.showSwatchesView();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    takeScreenshot();
                }
            }, 300);
            return true;
        } else startingText.setTextSize(18);

        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        root = Views.findById(this, R.id.root_view);
        startingText = Views.findById(this, R.id.starting_text);
        paletteView = Views.findById(this, R.id.palette);
        imageView = Views.findById(this, R.id.show_image);
        selectImage = Views.findById(this, R.id.fab_select_image);
    }

    public static int getDominantColor(Bitmap bitmap) {
        Bitmap bitmap1 = Bitmap.createScaledBitmap(bitmap, 1, 1, true);
        int color = bitmap1.getPixel(0, 0);
        return color;
    }

    private void setListeners() {
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewsVisible) {
                    hideViews();
                } else {
                    showViews();
                }
            }
        });

        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runTranslateAnimationWithEndAction(selectImage, fabHideTranslationY, performImageSearchRunnable);
            }
        });
    }

    private Runnable performImageSearchRunnable =
            new Runnable() {
                @Override
                public void run() {
                    performFileSearch();
                }
            };

    public void performFileSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private void showImage(Uri uri) {
        try {

            isImageSelected = true;
            Bitmap image = parcelImage(uri);
            imageView.setBackgroundColor(getDominantColor(image));
            generatePalette(image);
            hideViews();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Bitmap parcelImage(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor;
        parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        imageView.setImageBitmap(image);
        parcelFileDescriptor.close();
        return image;
    }

    private void generatePalette(Bitmap image) {
        Palette.generateAsync(image, new Palette.PaletteAsyncListener() {

            public void onGenerated(Palette palette) {
                if (palette != null) {
                    paletteView.updateWith(palette);
                }
            }
        });
    }


    private void showViews() {
        runTranslateAnimation(selectImage, 0);
        runTranslateAnimation(toolbar, 0);
        viewsVisible = true;
    }

    private void hideViews() {
        runTranslateAnimation(selectImage, fabHideTranslationY);
        runTranslateAnimation(toolbar, toolbarHideTranslationY);
        startingText.setVisibility(View.GONE);
        viewsVisible = false;
    }

    private void runTranslateAnimation(View view, int translateY) {
        view.animate()
                .translationY(translateY)
                .setInterpolator(new OvershootInterpolator(TENSION))
                .setStartDelay(ANIMATION_START_DELAY)
                .setDuration(ANIMATION_DURATION)
                .start();

    }

    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void runTranslateAnimationWithEndAction(View view, int translateY, Runnable runnable) {
        view.animate()
                .translationY(translateY)
                .setInterpolator(new OvershootInterpolator(TENSION))
                .setStartDelay(ANIMATION_START_DELAY)
                .setDuration(ANIMATION_DURATION)
                .withEndAction(runnable)
                .start();

    }


    private void takeScreenshot() {
        Date now = new Date();
        android.text.format.DateFormat.format("yyyy-MM-dd_hh:mm:ss", now);

        try {
            // image naming and path  to include sd card  appending name you choose for file
            File folder = new File(Environment.getExternalStorageDirectory().toString() + "/" + "Swatch");
            if (!folder.exists())
                folder.mkdirs();
            String mPath = folder.getAbsolutePath() + "/" + now + ".jpg";

            Bitmap bitmap;
            View v1 = this.getWindow().getDecorView().findViewById(R.id.container);
            v1.setDrawingCacheEnabled(true);
            bitmap = Bitmap.createBitmap(v1.getDrawingCache());
            v1.setDrawingCacheEnabled(false);
            File imageFile = new File(mPath);

            FileOutputStream outputStream = new FileOutputStream(imageFile);
            int quality = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
            outputStream.flush();
            outputStream.close();

            openScreenshot(imageFile);
        } catch (Throwable e) {
            // Several error may come out with file handling or OOM
            e.printStackTrace();
        }
    }

    private void openScreenshot(File imageFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(imageFile);
        intent.setDataAndType(uri, "image/*");
        startActivity(intent);
    }
}
