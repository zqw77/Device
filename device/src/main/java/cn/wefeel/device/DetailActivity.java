package cn.wefeel.device;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import java.util.ArrayList;
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
    private List<String> mDeviceInfo;
    private String mShareMessage;

    @ViewInject(R.id.lvDeviceInfo)
    private ListView lvDeviceInfo;
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
//            String info = getString(R.string.message_device,
//                    device.code,
//                    device.name,
//                    device.orgname,
//                    device.model,
//                    device.producer,
//                    device.type,
//                    getResources().getStringArray(R.array.positions)[device.posflag],
//                    device.station,
//                    device.online,
//                    getResources().getStringArray(R.array.states)[device.state],
//                    device.unit,
//                    device.life);
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                // for 24 api and more
//                tvDeviceInfo.setText(Html.fromHtml(info, Html.FROM_HTML_MODE_LEGACY));
//            } else {
//                // or for older api
//                tvDeviceInfo.setText(Html.fromHtml(info));
//            }
            //显示设备信息
            mDeviceInfo = new ArrayList<>();
            mDeviceInfo.add(getString(R.string.hint_code) + "：" + device.code);
            mDeviceInfo.add(getString(R.string.hint_name) + "：" + device.name);
            mDeviceInfo.add(getString(R.string.hint_orgname) + "：" + device.orgname);
            mDeviceInfo.add(getString(R.string.hint_model) + "：" + device.model);
            mDeviceInfo.add(getString(R.string.hint_producer) + "：" + device.producer);
            mDeviceInfo.add(getString(R.string.hint_type) + "：" + device.type);
            mDeviceInfo.add(getString(R.string.hint_posflag) + "：" + getResources().getStringArray(R.array.positions)[device.posflag]);
            mDeviceInfo.add(getString(R.string.hint_station) + "：" + device.station);
            mDeviceInfo.add(getString(R.string.hint_online) + "：" + device.online);
            mDeviceInfo.add(getString(R.string.hint_state) + "：" + getResources().getStringArray(R.array.states)[device.state]);
            mDeviceInfo.add(getString(R.string.hint_unit) + "：" + device.unit);
            mDeviceInfo.add(getString(R.string.hint_life) + "：" + device.life);

            TextView tvHeader1 = new TextView(this);
            tvHeader1.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvHeader1.setTextColor(Color.BLUE);
            tvHeader1.setText(R.string.subject_share_device);
            lvDeviceInfo.addHeaderView(tvHeader1);//加标题
            lvDeviceInfo.setAdapter(new MyArrayAdapter<String>(this, R.layout.item_deviceinfo, mDeviceInfo));
            //生成分享的内容
            mShareMessage = getString(R.string.subject_share_device);
            for (String message : mDeviceInfo)
                mShareMessage += "\n" + message;

            //显示维修信息
            TextView tvHeader = new TextView(this);
            tvHeader.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvHeader.setTextColor(Color.BLUE);
            tvHeader.setText(R.string.subject_share_log);
            lvLog.addHeaderView(tvHeader);//加标题
            lvLog.setEmptyView(this.findViewById(R.id.tvNoLog));//无维修记录时提示

            mHashMapList = (new MyData()).getLog(device.uid);//根据uid取维修记录
            SimpleAdapter adapter = new SimpleAdapter(this, mHashMapList, // 数据源
                    R.layout.item_log, // xml实现
                    new String[]{"code", "orgname", "repairdate", "repairperson", "place", "content"}, // 对应map的Key
                    new int[]{R.id.tvCode, R.id.tvOrgname, R.id.tvRepairDate, R.id.tvRepairPerson, R.id.tvPlace, R.id.tvContent}); // 对应R的Id
            lvLog.setAdapter(adapter);
            //生成分享的内容
            mShareMessage += "\n" + getString(R.string.subject_share_log);
            for (HashMap hashMap : mHashMapList) {
                mShareMessage += "\n" + hashMap.get("repairperson") + ":" + hashMap.get("content") + "(" + hashMap.get("repairdate") + ")";
            }
            if( mHashMapList.size()==0)
                mShareMessage+=getString(R.string.title_nolog);

        } else {
            mShareMessage = getString(R.string.hint_nodevice, mCode);
            tvDeviceInfo.setText(mShareMessage);
            tvDeviceInfo.setVisibility(View.VISIBLE);
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
            share.putExtra(Intent.EXTRA_TEXT, mShareMessage);
            startActivity(Intent.createChooser(share, getString(R.string.title_share)));
        }
        return super.onOptionsItemSelected(item);
    }

    ///增加了隔行变色的ArrayAdapter
    public class MyArrayAdapter<T> extends ArrayAdapter {
        private int[] colors = new int[]{R.color.light_blue, R.color.lightest_gray};

        public MyArrayAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            int colorPos = position % colors.length;
            view.setBackgroundResource(colors[colorPos]);
            return view;
        }
    }
}
