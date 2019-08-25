package com.example.steve;
/*监听任务池是否为空，并为每个用户创建任务执行线程*/

import java.util.HashMap;
import java.util.List;

public class TaskListener implements Runnable {
    public TaskListener() {
    }

    public void run() {
        while (true) {
            for (HashMap.Entry<String, List<String>> e : SteveApplication.taskPool.entrySet()) {
                if (!SteveApplication.threadPool.contains(e.getKey())) {
                    Thread t = new Thread(new TaskProcessor(e.getKey(), e.getValue()));
                    t.start();
                    SteveApplication.threadPool.add(e.getKey());
                }
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
