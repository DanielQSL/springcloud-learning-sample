package com.qsl.springcloud.service;

import com.qsl.springcloud.entities.Payment;

/**
 * @author qianshuailong
 * @date 2020/3/24
 */
public interface PaymentService {

    int create(Payment payment);

    Payment getPaymentById(Long id);
}
