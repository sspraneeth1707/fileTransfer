package com.mastercard.ids.fts.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTransferServiceFactoryTest {

    private FileTransferService smallFileTransferService;
    private FileTransferService largeFileTransferService;
    private FileTransferServiceFactory factory;

    @BeforeEach
    void setUp() {
        smallFileTransferService = Mockito.mock(FileTransferService.class);
        largeFileTransferService = Mockito.mock(FileTransferService.class);

        factory = new FileTransferServiceFactory(smallFileTransferService, largeFileTransferService);
        // Manually inject @Value field
        injectMinimumFileSize(factory, 1024L); // e.g., 1 KB
    }

    @Test
    void testGetService_smallFileSize_returnsSmallFileTransferService() {
        long fileSize = 512L;
        FileTransferService service = factory.getService(fileSize);
        assertEquals(smallFileTransferService, service);
    }

    @Test
    void testGetService_exactMinimumSize_returnsSmallFileTransferService() {
        long fileSize = 1024L;
        FileTransferService service = factory.getService(fileSize);
        assertEquals(smallFileTransferService, service);
    }

    @Test
    void testGetService_largeFileSize_returnsLargeFileTransferService() {
        long fileSize = 2048L;
        FileTransferService service = factory.getService(fileSize);
        assertEquals(largeFileTransferService, service);
    }

    // Utility to inject @Value field using reflection
    private void injectMinimumFileSize(FileTransferServiceFactory factory, long minimumSize) {
        try {
            var field = FileTransferServiceFactory.class.getDeclaredField("minimumFileSize");
            field.setAccessible(true);
            field.setLong(factory, minimumSize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
