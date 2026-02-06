package com.example.ibnsina;

public class InventoryModel {
    private String sl, category, code, productName, packSize, totalQty, looseQty, cartonQty, cartonSize, shortQty, excessQty, remark, status;

    public InventoryModel(String sl, String category, String code, String productName, String packSize,
                          String totalQty, String looseQty, String cartonQty, String cartonSize,
                          String shortQty, String excessQty, String remark, String status) {
        this.sl = sl;
        this.category = category; 
        this.code = code; 
        this.productName = productName;
        this.packSize = packSize; 
        this.totalQty = totalQty; 
        this.looseQty = looseQty;
        this.cartonQty = cartonQty; 
        this.cartonSize = cartonSize;
        this.shortQty = shortQty; 
        this.excessQty = excessQty; 
        this.remark = remark;
        this.status = status;
    }

    public String getSl() { return sl; }
    public String getCategory() { return category; }
    public String getCode() { return code; }
    public String getProductName() { return productName; }
    public String getPackSize() { return packSize; }
    public String getTotalQty() { return totalQty; }
    public String getLooseQty() { return looseQty; }
    public String getCartonQty() { return cartonQty; }
    public String getCartonSize() { return cartonSize; }
    public String getShortQty() { return shortQty; }
    public String getExcessQty() { return excessQty; }
    public String getRemark() { return remark; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}