package org.lintx.plugins.yinwuchat.velocity.announcement;

import org.lintx.plugins.yinwuchat.velocity.message.MessageManage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定期广播调度任务，每秒运行一次，检查各广播任务是否到达发送时间
 */
public class AnnouncementTask implements Runnable {

    @Override
    public void run() {
        List<AnnouncementTaskConfig> tasks = AnnouncementConfig.getInstance().tasks;
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < tasks.size(); i++) {
            AnnouncementTaskConfig task = tasks.get(i);
            if (!task.enable) {
                continue;
            }

            if (task.lastTime == null) {
                MessageManage.getInstance().broadcastAnnouncement(task, i);
                task.lastTime = now;
            } else {
                Duration duration = Duration.between(task.lastTime, now);
                if (duration.toMillis() >= (long) task.interval * 1000) {
                    MessageManage.getInstance().broadcastAnnouncement(task, i);
                    task.lastTime = now;
                }
            }
        }
    }
}
