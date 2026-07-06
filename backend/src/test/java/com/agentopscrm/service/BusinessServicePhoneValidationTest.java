package com.agentopscrm.service;

import com.agentopscrm.entity.Business;
import com.agentopscrm.repository.AgentLogRepository;
import com.agentopscrm.repository.BusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for BusinessService phone validation bug fix.
 * 
 * Bug: Creating a business without entering a phone still displays "Invalid phone format"
 * Fix: Phone is optional. Null, undefined, empty string and whitespace-only values must be valid.
 *      Blank phone values are normalized to null before submitting.
 */
@ExtendWith(MockitoExtension.class)
class BusinessServicePhoneValidationTest {

    @Mock
    private BusinessRepository businessRepository;

    @Mock
    private AgentLogRepository agentLogRepository;

    private BusinessService businessService;

    @BeforeEach
    void setUp() {
        businessService = new BusinessService(businessRepository, agentLogRepository);
    }

    @Test
    void createBusiness_withNullPhone_succeeds() {
        when(businessRepository.existsByWebsiteUrl(anyString())).thenReturn(false);
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));

        Business result = businessService.createBusiness(
            "Test Business",
            "https://test.com",
            "Tech",
            "Test description",
            "test@example.com",
            null
        );

        assertNotNull(result);
        assertNull(result.getContactPhone(), "Null phone should remain null");
        verify(businessRepository).save(any(Business.class));
    }

    @Test
    void createBusiness_withEmptyPhone_normalizesToNull() {
        when(businessRepository.existsByWebsiteUrl(anyString())).thenReturn(false);
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));

        Business result = businessService.createBusiness(
            "Test Business",
            "https://test.com",
            "Tech",
            "Test description",
            "test@example.com",
            ""
        );

        assertNotNull(result);
        assertNull(result.getContactPhone(), "Empty phone should be normalized to null");
        verify(businessRepository).save(any(Business.class));
    }

    @Test
    void createBusiness_withWhitespaceOnlyPhone_normalizesToNull() {
        when(businessRepository.existsByWebsiteUrl(anyString())).thenReturn(false);
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));

        Business result = businessService.createBusiness(
            "Test Business",
            "https://test.com",
            "Tech",
            "Test description",
            "test@example.com",
            "   "
        );

        assertNotNull(result);
        assertNull(result.getContactPhone(), "Whitespace-only phone should be normalized to null");
        verify(businessRepository).save(any(Business.class));
    }

    @Test
    void createBusiness_withValidInternationalPhone_succeeds() {
        when(businessRepository.existsByWebsiteUrl(anyString())).thenReturn(false);
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));

        String validPhone = "+91 98765 43210";
        Business result = businessService.createBusiness(
            "Test Business",
            "https://test.com",
            "Tech",
            "Test description",
            "test@example.com",
            validPhone
        );

        assertNotNull(result);
        assertEquals(validPhone, result.getContactPhone());
        verify(businessRepository).save(any(Business.class));
    }

    @Test
    void createBusiness_withValidUSPhone_succeeds() {
        when(businessRepository.existsByWebsiteUrl(anyString())).thenReturn(false);
        when(businessRepository.save(any(Business.class))).thenAnswer(inv -> inv.getArgument(0));

        String validPhone = "+1 (555) 123-4567";
        Business result = businessService.createBusiness(
            "Test Business",
            "https://test.com",
            "Tech",
            "Test description",
            "test@example.com",
            validPhone
        );

        assertNotNull(result);
        assertEquals(validPhone, result.getContactPhone());
        verify(businessRepository).save(any(Business.class));
    }
}
