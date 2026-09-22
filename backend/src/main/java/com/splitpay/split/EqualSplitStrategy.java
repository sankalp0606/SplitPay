package com.splitpay.split;

import com.splitpay.entity.SplittingStrategyType;
import com.splitpay.exception.PaymentValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class EqualSplitStrategy implements PaymentSplitStrategy {

    private static final int CURRENCY_SCALE = 2;

    @Override
    public SplittingStrategyType getStrategyType() {
        return SplittingStrategyType.EQUAL_SPLIT;
    }

    @Override
    public List<BigDecimal> split(BigDecimal totalAmount, BigDecimal maxPartAmount, List<BigDecimal> requestedParts) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Total payment amount must be strictly greater than zero");
        }

        BigDecimal normalizedTotal = totalAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        int partCount;

        if (requestedParts != null && !requestedParts.isEmpty()) {
            partCount = requestedParts.size();
        } else if (maxPartAmount != null && maxPartAmount.compareTo(BigDecimal.ZERO) > 0) {
            partCount = normalizedTotal.divide(maxPartAmount, 0, RoundingMode.CEILING).intValue();
        } else {
            partCount = 2; // Default to 2 equal parts
        }

        if (partCount <= 0) {
            partCount = 1;
        }

        BigDecimal basePart = normalizedTotal.divide(BigDecimal.valueOf(partCount), CURRENCY_SCALE, RoundingMode.FLOOR);
        BigDecimal remainder = normalizedTotal.subtract(basePart.multiply(BigDecimal.valueOf(partCount)));

        List<BigDecimal> parts = new ArrayList<>(partCount);
        BigDecimal oneCent = new BigDecimal("0.01");

        for (int i = 0; i < partCount; i++) {
            BigDecimal currentPart = basePart;
            if (remainder.compareTo(BigDecimal.ZERO) > 0) {
                currentPart = currentPart.add(oneCent);
                remainder = remainder.subtract(oneCent);
            }
            parts.add(currentPart);
        }

        // Mathematical invariant check
        BigDecimal sum = parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(normalizedTotal) != 0) {
            throw new IllegalStateException(String.format(
                    "Equal split invariant failed: calculated sum %s does not match total %s", sum, normalizedTotal));
        }

        return parts;
    }
}
