package cn.wefeel.device;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.xutils.view.annotation.ContentView;

import java.util.HashMap;
import java.util.List;

import cn.wefeel.device.base.BaseActivity;
import cn.wefeel.device.data.MyData;

@ContentView(R.layout.activity_log)
public class LogActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListView listView1 = (ListView) this.findViewById(R.id.listView1);

        List<HashMap<String, Object>> list = (new MyData()).loadLog();

        SimpleAdapter adapter = new SimpleAdapter(this, list, // 数据源
                R.layout.item_log, // xml实现
                new String[]{"code", "orgname", "repairdate", "repairperson", "place", "content", "uid"}, // 对应map的Key
                new int[]{R.id.tvCode, R.id.tvOrgname, R.id.tvRepairDate, R.id.tvRepairPerson, R.id.tvPlace, R.id.tvContent}); // 对应R的Id

        listView1.setAdapter(adapter);
        //显示搜索总数
        this.setTitle(getString(R.string.title_activity_log, listView1.getCount()));

        listView1.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> map = (HashMap<String, Object>) (parent.getItemAtPosition(position));
                String uid = (String) map.get("uid");
                String code = (new MyData()).getCode(uid);
                //传给编辑窗口
                Intent intent = new Intent(LogActivity.this, DetailActivity.class);
                intent.putExtra("code", code);
                startActivityForResult(intent, 0);
            }
        });
    }
}
