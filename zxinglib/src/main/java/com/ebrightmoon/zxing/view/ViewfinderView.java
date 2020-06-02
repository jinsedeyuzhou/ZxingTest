/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ebrightmoon.zxing.view;

import com.ebrightmoon.zxing.ScannerOptions;
import com.ebrightmoon.zxing.common.Scanner;
import com.ebrightmoon.zxing.util.DisplayUtil;
import com.ebrightmoon.zxing.R;
import com.google.zxing.ResultPoint;
import com.ebrightmoon.zxing.camera.CameraManager;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 80L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 6;

    private CameraManager cameraManager;
    private final Paint paint;
    private Bitmap resultBitmap;
    private final int maskColor;
    private final int resultColor;
    private final int laserColor;
    private final int resultPointColor;
    private int scannerAlpha;
    private List<ResultPoint> possibleResultPoints;
    private List<ResultPoint> lastPossibleResultPoints;
    private int tipTextSize;//提示文字大小
    private int tipTextMargin;//提示文字与扫描框距离

    // 设置宽高
    private int width;
    private int height;
    public static int FRAME_WIDTH = -1;
    public static int FRAME_HEIGHT = -1;
    public static int FRAME_MARGINTOP = -1;
    // 扫描框边角颜色
    private int innercornercolor;
    // 扫描框边角长度
    private int innercornerlength;
    // 扫描框边角宽度
    private int innercornerwidth;
    // 扫描线默认高度
    private int laserLineHeight;//扫描线默认高度
    // 扫描线移动的y
    private int scanLineTop;
    private int animationDelay = 0;
    // 扫描线移动速度
    private int SCAN_VELOCITY;
    // 扫描线
    private Bitmap scanLight;
    // 是否展示小圆点
    private boolean isCircle;
    private static final int OPAQUE = 0xFF;
    private ScannerOptions scannerOptions;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
//    maskColor = resources.getColor(R.color.viewfinder_mask);
//    resultColor = resources.getColor(R.color.result_view);
//    laserColor = resources.getColor(R.color.viewfinder_laser);
//    resultPointColor = resources.getColor(R.color.possible_result_points);

        TypedArray attributes = getContext().obtainStyledAttributes(attrs, R.styleable.ViewfinderView);

        this.maskColor = attributes.getColor(R.styleable.ViewfinderView_zxing_viewfinder_mask,
                resources.getColor(R.color.zxing_viewfinder_mask));
        this.resultColor = attributes.getColor(R.styleable.ViewfinderView_zxing_result_view,
                resources.getColor(R.color.zxing_result_view));
        this.laserColor = attributes.getColor(R.styleable.ViewfinderView_zxing_viewfinder_laser,
                resources.getColor(R.color.zxing_viewfinder_laser));
        this.resultPointColor = attributes.getColor(R.styleable.ViewfinderView_zxing_possible_result_points,
                resources.getColor(R.color.zxing_possible_result_points));


        //-----------------------------  边框 -----------------------------------

        // 扫描框距离顶部
        float innerMarginTop = attributes.getDimension(R.styleable.ViewfinderView_inner_margintop, -1);
        if (innerMarginTop != -1) {
            FRAME_MARGINTOP = (int) innerMarginTop;
        }

        // 扫描框的宽度
        FRAME_WIDTH = (int) attributes.getDimension(R.styleable.ViewfinderView_inner_width, DisplayUtil.screenWidthPx / 2);

        // 扫描框的高度
        FRAME_HEIGHT = (int) attributes.getDimension(R.styleable.ViewfinderView_inner_height, DisplayUtil.screenWidthPx / 2);

        // 扫描框边角颜色
        innercornercolor = attributes.getColor(R.styleable.ViewfinderView_inner_corner_color, Color.parseColor("#45DDDD"));
        // 扫描框边角长度
        innercornerlength = (int) attributes.getDimension(R.styleable.ViewfinderView_inner_corner_length, 65);
        // 扫描框边角宽度
        innercornerwidth = (int) attributes.getDimension(R.styleable.ViewfinderView_inner_corner_width, 15);

        // 扫描控件
        scanLight = BitmapFactory.decodeResource(getResources(), attributes.getResourceId(R.styleable.ViewfinderView_inner_scan_bitmap, R.drawable.zfb_grid_scan_line));
        // 扫描速度
        SCAN_VELOCITY = attributes.getInt(R.styleable.ViewfinderView_inner_scan_speed, 5);

        isCircle = attributes.getBoolean(R.styleable.ViewfinderView_inner_scan_iscircle, true);

        attributes.recycle();
        scannerAlpha = 0;
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = null;
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    public void setScannerOptions(ScannerOptions scannerOptions) {
        this.scannerOptions = scannerOptions;
        laserLineHeight = dp2px(scannerOptions.getLaserLineHeight());

        innercornerwidth = dp2px(scannerOptions.getFrameCornerWidth());
        innercornerlength = dp2px(scannerOptions.getFrameCornerLength());
        tipTextSize = Scanner.sp2px(getContext(), scannerOptions.getTipTextSize());
        tipTextMargin = dp2px(scannerOptions.getTipTextToFrameMargin());
    }

    private int dp2px(int dp) {
        return Scanner.dp2px(getContext(), dp);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        //取扫描框
        Rect frame = cameraManager.getFramingRect();
        //取屏幕预览
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }

        //全屏不绘制扫描框以外4个区域
        if (!scannerOptions.isScanFullScreen()) {
            drawMask(canvas, frame);
        }

        // 如果有二维码结果的Bitmap，在扫取景框内绘制不透明的result Bitmap
        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(OPAQUE);
            canvas.drawBitmap(resultBitmap, frame.left, frame.top, paint);
        } else {
            drawFrameBounds(canvas, frame);
            drawScanLight(canvas, frame);
            drawText(canvas, frame);// 画扫描框下面的字
            List<ResultPoint> currentPossible = possibleResultPoints;
            List<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new ArrayList<>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(OPAQUE);
                paint.setColor(resultPointColor);

                if (isCircle) {
                    for (ResultPoint point : currentPossible) {
                        canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
                    }
                }
            }
            if (currentLast != null) {
                paint.setAlpha(OPAQUE / 2);
                paint.setColor(resultPointColor);

                if (isCircle) {
                    for (ResultPoint point : currentLast) {
                        canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
                    }
                }
            }

            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);
        }
    }

    /**
     * 绘制提示文字
     *
     * @param canvas
     * @param frame
     */
    private void drawText(Canvas canvas, Rect frame) {
        TextPaint textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(scannerOptions.getTipTextColor());
        textPaint.setTextSize(tipTextSize);

        float x = frame.left;//文字开始位置
        //根据 drawTextGravityBottom 文字在扫描框上方还是上文，默认下方
        float y = !scannerOptions.isTipTextToFrameTop() ? frame.bottom + tipTextMargin
                : frame.top - tipTextMargin;

        StaticLayout staticLayout = new StaticLayout(scannerOptions.getTipText(), textPaint, frame.width()
                , Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
        canvas.save();
        canvas.translate(x, y);
        staticLayout.draw(canvas);
        canvas.restore();
    }

    /**
     * 绘制取景框边框
     *
     * @param canvas
     * @param frame
     */
    private void drawFrameBounds(Canvas canvas, Rect frame) {

        /*paint.setColor(Color.WHITE);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        canvas.drawRect(frame, paint);*/
        if (!scannerOptions.isFrameHide()) {
            paint.setColor(scannerOptions.getFrameStrokeColor());//扫描边框色
            paint.setStrokeWidth(scannerOptions.getFrameStrokeWidth());
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(frame, paint);
        }
        if (!scannerOptions.isFrameCornerHide()) {
            paint.setColor(innercornercolor);
            paint.setStyle(Paint.Style.FILL);

            int corWidth = innercornerwidth;
            int corLength = innercornerlength;
            if (!scannerOptions.isFrameCornerInside()) {
                // 左上角
                canvas.drawRect(frame.left, frame.top, frame.left + corWidth, frame.top
                        + corLength, paint);
                canvas.drawRect(frame.left, frame.top, frame.left
                        + corLength, frame.top + corWidth, paint);
                // 右上角
                canvas.drawRect(frame.right - corWidth, frame.top, frame.right,
                        frame.top + corLength, paint);
                canvas.drawRect(frame.right - corLength, frame.top,
                        frame.right, frame.top + corWidth, paint);
                // 左下角
                canvas.drawRect(frame.left, frame.bottom - corLength,
                        frame.left + corWidth, frame.bottom, paint);
                canvas.drawRect(frame.left, frame.bottom - corWidth, frame.left
                        + corLength, frame.bottom, paint);
                // 右下角
                canvas.drawRect(frame.right - corWidth, frame.bottom - corLength,
                        frame.right, frame.bottom, paint);
                canvas.drawRect(frame.right - corLength, frame.bottom - corWidth,
                        frame.right, frame.bottom, paint);
            } else {
                // 左上角
                canvas.drawRect(frame.left - corWidth, frame.top, frame.left, frame.top + corLength, paint);
                canvas.drawRect(frame.left - corWidth, frame.top - corWidth, frame.left + corLength, frame.top, paint);
                // 右上角
                canvas.drawRect(frame.right, frame.top, frame.right + corWidth, frame.top + corLength, paint);
                canvas.drawRect(frame.right - corLength, frame.top - corWidth, frame.right + corWidth, frame.top, paint);
                // 左下角
                canvas.drawRect(frame.left - corWidth, frame.bottom - corLength, frame.left, frame.bottom, paint);
                canvas.drawRect(frame.left - corWidth, frame.bottom, frame.left + corLength, frame.bottom + corWidth, paint);
                // 右下角
                canvas.drawRect(frame.right, frame.bottom - corLength, frame.right + corWidth, frame.bottom, paint);
                canvas.drawRect(frame.right - corLength, frame.bottom, frame.right + corWidth, frame.bottom + corWidth, paint);
            }

        }
    }

    /**
     * 画扫描框外区域
     *
     * @param canvas
     * @param frame
     */
    private void drawMask(Canvas canvas, Rect frame) {
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }

    /**
     * 绘制移动扫描线
     *
     * @param canvas
     * @param frame
     */
    private void drawScanLight(Canvas canvas, Rect frame) {

        //全屏移动扫描线
        if (scannerOptions.isLaserMoveFullScreen()) {
            moveLaserSpeedFullScreen(cameraManager.getScreenResolution());//计算全屏移动位置
            drawLaserLineFullScreen(canvas, cameraManager.getScreenResolution());//绘制全屏扫描线
        } else {
            drawLaserLine(canvas, frame);//绘制扫描框内扫描线
            moveLaserSpeed(frame);//计算扫描框内移动位置
        }
        if (scannerOptions.getViewfinderCallback() != null) {
            scannerOptions.getViewfinderCallback().onDraw(this, canvas, frame);
        }
        /**
         *
         if (scanLineTop == 0) {
         scanLineTop = frame.top;
         }

         if (scanLineTop >= frame.bottom - 30) {
         scanLineTop = frame.top;
         } else {
         scanLineTop += SCAN_VELOCITY;
         }
         Rect scanRect = new Rect(frame.left, scanLineTop, frame.right,
         scanLineTop + 30);
         canvas.drawBitmap(scanLight, null, scanRect, paint);
         *
         */
    }

    /**
     * 画扫描线
     *
     * @param canvas
     * @param frame
     */
    private void drawLaserLine(Canvas canvas, Rect frame) {
        if (scannerOptions.getLaserStyle() == ScannerOptions.LaserStyle.COLOR_LINE) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(scannerOptions.getLaserLineColor());// 设置扫描线颜色
            canvas.drawRect(frame.left, scanLineTop, frame.right
                    , scanLineTop + laserLineHeight, paint);
        } else {
            if (scanLight == null)//图片资源文件转为 Bitmap
                scanLight = BitmapFactory.decodeResource(getResources(), scannerOptions.getLaserLineResId());
            int height = scanLight.getHeight();//取原图高
            //网格图片
            if (scannerOptions.getLaserStyle() == ScannerOptions.LaserStyle.RES_GRID) {
                RectF dstRectF = new RectF(frame.left, frame.top, frame.right, scanLineTop);
                Rect srcRect = new Rect(0, (int) (height - dstRectF.height())
                        , scanLight.getWidth(), height);
                canvas.drawBitmap(scanLight, srcRect, dstRectF, paint);
            }
            //线条图片
            else {
                //如果没有设置线条高度，则用图片原始高度
                if (laserLineHeight == dp2px(ScannerOptions.DEFAULT_LASER_LINE_HEIGHT)) {
                    laserLineHeight = scanLight.getHeight() / 2;
                }
                Rect laserRect = new Rect(frame.left, scanLineTop, frame.right
                        , scanLineTop + laserLineHeight);
                canvas.drawBitmap(scanLight, null, laserRect, paint);
            }
        }
    }

    /**
     * 画全屏宽扫描线
     *
     * @param canvas
     * @param point
     */
    private void drawLaserLineFullScreen(Canvas canvas, Point point) {
        if (scannerOptions.getLaserStyle() == ScannerOptions.LaserStyle.COLOR_LINE) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(scannerOptions.getLaserLineColor());// 设置扫描线颜色
            canvas.drawRect(0, scanLineTop, point.x, scanLineTop + laserLineHeight, paint);
        } else {
            if (scanLight == null)//图片资源文件转为 Bitmap
                scanLight = BitmapFactory.decodeResource(getResources(), scannerOptions.getLaserLineResId());
            int height = scanLight.getHeight();//取原图高
            //网格图片
            if (scannerOptions.getLaserStyle() == ScannerOptions.LaserStyle.RES_GRID) {
                int dstRectFTop = 0;
                if (scanLineTop >= height) {
                    dstRectFTop = scanLineTop - height;
                }
                RectF dstRectF = new RectF(0, dstRectFTop, point.x, scanLineTop);
                Rect srcRect = new Rect(0, (int) (height - dstRectF.height()), scanLight.getWidth(), height);
                canvas.drawBitmap(scanLight, srcRect, dstRectF, paint);
            }
            //线条图片
            else {
                //如果没有设置线条高度，则用图片原始高度
                if (laserLineHeight == dp2px(ScannerOptions.DEFAULT_LASER_LINE_HEIGHT)) {
                    laserLineHeight = scanLight.getHeight() / 2;
                }
                Rect laserRect = new Rect(0, scanLineTop, point.x, scanLineTop + laserLineHeight);
                canvas.drawBitmap(scanLight, null, laserRect, paint);
            }
        }
    }

    private void moveLaserSpeedFullScreen(Point point) {
        //初始化扫描线起始点为顶部位置
        int laserMoveSpeed = scannerOptions.getLaserLineMoveSpeed();
        if (laserMoveSpeed==0)
        {
            laserMoveSpeed=SCAN_VELOCITY;
        }
        // 每次刷新界面，扫描线往下移动 LASER_VELOCITY
        scanLineTop += laserMoveSpeed;
        if (scanLineTop >= point.y) {
            scanLineTop = 0;
        }
        if (animationDelay == 0) {
            animationDelay = (int) ((1.0f * 1000 * laserMoveSpeed) / point.y);
        }
        postInvalidateDelayed(animationDelay);
    }

    private void moveLaserSpeed(Rect frame) {
        //初始化扫描线起始点为扫描框顶部位置
        if (scanLineTop == 0) {
            scanLineTop = frame.top;
        }
        int laserMoveSpeed = scannerOptions.getLaserLineMoveSpeed();
        if (laserMoveSpeed==0)
        {
            laserMoveSpeed=SCAN_VELOCITY;
        }
        // 每次刷新界面，扫描线往下移动 LASER_VELOCITY
        scanLineTop += laserMoveSpeed;
        if (scanLineTop >= frame.bottom) {
            scanLineTop = frame.top;
        }
        if (animationDelay == 0) {
            animationDelay = (int) ((1.0f * 1000 * laserMoveSpeed) / (frame.bottom - frame.top));
        }

        // 只刷新扫描框的内容，其他地方不刷新
        postInvalidateDelayed(animationDelay, frame.left - POINT_SIZE, frame.top - POINT_SIZE
                , frame.right + POINT_SIZE, frame.bottom + POINT_SIZE);

    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

   public void laserLineBitmapRecycle() {
        if (scanLight != null) {
            scanLight.recycle();
            scanLight = null;
        }
    }

}
