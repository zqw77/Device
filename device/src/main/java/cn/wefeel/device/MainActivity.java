package cn.wefeel.device;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.xutils.view.annotation.ContentView;

import java.io.File;

import cn.wefeel.device.base.BaseActivity;
import cn.wefeel.device.magnet.MagnetImageView;
import cn.wefeel.library.softwareupgrader.Upgrader;

@ContentView(R.layout.activity_main)
public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
        MagnetImageView mvDevice = (MagnetImageView) this.findViewById(R.id.mvDevice);
        mvDevice.setOnClickIntent(new MagnetImageView.OnViewClickListener() {

            @Override
            public void onViewClick(MagnetImageView view) {
                Intent intent = new Intent(view.getContext(), SearchActivity.class);
                //intent.putExtra("url", imageListAdapter.getItem(position).toString());
                startActivity(intent);
            }
        });
        MagnetImageView mvRepair = (MagnetImageView) this.findViewById(R.id.mvRepair);
        mvRepair.setOnClickIntent(new MagnetImageView.OnViewClickListener() {

            @Override
            public void onViewClick(MagnetImageView view) {
                Intent intent = new Intent(view.getContext(), LogActivity.class);
                startActivity(intent);
            }
        });
        MagnetImageView mvUpdate = (MagnetImageView) this.findViewById(R.id.mvUpdate);
        mvUpdate.setOnClickIntent(new MagnetImageView.OnViewClickListener() {

            @Override
            public void onViewClick(MagnetImageView view) {
                //检查数据更新
                Updater.check(MainActivity.this, false);
            }
        });
        MagnetImageView mvUpgrade = (MagnetImageView) this.findViewById(R.id.mvUpgrade);
        mvUpgrade.setOnClickIntent(new MagnetImageView.OnViewClickListener() {

            @Override
            public void onViewClick(MagnetImageView view) {
                //检查软件升级
                Upgrader.check(MainActivity.this, Constants.UPGRADE_FILE, Upgrader.NO_KEEP_SILENT);
            }
        });
        //设置扫描按钮
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });

        //检查软件升级
        Upgrader.check(this, Constants.UPGRADE_FILE, Upgrader.KEEP_SILENT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_viewlog) {    //数据更新日志菜单
            //先检查是否有数据更新日志文件
            String fullName = this.getFilesDir() + "/" + Constants.UPDATELOG_FILE_NAME;
            if (new File(fullName).exists()) {
                //显示数据更新日志
                Intent intent = new Intent(this, ExplorerActivity.class);
                intent.putExtra("url", "file://" + fullName);
                this.startActivity(intent);
            } else {
                Toast.makeText(this, R.string.hint_noupdatelog, Toast.LENGTH_LONG).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null) {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null) {
                String result = scanResult.getContents();
//                Snackbar.make(view, result, Snackbar.LENGTH_LONG).setAction("Action", null).show();
//                Log.d(Constants.TAG, result);
                Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                //显示设备详细信息
                Intent intent = new Intent(this, DetailActivity.class);
                intent.putExtra("code", result);
                startActivity(intent);
            }
        }
    }

    /**
     * 调用扫描模块扫描二维码或条形码，扫描完成会自动调onActivityResult
     */
    private void startScan() {
        // 扫描操作
        IntentIntegrator integrator = new IntentIntegrator(this);////IntentIntegrator扫描二维码时默认横屏
        integrator.setPrompt(this.getString(R.string.hint_scan));
        integrator.setOrientationLocked(false);
        integrator.setCaptureActivity(ScanActivity.class);//为了竖屏需要加这个ScanActivity，否则可直接CaptureActivity
        integrator.initiateScan();
    }
}
