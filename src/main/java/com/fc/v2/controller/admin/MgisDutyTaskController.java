package com.fc.v2.controller.admin;

import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.model.custom.duty.DutyBoard;
import com.fc.v2.service.ITMgisDutyTaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 到期提醒看板 Controller（scheduling-job 形状）。
 *
 * <p>进出的门唯「周期执行」一处：这里只开只读的看板，不开手工立单改单；
 * 页面不预先写死局面，灵不灵但看服务回传。</p>
 *
 * @author fuce
 * @date 2026-09-30
 */
@Api(value = "到期提醒看板")
@Controller
@RequestMapping("/mgisDutyTask")
public class MgisDutyTaskController extends BaseController {

    private final String prefix = "admin/mgisDutyTask";

    @Autowired
    private ITMgisDutyTaskService mgisDutyTaskService;

    @ApiOperation(value = "本轮看板跳转", notes = "本轮看板跳转")
    @GetMapping("/view")
    public String view(ModelMap model) {
        return prefix + "/board";
    }

    @ApiOperation(value = "最近一轮局面", notes = "局面由服务层回传，页面不预置")
    @GetMapping("/board")
    @ResponseBody
    public AjaxResult board() {
        DutyBoard data = mgisDutyTaskService.latestBoard();
        // 一轮都还没跑过：按本轮无事回话，不当故障。
        if (data == null) {
            data = new DutyBoard();
            data.setIdle(true);
        }
        return AjaxResult.success(200, "操作成功", data);
    }
}
