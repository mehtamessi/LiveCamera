package app.test.visonapi.gesture;

import android.graphics.PointF;
import android.util.Log;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;


import java.util.HashMap;
import java.util.Map;

import app.test.visonapi.gesture.view.FaceGraphic;
import app.test.visonapi.gesture.view.GraphicOverlay;

/**
 * Created by Nayan on 14/11/2018.
 */

public class GooglyFaceTracker extends Tracker<Face> {

    public static final int CHECK_ROTATION_R = 1;
    public static final int CHECK_ROTATION_L = 2;
    public static final int CHECK_SMILE = 3;
    public static final int CHECK_EYE = 4;
    private static final float EYE_CLOSED_THRESHOLD = 0.4f;
    String[] check_array = {"Turn Right", "Turn Left", "Please Smile", "Blink Your Eyes"};
    private Map<Integer, PointF> mPreviousProportions = new HashMap<>();
    private double leftEyeOpenProbability = -1.0;
    private double rightEyeOpenProbability = -1.0;
    private boolean mPreviousIsLeftOpen = true;
    private boolean mPreviousIsRightOpen = true;
    private OnActionPerformed listener;
    private int CHECK;
    private int EYE_OPEN = 1;
    private int EYE_CLOSE = 2;
    private int EYE_STATE = EYE_OPEN;

    private GraphicOverlay mOverlay;
    private FaceGraphic mFaceGraphic;

    private boolean openFlag = false;
    private boolean closeFlag = false;

    GooglyFaceTracker(GraphicOverlay overlay, int check, OnActionPerformed listener) {
        this.listener = listener;
        CHECK = check;
        mOverlay = overlay;
        mFaceGraphic = new FaceGraphic(overlay);
    }

    @Override
    public void onNewItem(int faceId, Face face) {
        mFaceGraphic.setId(faceId);
    }

    @Override
    public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {

        // listener.updateGestureMessage(check_array[CHECK - 1]);

        mOverlay.add(mFaceGraphic);
        mFaceGraphic.updateFace(face);

        Log.w("EULER Y", "" + face.getEulerY());
        Log.w("EULER Z", "" + face.getEulerZ());


/*
        float eulerY = face.getEulerY();
        Log.w("CHECK", "" + CHECK);
        Log.w("Rotation", "" + eulerY);
        Log.w("Smiling", "" + face.getIsSmilingProbability());

        float leftOpenScore = face.getIsLeftEyeOpenProbability();

        int isLeftO = 0; //1=true 2=false
        int isRightO = 0; //1=true 2=false

        boolean isLeftOpen = false;
        if (leftOpenScore > 0.9) {
            isLeftOpen = true;
            isLeftO = 1;
        }
        if (leftOpenScore < 0) {
            isLeftOpen = false;
            isLeftO = 2;
        }

        float rightOpenScore = face.getIsRightEyeOpenProbability();
        boolean isRightOpen = false;
        if (rightOpenScore > 0.9) {
            isRightOpen = true;
            isRightO = 1;
        }
        if (rightOpenScore < 0) {
            isRightOpen = false;
            isRightO = 2;
        }

        Log.w("L Eye", isLeftO + "       " + isLeftOpen + "    " + leftOpenScore);
        Log.w("R Eye", isRightO + "       " + isRightOpen + "    " + rightOpenScore);


        if (!isLeftOpen || !isRightOpen) {
            EYE_STATE = EYE_CLOSE;
        } else if (isLeftOpen || isRightOpen) {
            EYE_STATE = EYE_OPEN;
        }

        if (CHECK == CHECK_EYE) {

            if (!isLeftOpen && !isRightOpen) {
                closeFlag = true;
            }
            if (isLeftOpen && isRightOpen) {
                openFlag = true;
            }

            if (openFlag) {
                if (!closeFlag) {
                    openFlag = false;
                }
            }

            if (openFlag && closeFlag) {
                if (isLeftOpen && isRightOpen) {
                    listener.clickPicture();
                }
            }
        }*/
        /*if (isEyeBlinked(face)) {
            listener.clickPicture();
        }*/

        //new requirement 18 March 2018


        if (listener.getFaceDetection()) {

            //if gesture complete capture picture
            if (listener.getGesture()) {
                if (face.getEulerZ() > -2 && face.getEulerZ() < 12 &&
                        face.getIsLeftEyeOpenProbability() > 0.7 && face.getIsRightEyeOpenProbability() > 0.7) {
                    listener.setFaceDetection(false);
                    listener.clickPicture();
                }
            } else {
                //check eye blink
                if (isEyeBlinked(face)) {
                    listener.setGesture(true);

                }
                //check head tilt
                if (face.getEulerZ() > 35 || face.getEulerZ() < -35) {
                    listener.setGesture(true);
                }


            }
        }


    }

    @Override
    public void onMissing(FaceDetector.Detections<Face> detectionResults) {
        leftEyeOpenProbability = 0.0;
        rightEyeOpenProbability= 0.0;
        Log.w("MISSING", "MISSING");
        mOverlay.remove(mFaceGraphic);
    }

    @Override
    public void onDone() {
        mOverlay.remove(mFaceGraphic);
    }


    private boolean isEyeBlinked(Face face) {

        float currentLeftEyeOpenProbability = face.getIsLeftEyeOpenProbability();
        float currentRightEyeOpenProbability = face.getIsRightEyeOpenProbability();

        if (currentLeftEyeOpenProbability == -1.0 || currentRightEyeOpenProbability == -1.0) {
            return false;
        }

        if (leftEyeOpenProbability > 0.9 && rightEyeOpenProbability > 0.9) {
            boolean blinked = false;
            Log.w("Left Eye Prob", "" + currentLeftEyeOpenProbability);
            Log.w("Right Eye Prob", "" + currentRightEyeOpenProbability);
            if (currentLeftEyeOpenProbability < 0.4 && currentRightEyeOpenProbability < 0.4) {
                Log.w("Left Eye Prob", "" + currentLeftEyeOpenProbability);
                Log.w("Right Eye Prob", "" + currentRightEyeOpenProbability);
                Log.w("FaceId ", "CLOSE: " + face.getId());
                if (face.getId() == listener.getFaceId()) {
                    blinked = true;
                }
            }
            leftEyeOpenProbability = currentLeftEyeOpenProbability;
            rightEyeOpenProbability = currentRightEyeOpenProbability;
            listener.setFaceId(face.getId());
            Log.w("FaceId ", "OPEN: " + face.getId());
            return blinked;
        } else {
            leftEyeOpenProbability = currentLeftEyeOpenProbability;
            rightEyeOpenProbability = currentRightEyeOpenProbability;
            listener.setFaceId(face.getId());
            Log.w("FaceId ", "OPEN: " + face.getId());
            return false;
        }
    }

}