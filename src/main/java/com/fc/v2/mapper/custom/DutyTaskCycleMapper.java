package com.fc.v2.mapper.custom;

import java.util.Date;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 到期提醒周期执行专用回写。
 *
 * 不走 MyBatis-Plus 的 updateById：定时线程没有登录会话，自动填充 updateBy 时
 * ShiroUtils.getLoginName() 会抛异常，导致「发得出去、回写不成功」而误判未发。
 * 这里只动周期执行自己的三列，并以 status=0 作库层幂等闸——两轮并发时只有一轮
 * 能把同一条从待提醒改写成功（返回 0 即按未成功处理），同一件事绝不发第二遍。
 *
 * @author fuce
 * @date 2026-09-30
 */
public interface DutyTaskCycleMapper {

    /** 回写已提醒：仅待提醒条目可改；成功返回 1，重复轮次/状态已变返回 0。 */
    @Update("UPDATE t_mgis_duty_task SET status = 1, sent_at = #{sentAt}, suspend_reason = NULL "
            + "WHERE id = #{id} AND status = 0 AND del_flag = 0")
    int markSent(@Param("id") Long id, @Param("sentAt") Date sentAt);

    /** 转挂起：仅待提醒条目可改，写清挂起缘故。 */
    @Update("UPDATE t_mgis_duty_task SET status = 2, suspend_reason = #{reason} "
            + "WHERE id = #{id} AND status = 0 AND del_flag = 0")
    int markSuspended(@Param("id") Long id, @Param("reason") String reason);
}
