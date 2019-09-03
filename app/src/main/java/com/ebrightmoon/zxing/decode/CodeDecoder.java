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

package com.ebrightmoon.zxing.decode;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import java.util.EnumMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

/**
 * This class does the work of decoding the user's request and extracting all the data
 * to be encoded in a barcode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class CodeDecoder {

    private static final String TAG = CodeDecoder.class.getSimpleName();



    private CodeDecoder() {
        throw new UnsupportedOperationException("can't support the operater");
    }

    /**
     * 解析图片中的 二维码 或者 条形码
     *
     * @param photo 待解析的图片
     * @return Result 解析结果，解析识别时返回NULL
     */
    public static Result decodeFromPhoto(Bitmap photo) {
        Result rawResult = null;
        if (photo != null) {
            Bitmap smallBitmap = zoomBitmap(photo, photo.getWidth() / 2, photo.getHeight() / 2);// 为防止原始图片过大导致内存溢出，这里先缩小原图显示，然后释放原始Bitmap占用的内存
            photo.recycle(); // 释放原始图片占用的内存，防止out of memory异常发生

            MultiFormatReader multiFormatReader = new MultiFormatReader();

            // 解码的参数
            Hashtable<DecodeHintType, Object> hints = new Hashtable<>(2);
            // 可以解析的编码类型
            Vector<BarcodeFormat> decodeFormats = new Vector<>();
            if (decodeFormats.isEmpty()) {
                decodeFormats = new Vector<>();

                Vector<BarcodeFormat> PRODUCT_FORMATS = new Vector<>(5);
                PRODUCT_FORMATS.add(BarcodeFormat.UPC_A);
                PRODUCT_FORMATS.add(BarcodeFormat.UPC_E);
                PRODUCT_FORMATS.add(BarcodeFormat.EAN_13);
                PRODUCT_FORMATS.add(BarcodeFormat.EAN_8);
                // PRODUCT_FORMATS.add(BarcodeFormat.RSS14);
                Vector<BarcodeFormat> ONE_D_FORMATS = new Vector<>(PRODUCT_FORMATS.size() + 4);
                ONE_D_FORMATS.addAll(PRODUCT_FORMATS);
                ONE_D_FORMATS.add(BarcodeFormat.CODE_39);
                ONE_D_FORMATS.add(BarcodeFormat.CODE_93);
                ONE_D_FORMATS.add(BarcodeFormat.CODE_128);
                ONE_D_FORMATS.add(BarcodeFormat.ITF);
                Vector<BarcodeFormat> QR_CODE_FORMATS = new Vector<>(1);
                QR_CODE_FORMATS.add(BarcodeFormat.QR_CODE);
                Vector<BarcodeFormat> DATA_MATRIX_FORMATS = new Vector<>(1);
                DATA_MATRIX_FORMATS.add(BarcodeFormat.DATA_MATRIX);

                // 这里设置可扫描的类型，我这里选择了都支持
                decodeFormats.addAll(ONE_D_FORMATS);
                decodeFormats.addAll(QR_CODE_FORMATS);
                decodeFormats.addAll(DATA_MATRIX_FORMATS);
            }
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
            // 设置继续的字符编码格式为UTF8
            // hints.put(DecodeHintType.CHARACTER_SET, "UTF8");
            // 设置解析配置参数
            multiFormatReader.setHints(hints);

            // 开始对图像资源解码
            try {
                rawResult = multiFormatReader.decodeWithState(new BinaryBitmap(new HybridBinarizer(new BitmapLuminanceSource(smallBitmap))));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rawResult;
    }


    /**
     * Resize the bitmap
     *
     * @param bitmap 图片引用
     * @param width 宽度
     * @param height 高度
     * @return 缩放之后的图片引用
     */
    public static Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) width / w);
        float scaleHeight = ((float) height / h);
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }




}
