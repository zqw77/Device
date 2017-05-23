package cn.wefeel.device.entity;

import org.xutils.db.annotation.Column;
import org.xutils.db.annotation.Table;

/**
 * 线路名称表
 */

@Table(name="line")
public class Line {
    @Column(name="id",isId = true,autoGen = true)
    public int id;
    @Column(name="line")
    public String line;
    @Column(name="station")
    public String station;

    public Line() {
    }

    public Line(String line, String station) {
        this.line = line;
        this.station = station;
    }
}
