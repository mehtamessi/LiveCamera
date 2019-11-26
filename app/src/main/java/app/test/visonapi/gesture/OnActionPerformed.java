package app.test.visonapi.gesture;

/**
 * Created by Nayan on 14/11/2018.
 */

public interface OnActionPerformed {

    void clickPicture();

    void updateGestureMessage(String message);

    boolean getGesture();

    void setGesture(boolean completed);

    boolean getFaceDetection();

    void setFaceDetection(boolean faceDetection);

    public boolean getPosition();

    public int getFaceId();

    public void setFaceId(int faceId);


}
