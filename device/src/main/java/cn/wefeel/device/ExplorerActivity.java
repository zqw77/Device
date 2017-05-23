package cn.wefeel.device;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import cn.wefeel.device.base.BaseActivity;

/**
 * 用浏览器显示一个url
 */
@ContentView(R.layout.activity_explorer)
public class ExplorerActivity extends BaseActivity {

    @ViewInject(R.id.wvExplorer)
    WebView wvExplorer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wvExplorer.getSettings().setDefaultTextEncodingName("utf-8");
        Intent intent = getIntent();
        wvExplorer.loadUrl(intent.getStringExtra("url"));
    }
}
