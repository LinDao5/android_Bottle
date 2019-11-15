package com.androidwave.filepicker.utils;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.widget.TextView;

import com.androidwave.filepicker.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Global {


    public static ProgressDialog mProgressDialog = null;
    public static void showLoading(Context context, String title)
    {
        if (mProgressDialog != null){return;}
        String strPleaseWaitAwhile = "Loading...";
        mProgressDialog.setMessage(strPleaseWaitAwhile);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
            }
        });
        mProgressDialog.show();
        mProgressDialog.setContentView(R.layout.diaolog_loading);
        TextView tvLoadingMsg = (TextView) mProgressDialog.findViewById(R.id.loading_msg);
        tvLoadingMsg.setText(strPleaseWaitAwhile);
    }

    public static void hideLoading()
    {
        if (mProgressDialog != null && mProgressDialog.isShowing())
        {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }


    public static int convertDpToPixel(float dp){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    public static boolean isValidPassword(final String password) {

        Pattern pattern;
        Matcher matcher;
        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[A-Za-z])(?=.*[$&+,:;=\\\\?@#|/'<>.^*()%!-])(?=\\S+$).{6,}$";
        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);

        return matcher.matches();

    }


}
