package com.fc.v2.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.custom.DutyCycleSnapshot;
import com.fc.v2.service.ITMgisDutyTaskService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;

/**
 * 到期提醒周期执行看板 Controller（scheduling-job 形状：只读）。
 *
 * 进出的门唯「周期执行」一处：本控制器不立单、不改单、不手工触发，
 * 只把服务层上一轮跑完回传的局面搬上屏；灵不灵但看服务回传，页面不预写死。
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "到期提醒周期看板")
@Controller
@RequestMapping("/mgisDutyTask")
public class MgisDutyTaskController extends BaseController {

    private final String prefix = "admin/mgisDutyTask";

    @Autowired
    private ITMgisDutyTaskService mgisDutyTaskService;

    @ApiOperation(value = "周期看板跳转", notes = "周期看板跳转")
    @GetMapping("/view")
    @RequiresPermissions("mgisDutyTask:view")
    public String view(ModelMap model) {
        return prefix + "/board";
    }

    @Log(title = "到期提醒周期看板", action = "snapshot")
    @ApiOperation(value = "本轮局面", notes = "取服务层上一轮周期执行回传的局面；尚未跑过返回 data=null，页面按空屏提示，不当故障")
    @GetMapping("/snapshot")
    @RequiresPermissions("mgisDutyTask:snapshot")
    @ResponseBody
    public AjaxResult snapshot() {
        DutyCycleSnapshot snap = mgisDutyTaskService.latestSnapshot();
        return AjaxResult.successData(200, snap);
    }
}
