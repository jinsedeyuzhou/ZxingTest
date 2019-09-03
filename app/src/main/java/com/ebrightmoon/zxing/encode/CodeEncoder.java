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

package com.ebrightmoon.zxing.encode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import android.graphics.Bitmap;
import android.widget.ImageView;

import java.util.EnumMap;
import java.util.Map;

/**
 * This class does the work of decoding the user's request and extracting all the data
 * to be encoded in a barcode.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
final class CodeEncoder {

    private static final String TAG = CodeEncoder.class.getSimpleName();

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;


    private CodeEncoder() {
        throw new UnsupportedOperationException("can't support the operater");
    }

    /**
     * 生成二维码
     *
     * @param contentsToEncode
     * @param
     * @return
     * @throws WriterException
     */
    public static Bitmap encodeAsBitmap(String contentsToEncode, int qrwidth, int qrheight) {
        try {


            if (contentsToEncode == null) {
                return null;
            }
            Map<EncodeHintType, Object> hints = null;
            String encoding = guessAppropriateEncoding(contentsToEncode);
            if (encoding != null) {
                hints = new EnumMap<>(EncodeHintType.class);
                hints.put(EncodeHintType.CHARACTER_SET, encoding);
            }
            BitMatrix result;
            try {
                result = new MultiFormatWriter().encode(contentsToEncode, BarcodeFormat.QR_CODE, qrwidth, qrheight, hints);
            } catch (IllegalArgumentException iae) {
                // Unsupported format
                return null;
            }
            int width = result.getWidth();
            int height = result.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return  null;
    }

    public static Bitmap creatQRCode(String content, int width, int height) {
        return encodeAsBitmap(content, width, height);
    }

    public static Bitmap creatQRCode(String content) {
        return encodeAsBitmap(content, 800, 800);
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

    // ============================== 生成条形码 =================================




    public static Bitmap createBarCode(CharSequence content, int BAR_WIDTH, int BAR_HEIGHT, int backgroundColor, int codeColor) {
        /**
         * 条形码的编码类型
         */
        BarcodeFormat barcodeFormat = BarcodeFormat.CODE_39;
        final int backColor = backgroundColor;
        final int barCodeColor = codeColor;

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result = null;
        try {
            result = writer.encode(content + "", barcodeFormat, BAR_WIDTH, BAR_HEIGHT, null);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        // All are 0, or black, by default
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? barCodeColor : backColor;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    //----------------------------------------------------------------------------------------------生成条形码开始

    /**
     * 生成条形码
     *
     * @param contents      需要生成的内容
     * @param desiredWidth  生成条形码的宽带
     * @param desiredHeight 生成条形码的高度
     * @return
     */
    public static Bitmap createBarCode(String contents, int desiredWidth, int desiredHeight) {
        return createBarCode(contents, desiredWidth, desiredHeight, 0xFF000000, 0xFFFFFFFF);
    }

    /**
     * 生成条形码
     * desiredWidth  生成条形码的宽带
     * desiredHeight 生成条形码的高度
     *
     * @param contents 需要生成的内容
     * @return 条形码的Bitmap
     */
    public static Bitmap createBarCode(String contents) {
        return createBarCode(contents, 1000, 300);
    }

    public static void createBarCode(String content, int codeWidth, int codeHeight, ImageView iv_code) {
        iv_code.setImageBitmap(createBarCode(content, codeWidth, codeHeight));
    }

    public static void createBarCode(String content, ImageView iv_code) {
        iv_code.setImageBitmap(createBarCode(content));
    }

}
