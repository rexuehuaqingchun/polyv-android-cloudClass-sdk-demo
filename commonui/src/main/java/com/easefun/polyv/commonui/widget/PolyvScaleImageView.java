package com.easefun.polyv.commonui.widget;


import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;

import com.bumptech.glide.load.resource.gif.GifDrawable;

public class PolyvScaleImageView extends AppCompatImageView {
    private ScaleGestureDetector scaleGestureDetector;//缩放
    private GestureDetector gestureDetector;//双击
    private Matrix scaleMatrix;//当前的平移、缩放

    private boolean isDrawablePrepared;
    private Runnable runnable;
    private float scaleX = 1;
    private float minScaleX, midScaleX, maxScaleX;

    private int lastPointerCount;
    private float lastX;
    private float lastY;
    private int touchSlop;//系统默认缩放
    private boolean isCanDrag, isInterceptDrag, isDraged;

    private OnClickListener onClickListener;

    public PolyvScaleImageView(Context context) {
        this(context, null);
    }

    public PolyvScaleImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PolyvScaleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(true);
        setScaleType(ScaleType.MATRIX);
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        scaleMatrix = new Matrix();
        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {//双击不触发
                if (onClickListener != null) {
                    onClickListener.onClick(PolyvScaleImageView.this);
                }
                return super.onSingleTapConfirmed(e);
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (canScale()) {
                    if (getIvScaleX() < midScaleX) {
                        scaleMatrix.postScale(midScaleX / getIvScaleX(), midScaleX / getIvScaleX(), getWidth() / 2.0f, getHeight() / 2.0f);
                        setImageMatrix(scaleMatrix);
                    } else {
                        reset();
                    }
                }
                return canScale();
            }
        });
        scaleGestureDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (canScale()) {
                    float scale = getIvScaleX();
                    float scaleFactor = detector.getScaleFactor();
                    //缩放范围的控制, 放大时需要小于最大，缩小时需要大于最小
                    if ((scale < maxScaleX && scaleFactor > 1.0f) || (scale > minScaleX && scaleFactor < 1.0f)) {
                        if (scale * scaleFactor < minScaleX) {
                            scaleFactor = minScaleX / scale;
                        }
                        if (scale * scaleFactor > maxScaleX) {
                            scaleFactor = maxScaleX / scale;
                        }

                        scaleMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                        checkBorderForScale();
                        setImageMatrix(scaleMatrix);
                    }
                }
                return canScale();
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return canScale();
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
            }
        });
    }


    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        this.onClickListener = l;
    }

    private void checkBorderForScale() {
        RectF rect = getMatrixRectF();

        float deltaX = 0;
        float deltaY = 0;
        int width = getWidth();
        int height = getHeight();

        if (rect.width() >= width) {
            if (rect.left > 0) { //和屏幕左边有空隙
                deltaX = -rect.left; //左边移动
            }
            // 和屏幕as
            if (rect.right < width) {
                deltaX = width - rect.right;
            }
        }

        if (rect.height() >= height) {
            if (rect.top > 0) {
                deltaY = -rect.top;
            }
            if (rect.bottom < height) {
                deltaY = height - rect.bottom;
            }
        }

        //如果宽度或者高度小于控件的宽和高 居中处理
        if (rect.width() < width) {
            deltaX = getWidth() / 2 - rect.right + rect.width() / 2;
        }

        if (rect.height() < height) {
            deltaY = getHeight() / 2 - rect.bottom + rect.height() / 2;
        }
        scaleMatrix.postTranslate(deltaX, deltaY);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(runnable);
    }

    private boolean canScale() {
        return isDrawablePrepared && getVisibility() == View.VISIBLE;
    }

    public void drawablePrepared(final Drawable drawable) {
        if (drawable == null)
            return;
        isDrawablePrepared = true;
        setVisibility(View.INVISIBLE);
        setImageDrawable(drawable);
        post(runnable = new Runnable() {
            @Override
            public void run() {
                int width = getWidth();
                int height = getHeight();
                int drawableWidth = drawable.getIntrinsicWidth();
                int drawableHeight = drawable.getIntrinsicHeight();
                float wdScale = width * 1.0f / drawableWidth;
                float hdScale = height * 1.0f / drawableHeight;
                if (width > drawableWidth && height > drawableHeight) {
                    scaleX = Math.min(wdScale, hdScale);
                } else if (width > drawableWidth && height < drawableHeight) {
                    scaleX = hdScale;//不能用竖直滚动布局嵌套，不然imageView的height会随着drawableHeight而大于可见区域的height
                } else if (width < drawableWidth && height > drawableHeight) {
                    scaleX = wdScale;
                } else if (width < drawableWidth && height < drawableHeight) {
                    scaleX = Math.min(wdScale, hdScale);
                }
                minScaleX = scaleX * 0.5f;
                midScaleX = scaleX * 2;
                maxScaleX = scaleX * 5;
                reset();
                setVisibility(View.VISIBLE);
                if (drawable instanceof GifDrawable) {
                    ((GifDrawable) drawable).start();//显示gif
                }
            }
        });
    }

    private void reset() {
        if (!isDrawablePrepared)
            return;
        Drawable drawable = getDrawable();
        float drawableWidth = drawable.getIntrinsicWidth();
        float drawableHeight = drawable.getIntrinsicHeight();
        float dx = (getWidth() - drawableWidth) / 2.0f;
        float dy = (getHeight() - drawableHeight) / 2.0f;
        scaleMatrix.reset();
        scaleMatrix.postTranslate(dx, dy);//图片移至中间
        scaleMatrix.postScale(scaleX, scaleX, getWidth() / 2.0f, getHeight() / 2.0f);
        setImageMatrix(scaleMatrix);
    }

    private float getIvScaleX() {
        float[] values = new float[9];
        scaleMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    //获取转换后图片的宽高
    private RectF getMatrixRectF() {
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            //用matrix进行map一下
            scaleMatrix.mapRect(rectF);
        }

        return rectF;
    }

    //判断当前移动距离是否大于系统默认最小移动距离
    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx * dx + dy * dy) > touchSlop;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!canScale())
            return super.onTouchEvent(event);
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }
        int pointCount = event.getPointerCount();
        if (pointCount == 2) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        scaleGestureDetector.onTouchEvent(event);
        //计算多指触控中心点
        float currentX = 0;
        float currentY = 0;
        for (int i = 0; i < pointCount; i++) {
            currentX += event.getX(i);
            currentY += event.getY(i);
        }
        currentX /= pointCount;
        currentY /= pointCount;

        if (lastPointerCount != pointCount) {//pointCount=2：1-2-2-2//=3：1-2-3-3，其余pointCount!=2
            isCanDrag = false;
            lastX = currentX;
            lastY = currentY;
        }
        lastPointerCount = pointCount;

        RectF rectF = getMatrixRectF();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                //请求不被拦截
                if (rectF.width() > getWidth() || rectF.height() > getHeight()) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = currentX - lastX;
                float dy = currentY - lastY;
                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }
                if (isCanDrag && !isInterceptDrag) {
                    boolean isCheckLeftAndRight, isCheckTopAndBottom;
                    isCheckLeftAndRight = isCheckTopAndBottom = true;
                    float tempX = dx;
                    float tempY = dy;
                    if (rectF.width() < getWidth()) {
                        dx = 0;
                        isCheckLeftAndRight = false;
                    }
                    if (rectF.height() < getHeight()) {
                        dy = 0;
                        isCheckTopAndBottom = false;
                    }
                    //边界
                    if (isCheckTopAndBottom) {
                        if (dy > 0 && dy + rectF.top > 0)//下拉
                            dy = -rectF.top;
                        if (dy < 0 && rectF.bottom + dy < getHeight())//上拉
                            dy = getHeight() - rectF.bottom;
                    }
                    if (isCheckLeftAndRight) {
                        if (dx > 0 && dx + rectF.left > 0)//右拉
                            dx = -rectF.left;
                        if (dx < 0 && rectF.right + dx < getWidth())//左拉
                            dx = getWidth() - rectF.right;
                    }
                    if (dx == 0 && Math.abs(tempX) > Math.abs(tempY) && !isDraged) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                        isInterceptDrag = true;
                    } else {
                        isDraged = true;
                        scaleMatrix.postTranslate(dx, dy);
                        setImageMatrix(scaleMatrix);
                    }
                }
                lastX = currentX;
                lastY = currentY;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDraged = false;
                isInterceptDrag = false;
                isCanDrag = false;
                lastPointerCount = 0;
                lastX = 0;
                lastY = 0;
                break;
        }
        return super.onTouchEvent(event);
    }
}
