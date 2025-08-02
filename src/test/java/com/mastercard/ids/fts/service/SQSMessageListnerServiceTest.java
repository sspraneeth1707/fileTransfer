package com.mastercard.ids.fts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.mockito.Mockito.*;

class SQSMessageListnerServiceTest {

    @Mock
    private OutboundFileListProcessor outboundFileListProcessor;

    @InjectMocks
    private SQSMessageListnerService listenerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testReceiveMessage_validMessage_success() {
        // Arrange
        String message = """
        {
          "Records": [
            {
              "s3": {
                "object": {
                  "key": "test-key.txt",
                  "size": 1234
                }
              }
            }
          ]
        }
        """;

        // Act
        listenerService.receiveMessage(message);

        // Assert
        verify(outboundFileListProcessor, times(1))
                .processOutboundFiles("test-key.txt", 1234L);
    }

    @Test
    void testReceiveMessage_testEvent_ignored() {
        // Arrange
        String message = """
        {
          "Event": "s3:TestEvent"
        }
        """;

        // Act
        listenerService.receiveMessage(message);

        // Assert
        verifyNoInteractions(outboundFileListProcessor);
    }

    @Test
    void testReceiveMessage_missingRecords_noProcessing() {
        // Arrange
        String message = """
        {
          "MessageId": "abc-123"
        }
        """;

        // Act
        listenerService.receiveMessage(message);

        // Assert
        verifyNoInteractions(outboundFileListProcessor);
    }

    @Test
    void testReceiveMessage_malformedJson_logsError() {
        // Arrange: malformed JSON
        String message = "{ not-a-json }";

        // Act
        listenerService.receiveMessage(message);

        // Assert: error is logged, no call to processor
        verifyNoInteractions(outboundFileListProcessor);
    }

    @Test
    void testReceiveMessage_unexpectedError_logsError() {
        // Arrange: valid message, but processor throws
        String message = """
        {
          "Records": [
            {
              "s3": {
                "object": {
                  "key": "bad-key.txt",
                  "size": 9999
                }
              }
            }
          ]
        }
        """;

        doThrow(new RuntimeException("simulated failure"))
                .when(outboundFileListProcessor)
                .processOutboundFiles("bad-key.txt", 9999L);

        // Act
        listenerService.receiveMessage(message);

        // Assert
        verify(outboundFileListProcessor).processOutboundFiles("bad-key.txt", 9999L);
    }
}
