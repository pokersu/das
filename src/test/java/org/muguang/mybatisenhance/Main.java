package org.muguang.mybatisenhance;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class Main {
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final ExecutorService executor = Executors.newCachedThreadPool();


    public static void main(String[] args) throws Exception {
        List<String> timeStrList = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(10000);
        for (int i = 0; i < 10000; i++) {
            Date date = new Date();
            String dateStr = sdf.format(date);
            timeStrList.add(dateStr);
        }

        for (String str : timeStrList) {
            executor.execute(()->{
                try {
                    Date date = sdf.parse(str);
                    Calendar instance = Calendar.getInstance();
                    instance.setTime(date);
                    int year = instance.get(Calendar.YEAR);
                    if (year!=2022){
                        System.out.println("exception time: " + date);
                    }
                } catch (Exception e) {
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdownNow();
    }

}
