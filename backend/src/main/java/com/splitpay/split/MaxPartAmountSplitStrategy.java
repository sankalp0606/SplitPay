package com.splitpay.split;

import com.splitpay.entity.SplittingStrategyType;
import com.splitpay.exception.PaymentValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class MaxPartAmountSplitStrategy implements PaymentSplitStrategy {

    private static final int CURRENCY_SCALE = 2;

    @Override
    public SplittingStrategyType getStrategyType() {
        return SplittingStrategyType.MAX_PART_AMOUNT;
    }

    @Override
    public List<BigDecimal> split(BigDecimal totalAmount, BigDecimal maxPartAmount, List<BigDecimal> requestedParts) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Total payment amount must be strictly greater than zero");
        }
        if (maxPartAmount == null || maxPartAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Maximum part amount must be strictly greater than zero");
        }

        BigDecimal normalizedTotal = totalAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
        BigDecimal normalizedMax = maxPartAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

        List<BigDecimal> parts = new ArrayList<>();
        BigDecimal remaining = normalizedTotal;

        while (remaining.compareTo(normalizedMax) > 0) {
            parts.add(normalizedMax);
            remaining = remaining.subtract(normalizedMax);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            parts.add(remaining);
        }

        // Mathematical invariant check: Zero money lost, zero money created
        BigDecimal sum = parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(normalizedTotal) != 0) {
            throw new IllegalStateException(String.format(
                    "Splitting invariant failed: calculated sum %s does not match total amount %s", sum, normalizedTotal));
        }

        return parts;
    }
}
