package com.splitpay.split;

import com.splitpay.entity.SplittingStrategyType;
import com.splitpay.exception.PaymentValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSplitServiceTest {

    private PaymentSplitService splitService;
    private final BigDecimal defaultMax = new BigDecimal("1990.00");

    @BeforeEach
    void setUp() {
        MaxPartAmountSplitStrategy maxStrategy = new MaxPartAmountSplitStrategy();
        EqualSplitStrategy equalStrategy = new EqualSplitStrategy();
        CustomSplitStrategy customStrategy = new CustomSplitStrategy();

        SplitStrategyFactory factory = new SplitStrategyFactory(List.of(maxStrategy, equalStrategy, customStrategy));
        splitService = new PaymentSplitService(factory, defaultMax, SplittingStrategyType.MAX_PART_AMOUNT);
    }

    @Test
    @DisplayName("Splitting ₹5,000 with max ₹1,990 produces exact parts [₹1,990, ₹1,990, ₹1,020]")
    void split5000_WithDefaultMax_ProducesExpectedParts() {
        BigDecimal total = new BigDecimal("5000.00");
        List<BigDecimal> parts = splitService.split(total, SplittingStrategyType.MAX_PART_AMOUNT, defaultMax, null);

        assertThat(parts).hasSize(3);
        assertThat(parts.get(0)).isEqualByComparingTo("1990.00");
        assertThat(parts.get(1)).isEqualByComparingTo("1990.00");
        assertThat(parts.get(2)).isEqualByComparingTo("1020.00");

        BigDecimal sum = parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("Splitting ₹7,500 with max ₹1,990 produces exact parts [₹1,990, ₹1,990, ₹1,990, ₹1,530]")
    void split7500_WithDefaultMax_ProducesExpectedParts() {
        BigDecimal total = new BigDecimal("7500.00");
        List<BigDecimal> parts = splitService.split(total, SplittingStrategyType.MAX_PART_AMOUNT, defaultMax, null);

        assertThat(parts).hasSize(4);
        assertThat(parts.get(0)).isEqualByComparingTo("1990.00");
        assertThat(parts.get(1)).isEqualByComparingTo("1990.00");
        assertThat(parts.get(2)).isEqualByComparingTo("1990.00");
        assertThat(parts.get(3)).isEqualByComparingTo("1530.00");

        BigDecimal sum = parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(total);
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.00", "100.00", "1990.00", "1999.00", "2000.00", "2001.00", "5000.00", "10000.00", "50000.00", "125432.75"})
    @DisplayName("Every split amount mathematically preserves the exact total sum with zero money loss/gain")
    void splitAmounts_AlwaysPreserveExactTotal(String amountStr) {
        BigDecimal total = new BigDecimal(amountStr);
        List<BigDecimal> parts = splitService.split(total, SplittingStrategyType.MAX_PART_AMOUNT, defaultMax, null);

        assertThat(parts).isNotEmpty();
        for (BigDecimal part : parts) {
            assertThat(part).isGreaterThan(BigDecimal.ZERO);
            assertThat(part).isLessThanOrEqualTo(defaultMax);
        }

        BigDecimal sum = parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("Decimal amount ₹5,000.50 preserves paise correctly")
    void splitDecimalAmount_PreservesPaise() {
        BigDecimal total = new BigDecimal("5000.50");
        List<BigDecimal> parts = splitService.split(total, SplittingStrategyType.MAX_PART_AMOUNT, defaultMax, null);

        assertThat(parts).hasSize(3);
        assertThat(parts.get(0)).isEqualByComparingTo("1990.00");
        assertThat(parts.get(1)).isEqualByComparingTo("1990.00");
        assertThat(parts.get(2)).isEqualByComparingTo("1020.50");

        BigDecimal sum = parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("Rejects zero or negative amounts with PaymentValidationException")
    void rejectNonPositiveAmounts() {
        assertThatThrownBy(() -> splitService.split(BigDecimal.ZERO, SplittingStrategyType.MAX_PART_AMOUNT, defaultMax, null))
                .isInstanceOf(PaymentValidationException.class);

        assertThatThrownBy(() -> splitService.split(new BigDecimal("-50.00"), SplittingStrategyType.MAX_PART_AMOUNT, defaultMax, null))
                .isInstanceOf(PaymentValidationException.class);
    }

    @Test
    @DisplayName("Equal split divides amount into parts and preserves exact sum")
    void equalSplitStrategy_PreservesSum() {
        BigDecimal total = new BigDecimal("100.00");
        // Equal split into 3 parts
        List<BigDecimal> parts = splitService.split(total, SplittingStrategyType.EQUAL_SPLIT, new BigDecimal("35.00"), null);

        assertThat(parts).hasSize(3);
        BigDecimal sum = parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(total);
    }

    @Test
    @DisplayName("Custom split validates part sum equals total amount")
    void customSplit_ValidatesSum() {
        BigDecimal total = new BigDecimal("3000.00");
        List<BigDecimal> customParts = List.of(new BigDecimal("1000.00"), new BigDecimal("2000.00"));

        List<BigDecimal> result = splitService.split(total, SplittingStrategyType.CUSTOM_SPLIT, null, customParts);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualByComparingTo("1000.00");
        assertThat(result.get(1)).isEqualByComparingTo("2000.00");

        // Custom parts that don't match total should throw PaymentValidationException
        List<BigDecimal> mismatchedParts = List.of(new BigDecimal("1000.00"), new BigDecimal("1500.00"));
        assertThatThrownBy(() -> splitService.split(total, SplittingStrategyType.CUSTOM_SPLIT, null, mismatchedParts))
                .isInstanceOf(PaymentValidationException.class);
    }
}
