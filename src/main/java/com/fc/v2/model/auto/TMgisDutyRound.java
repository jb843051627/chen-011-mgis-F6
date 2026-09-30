package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 到期提醒本轮局面（按地市）对象 t_mgis_duty_round
 *
 * <p>一行一地市、只记一轮；看板灵不灵只看服务回传，页面不预先写死。</p>
 *
 * @author fuce
 * @date 2026-09-30
 */
@TableName("t_mgis_duty_round")
@ApiModel(value = "TMgisDutyRound", description = "到期提醒本轮局面")
public class TMgisDutyRound implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 轮次号（按本轮时刻生成） */
    @TableField("round_no")
    @ApiModelProperty(value = "轮次号")
    private String roundNo;

    /** 本轮执行时刻 */
    @TableField("run_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "本轮执行时刻")
    private Date runAt;

    /** 地市代号 */
    @TableField("city_code")
    @ApiModelProperty(value = "地市代号")
    private String cityCode;

    /** 本轮本市已发条数 */
    @TableField("sent_count")
    @ApiModelProperty(value = "本轮本市已发条数")
    private Integer sentCount;

    /** 本轮本市待发条数（到点未发/留轮） */
    @TableField("pending_count")
    @ApiModelProperty(value = "本轮本市待发条数")
    private Integer pendingCount;

    /** 本轮本市挂起条数 */
    @TableField("suspended_count")
    @ApiModelProperty(value = "本轮本市挂起条数")
    private Integer suspendedCount;

    /** 本轮是否无事 0有动作 1本轮无事 */
    @TableField("idle_flag")
    @ApiModelProperty(value = "本轮是否无事 0有动作 1本轮无事")
    private Integer idleFlag;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoundNo() {
        return roundNo;
    }

    public void setRoundNo(String roundNo) {
        this.roundNo = roundNo;
    }

    public Date getRunAt() {
        return runAt;
    }

    public void setRunAt(Date runAt) {
        this.runAt = runAt;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public Integer getSentCount() {
        return sentCount;
    }

    public void setSentCount(Integer sentCount) {
        this.sentCount = sentCount;
    }

    public Integer getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Integer pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Integer getSuspendedCount() {
        return suspendedCount;
    }

    public void setSuspendedCount(Integer suspendedCount) {
        this.suspendedCount = suspendedCount;
    }

    public Integer getIdleFlag() {
        return idleFlag;
    }

    public void setIdleFlag(Integer idleFlag) {
        this.idleFlag = idleFlag;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
