package cn.wefeel.device;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TabHost;
import android.widget.TextView;

import java.util.ArrayList;

import cn.wefeel.device.base.BaseActivity;
import cn.wefeel.device.data.MyData;

public class SearchActivity extends BaseActivity {

    String mAllStation;
    String mAllOrgname;
    Cursor mCursor;
    SearchView svSearch;
    TextView tvSelectedStation;
    TextView tvSelectedOrgname;
    ListView lvDevice;
    TextView tvHeader;
    TabHost thSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mAllStation = getString(R.string.all_station);//"（全部车站）";
        mAllOrgname = getString(R.string.all_orgname);//"（全部车间）";

        Toolbar toolbar = (Toolbar) findViewById(R.id.barTool);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        //选择车站按钮
        tvSelectedStation = (TextView) this.findViewById(R.id.tvSelectedStation);
        tvSelectedStation.setText(mAllStation);
        Button btnSelectStation = (Button) this.findViewById(R.id.btnSelectStation);
        btnSelectStation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyData db = new MyData();
                ArrayList<String> stationArray = db.getStationsArray();//获取所有车站
                stationArray.add(0, mAllStation);//加上“全部车站”到最前面
                final CharSequence[] stations = stationArray.toArray(new CharSequence[stationArray.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setIcon(android.R.drawable.ic_menu_search);//调用Android的默认搜索图标
                builder.setTitle(getString(R.string.title_select_station));
                builder.setItems(stations, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tvSelectedStation.setText(stations[which]);
                        search();
                    }
                });
                builder.show();
            }
        });
        //选择车间按钮
        tvSelectedOrgname = (TextView) this.findViewById(R.id.tvSelectedOrgname);
        tvSelectedOrgname.setText(mAllOrgname);
        Button btnSelectOrgname = (Button) this.findViewById(R.id.btnSelectOrgname);
        btnSelectOrgname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyData db = new MyData();
                ArrayList<String> orgnamesArray = db.getOrgnamesArray();//获取所有车间
                orgnamesArray.add(0, mAllOrgname);//加上“全部车间”到最前面
                final CharSequence[] orgnames = orgnamesArray.toArray(new CharSequence[orgnamesArray.size()]);
                AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                builder.setIcon(android.R.drawable.ic_menu_search);//调用Android的默认搜索图标
                builder.setTitle(getString(R.string.title_select_orgname));
                builder.setItems(orgnames, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tvSelectedOrgname.setText(orgnames[which]);
                        search();
                    }
                });
                builder.show();
            }
        });
        //
        lvDevice = (ListView) this.findViewById(R.id.lvDevice);
        lvDevice.setEmptyView(this.findViewById(R.id.tvNoDevice));
        lvDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //显示明细
                Cursor c = (Cursor) parent.getItemAtPosition(position);
                String code = c.getString(c.getColumnIndex("code"));
                Intent intent = new Intent(view.getContext(), DetailActivity.class);
                intent.putExtra("code", code);
                startActivity(intent);
            }
        });
        tvHeader = new TextView(this);
        tvHeader.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvHeader.setTextColor(Color.BLUE);
        tvHeader.setOnClickListener(null);
        lvDevice.addHeaderView(tvHeader);

        //按状态统计设备数量
        MyData db = new MyData();
        final ContentValues values = db.getCountByState();
        //显示tab标签
        thSearch = (TabHost) findViewById(R.id.thSearch);
        thSearch.setup();
        final String[] states = getResources().getStringArray(R.array.states);
        for (int i = 1; i < states.length; i++) {
            String key = String.valueOf(i);
            String label = values.getAsString(key);
            if (label == null) label = "0";//避免显示null
            TabHost.TabSpec tab = thSearch.newTabSpec(key);
            tab.setIndicator(states[i] + "\n(" + label + ")");
            tab.setContent(R.id.tab1);//因界面相同都指向同一个tab1
            thSearch.addTab(tab);
        }
        thSearch.setCurrentTab(3);//默认是0，如不切到3或其他，下面就不会真正执行setCurrentTab(0)
        thSearch.setCurrentTab(0);//setCurrentTab中有某个初始化操作，之后才能正常显示tab
        thSearch.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                search();
            }
        });
        search();//填充listview
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void search() {
        try {
            String state = thSearch.getCurrentTabTag();
            String station = tvSelectedStation.getText().toString().equals(mAllStation) ? null : tvSelectedStation.getText().toString();
            String orgname = tvSelectedOrgname.getText().toString().equals(mAllOrgname) ? null : tvSelectedOrgname.getText().toString();
            String key = null;
            if (svSearch != null) {//search()如在菜单生成前调用就会null
                key = svSearch.getQuery().toString();
            }

            long start = System.nanoTime();
            if (mCursor != null) mCursor.close();
            mCursor = (new MyData()).queryDevice(Integer.valueOf(state), station, orgname, key);
//            Log.e(TAG,"查询时间："+String.valueOf(System.nanoTime()-start));
//            List<HashMap<String, Object>> mapList = (new MyData()).loadDevice();效率太低不能用
//            SimpleAdapter adapter = new SimpleAdapter(this.getContext(), mapList, R.layout.item_device, from, to);
            String[] from = {"code", "station", "orgname", "type", "name", "producer", "model", "online"};
            int[] to = {R.id.tvCode, R.id.tvStation, R.id.tvOrgname, R.id.tvType, R.id.tvName, R.id.tvProducer, R.id.tvModel, R.id.tvOnline};
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.item_device, mCursor, from, to, SimpleCursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

            tvHeader.setText(getString(R.string.hint_searchresult, adapter.getCount()));
            lvDevice.setAdapter(adapter);
//            Log.e(TAG,"填充时间："+String.valueOf(System.nanoTime()-start));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        MenuItem menuItem = menu.findItem(R.id.action_search);//
        svSearch = (SearchView) MenuItemCompat.getActionView(menuItem);//加载searchview
        svSearch.setSubmitButtonEnabled(true);//设置是否显示搜索按钮
        svSearch.setQueryHint(getString(R.string.hint_search));//设置提示信息
        svSearch.setIconifiedByDefault(true);//设置搜索默认为图标
        svSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                search();
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (mCursor != null) mCursor.close();
        super.onDestroy();
    }

}
