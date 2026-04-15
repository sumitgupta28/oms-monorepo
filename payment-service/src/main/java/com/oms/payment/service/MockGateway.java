package com.oms.payment.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@Getter
public class MockGateway {

    @Value("${mock.payment.failure-rate:0.1}")
    private double failureRate;

    @Value("${mock.payment.delay-ms:1000}")
    private long delayMs;

    public boolean process() {
        return ThreadLocalRandom.current().nextDouble() >= failureRate;
    }
}
