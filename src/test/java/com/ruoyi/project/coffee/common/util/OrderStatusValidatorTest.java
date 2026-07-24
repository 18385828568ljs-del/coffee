package com.ruoyi.project.coffee.common.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

class OrderStatusValidatorTest
{
    @Test
    void validateForCancelShouldAllowOnlyPendingOrder()
    {
        assertDoesNotThrow(() -> OrderStatusValidator.validateForCancel(OrderStatusValidator.STATUS_PENDING));

        assertServiceMessage("订单状态异常", () -> OrderStatusValidator.validateForCancel(null));
        assertServiceMessage("只能取消待支付订单",
            () -> OrderStatusValidator.validateForCancel(OrderStatusValidator.STATUS_PAID));
        assertServiceMessage("只能取消待支付订单",
            () -> OrderStatusValidator.validateForCancel(OrderStatusValidator.STATUS_SHIPPED));
        assertServiceMessage("只能取消待支付订单",
            () -> OrderStatusValidator.validateForCancel(OrderStatusValidator.STATUS_COMPLETED));
        assertServiceMessage("订单已取消",
            () -> OrderStatusValidator.validateForCancel(OrderStatusValidator.STATUS_CANCELLED));
    }

    @Test
    void validateForPayShouldAllowOnlyPendingOrder()
    {
        assertDoesNotThrow(() -> OrderStatusValidator.validateForPay(OrderStatusValidator.STATUS_PENDING));

        assertServiceMessage("订单状态异常", () -> OrderStatusValidator.validateForPay(null));
        assertServiceMessage("订单已支付",
            () -> OrderStatusValidator.validateForPay(OrderStatusValidator.STATUS_PAID));
        assertServiceMessage("订单已支付",
            () -> OrderStatusValidator.validateForPay(OrderStatusValidator.STATUS_SHIPPED));
        assertServiceMessage("订单已支付",
            () -> OrderStatusValidator.validateForPay(OrderStatusValidator.STATUS_COMPLETED));
        assertServiceMessage("订单已取消",
            () -> OrderStatusValidator.validateForPay(OrderStatusValidator.STATUS_CANCELLED));
        assertServiceMessage("仅未支付订单可以支付", () -> OrderStatusValidator.validateForPay(99));
    }

    @Test
    void validateForConfirmShouldAllowOnlyShippedOrder()
    {
        assertDoesNotThrow(() -> OrderStatusValidator.validateForConfirm(OrderStatusValidator.STATUS_SHIPPED));

        assertServiceMessage("只能确认已发货订单", () -> OrderStatusValidator.validateForConfirm(null));
        assertServiceMessage("只能确认已发货订单",
            () -> OrderStatusValidator.validateForConfirm(OrderStatusValidator.STATUS_PENDING));
        assertServiceMessage("只能确认已发货订单",
            () -> OrderStatusValidator.validateForConfirm(OrderStatusValidator.STATUS_PAID));
        assertServiceMessage("只能确认已发货订单",
            () -> OrderStatusValidator.validateForConfirm(OrderStatusValidator.STATUS_COMPLETED));
        assertServiceMessage("只能确认已发货订单",
            () -> OrderStatusValidator.validateForConfirm(OrderStatusValidator.STATUS_CANCELLED));
    }

    @Test
    void requireStatusShouldRejectNullOrDifferentStatusWithProvidedMessage()
    {
        assertDoesNotThrow(() -> OrderStatusValidator.requireStatus(2, 2, "状态不正确"));

        assertServiceMessage("状态不正确", () -> OrderStatusValidator.requireStatus(null, 2, "状态不正确"));
        assertServiceMessage("状态不正确", () -> OrderStatusValidator.requireStatus(1, 2, "状态不正确"));
    }

    private static void assertServiceMessage(String expectedMessage, ThrowingRunnable runnable)
    {
        ServiceException exception = assertThrows(ServiceException.class, runnable::run);
        assertEquals(expectedMessage, exception.getMessage());
    }

    private interface ThrowingRunnable
    {
        void run();
    }
}
