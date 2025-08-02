package com.mastercard.ids.fts.utils;

import com.mastercard.ids.fts.model.FileDownloadRequest;
import com.mastercard.ids.fts.model.FileInfo;
import com.mastercard.ids.fts.model.FileMetadata;
import com.mastercard.ids.fts.model.S3FileData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UtilsTest {

    @InjectMocks
    public Utils utils;

    @Test
    void testGetFileExtension() {
        assertEquals("txt", utils.getFileExtension("file.txt"));
        assertEquals("txt", utils.getFileExtension("file.upload.txt"));
        assertEquals("tar.gz", utils.getFileExtension("archive.tar.gz")); // Optional: if supporting compound extensions
        assertEquals("", utils.getFileExtension("file"));
        assertEquals("", utils.getFileExtension(".hiddenfile"));
        assertEquals("", utils.getFileExtension("noExtension."));
    }

    @Test
    void testGetFileMetadata_withFileInfo() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("file123");
        fileInfo.setName("test.txt");
        fileInfo.setChecksum("abcd1234");
        fileInfo.setContentType("text/plain");
        fileInfo.setSize(2048L);

        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setKey("test");
        fileMetadata.setValue("value");

        fileInfo.setMetadata(List.of(fileMetadata));

        FileDownloadRequest request = new FileDownloadRequest();
        request.setSender("Alice");
        request.setReceiver("Bob");
        request.setFileInfo(fileInfo);

        Map<String, String> metadata = utils.getFileMetadata(request);
        assertEquals("Alice", metadata.get("x-mc-sender"));
        assertEquals("Bob", metadata.get("x-mc-receiver"));
        assertEquals("file123", metadata.get("x-mc-file-id"));
        assertTrue(metadata.get("metaData").contains("key=test")); //File custom metadata is being stored as a string
    }

    @Test
    void testGetFileMetadata_withFileInfo_Null() {
        FileDownloadRequest request = new FileDownloadRequest();
        request.setSender("Alice");
        request.setReceiver("Bob");
        request.setFileInfo(null);

        Map<String, String> metadata = utils.getFileMetadata(request);
        assertEquals("Alice", metadata.get("x-mc-sender"));
        assertEquals("Bob", metadata.get("x-mc-receiver"));
        assertNull(metadata.get("x-mc-file-id"));
        assertNull(metadata.get("metaData"));
    }

    @Test
    void testGetFileMetadata_withMetaData_Null() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("file123");
        fileInfo.setName("test.txt");
        fileInfo.setChecksum("abcd1234");
        fileInfo.setContentType("text/plain");
        fileInfo.setSize(2048L);

        fileInfo.setMetadata(null);

        FileDownloadRequest request = new FileDownloadRequest();
        request.setSender("Alice");
        request.setReceiver("Bob");
        request.setFileInfo(fileInfo);

        Map<String, String> metadata = utils.getFileMetadata(request);
        assertEquals("Alice", metadata.get("x-mc-sender"));
        assertEquals("Bob", metadata.get("x-mc-receiver"));
        assertEquals("file123", metadata.get("x-mc-file-id"));
        assertNull(metadata.get("metaData"));
    }

    @Test
    void testGetDownloadFileHeader() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("file-id");
        fileInfo.setName("download.txt");
        fileInfo.setContentType("text/plain");

        FileDownloadRequest request = new FileDownloadRequest();
        request.setSender("SenderA");
        request.setReceiver("ReceiverB");
        request.setFileInfo(fileInfo);

        HttpHeaders headers = utils.getDownloadFileHeader(request);
        assertEquals("SenderA", headers.getFirst("x-mc-sender"));
        assertEquals("ReceiverB", headers.getFirst("x-mc-receiver"));
        assertEquals("file-id", headers.getFirst("x-mc-file-id"));
        assertEquals("text/plain", headers.getFirst("content-type"));
        assertEquals("download.txt", headers.getFirst("x-mc-fileName"));
    }

    @Test
    void testGetUploadFileHeader() {
        byte[] content = "Hello World".getBytes();
        Map<String, String> metadata = Map.of(
                "x-mc-sender", "Sender1",
                "x-mc-receiver", "Receiver1",
                "x-mc-file-name", "upload.txt",
                "x-mc-file-size", "1000",
                "content-type", "text/plain"
        );

        S3FileData fileData = new S3FileData();
        fileData.setMetadata(metadata);
        fileData.setFileContent(Optional.of(content));

        HttpHeaders headers = utils.getUploadFileHeader(fileData);
        assertEquals("Receiver1", headers.getFirst("x-mc-receiver"));
        assertEquals("upload.txt", headers.getFirst("x-mc-file-name"));
        assertEquals(DigestUtils.md5DigestAsHex(content), headers.getFirst("x-mc-checksum"));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, headers.getContentType());
    }

    @Test
    void testVerifyChecksum() {
        byte[] content = "TestContent".getBytes();
        String checksum = DigestUtils.md5DigestAsHex(content);
        assertTrue(utils.verifyChecksum(content, checksum));
        assertFalse(utils.verifyChecksum(content, "invalidChecksum"));
    }

    @Test
    void testGetS3FileNameKey() {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setId("abc123");
        fileInfo.setName("doc.pdf");

        FileDownloadRequest request = new FileDownloadRequest();
        request.setFileInfo(fileInfo);

        // Caution: dynamic time makes exact match tough — assert format or prefix instead
        String key = utils.getS3FileNamekey(request);
        assertTrue(key.contains("abc123"));
        assertTrue(key.endsWith(".pdf"));
    }
}

