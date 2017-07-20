package cn.wefeel.device;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

/**
 * IntentIntegrator扫描二维码时默认横屏，为了竖屏和闪光灯需要加这个ScanActivity
 */
//@ContentView(R.layout.activity_scan)无法使用，大约是因为有implements存在的原因
public class ScanActivity extends AppCompatActivity implements DecoratedBarcodeView.TorchListener{ // 实现相关接口
    // 添加一个按钮用来控制闪光灯，同时添加两个按钮表示其他功能，先用Toast表示

//    @ViewInject(R.id.btnSwitchLight)
    Button SwichLightButton;
//    @ViewInject(R.id.dbv_custom)
    DecoratedBarcodeView mDBV;

    private CaptureManager captureManager;
    private boolean isLightOn = false;

    @Override
    protected void onPause() {
        super.onPause();
        captureManager.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        captureManager.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        captureManager.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        captureManager.onSaveInstanceState(outState);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mDBV.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        SwichLightButton=(Button)this.findViewById(R.id.btnSwitchLight);
        mDBV=(DecoratedBarcodeView)this.findViewById(R.id.dbv_custom);

        mDBV.setTorchListener(this);

        // 如果没有闪光灯功能，就去掉相关按钮
        if(!hasFlash()) {
            SwichLightButton.setVisibility(View.GONE);
        }else{
            SwichLightButton.setText("开灯");
            // 点击切换闪光灯
            SwichLightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isLightOn){
                        mDBV.setTorchOff();
                    }else{
                        mDBV.setTorchOn();
                    }
                }
            });
        }

        //重要代码，初始化捕获
        captureManager = new CaptureManager(this,mDBV);
        captureManager.initializeFromIntent(getIntent(),savedInstanceState);
        captureManager.decode();

    }

    // torch 手电筒
    @Override
    public void onTorchOn() {
        isLightOn = true;
        SwichLightButton.setText(R.string.hint_turnoff);
    }

    @Override
    public void onTorchOff() {
        isLightOn = false;
        SwichLightButton.setText(R.string.hint_turnon);
    }

    // 判断是否有闪光灯功能
    private boolean hasFlash() {
        return getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

}