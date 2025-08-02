package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.*;
import com.mastercard.ids.fts.repository.FileRepository;
import com.mastercard.ids.fts.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InboundFileListProcessorTest {
    @Mock FileTransferServiceFactory fileTransferServiceFactory;
    @Mock FileRepository fileRepository;
    @Mock Utils utils;
    @Mock FileTransferService fileTransferService;
    @InjectMocks InboundFileListProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new InboundFileListProcessor(fileTransferServiceFactory, fileRepository, utils);
        // Set @Value fields via reflection
        setField("ftsReceiverLogicalAddress", "receiver");
        setField("ftsSenderLogicalAddress", "sender");
    }
    private void setField(String name, Object value) {
        try {
            var field = InboundFileListProcessor.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(processor, value);
        } catch (Exception ignored) {}
    }

    @Test
    void process_returnsEmpty_whenAllFilesProcessed() {
        FileInfo file = new FileInfo(); file.setId("1"); file.setName("file.csv");
        when(fileRepository.findExistingCompletedFileIds()).thenReturn(List.of("1"));
        List<FileInfo> result = processor.process("inv1", List.of(file));
        assertTrue(result.isEmpty());
    }

    @Test
    void process_returnsEmpty_whenNoValidFileTypes() {
        FileInfo file = new FileInfo(); file.setId("1"); file.setName("file.txt");
        when(fileRepository.findExistingCompletedFileIds()).thenReturn(List.of());
        when(utils.getFileExtension(any())).thenReturn("txt"); // Fix: mock file extension
        List<FileInfo> result = processor.process("inv1", List.of(file));
        assertTrue(result.isEmpty());
    }

    @Test
    void process_savesAndProcessesValidFiles() {
        FileInfo file = new FileInfo(); file.setId("1"); file.setName("file.csv"); file.setSize(10L); file.setCreatedDate(LocalDateTime.now().toString());
        when(fileRepository.findExistingCompletedFileIds()).thenReturn(List.of());
        when(utils.getFileExtension(any())).thenReturn("csv");
        when(fileRepository.saveAll(any())).thenReturn(List.of());
        InboundFile inboundFile = InboundFile.builder().fileId("1").fileName("file.csv").fileSize(10L).fileCreatedDate(LocalDateTime.now()).fileDownloadStatus("PENDING").fileDownloadTs(LocalDateTime.now()).build();
        when(fileRepository.findNotCompletedFiles()).thenReturn(List.of(inboundFile));
        when(fileTransferServiceFactory.getService(anyLong())).thenReturn(fileTransferService);
        when(fileTransferService.transferInbound(any())).thenReturn(CompletableFuture.completedFuture("ok"));
        List<FileInfo> result = processor.process("inv1", List.of(file));
        assertEquals(1, result.size());
    }

    @Test
    void filterProcessedFileIds_returnsOnlyNew() {
        List<String> ids = List.of("1", "2", "3");
        when(fileRepository.findExistingCompletedFileIds()).thenReturn(List.of("2"));
        List<String> result = invokeFilterProcessedFileIds(ids);
        assertEquals(List.of("1", "3"), result);
    }

    @Test
    void filterProcessedFileIds_returnsEmptyIfAllExist() {
        List<String> ids = List.of("1", "2");
        when(fileRepository.findExistingCompletedFileIds()).thenReturn(List.of("1", "2"));
        List<String> result = invokeFilterProcessedFileIds(ids);
        assertTrue(result.isEmpty());
    }

    @Test
    void isValidFileType_trueForCsvZipXlsx() throws Exception {
        when(utils.getFileExtension(any())).thenReturn("csv");
        assertTrue(invokeIsValidFileType("file.csv"));
        when(utils.getFileExtension(any())).thenReturn("zip");
        assertTrue(invokeIsValidFileType("file.zip"));
        when(utils.getFileExtension(any())).thenReturn("xlsx");
        assertTrue(invokeIsValidFileType("file.xlsx"));
    }

    @Test
    void isValidFileType_falseForOtherOrNull() throws Exception {
        when(utils.getFileExtension(any())).thenReturn("txt");
        assertFalse(invokeIsValidFileType("file.txt"));
        assertFalse(invokeIsValidFileType(null));
    }

    @Test
    void convertToFileInfo_mapsFieldsCorrectly() throws Exception {
        InboundFile file = InboundFile.builder().fileId("id").fileName("name").fileSize(5L).fileCreatedDate(LocalDateTime.now()).fileChecksum("chk").fileDownloadStatus("PENDING").fileDownloadTs(LocalDateTime.now()).contentType("ct").fileProfile("profile").build();
        FileInfo info = invokeConvertToFileInfo(file);
        assertEquals("id", info.getId());
        assertEquals("name", info.getName());
        assertEquals(5L, info.getSize());
        assertEquals("chk", info.getChecksum());
        assertEquals("ct", info.getContentType());
        assertEquals("profile", info.getFileProfile());
    }

    @Test
    void parseToDateTime_parsesValid() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime parsed = invokeParseToDateTime(now.toString());
        assertEquals(now.getYear(), parsed.getYear());
    }

    @Test
    void parseToDateTime_returnsNowIfNull() throws Exception {
        LocalDateTime parsed = invokeParseToDateTime(null);
        assertNotNull(parsed);
    }

    @Test
    void parseToDateTime_throwsForInvalid() throws Exception {
        assertThrows(Exception.class, () -> invokeParseToDateTime("not-a-date"));
    }

    // Reflection helpers for private methods
    private List<String> invokeFilterProcessedFileIds(List<String> ids) {
        try {
            var m = InboundFileListProcessor.class.getDeclaredMethod("filterProcessedFileIds", List.class);
            m.setAccessible(true);
            return (List<String>) m.invoke(processor, ids);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private boolean invokeIsValidFileType(String name) {
        try {
            var m = InboundFileListProcessor.class.getDeclaredMethod("isValidFileType", String.class);
            m.setAccessible(true);
            return (boolean) m.invoke(processor, name);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private FileInfo invokeConvertToFileInfo(InboundFile file) {
        try {
            var m = InboundFileListProcessor.class.getDeclaredMethod("convertToFileInfo", InboundFile.class);
            m.setAccessible(true);
            return (FileInfo) m.invoke(processor, file);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private LocalDateTime invokeParseToDateTime(String s) {
        try {
            var m = InboundFileListProcessor.class.getDeclaredMethod("parseToDateTime", String.class);
            m.setAccessible(true);
            return (LocalDateTime) m.invoke(processor, s);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
