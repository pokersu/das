package org.muguang.mybatisenhance.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/event")
public class EventController {


    private final Map<String, SseEmitter> connectionPool = new ConcurrentHashMap<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    @RequestMapping("/connect/{userId}")
    public SseEmitter connect(@PathVariable("userId") String userId){
        return connectionPool.computeIfAbsent(userId, this::buildConnection);
    }

    @RequestMapping("/start")
    public String start(){
        if (!isRunning.compareAndSet(false, true)){
            return "already started";
        }
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(()->{
            while (isRunning.get()){
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                connectionPool.keySet().forEach(uid->{
                    try {
                        connectionPool.get(uid).send("hello: " + sdf.format(new Date()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        });
        return "started";
    }

    @RequestMapping("/stop")
    public String stop() {
        if (!isRunning.compareAndSet(true, false)) {
            return "already stopped";
        }
        return "stopped";
    }

    private SseEmitter buildConnection(String userId){
        SseEmitter sseEmitter = new SseEmitter(0L);
        sseEmitter.onCompletion(()-> System.out.println("connected: " + userId));
        sseEmitter.onTimeout(()->System.out.println("timeout: " + userId));
        sseEmitter.onError(throwable ->System.out.println("error: " + userId));
        return sseEmitter;
    }

    @RequestMapping("/close/{userId}")
    public String close(@PathVariable("userId") String userId){
        connectionPool.remove(userId);
        return "success";
    }
}
