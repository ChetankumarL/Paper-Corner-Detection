package com.example.papercornerdetection;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC3;


public class MainActivity extends Activity {

    public static final String TAG = "MainActivity";
    public static final String TEMP_PHOTO_FILE_NAME = "temp_photo.PNG";
    public static final int REQUEST_CODE_GALLERY = 0x1;
    public static final int REQUEST_CODE_TAKE_PICTURE = 0x2;
    private ContentResolver mContentResolver;
    private ImageView mImageView;
    private File mFileTemp;
    final int IMAGE_MAX_SIZE = 1024;
    public Mat originalMat;

    /**
     * OpenCVLoader Success Callback
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpView();  //Set up View
        setUpClickListeners(); //Handle Click Events
    }


    private void setUpView() {
        mContentResolver = getContentResolver();
        mImageView = findViewById(R.id.ivImage);
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            mFileTemp = new File(Environment.getExternalStorageDirectory(), TEMP_PHOTO_FILE_NAME);
        } else {
            mFileTemp = new File(getFilesDir(), TEMP_PHOTO_FILE_NAME);
        }
    }

    private void setUpClickListeners() {
        findViewById(R.id.bLoadImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File file = new File(mFileTemp.getPath());
                if (file.exists()) {
                    file.delete();
                }
                openGallery(); //Choose from Gallery
            }
        });

        findViewById(R.id.bClickImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File file = new File(mFileTemp.getPath());
                if (file.exists()) {
                    Log.w("myApp", "old image exists and is going to be deleted ");
                    file.delete();
                }
                takePicture(); //Camera View
            }
        });
    }

    /**
     * Request Camera view
     */
    private void takePicture() {

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            Uri mImageCaptureUri;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                mImageCaptureUri = Uri.fromFile(mFileTemp);
            } else {
                mImageCaptureUri = InternalStorageContentProvider.CONTENT_URI;
            }
            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
            intent.putExtra("return-data", true);
            startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
        } catch (ActivityNotFoundException e) {

            Log.d(TAG, "cannot take picture", e);
        }
    }

    /**
     * Request Gallery View
     */
    private void openGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, REQUEST_CODE_GALLERY);
    }

    /**
     * Apply Border Detection for Paper with contrast background
     */
    private void startCropImage() {
        try {
            Bitmap originalBitmap;
            Bitmap currentBitmap;
            originalBitmap = getBitmap(mFileTemp.getPath());
            Bitmap tempBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
            originalMat = new Mat(tempBitmap.getHeight(), tempBitmap.getWidth(), CvType.CV_8UC4);
            Utils.bitmapToMat(tempBitmap, originalMat);
            currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, false);
            findPaperCorner(currentBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private Uri getImageUri(String path) {
        return Uri.fromFile(new File(path));
    }

    private Bitmap getBitmap(String path) {
        Uri uri = getImageUri(path);
        InputStream in;
        try {
            in = mContentResolver.openInputStream(uri);

            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(in, null, o);
            in.close();

            int scale = 1;
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            in = mContentResolver.openInputStream(uri);
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            in.close();

            return b;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "file " + path + " not found");
        } catch (IOException e) {
            Log.e(TAG, "file " + path + " not found");
        }
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_GALLERY:
                try {

                    InputStream inputStream = getContentResolver().openInputStream(data.getData());
                    FileOutputStream fileOutputStream = new FileOutputStream(mFileTemp);
                    copyStream(inputStream, fileOutputStream);
                    fileOutputStream.close();
                    inputStream.close();

                    startCropImage();
                } catch (Exception e) {
                    Log.e(TAG, "Error while creating temp file", e);
                }

                break;
            case REQUEST_CODE_TAKE_PICTURE:
                // Mat grayImage = Imgcodecs.imread(mFileTemp.getPath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
                //Imgproc.threshold(grayImage, grayImage, 0, 255, Imgproc.THRESH_OTSU);
                // Imgcodecs.imwrite(mFileTemp.getPath(), grayImage);

                startCropImage();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * @param input
     * @param output
     * @throws IOException
     */
    public static void copyStream(InputStream input, OutputStream output)
            throws IOException {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     * find Paper Corner
     *
     * @param sourceBitmap
     * @return
     */
    private Bitmap findPaperCorner(Bitmap sourceBitmap) {

        Bitmap roiBitmap = Bitmap.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Mat sourceMat = new Mat(sourceBitmap.getWidth(), sourceBitmap.getHeight(), CV_8UC3);
        Utils.bitmapToMat(sourceBitmap, sourceMat);
        sourceMat.convertTo(sourceMat, -1, 1, 50); //apply brightness
        final Mat mat = new Mat();
        sourceMat.copyTo(mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(mat, mat, 146, 150, Imgproc.THRESH_BINARY);

        // find contours
        List<MatOfPoint> contours = new ArrayList<>();
        List<RotatedRect> boundingRects = new ArrayList<>();
        Imgproc.findContours(mat, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        // find appropriate bounding rectangles
        for (MatOfPoint contour : contours) {
            MatOfPoint2f areaPoints = new MatOfPoint2f(contour.toArray());
            RotatedRect boundingRect = Imgproc.minAreaRect(areaPoints);
            boundingRects.add(boundingRect);
        }
        Point rect_points[];
        RotatedRect documentRect = getBestRectByArea(boundingRects);
        Scalar redcolor = new Scalar(255, 0, 0);
        if (documentRect != null) {
            rect_points = new Point[4];
            documentRect.points(rect_points);
            for (int i = 0; i < 4; ++i) {
               // Imgproc.line(sourceMat, rect_points[i], rect_points[(i + 1) % 4], redcolor, 5); //Draw line of rectangle
            }
        }
        Log.e(TAG, "ContourSize: " + contours.size());
        double maxVal = 0;
        int maxValIdx = 0;
        for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
            double contourArea = Imgproc.contourArea(contours.get(contourIdx));
            if (maxVal < contourArea) {
                maxVal = contourArea;
                maxValIdx = contourIdx;
            }
        }
        Rect rect = null;
        //  Imgproc.drawContours(sourceMat, contours, maxValIdx, new Scalar(0,255,0),  -2); //Draw/fill contour area
        if (!contours.isEmpty()) {
            Log.e("largestContour", "" + contours.get(maxValIdx));
            rect = Imgproc.boundingRect(contours.get(maxValIdx));
            sourceMat.submat(rect);
        }

        //crop Contour area from original bitmap/Mat
        Rect rectCrop = new Rect(rect.x, rect.y, rect.width, rect.height);
        Rect roi = rectCrop;
        Mat cropped = new Mat(sourceMat, roi);
        cropped.clone();

        showImage(cropped);//Show Image bitmap from Mat

        return roiBitmap;
    }

    /**
     * Find Biggest rectangle from contour
     *
     * @param boundingRects
     * @return
     */
    public static RotatedRect getBestRectByArea(List<RotatedRect> boundingRects) {
        RotatedRect bestRect = null;

        if (boundingRects.size() >= 1) {
            RotatedRect boundingRect;
            Point[] vertices = new Point[4];
            Rect rect;
            double maxArea;
            int ixMaxArea = 0;

            // find best rect by area
            boundingRect = boundingRects.get(ixMaxArea);
            boundingRect.points(vertices);
            rect = Imgproc.boundingRect(new MatOfPoint(vertices));
            maxArea = rect.area();

            for (int ix = 1; ix < boundingRects.size(); ix++) {
                boundingRect = boundingRects.get(ix);
                boundingRect.points(vertices);
                rect = Imgproc.boundingRect(new MatOfPoint(vertices));

                if (rect.area() > maxArea) {
                    maxArea = rect.area();
                    ixMaxArea = ix;
                }
            }

            bestRect = boundingRects.get(ixMaxArea);
        }

        return bestRect;
    }

    /**
     * Show Crop bitmap
     * @param img
     */
    public void showImage(Mat img) {
        Bitmap img2 = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, img2);
        mImageView.setImageBitmap(img2);
    }
}
