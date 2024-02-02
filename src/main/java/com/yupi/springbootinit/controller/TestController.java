package com.yupi.springbootinit.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@RestController
public class TestController {

    @GetMapping("/test")
    public String  test(){
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                System.out.println("gagaga");
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        future.orTimeout(5, TimeUnit.SECONDS).getNow(null);
        return "return";
    }
}
