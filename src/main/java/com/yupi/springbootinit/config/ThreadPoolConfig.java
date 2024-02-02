package com.yupi.springbootinit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 自定义线程池
 */
@Configuration
@Slf4j
public class ThreadPoolConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        // 实现函数式接口
        ThreadFactory threadFactory = Thread::new;
        /*
          1--首先，corePoolSize 参数非常关键，它代表了在正常情况下需要的线程数量。你可以根据希望的系统运行状况以及同时执行的任务数设定这个值。

          2--接着是 maximumPoolSize，这个参数的设定应当与我们的下游系统的瓶颈相关联。
          比如，如果我们的 AI 系统一次只允许两个任务同时执行，那么 maximumPoolSize 就应设为两个，这就是其上限，不宜过大。所以，这个参数值的设定就应当对应于极限情况。

          3--至于 keepAliveTime 空闲存活时间，并不需要过于纠结，这个问题相对较小。你可以设定为几秒钟，虽然这可能稍微有点短。
          你可能需要根据任务以及人员的变动频率进行设定，但无需过于纠结，通常设定为秒级或分钟级别就可以了。

          4--再看 workQueue 工作队列的长度，建议你结合系统的瓶颈进行设定。在我们的场景中，可以设定为 20。
          如果下游系统最多允许一小时的任务排队，那么你这边就可以设置 20 个任务排队，而核心线程数则设定为 4。

          todo  corePoolSize满了任务会存储进workQueue 若workQueue也满了则会临时分配线程直至数量为maximumPoolSize 再超出直接抛出异常

          5--threadFactory 线程工厂这里就不细说了，应根据具体情况设定
          至于 RejectedExecutionHandler 拒绝策略，我们可以直接选择丢弃、抛出异常，然后交由数据库处理，或者标记任务状态为已拒绝，表示任务已满，无法再接受新的任务。
         */
        return new ThreadPoolExecutor(2, 4, 100
                , TimeUnit.SECONDS, new ArrayBlockingQueue<>(4), threadFactory, myRejectedHandler());
    }

    // 自定义拒绝策略
    @Bean("myRejectedHandler")
    public RejectedExecutionHandler myRejectedHandler() {
        return (Runnable r, ThreadPoolExecutor executor) -> {
            if (r != null) {
                log.info("线程任务被拒绝");
                // 尝试重新提交
                try {
                    Thread.sleep(3000);
                    log.info("线程尝试重新提交任务");
                    executor.execute(r);
                } catch (InterruptedException e) {
                    log.error("重新提交线程任务-- 系统休眠异常");
                }
            }
        };
    }
}
