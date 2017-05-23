package cn.wefeel.device;

import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;

import cn.wefeel.device.base.BaseActivity;
import cn.wefeel.device.data.MyData;
import cn.wefeel.device.entity.Device;

/**
 * 显示设备详细信息及其维修情况
 */
@ContentView(R.layout.activity_detail)
public class DetailActivity extends BaseActivity {
    private String mCode;

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
            String info = "";
            info += "设备编码：" + device.code;
            info += "\n设备名称：" + device.name;
            info += "\n归属部门：" + device.orgname;
            info += "\n设备型号：" + device.model;
            info += "\n生产厂家：" + device.producer;
            info += "\n设备类型：" + device.type;
            info += "\n所在位置：" + getResources().getStringArray(R.array.positions)[device.posflag];//posNames[device.posflag]+;
            info += "\n所在车站：" + device.station;
            info += "\n上道日期：" + device.online;
            info += "\n设备状态：" + getResources().getStringArray(R.array.states)[device.state];
            info += "\n计量单位：" + device.unit;
            info += "\n使用寿命：" + device.life;
            tvDeviceInfo.setText(info);

            //显示维修信息
            TextView tvHeader=new TextView(this);
            tvHeader.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tvHeader.setTextColor(Color.BLUE);
            tvHeader.setText(R.string.hint_log);
            lvLog.addHeaderView(tvHeader);//加标题
            lvLog.setEmptyView(this.findViewById(R.id.tvNoLog));//无维修记录时提示

            List<HashMap<String, Object>> mapList = (new MyData()).getLog(device.code);
            SimpleAdapter mAdapter = new SimpleAdapter(this, mapList, // 数据源
                    R.layout.item_log, // xml实现
                    new String[]{"code", "orgname", "repairdate", "repairperson", "place", "content"}, // 对应map的Key
                    new int[]{R.id.tvCode, R.id.tvOrgname, R.id.tvRepairDate, R.id.tvRepairPerson, R.id.tvPlace, R.id.tvContent}); // 对应R的Id
            lvLog.setAdapter(mAdapter);

        } else {
            tvDeviceInfo.setText(getString(R.string.hint_nodevice,mCode));
        }
    }
}
