package com.yupi.springbootinit;

import com.yupi.springbootinit.config.WxOpenConfig;
import com.yupi.springbootinit.manager.AIManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.*;

/**
 * 主类测试
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@SpringBootTest
@Slf4j
class MainApplicationTests {

    @Resource
    private WxOpenConfig wxOpenConfig;

    @Resource
    private AIManager aiManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    public static void main(String[] args) {
        ThreadFactory threadFactory = Thread::new;
        new ThreadPoolExecutor(2, 4, 100
                , TimeUnit.SECONDS, new ArrayBlockingQueue<>(4), threadFactory);
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                System.out.println("gagaga");
                Thread.sleep(500000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        future.orTimeout(1, TimeUnit.SECONDS);
        for (int i = 0; ; i++) {
            if (i % 10000000 == 0)
                System.out.println(111);
        }
    }

    public static void add(List<Integer> list) {
        list.add(1);
    }

    @Test
    void contextLoads() {
        System.out.println(wxOpenConfig);
    }

    @Test
    void AiTest() {
        String s = aiManager.doChat("""
                分析需求：
                分析人口增长趋势
                原始数据：
                年份，人数
                2000，100
                2010，200
                2020，350""");
        log.info(s);
    }
}
