package cn.wefeel.device;

import android.os.Bundle;

import com.journeyapps.barcodescanner.CaptureActivity;

/**
 * IntentIntegrator扫描二维码时默认横屏，为了竖屏需要加这个ScanActivity
 */
public class ScanActivity extends CaptureActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
