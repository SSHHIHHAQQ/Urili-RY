package com.ruoyi.integration.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.integration.service.IUpstreamSyncService;

/**
 * 上游同步任务分发入口，由 RuoYi Quartz 调度。
 */
@Component("upstreamSyncDispatchTask")
public class UpstreamSyncDispatchTask
{
    private static final Logger log = LoggerFactory.getLogger(UpstreamSyncDispatchTask.class);

    @Autowired
    private IUpstreamSyncService upstreamSyncService;

    public void dispatch()
    {
        int handled = upstreamSyncService.dispatchPendingTasks();
        log.info("Upstream sync dispatcher handled {} task(s)", handled);
    }
}
