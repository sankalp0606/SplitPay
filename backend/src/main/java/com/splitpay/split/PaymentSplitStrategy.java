package com.splitpay.split;

import com.splitpay.entity.SplittingStrategyType;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentSplitStrategy {

    SplittingStrategyType getStrategyType();

    /**
     * Splits totalAmount into multiple parts according to the strategy.
     * Guaranteed: sum(parts) == totalAmount exactly, each part > 0.
     *
     * @param totalAmount Total amount to split
     * @param maxPartAmount Configured or overridden maximum amount per part (if applicable)
     * @param requestedParts Optional explicit part values (for custom split)
     * @return Ordered list of BigDecimal part amounts
     */
    List<BigDecimal> split(BigDecimal totalAmount, BigDecimal maxPartAmount, List<BigDecimal> requestedParts);
}
