package cn.wefeel.device.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 存储参数的表
 */
@Table(name = "parameter")
public class Parameter {
    @Column(name = "id", isId = true, autoGen = true)
    public int id;
    @Column(name = "name")
    public String name;
    @Column(name = "value")
    public String value;
    @Column(name = "memo")
    public String memo;

    public Parameter() {
    }

    public Parameter(String name, String value, String memo) {
        this.memo = memo;
        this.name = name;
        this.value = value;
    }

}
