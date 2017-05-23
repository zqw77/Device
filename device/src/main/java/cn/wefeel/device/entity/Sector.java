package cn.wefeel.device.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 部门名称表
 */

@Table(name="sector")
public class Sector {
    @Column(name="id",isId = true,autoGen = true)
    private int id;
    @Column(name="name")
    private String name;
    @Column(name="parent")
    private String parent;

    public Sector() {
    }

    public Sector(String name, String parent) {
        this.name = name;
        this.parent = parent;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }
}
