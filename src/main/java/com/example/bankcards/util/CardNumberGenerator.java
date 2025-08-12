package com.example.bankcards.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.atomic.AtomicLong;

public class CardNumberGenerator {

    @Value("${bankcards.bin}")
    private String binValue;

    public static String BIN;

    @PostConstruct
    public void init() {
        BIN = binValue;
    }

    private static final AtomicLong sequence = new AtomicLong(1000);

    /**
     * Генерирует валидный по алгоритму Луна 16-значный номер карты
     */
    public static String generate() {
        String sequencePart = String.format("%010d", sequence.getAndIncrement());
        String base = BIN + sequencePart;
        char checkDigit = (char) ('0' + calculateLuhnCheckDigit(base));
        return base + checkDigit;
    }

    /**
     * Вычисляет контрольную цифру по алгоритму Луна
     */
    private static int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(number.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit = digit % 10 + 1;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }
}