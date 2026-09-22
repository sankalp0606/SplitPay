package com.splitpay.split;

import com.splitpay.entity.SplittingStrategyType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SplitStrategyFactory {

    private final Map<SplittingStrategyType, PaymentSplitStrategy> strategies = new EnumMap<>(SplittingStrategyType.class);

    public SplitStrategyFactory(List<PaymentSplitStrategy> strategyList) {
        for (PaymentSplitStrategy strategy : strategyList) {
            strategies.put(strategy.getStrategyType(), strategy);
        }
    }

    public PaymentSplitStrategy getStrategy(SplittingStrategyType type) {
        PaymentSplitStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported splitting strategy: " + type);
        }
        return strategy;
    }
}
