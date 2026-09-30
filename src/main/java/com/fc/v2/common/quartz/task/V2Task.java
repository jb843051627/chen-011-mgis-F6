package com.fc.v2.common.quartz.task;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TSysUser;
import com.fc.v2.model.custom.DutyCycleSnapshot;
import com.fc.v2.service.ITMgisDutyTaskService;
import com.fc.v2.service.ITSysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *测试类
 * @CLASSNAME   V2Task
 * @Description 定时调度具体工作类
 * @Auther Jan  橙寂
 * @DATE 2019/9/2 0002 15:33
 */
@Component("v2Task")
public class V2Task {

	@Autowired
    private ITSysUserService userService;

	@Autowired
	private ITMgisDutyTaskService dutyTaskService;

    /**
     * 无参的任务
     */
    public void runTask1()
    {
        System.out.println("正在执行定时任务，无参方法");
    }

    /**
     * 有参任务
     * 目前仅执行常见的数据类型  Integer Long  带L  string  带 ''  bool Double 带 d
     * @param a
     * @param b
     */
    public void runTask2(Integer a,Long b,String c,Boolean d,Double e)
    {
    	List<TSysUser> list=  userService.selectTSysUserList(new QueryWrapper<TSysUser>());
    	System.err.println("用户查询num："+list.size());
        System.out.println("正在执行定时任务，带多个参数的方法"+a+"   "+b+" "+c+"  "+d+" "+e+"执行时间:"+DateUtil.now());
    }

    /**
     * 五月处务会新规：到期提醒周期执行。进出的门唯此一处——唤起后整套执行压给服务层，
     * 本方法不挑条、不发送、不回写，只接服务回传的本轮局面。注册到 t_sys_quartz_job 的
     * invoke_target 即 v2Task.expireRemindCycle()。
     */
    public void expireRemindCycle()
    {
        DutyCycleSnapshot snap = dutyTaskService.runCycle();
        if (snap.isConcurrentSkip()) {
            System.out.println("到期提醒周期执行：上一轮未完成，本轮跳过 " + DateUtil.now());
            return;
        }
        if (snap.isIdle()) {
            // 没有一条到开口的（窗外、窗内都可能落到这）：报一句本轮无事，不当故障
            System.out.println("到期提醒周期执行：本轮无事 " + DateUtil.now());
            return;
        }
        if (!snap.isInWindow()) {
            System.out.println("到期提醒周期执行：当前不在发送时段(08:30-17:30)，条子留到下一轮 " + DateUtil.now());
            return;
        }
        System.out.println("到期提醒周期执行完成：本轮发出 " + snap.getSentThisRound()
                + " 条，新挂起 " + snap.getSuspendedThisRound()
                + " 条；当日局面 已发/待发/挂起 = "
                + snap.getTotalSent() + "/" + snap.getTotalWaiting() + "/" + snap.getTotalSuspended()
                + " " + DateUtil.now());
    }
}
