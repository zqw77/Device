package cn.wefeel.device.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 设备维修记录表
 */
@Table(name="log")
public class Log {
    @Column(name="id",isId = true)
    public String id;
    @Column(name= "uid" )
    public String uid;//设备id
    @Column(name="code")
    public String code;
    @Column(name="orgname")
    public String orgname;
    @Column(name="repairdate")
    public String repairdate;
    @Column(name="repairperson")
    public String repairperson;
    @Column(name="place")
    public String place;
    @Column(name="content")
    public String content;
    @Column(name="flag")
    public int flag;

    public Log() {

    }
}
