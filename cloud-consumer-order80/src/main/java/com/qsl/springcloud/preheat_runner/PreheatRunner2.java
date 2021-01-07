package com.qsl.springcloud.preheat_runner;

import com.netflix.appinfo.InstanceInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

/**
 * 服务预热
 * 注意：需要在配置文件加上 eureka.instance.initial-status=DOWN 配置。这样该服务在预热完成之前不会被其他应用调到
 *
 * @author qianshuailong
 * @date 2020/12/4
 */
@Slf4j
@Component
public class PreheatRunner2 implements ApplicationRunner, ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
     * 调用的微服务列表
     */
    private Set<String> apps = new HashSet<>();

    @Resource(name = "preheatRunnerTemplate")
    private RestTemplate restTemplate;

    private static final String URL = "http://localhost";

    @Value("${server.port}")
    private int port;

    @Autowired
    private EurekaServiceRegistry eurekaServiceRegistry;

    @Autowired
    private EurekaRegistration eurekaRegistration;

    @Value("${msv.preheat.threadCount:10}")
    private int threadCount;

    @Value("${msv.preheat.requestCount:100}")
    private int requestCount;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        eurekaServiceRegistry.setStatus(eurekaRegistration, InstanceInfo.InstanceStatus.DOWN.name());
//        //获取所依赖的下游微服务
//        getApps();
        //预热
        preheatThread();
        eurekaServiceRegistry.setStatus(eurekaRegistration, InstanceInfo.InstanceStatus.UP.name());
    }

//    /**
//     * 获取所依赖的下游微服务
//     */
//    private void getApps() {
//        //获取使用了@FeignClient注解的所有bean
//        Map<String, Object> map = applicationContext.getBeansWithAnnotation(FeignClient.class);
//        if (CollectionUtils.isEmpty(map)) {
//            return;
//        }
//        map.keySet().forEach(k -> {
//            if (!k.contains(".")) {
//                return;
//            }
//            try {
//                FeignClient annotation = Class.forName(k).getAnnotation(FeignClient.class);
//                String app = annotation.name();
//                if (!StringUtils.isEmpty(app)) {
//                    apps.add(app);
//                    return;
//                }
//                app = annotation.value();
//                if (!StringUtils.isEmpty(app)) {
//                    apps.add(app);
//                }
//            } catch (ClassNotFoundException e) {
//                log.error(e.getMessage(), e);
//            }
//        });
//    }

    private void preheatThread() {
        final String url = URL + ":" + port + "/info";
        long start = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(1000);
                restTemplate.getForEntity(url, String.class);
                break;
            } catch (Exception e) {
                log.warn("tomcate未启动完成，继续检测……", e);
            }
        }
        log.info("开始预热线程池");
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            //不考虑使用线程池，预热完成后即废弃线程。
            new Thread(() -> {
                {
                    //预热ribbon。这里ribbon预热次数，和线程数量保持一致，防止对下游服务造成过大压力
                    for (String app : apps) {
                        try {
                            restTemplate.getForEntity("http://" + app + "/info", String.class);
                        } catch (Exception e) {
                            log.warn("预热ribbon线程异常:" + e.getMessage(), e);
                        }
                    }
                    for (int j = 0; j < requestCount; j++) {
                        //tomcat线程池可以多预热几次
                        try {
                            restTemplate.getForEntity(url, String.class);
                        } catch (Exception e) {
                            log.warn("预热tomcat线程异常:" + e.getMessage(), e);
                        }
                    }
                    countDownLatch.countDown();
                }
            }, "preheat-threadpool-" + i).start();
        }
        try {
            countDownLatch.await();
            log.info("完成预热,总耗时:{}s", (System.currentTimeMillis() - start) / 1000);
        } catch (InterruptedException e) {
            log.error("预热wait()中断", e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @LoadBalanced
    @Bean("preheatRunnerTemplate")
    RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(2000);
        return new RestTemplate(requestFactory);
    }

}
