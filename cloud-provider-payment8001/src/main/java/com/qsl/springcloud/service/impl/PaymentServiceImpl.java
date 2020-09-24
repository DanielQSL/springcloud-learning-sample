package com.qsl.springcloud.service.impl;

import com.qsl.springcloud.dao.PaymentDao;
import com.qsl.springcloud.entities.Payment;
import com.qsl.springcloud.service.PaymentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author qianshuailong
 * @date 2020/3/24
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Resource
    private PaymentDao paymentDao;

    @Override
    public int create(Payment payment) {
        return paymentDao.create(payment);
    }

    @Override
    public Payment getPaymentById(Long id) {
        return paymentDao.getPaymentById(id);
    }
}
