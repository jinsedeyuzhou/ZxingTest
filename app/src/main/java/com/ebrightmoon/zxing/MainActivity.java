package com.ebrightmoon.zxing;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.ebrightmoon.zxing.encode.CodeEncoder;

/**
 * Time: 2019-09-03
 * Author:wyy
 * Description:
 */
public class MainActivity extends AppCompatActivity {
    private EditText mEtQrCode;
    private ImageView mIvCreateQrCode;
    private ImageView mIvQrCode;
    private LinearLayout mActivityCodeTool;
    private LinearLayout mLlCode;
    private LinearLayout mLlQrRoot;
    private EditText mEtBarCode;
    private ImageView mIvCreateBarCode;
    private ImageView mIvBarCode;
    private LinearLayout mLlBarCode;
    private LinearLayout mLlBarRoot;
    private LinearLayout mLlScaner;
    private LinearLayout mLlQr;
    private LinearLayout mLlBar;
    private NestedScrollView nestedScrollView;
    private LinearLayout ll_content;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ll_content = findViewById(R.id.ll_content);
        StatusBarUtils.darkMode(this);
        StatusBarUtils.setPadding(this,ll_content);
        initView();
        initEvent();
    }

    private void initEvent() {

        mIvCreateQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = mEtQrCode.getText().toString();
                mLlCode.setVisibility(View.VISIBLE);
                //二维码生成方式一  推荐此方法
                mIvQrCode.setImageBitmap(CodeEncoder.creatQRCode(str));

                //二维码生成方式二 默认宽和高都为800 背景为白色 二维码为黑色
                // RxQRCode.createQRCode(str,mIvQrCode);

                mIvQrCode.setVisibility(View.VISIBLE);
                nestedScrollView.computeScroll();

            }
        });

        mIvCreateBarCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str1 = mEtBarCode.getText().toString();
                mLlBarCode.setVisibility(View.VISIBLE);
                //条形码生成方式一  推荐此方法
                mIvBarCode.setImageBitmap(CodeEncoder.createBarCode(str1));

                //条形码生成方式二  默认宽为1000 高为300 背景为白色 二维码为黑色
                //mIvBarCode.setImageBitmap(RxBarCode.createBarCode(str1, 1000, 300));

                mIvBarCode.setVisibility(View.VISIBLE);

            }
        });


        mLlScaner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, CaptureActivity.class));
            }
        });

        mLlQr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLlQrRoot.setVisibility(View.VISIBLE);
                mLlBarRoot.setVisibility(View.GONE);
            }
        });

        mLlBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLlBarRoot.setVisibility(View.VISIBLE);
                mLlQrRoot.setVisibility(View.GONE);
            }
        });

    }


    protected void initView() {
        mEtQrCode =  findViewById(R.id.et_qr_code);
        mIvCreateQrCode = findViewById(R.id.iv_create_qr_code);
        mIvQrCode =  findViewById(R.id.iv_qr_code);
        mLlCode =  findViewById(R.id.ll_code);
        mLlQrRoot =  findViewById(R.id.ll_qr_root);
        nestedScrollView = findViewById(R.id.nestedScrollView);
        mEtQrCode = findViewById(R.id.et_qr_code);
        mEtBarCode = findViewById(R.id.et_bar_code);
        mIvCreateBarCode = findViewById(R.id.iv_create_bar_code);
        mIvBarCode = findViewById(R.id.iv_bar_code);
        mLlBarCode = findViewById(R.id.ll_bar_code);
        mLlBarRoot = findViewById(R.id.ll_bar_root);
        mLlScaner = findViewById(R.id.ll_scaner);
        mLlQr = findViewById(R.id.ll_qr);
        mLlBar = findViewById(R.id.ll_bar);

    }

}
