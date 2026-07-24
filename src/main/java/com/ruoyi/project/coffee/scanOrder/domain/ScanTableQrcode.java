package com.ruoyi.project.coffee.scanOrder.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.framework.aspectj.lang.annotation.Excel;
import com.ruoyi.framework.web.domain.BaseEntity;

/**
 * 扫码点单-桌台二维码对象 t_scan_table_qrcode
 */
public class ScanTableQrcode extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 桌台ID */
    private Long tableId;

    /** 门店ID */
    @Excel(name = "门店ID")
    private Long shopId;

    /** 门店名称 */
    @Excel(name = "门店名称")
    private String shopName;

    /** 桌号 */
    @Excel(name = "桌号")
    private String tableNo;

    /** 场景(dine_in-堂食,take_out-外带) */
    private String scene;

    /** 二维码直链 */
    private String qrUrl;

    /** 状态(0-停用,1-启用) */
    @Excel(name = "状态")
    private Integer status;

    public Long getTableId() { return tableId; }
    public void setTableId(Long tableId) { this.tableId = tableId; }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getTableNo() { return tableNo; }
    public void setTableNo(String tableNo) { this.tableNo = tableNo; }

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }

    public String getQrUrl() { return qrUrl; }
    public void setQrUrl(String qrUrl) { this.qrUrl = qrUrl; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("tableId", tableId)
            .append("shopId", shopId)
            .append("shopName", shopName)
            .append("tableNo", tableNo)
            .append("scene", scene)
            .append("status", status)
            .toString();
    }
}
