package com.splitpay.split;

import com.splitpay.entity.SplittingStrategyType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PaymentSplitService {

    private final SplitStrategyFactory strategyFactory;
    private final BigDecimal defaultMaxPartAmount;
    private final SplittingStrategyType defaultStrategy;

    public PaymentSplitService(
            SplitStrategyFactory strategyFactory,
            @Value("${splitpay.splitting.max-part-amount:1990.00}") BigDecimal defaultMaxPartAmount,
            @Value("${splitpay.splitting.default-strategy:MAX_PART_AMOUNT}") SplittingStrategyType defaultStrategy) {
        this.strategyFactory = strategyFactory;
        this.defaultMaxPartAmount = defaultMaxPartAmount;
        this.defaultStrategy = defaultStrategy;
    }

    public List<BigDecimal> split(
            BigDecimal totalAmount,
            SplittingStrategyType requestedStrategy,
            BigDecimal customMaxPartAmount,
            List<BigDecimal> requestedParts) {

        SplittingStrategyType strategyType = requestedStrategy != null ? requestedStrategy : defaultStrategy;
        BigDecimal maxPartAmount = customMaxPartAmount != null && customMaxPartAmount.compareTo(BigDecimal.ZERO) > 0
                ? customMaxPartAmount
                : defaultMaxPartAmount;

        PaymentSplitStrategy strategy = strategyFactory.getStrategy(strategyType);
        return strategy.split(totalAmount, maxPartAmount, requestedParts);
    }

    public BigDecimal getDefaultMaxPartAmount() {
        return defaultMaxPartAmount;
    }

    public SplittingStrategyType getDefaultStrategy() {
        return defaultStrategy;
    }
}
