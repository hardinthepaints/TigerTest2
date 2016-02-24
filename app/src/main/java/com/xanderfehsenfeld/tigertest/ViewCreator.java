package com.xanderfehsenfeld.tigertest;

import android.app.Activity;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;

/**
 * Created by Xander on 2/19/16.
 */

/* TODO remove when done developing */
public class ViewCreator {

    /** getRoundButton
     *      get a round button based on the parameters in the context and with the given info
     *      requires sdk > JELLY BEAN
     * @param c
     * @param radius
     * @return
     */
//    public static RelativeLayout getRoundButton(Activity c, int radius ){
//
//        RelativeLayout container = new RelativeLayout(c);
//
//        /* set layout params to be identical to that of the default */
//        container.setLayoutParams(c.findViewById(R.id.topContainerA).getLayoutParams());
//        container.setId(R.id.topContainerA);
//        container.setGravity(Gravity.CENTER);
//
//        /* create button */
//        Button button = new Button(c);
//        button.setText(((Button)c.findViewById(R.id.btnStart)).getText());
//        button.setId(R.id.btnStart);
//
//        button.setBackground(c.getResources().getDrawable(R.drawable.circular_button_notpressed));
//        button.setTextColor(c.getResources().getColor(R.color.PingTextColor));
//
//        button.setWidth(radius * 2);
//        button.setHeight(radius * 2);
//
//        container.addView(button);
//
//
//        /* adjust text size */
//        Paint paint = button.getPaint();
//        button.setMaxLines(1);
//        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//
//        String text = "" + button.getText();
//
//        Rect r = new Rect();
//        paint.getTextBounds(text, 0, text.length(), r);
//        float ratio = (float)( radius * 1.5 ) / r.width();
//        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, button.getTextSize() * ratio);
//
//
//        return container;
//    }

    public static Button getRoundButton(Activity c, int radius ){

        /* create button */
        Button button = new Button(c);
        button.setText(((Button)c.findViewById(R.id.btnStart)).getText());
        button.setId(R.id.btnStart);

        button.setBackground(c.getResources().getDrawable(R.drawable.circular_button_notpressed));
        button.setTextColor(c.getResources().getColor(R.color.PingTextColor));

        button.setWidth(radius * 2);
        button.setHeight(radius * 2);

        /* adjust text size */
        Paint paint = button.getPaint();
        button.setMaxLines(1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }

        String text = "" + button.getText();

        Rect r = new Rect();
        paint.getTextBounds(text, 0, text.length(), r);
        float ratio = (float)( radius * 1.5 ) / r.width();
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, button.getTextSize() * ratio);


        return button;
    }


}
