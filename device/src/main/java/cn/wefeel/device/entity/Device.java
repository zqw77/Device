package cn.wefeel.device.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 设备表
 */
@Table(name = "device"
        ,onCreated = "CREATE UNIQUE INDEX code_unique ON Device(code);CREATE INDEX stateindex ON Device(state)") //为表创建code唯一索引
public class Device {

    @Column(name = "id", isId = true, autoGen = true)//自增id
    public int id;  //xutil3也可以用private配setId和getId，但我觉得太麻烦，直接public多方便
    @Column(name= "uid" )
    public String uid;
    @Column(name = "code") //设备编码
    public String code;
    @Column(name = "station") //站名
    public String station;
    @Column(name = "orgname")	//所在机构
    public String orgname;
    @Column(name = "state")	//设备状态：0.其它 1.在道 2.在修 3. 备品 4. 废品
    public int state;
    @Column(name = "posflag")	//设备位置标记：1.车站 2.道口 3.机房 4. 调度所 5. 其他
    public int posflag;
    @Column(name = "type")	//设备类型
    public String type;
    @Column(name = "name")	//设备名称
    public String name;
    @Column(name = "producer")	//厂家
    public String producer;
    @Column(name = "model")	//型号
    public String model;
    @Column(name = "unit")	//单位
    public String unit;
    @Column(name = "online")	//上道时间
    public String online;
    @Column(name = "life")	//寿命
    public String life;
    @Column(name = "flag") //0未修改，1已新增，2已编辑，3已删除
    public int flag;

    //这里注意，要使用构造器初始化时，必须再提供一个无参构造器
    //我觉得是和注解有关吧
    public Device() {
    }

}
