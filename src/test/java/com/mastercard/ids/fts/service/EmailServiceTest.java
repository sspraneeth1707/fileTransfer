package com.mastercard.ids.fts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private SesClient sesClient;

    @InjectMocks
    private EmailService emailService;

    // Inject @Value fields manually since we're not running with Spring context
    private final String senderEmail = "sender@example.com";
    private final String receiverEmail = "receiver@example.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        emailService = new EmailService(sesClient);
        // manually set fields with reflection (or use setter injection in real code)
        injectValue(emailService, "senderEmail", senderEmail);
        injectValue(emailService, "receiverEmail", receiverEmail);
    }

    @Test
    void testSendEmail_success() {
        // Arrange
        String subject = "Test Subject";
        String body = "Test Body";

        SendEmailResponse mockResponse = SendEmailResponse.builder().messageId("abc-123").build();
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenReturn(mockResponse);

        // Act
        emailService.sendEmail(subject, body);

        // Assert
        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
    }

    @Test
    void testSendEmail_failure_logsError() {
        // Arrange
        String subject = "Test Subject";
        String body = "Test Body";

        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenThrow(SesException.builder().message("Simulated SES failure").build());

        // Act
        emailService.sendEmail(subject, body);

        // Assert
        verify(sesClient, times(1)).sendEmail(any(SendEmailRequest.class));
        // (Log output is not asserted here, but you can use LogCaptor or similar if needed)
    }

    // Utility to inject private field values using reflection
    private void injectValue(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
