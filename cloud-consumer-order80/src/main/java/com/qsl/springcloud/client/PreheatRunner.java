package com.qsl.springcloud.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaServiceRegistry;
import org.springframework.stereotype.Component;

/**
 * 服务预热
 * 注意：需要在配置文件加上 eureka.instance.initial-status=DOWN 配置。这样该服务在预热完成之前不会被其他应用调到
 *
 * @author qianshuailong
 * @date 2020/12/4
 */
@Slf4j
@Component
public class PreheatRunner implements ApplicationRunner {

    @Autowired
    private EurekaServiceRegistry eurekaServiceRegistry;

    @Autowired
    private EurekaRegistration eurekaRegistration;

    @Override
    public void run(ApplicationArguments applicationArguments) {
        long start = System.currentTimeMillis();
        try {
            Thread.sleep(30 * 1000);
        } catch (Exception e) {
            log.error("预热接口出错", e);
        }
        log.info("EurekaRegister time:{}", System.currentTimeMillis() - start);
        eurekaServiceRegistry.setStatus(eurekaRegistration, "UP");
    }

}
