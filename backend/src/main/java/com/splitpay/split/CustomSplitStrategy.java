package com.splitpay.split;

import com.splitpay.entity.SplittingStrategyType;
import com.splitpay.exception.PaymentValidationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class CustomSplitStrategy implements PaymentSplitStrategy {

    private static final int CURRENCY_SCALE = 2;

    @Override
    public SplittingStrategyType getStrategyType() {
        return SplittingStrategyType.CUSTOM_SPLIT;
    }

    @Override
    public List<BigDecimal> split(BigDecimal totalAmount, BigDecimal maxPartAmount, List<BigDecimal> requestedParts) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentValidationException("Total payment amount must be strictly greater than zero");
        }
        if (requestedParts == null || requestedParts.isEmpty()) {
            throw new PaymentValidationException("Custom split requires a non-empty list of part amounts");
        }

        BigDecimal normalizedTotal = totalAmount.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

        List<BigDecimal> normalizedParts = requestedParts.stream()
                .map(part -> {
                    if (part == null || part.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new PaymentValidationException("All custom split parts must be strictly greater than zero");
                    }
                    return part.setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);
                })
                .toList();

        BigDecimal sum = normalizedParts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(normalizedTotal) != 0) {
            throw new PaymentValidationException(String.format(
                    "The sum of custom parts (₹%s) does not match the total order amount (₹%s)", sum, normalizedTotal));
        }

        return normalizedParts;
    }
}
