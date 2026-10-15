package com.ruoyi.equipment.vo;

public class CountBucket
{
    private String code;
    private String label;
    private long count;

    public CountBucket() { }
    public CountBucket(String code, String label, long count) { this.code = code; this.label = label; this.count = count; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
