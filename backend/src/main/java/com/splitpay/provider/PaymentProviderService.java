package com.splitpay.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PaymentProviderService {

    private final Map<String, PaymentProvider> providers = new ConcurrentHashMap<>();
    private final String activeProviderName;

    public PaymentProviderService(
            List<PaymentProvider> providerList,
            @Value("${splitpay.provider.active-provider:mock}") String activeProviderName) {
        this.activeProviderName = activeProviderName.toLowerCase();
        for (PaymentProvider provider : providerList) {
            this.providers.put(provider.getProviderName().toLowerCase(), provider);
            // Also alias common keys
            if (provider instanceof MockPaymentProvider) {
                this.providers.put("mock", provider);
            }
        }
        log.info("Initialized PaymentProviderService with active provider: [{}]", this.activeProviderName);
    }

    public PaymentProvider getActiveProvider() {
        PaymentProvider provider = providers.get(activeProviderName);
        if (provider == null) {
            log.warn("Active provider '{}' not found, defaulting to mock", activeProviderName);
            provider = providers.get("mock");
        }
        return provider;
    }
}
