package com.gfm.entity;

import java.util.Date;
import javax.persistence.*;

@Table(name = "ec_order_customs_log")
public class EcOrderCustomsLog {
    /**
     * 主键id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户id
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 主订单号
     */
    @Column(name = "order_no")
    private String orderNo;

    /**
     * 报关清关状态 1:报关申请中; 2:报关成功; 3:报关失败; 4:清关审核中; 5:清关审核通过放行; 6:清关审核退单; 7:清关审核异常; 8:清关仓库出货;
     */
    @Column(name = "customs_status")
    private Integer customsStatus;

    /**
     * 状态描述
     */
    private String description;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @Column(name = "update_time")
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 获取主键id
     *
     * @return id - 主键id
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置主键id
     *
     * @param id 主键id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户id
     *
     * @return user_id - 用户id
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * 设置用户id
     *
     * @param userId 用户id
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取主订单号
     *
     * @return order_no - 主订单号
     */
    public String getOrderNo() {
        return orderNo;
    }

    /**
     * 设置主订单号
     *
     * @param orderNo 主订单号
     */
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    /**
     * 获取报关清关状态 1:报关申请中; 2:报关成功; 3:报关失败; 4:清关审核中; 5:清关审核通过放行; 6:清关审核退单; 7:清关审核异常; 8:清关仓库出货;
     *
     * @return customs_status - 报关清关状态 1:报关申请中; 2:报关成功; 3:报关失败; 4:清关审核中; 5:清关审核通过放行; 6:清关审核退单; 7:清关审核异常; 8:清关仓库出货;
     */
    public Integer getCustomsStatus() {
        return customsStatus;
    }

    /**
     * 设置报关清关状态 1:报关申请中; 2:报关成功; 3:报关失败; 4:清关审核中; 5:清关审核通过放行; 6:清关审核退单; 7:清关审核异常; 8:清关仓库出货;
     *
     * @param customsStatus 报关清关状态 1:报关申请中; 2:报关成功; 3:报关失败; 4:清关审核中; 5:清关审核通过放行; 6:清关审核退单; 7:清关审核异常; 8:清关仓库出货;
     */
    public void setCustomsStatus(Integer customsStatus) {
        this.customsStatus = customsStatus;
    }

    /**
     * 获取状态描述
     *
     * @return description - 状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 设置状态描述
     *
     * @param description 状态描述
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 获取创建时间
     *
     * @return create_time - 创建时间
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取修改时间
     *
     * @return update_time - 修改时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置修改时间
     *
     * @param updateTime 修改时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取备注
     *
     * @return remark - 备注
     */
    public String getRemark() {
        return remark;
    }

    /**
     * 设置备注
     *
     * @param remark 备注
     */
    public void setRemark(String remark) {
        this.remark = remark;
    }
}