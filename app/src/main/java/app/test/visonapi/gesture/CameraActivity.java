package app.test.visonapi.gesture;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Random;

import app.test.visonapi.R;
import app.test.visonapi.Utility;
import app.test.visonapi.gesture.view.CameraSourcePreview;
import app.test.visonapi.gesture.view.GraphicOverlay;

public final class CameraActivity extends AppCompatActivity implements OnActionPerformed, View.OnClickListener, SensorEventListener {
    private static final String TAG = CameraActivity.class.getName();

    private static final int RC_HANDLE_GMS = 9001;

    // permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    //new
    CameraSource.PictureCallback mPicture;
    ImageView imgSwitch, imgFlash;
    TextView txtGestureMessage;
    RelativeLayout topLayout;
    Random random = new Random();
    //    int randomNumber = random.nextInt(4) + 1;
    int randomNumber = GooglyFaceTracker.CHECK_EYE;
    final int CHECK = randomNumber;
    ProgressDialog dialog = null;
    boolean flashmode = false;
    private CameraSource mCameraSource = null;
    private CameraSourcePreview mPreview;
    private GraphicOverlay mGraphicOverlay;
    private boolean mIsFrontFacing = false;
    private boolean isGestureCompleted = false;
    private boolean faceDetection = true;
    private Camera camera = null;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private boolean hold = false;

    private int faceId = -1;

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);

        imgSwitch = (ImageView) findViewById(R.id.imgSwitch);
        imgFlash = (ImageView) findViewById(R.id.imgFlash);
        txtGestureMessage = (TextView) findViewById(R.id.txtGestureMessage);
        imgSwitch.setOnClickListener(this);
        imgFlash.setOnClickListener(this);


        topLayout = (RelativeLayout) findViewById(R.id.topLayout);


        if (savedInstanceState != null) {
            mIsFrontFacing = savedInstanceState.getBoolean("IsFrontFacing");
        }

        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(CHECK);
            startCameraSource();
        } else {
            requestCameraPermission();
        }

        sensorActivity();

    }

    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(topLayout, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        mPreview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source");
            // we have permission, so create the camerasource
            createCameraSource(CHECK);
            return;
        }

        Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Face Tracker sample")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("IsFrontFacing", mIsFrontFacing);
    }

    @NonNull
    private FaceDetector createFaceDetector(Context context, final int check) {

        FaceDetector detector = new FaceDetector.Builder(context)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setProminentFaceOnly(mIsFrontFacing)
                .setMinFaceSize(mIsFrontFacing ? 0.35f : 0.15f)
                .build();

        Detector.Processor<Face> processor;
        if (mIsFrontFacing) {
            Tracker<Face> tracker = new GooglyFaceTracker(mGraphicOverlay, check, this);
            processor = new LargestFaceFocusingProcessor.Builder(detector, tracker).build();
        } else {
            MultiProcessor.Factory<Face> factory = new MultiProcessor.Factory<Face>() {
                @Override
                public Tracker<Face> create(Face face) {
                    return new GooglyFaceTracker(mGraphicOverlay, check, CameraActivity.this);
                }
            };
            processor = new MultiProcessor.Builder<>(factory).build();
        }

        detector.setProcessor(processor);

        if (!detector.isOperational()) {

            Log.w(TAG, "Face detector dependencies are not yet available.");

            IntentFilter lowStorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowStorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }
        return detector;
    }

    private void createCameraSource(int check) {
        final Context context = getApplicationContext();
        if (check == GooglyFaceTracker.CHECK_ROTATION_L) {
            txtGestureMessage.setText("Turn Left");
        } else if (check == GooglyFaceTracker.CHECK_ROTATION_R) {
            txtGestureMessage.setText("Turn Right");
        } else if (check == GooglyFaceTracker.CHECK_EYE) {
            //    txtGestureMessage.setText("Blink Your Eyes");
            txtGestureMessage.setText("Blink eyes or tilt your face");
        } else if (check == GooglyFaceTracker.CHECK_SMILE) {
            txtGestureMessage.setText("Please Smile");
        }

        final FaceDetector detector = createFaceDetector(context, check);

        int facing = CameraSource.CAMERA_FACING_FRONT;
        if (!mIsFrontFacing) {
            facing = CameraSource.CAMERA_FACING_BACK;
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setFacing(facing)
                .setRequestedPreviewSize(640, 480)
                .setRequestedFps(60.0f)
                .setAutoFocusEnabled(true)
                .build();

        mPicture = new CameraSource.PictureCallback() {

            @Override
            public void onPictureTaken(final byte[] bytes) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap = Bitmap.createScaledBitmap(bitmap, 600, 650, true);
                        while (bitmap.getWidth() > 600) {
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                        }


                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            try {
                                int angle = 0;
                                ExifInterface exif = new ExifInterface(new ByteArrayInputStream(bytes));
                                int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
                                    angle = 90;
                                } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
                                    angle = 180;
                                } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
                                    angle = 270;
                                }
                                if (angle != 0) {
                                    bitmap = rotateBitmap(bitmap, angle);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }

                        final Bitmap bitmap2 = bitmap;


                        dialog = new ProgressDialog(CameraActivity.this);
                        dialog.setMessage("Processing image, please wait...");
                        dialog.setCanceledOnTouchOutside(false);
                        dialog.setOnKeyListener(new Dialog.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface arg0, int keyCode,
                                                 KeyEvent event) {
                                // TODO Auto-generated method stub
                                if (keyCode == KeyEvent.KEYCODE_BACK) {

                                }
                                return true;
                            }
                        });
                        dialog.show();

                        Thread timer = new Thread() {

                            @Override
                            public void run() {

                                try {

                                    Bitmap bitmap1 = bitmap2;

                                    FaceDetector detectorLocal = new FaceDetector.Builder(context)
                                            .setTrackingEnabled(false)
                                            .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                                            .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                                            .setMode(FaceDetector.ACCURATE_MODE)
                                            .build();
                                    SparseArray<Face> faces = null;
                                    if (detectorLocal.isOperational() && bitmap1 != null) {
                                        Log.w("TAG", "isOperational");
                                        int count = 0;
                                        while (count < 4) {
                                            Frame frame = new Frame.Builder().setBitmap(bitmap1).build();
                                            faces = detectorLocal.detect(frame);
                                            if (faces.size() > 0) {
                                                break;
                                            } else {
                                                bitmap1 = rotateBitmap(bitmap1, 90);
                                            }
                                            count++;
                                        }

                                    }else {
                                        Log.w(TAG, "Face detector dependencies are not yet available.");
                                    }


                                    if (dialog.isShowing()) {
                                        dialog.dismiss();
                                    }
                                    final Face face;
                                    final boolean multipleFaces;
                                    if (faces.size() == 1) {
                                        face = faces.valueAt(0);
                                        multipleFaces = false;
                                    } else if (faces.size() > 1) {
                                        face = faces.valueAt(0);
                                        multipleFaces = true;
                                    } else {
                                        multipleFaces = false;
                                        face = null;
                                    }

                                    final Bitmap finalBitmap = bitmap1;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                            if (!multipleFaces) {

                                                if (face != null) {

                                                    //  Face face = faces.valueAt(0);
                                                    //check if eyes are closed in image
                                                    if (face.getIsLeftEyeOpenProbability() > 0.5 && face.getIsRightEyeOpenProbability() > 0.5) {

                                                        if (face.getEulerZ() > -5 && face.getEulerZ() < 15) {

                                                       /* runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {*/

                                                            int colour = finalBitmap.getPixel(10, 10);

                                                            int red = Color.red(colour);
                                                            int blue = Color.blue(colour);
                                                            int green = Color.green(colour);
                                                            int alpha = Color.alpha(colour);

                                                            String hex = String.format("#%02x%02x%02x%02x", alpha, red, green, blue);

                                                            Log.w(TAG, "hex: " + hex);

//                                                            TempImageUri tempImageUri = new TempImageUri();
//                                                            tempImageUri.setUri(Utility.bitmapToBase64(finalBitmap));
//                                                            DBManager.getInstance(CameraActivity.this).addOrUpdateRowInDB(tempImageUri);
//*/
//                                                            Intent returnIntent = new Intent();
//                                                            setResult(Activity.RESULT_OK, returnIntent);
//                                                            CameraActivity.this.finish();



                                                        } else {
                                                       /* runOnUiThread(new Runnable() {
                                                            @Override
                                                            public void run() {*/
                                                            String message = "Your head is tilted in image. Please capture it again.";
                                                            Utility.displayPreview(CameraActivity.this, finalBitmap, message, new Utility.OnDialogListener() {
                                                                @Override
                                                                public void onProceed() {
                                                                    setFaceDetection(true);
                                                                    setGesture(false);
                                                                }
                                                            });

                                                          /*  }
                                                        });*/
                                                        }

                                                    } else {
                                              /*      runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {*/
                                                        String message = "Eyes are closed in this image. Please capture it again.";
                                                        Utility.displayPreview(CameraActivity.this, finalBitmap, message, new Utility.OnDialogListener() {
                                                            @Override
                                                            public void onProceed() {
                                                                setFaceDetection(true);
                                                                setGesture(false);
                                                            }
                                                        });

                                                        /*}
                                                    });*/
                                                    }
                                                } else {
                                              /*  runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {*/
                                                    Toast.makeText(CameraActivity.this, "Unable to detect face from Image", Toast.LENGTH_LONG).show();
                                                    setFaceDetection(true);
                                                    setGesture(false);
                                                /*    }
                                                });*/
                                                }

                                            } else {
                                              /*  runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {*/
                                                String message = "Multiple faces detected. Please capture it again.";
                                                Utility.displayPreview(CameraActivity.this, finalBitmap, message, new Utility.OnDialogListener() {
                                                    @Override
                                                    public void onProceed() {
                                                        setFaceDetection(true);
                                                        setGesture(false);
                                                    }
                                                });
                                                /*    }
                                                });*/
                                            }


                                        }
                                    });
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        };

                        timer.start();


                    }
                });

            }

        };


    }

    private void startCameraSource() {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
                /*Camera camera = getCamera(mCameraSource);
                setCameraDisplayOrientation(camera);*/
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    @Override
    public void clickPicture() {
//        txtGestureMessage.setVisibility(View.GONE);
        try {
            //setCameraDisplayOrientation(getCamera(mCameraSource));
            mCameraSource.takePicture(null, mPicture);
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    @Override
    public void updateGestureMessage(final String message) {
/*
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtGestureMessage.setText(message);
            }
        });
*/

    }

    @Override
    public boolean getGesture() {
        return isGestureCompleted;
    }

    @Override
    public void setGesture(boolean completed) {
        isGestureCompleted = completed;
    }

    @Override
    public boolean getFaceDetection() {
        return faceDetection;
    }

    @Override
    public void setFaceDetection(boolean faceDetection) {
        this.faceDetection = faceDetection;
    }

    @Override
    public boolean getPosition() {
        return hold;
    }

    @Override
    public int getFaceId() {
        return faceId;
    }

    @Override
    public void setFaceId(int faceId) {
        this.faceId = faceId;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imgSwitch:
                mIsFrontFacing = !mIsFrontFacing;

                if (mCameraSource != null) {
                    mCameraSource.release();
                    mCameraSource = null;
                }

                createCameraSource(CHECK);
                startCameraSource();
                break;
            case R.id.imgFlash:
                flashOnButton();
                break;
        }
    }

    private void flashOnButton() {
        camera = getCamera(mCameraSource);
        if (camera != null) {
            try {
                Camera.Parameters param = camera.getParameters();
                param.setFlashMode(!flashmode ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
                camera.setParameters(param);
                flashmode = !flashmode;
                if (flashmode) {
                    imgFlash.setImageResource(R.drawable.ic_flash_off);
                } else {
                    imgFlash.setImageResource(R.drawable.ic_flash_on);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private Camera getCamera(@NonNull CameraSource cameraSource) {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields) {
            if (field.getType() == Camera.class) {
                field.setAccessible(true);
                try {
                    Camera camera = (Camera) field.get(cameraSource);

                    if (camera != null) {
                        return camera;
                    }
                    return null;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        return null;
    }

    public void sensorActivity() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        float[] g = new float[3];
        g = sensorEvent.values.clone();

        double norm_Of_g = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);

// Normalize the accelerometer vector

        double[] h = new double[3];

        h[0] = g[0] / norm_Of_g;
        h[1] = g[1] / norm_Of_g;
        h[2] = g[2] / norm_Of_g;

        int inclination = (int) Math.round(Math.toDegrees(Math.acos(h[1])));


        int rotation = (int) Math.round(Math.toDegrees(Math.atan2(g[0], g[1])));
        Log.w("rotation", "" + rotation);

        if (rotation > 20 || rotation < -10) {
            setGesture(false);
            hold = false;
            //    txtGestureMessage.setText("Please hold phone straight");
        } else {
            hold = true;
            //    txtGestureMessage.setText("Blink eyes or tilt your face");
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.w("SensorAccuracy", "" + i);
    }
}
