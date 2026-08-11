package com.quantrity.antscaledisplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationRepositoryTest {
    @Test
    public void acceptsOnlyFreshCodesForAnActiveRequest() {
        NotificationRepository repository = new NotificationRepository();
        List<String> observed = new ArrayList<>();
        NotificationRepository.MfaRequest request =
                repository.registerMfaRequest(100, observed::add);

        try {
            assertFalse(repository.postMfaCode("123456", 99));
            assertFalse(repository.postMfaCode("invalid", 100));
            assertTrue(repository.postMfaCode("123456", 100));
            assertEquals(Collections.singletonList("123456"), observed);
        } finally {
            request.close();
        }
    }

    @Test
    public void consumesOnlyTheFirstCode() {
        NotificationRepository repository = new NotificationRepository();
        List<String> observed = new ArrayList<>();
        NotificationRepository.MfaRequest request =
                repository.registerMfaRequest(100, observed::add);

        try {
            assertTrue(repository.postMfaCode("123456", 101));
            assertFalse(repository.postMfaCode("654321", 102));
            assertEquals(Collections.singletonList("123456"), observed);
        } finally {
            request.close();
        }
    }

    @Test
    public void closingOrReplacingARequestStopsDeliveryToIt() {
        NotificationRepository repository = new NotificationRepository();
        List<String> firstObserved = new ArrayList<>();
        List<String> secondObserved = new ArrayList<>();
        NotificationRepository.MfaRequest first =
                repository.registerMfaRequest(100, firstObserved::add);
        NotificationRepository.MfaRequest second =
                repository.registerMfaRequest(200, secondObserved::add);

        first.close();
        try {
            assertFalse(repository.postMfaCode("123456", 199));
            assertTrue(repository.postMfaCode("654321", 201));
            assertTrue(firstObserved.isEmpty());
            assertEquals(Collections.singletonList("654321"), secondObserved);
        } finally {
            second.close();
        }

        NotificationRepository.MfaRequest closed =
                repository.registerMfaRequest(300, firstObserved::add);
        closed.close();
        assertFalse(repository.postMfaCode("123456", 301));
        assertTrue(firstObserved.isEmpty());
    }
}
