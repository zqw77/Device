package cn.wefeel.device;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.HashMap;
import java.util.List;

import cn.wefeel.device.base.BaseActivity;
import cn.wefeel.device.data.MyData;
import cn.wefeel.device.entity.Device;

/**
 * 显示设备详细信息及其维修情况
 */
@ContentView(R.layout.activity_detail)
public class DetailActivity extends BaseActivity {
    private String mCode;
    private List<HashMap<String, Object>> mHashMapList;

    @ViewInject(R.id.tvDeviceInfo)
    private TextView tvDeviceInfo;
    @ViewInject(R.id.lvLog)
    private ListView lvLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mCode = intent.getStringExtra("code");

        MyData db = new MyData();
        Device device = db.getDevice(mCode);
        if (device != null) {
            //显示设备信息
            String info = getString(R.string.message_device,
                    device.code,
                    device.name,
                    device.orgname,
                    device.model,
                    device.producer,
                    device.type,
                    getResources().getStringArray(R.array.positions)[device.posflag],
                    device.station,
                    device.online,
                    getResources().getStringArray(R.array.states)[device.state],
                    device.unit,
                    device.life);

//            info += "<b>设备编码：</b>" + device.code;
//            info += "<br><b>设备名称：</b>" + device.name;
//            info += "\n归属部门：" + device.orgname;
//            info += "\n设备型号：" + device.model;
//            info += "\n生产厂家：" + device.producer;
//            info += "\n设备类型：" + device.type;
//            info += "\n所在位置：" + getResources().getStringArray(R.array.positions)[device.posflag];//posNames[device.posflag]+;
//            info += "\n所在车站：" + device.station;
//            info += "\n上道日期：" + device.online;
//            info += "\n设备状态：" + getResources().getStringArray(R.array.states)[device.state];
//            info += "\n计量单位：" + device.unit;
//            info += "\n使用寿命：" + device.life;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // for 24 api and more
                tvDeviceInfo.setText(Html.fromHtml(info, Html.FROM_HTML_MODE_LEGACY));
            } else {
                // or for older api
                tvDeviceInfo.setText(Html.fromHtml(info));
            }


            //显示维修信息
            TextView tvHeader = new TextView(this);
            tvHeader.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvHeader.setTextColor(Color.BLUE);
            tvHeader.setText(R.string.hint_log);
            lvLog.addHeaderView(tvHeader);//加标题
            lvLog.setEmptyView(this.findViewById(R.id.tvNoLog));//无维修记录时提示

            mHashMapList = (new MyData()).getLog(device.uid);//根据uid取维修记录
            SimpleAdapter adapter = new SimpleAdapter(this, mHashMapList, // 数据源
                    R.layout.item_log, // xml实现
                    new String[]{"code", "orgname", "repairdate", "repairperson", "place", "content"}, // 对应map的Key
                    new int[]{R.id.tvCode, R.id.tvOrgname, R.id.tvRepairDate, R.id.tvRepairPerson, R.id.tvPlace, R.id.tvContent}); // 对应R的Id
            lvLog.setAdapter(adapter);

        } else {
            tvDeviceInfo.setText(getString(R.string.hint_nodevice, mCode));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_share, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {  //分享功能
            Intent share = new Intent(android.content.Intent.ACTION_SEND);
            share.setType("text/plain");
            //设备信息
            String deviceMessage = tvDeviceInfo.getText().toString();
            //维修信息
            String repairMessage = "";
            if (mHashMapList != null && mHashMapList.size() > 0) {
                repairMessage = getString(R.string.subject_share_log);
                for (HashMap hashMap : mHashMapList) {
                    repairMessage += "\n" + hashMap.get("repairperson") + ":" + hashMap.get("content") + "(" + hashMap.get("repairdate") + ")";
                }
            }
            share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.subject_share_device));
            share.putExtra(Intent.EXTRA_TEXT, deviceMessage + repairMessage);
            startActivity(Intent.createChooser(share, getString(R.string.title_share)));
        }
        return super.onOptionsItemSelected(item);
    }
}
