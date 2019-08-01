package org.lintx.plugins.yinwuchat.bungee.announcement;

import org.lintx.plugins.yinwuchat.bungee.MessageManage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Task implements Runnable {

    @Override
    public void run() {
        List<TaskConfig> tasks = Config.getInstance().tasks;
        if (tasks.size()<=0){
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (TaskConfig task : tasks){
            if (!task.enable){
                continue;
            }
            if (task.lastTime==null){
                MessageManage.getInstance().broadcast(task.list,task.server);
                task.lastTime = now;
            }
            else {
                Duration duration = Duration.between(task.lastTime,now);
                if (duration.toMillis()>=task.interval*1000){
                    MessageManage.getInstance().broadcast(task.list,task.server);
                    task.lastTime = now;
                }
            }
        }
    }
}
