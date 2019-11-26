package app.test.visonapi;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

public class Utility {
    public static void displayPreview(Context context, Bitmap bitmap, String message, final OnDialogListener listener){
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.image_preview_dialog);
        dialog.setCanceledOnTouchOutside(false);

        ImageView imageProfile = (ImageView) dialog.findViewById(R.id.imgProfile);
        TextView txtMessage = (TextView) dialog.findViewById(R.id.txtMessage);
        TextView btnProceed = (TextView) dialog.findViewById(R.id.btnProceed);

        imageProfile.setImageBitmap(bitmap);
        txtMessage.setText(message);

        btnProceed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                listener.onProceed();
            }
        });

        dialog.setOnKeyListener(new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                                 KeyEvent event) {
                // TODO Auto-generated method stub
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    dialog.dismiss();
                    listener.onProceed();
                }
                return true;
            }
        });

        dialog.show();
    }
    public interface OnDialogListener{
        void onProceed();
    }

    public interface OnTwoButtonDialogListener{
        void onProceed();

        void onCancle();
    }

}
