package org.sid.bluetoothsearch.datarepository;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "bluetooth")
public class Bluetooth {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "Id")
    private Long bId;
    @ColumnInfo(name = "name")
    private String bName;
    @ColumnInfo(name = "longitude")
    private Double bLongitute;
    @ColumnInfo(name = "latitude")
    private Double bLatitude;
    @ColumnInfo(name = "mac_address")
    private String bAddress;
    public boolean isFavorited;
    @ColumnInfo(name = "type")
    private String bType;
    @ColumnInfo(name = "bounded")
    private String bBounded;
    @ColumnInfo(name = "majorclass")
    private String majorClass;

    public Bluetooth() {
    }

    public Bluetooth(String bName, String bAddress, String bType,String bBounded, String majorClass, Double bLongitute, Double bLatitude, boolean isFavorited) {
        this.bId = bId;
        this.bName = bName;
        this.bLongitute = bLongitute;
        this.bLatitude = bLatitude;
        this.bAddress = bAddress;
        this.isFavorited = isFavorited;
        this.bType = bType;
        this.bBounded = bBounded;
        this.majorClass = majorClass;
    }

    public Long getbId() {
        return bId;
    }

    public void setbId(Long bId) {
        this.bId = bId;
    }

    public String getbName() {
        return bName;
    }

    public void setbName(String bName) {
        this.bName = bName;
    }

    public Double getbLongitute() {
        return bLongitute;
    }

    public void setbLongitute(Double bLongitute) {
        this.bLongitute = bLongitute;
    }

    public Double getbLatitude() {
        return bLatitude;
    }

    public void setbLatitude(Double bLatitude) {
        this.bLatitude = bLatitude;
    }

    public String getbAddress() {
        return bAddress;
    }

    public void setbAddress(String bAddress) {
        this.bAddress = bAddress;
    }

    public boolean isFavorited() {
        return isFavorited;
    }

    public void setFavorited(boolean favorited) {
        isFavorited = favorited;
    }

    public String getbType() {
        return bType;
    }

    public void setbType(String bType) {
        this.bType = bType;
    }

    public String getbBounded() {
        return bBounded;
    }

    public void setbBounded(String bBounded) {
        this.bBounded = bBounded;
    }

    public String getMajorClass() {
        return majorClass;
    }

    public void setMajorClass(String majorClass) {
        this.majorClass = majorClass;
    }
}
