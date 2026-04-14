package com.oms.payment.service;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Getter
public class MockGateway {

    @Value("${mock.payment.failure-rate:0.1}")
    private double failureRate;

    @Value("${mock.payment.delay-ms:1000}")
    private long delayMs;

    private final Random random = new Random();

    public boolean process() {
        return random.nextDouble() >= failureRate;
    }
}