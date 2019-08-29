package com.example.steve;
/*监听任务池是否为空，并为每个用户创建任务执行线程*/

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class TaskListener implements Runnable {
    public TaskListener() {
    }

    public void run() {
        while (true) {
            Iterator<HashMap.Entry<String, List<String>>> it = SteveApplication.taskPool.entrySet().iterator();
            synchronized (SteveApplication.taskPool){
                while (it.hasNext()) {
                    HashMap.Entry<String, List<String>> next = it.next();
                    if (!SteveApplication.threadPool.contains(next.getKey())) {
                        Thread t = new Thread(new TaskProcessor(next.getKey(), next.getValue()));
                        t.start();
                        SteveApplication.threadPool.add(next.getKey());
                    }
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
