package com.gu.android.mediacodec.mvp.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.basemodule.log.LogUtil;
import com.gu.android.mediacodec.R;

public class StatusView extends LinearLayout {

  private int mTextSize;
  private int bgColor;
  float[] outs = {90, 90, 90, 90, 90, 90, 90, 90};

  public StatusView(Context context) {
    this(context, null);
  }

  public StatusView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StatusView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {
    setOrientation(HORIZONTAL);
    setGravity(Gravity.CENTER);
    mTextSize = 12;
    bgColor = 0xff888888;
    String text = "";
    if (attrs != null) {
      TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.StatusView);
      if (array != null) {
        bgColor = array.getColor(R.styleable.StatusView_bg_color, 0xff888888);
        mTextSize = array.getDimensionPixelSize(R.styleable.StatusView_textSize, 12);
        text = String.valueOf(array.getText(R.styleable.StatusView_text));
        array.recycle();
      }
    }
    addViews(context, text, mTextSize, Color.RED, bgColor);
  }

  private void addViews(
      Context context, String text, int textSize, int dotColor, int backgroundColor) {
    TextView textView = createTextView(context, text, textSize);
    int textViewHeight = textView.getMeasuredHeight();
    int padding = textViewHeight / 3;
    ImageView dot = createDotImageView(context, textViewHeight, dotColor);
    LogUtil.log("dot", "dot width=" + dot.getMeasuredWidth());
    addView(textView);
    MarginLayoutParams params = (MarginLayoutParams) dot.getLayoutParams();
    params.setMargins(padding, 0, 0, 0);
    addView(dot, params);
    ShapeDrawable backgroundDrawable = createBackgroundDrawable(backgroundColor, padding);
    setBackground(backgroundDrawable);
  }

  private TextView createTextView(Context context, String text, int textSize) {
    TextView textView = new TextView(context);
    textView.setText(text);
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    textView.setTextColor(Color.WHITE);
    int spec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    textView.measure(spec, spec);
    return textView;
  }

  private ImageView createDotImageView(Context context, int size, int color) {
    ImageView imageView = new ImageView(context);
    imageView.setLayoutParams(new LayoutParams(size, size));
    DotDrawable dotDrawable = new DotDrawable(size, color);
    imageView.setImageDrawable(dotDrawable);
    return imageView;
  }

  private ShapeDrawable createBackgroundDrawable(int backgroundColor, int padding) {
    RoundRectShape shape = new RoundRectShape(outs, null, null);
    ShapeDrawable drawable = new ShapeDrawable(shape);
    drawable.getPaint().setColor(backgroundColor);
    drawable.setPadding(padding, padding, padding, padding);
    return drawable;
  }

  public void update(String text, int dotColor) {
    if (getChildCount() == 2) {
      removeAllViews();
      addViews(getContext(), text, mTextSize, dotColor, bgColor);
    }
  }
}
