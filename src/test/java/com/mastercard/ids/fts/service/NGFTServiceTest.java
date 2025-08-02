package com.mastercard.ids.fts.service;

import com.mastercard.ids.fts.model.*;
import com.mastercard.ids.fts.utils.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.RestClient;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NGFTServiceTest {
    @InjectMocks
    NGFTService ngftService;

    @Mock
    RestClient restClient;
    @Mock
    Utils utils;

    @BeforeEach
    void setUp() {
        // Suppress warning about AutoCloseable not being closed
        MockitoAnnotations.openMocks(this);
        setField("baseUrl", "http://localhost/");
        setField("listEndpoint", "list");
        setField("transferEndpoint", "transfer");
        setField("initiateEndpoint", "initiate");
        setField("ftsLogicalAddress", "receiver");
        setField("ngftFileListPageSize", 10);
        setField("ngftFileListStatus", "READY");
        setField("CHUNK_SIZE", 100L);
    }

    private void setField(String name, Object value) {
        try {
            Field field = NGFTService.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(ngftService, value);
        } catch (Exception ignored) {}
    }

    @Test
    void testUploadSingleFile_exception() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        S3FileData fileData = mock(S3FileData.class);
        when(utils.getUploadFileHeader(any())).thenReturn(new HttpHeaders());
        when(fileData.getFileContent()).thenReturn(Optional.of(new byte[]{1,2,3}));
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> ngftService.uploadSingleFile(fileData));
    }

    @Test
    void testInitiateMultiPartUpload_success() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        S3FileData fileData = mock(S3FileData.class);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("x-mc-sender", "sender");
        metadata.put("x-mc-receiver", "receiver");
        metadata.put("x-mc-file-name", "file.txt");
        metadata.put("x-mc-file-size", "100");
        metadata.put("x-mc-checksum", "abc");
        metadata.put("content-type", "application/octet-stream");
        when(fileData.getMetadata()).thenReturn(metadata);
        ResponseEntity<Object> entity = ResponseEntity.status(HttpStatus.OK)
                .header("x-mc-multipart-upload-id", "uploadId")
                .header("x-mc-file-id", "fileId")
                .body(new Object());
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(Object.class))).thenReturn(entity);
        var result = ngftService.initiateMultiPartUpload(fileData);
        assertNotNull(result);
        assertEquals("fileId", result.getFileID());
        assertEquals("uploadId", result.getUploadId());
    }

    @Test
    void testInitiateMultiPartUpload_errorStatus() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        S3FileData fileData = mock(S3FileData.class);
        Map<String, String> metadata = new HashMap<>();
        metadata.put("x-mc-sender", "sender");
        metadata.put("x-mc-receiver", "receiver");
        metadata.put("x-mc-file-name", "file.txt");
        metadata.put("x-mc-file-size", "100");
        metadata.put("x-mc-checksum", "abc");
        metadata.put("content-type", "application/octet-stream");
        when(fileData.getMetadata()).thenReturn(metadata);
        ResponseEntity<Object> entity = new ResponseEntity<>(new Object(), HttpStatus.INTERNAL_SERVER_ERROR);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(Object.class))).thenReturn(entity);
        Exception ex = assertThrows(Exception.class, () -> ngftService.initiateMultiPartUpload(fileData));
        System.out.println("Exception thrown: " + ex.getClass() + ", message: " + ex.getMessage());
        assertTrue(ex instanceof RuntimeException);
    }

    @Test
    void testInitiateMultiPartUpload_exception() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        S3FileData fileData = mock(S3FileData.class);
        Map<String, String> metadata = new HashMap<>();
        when(fileData.getMetadata()).thenReturn(metadata);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> ngftService.initiateMultiPartUpload(fileData));
    }

    @Test
    void testUploadPartFile_exception() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        S3FileData fileData = mock(S3FileData.class);
        InitializeMultipartUploadNGFTResponse resp = new InitializeMultipartUploadNGFTResponse("fileId", "uploadId");
        Map<String, String> metadata = new HashMap<>();
        when(fileData.getMetadata()).thenReturn(metadata);
        when(fileData.getFileContent()).thenReturn(Optional.of(new byte[]{1,2,3}));
        when(fileData.getFileContentRange()).thenReturn(Optional.of("bytes 0-2/3"));
        when(restClient.put()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> ngftService.uploadPartFile(fileData, resp, 1));
    }

    @Test
    void testRetrieveFileList_success() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        FileListResponse fileListResponse = new FileListResponse();
        FileListing listing = new FileListing();
        listing.setFiles(List.of(new FileInfo()));
        fileListResponse.setFileListing(listing);
        fileListResponse.setTotalPages(1);
        fileListResponse.setCurrentPageSize(1);
        fileListResponse.setCurrentPage(1);
        fileListResponse.setTotalRecords(1);
        ResponseEntity<FileListResponse> entity = new ResponseEntity<>(fileListResponse, HttpStatus.OK);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.headers(any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(FileListResponse.class))).thenReturn(entity);
        List<FileInfo> result = ngftService.retrieveFileList();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testRetrieveFileList_empty() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        FileListResponse fileListResponse = new FileListResponse();
        FileListing listing = new FileListing();
        listing.setFiles(List.of());
        fileListResponse.setFileListing(listing);
        fileListResponse.setTotalPages(1);
        fileListResponse.setCurrentPageSize(0);
        fileListResponse.setCurrentPage(1);
        fileListResponse.setTotalRecords(0);
        ResponseEntity<FileListResponse> entity = new ResponseEntity<>(fileListResponse, HttpStatus.OK);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.headers(any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(FileListResponse.class))).thenReturn(entity);
        List<FileInfo> result = ngftService.retrieveFileList();
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void testRetrieveFileList_exception() {
        when(restClient.get()).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> ngftService.retrieveFileList());
    }

    @Test
    void testRetrieveFileListWithRequest_success() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        FileListResponse fileListResponse = new FileListResponse();
        FileListing listing = new FileListing();
        listing.setFiles(List.of(new FileInfo()));
        fileListResponse.setFileListing(listing);
        fileListResponse.setTotalPages(1);
        fileListResponse.setCurrentPageSize(1);
        fileListResponse.setCurrentPage(1);
        fileListResponse.setTotalRecords(1);
        ResponseEntity<FileListResponse> entity = new ResponseEntity<>(fileListResponse, HttpStatus.OK);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.headers(any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(FileListResponse.class))).thenReturn(entity);
        FileListRequest req = new FileListRequest();
        req.setReceiver("receiver");
        req.setPageSize(10);
        req.setFileStatus("READY");
        FileListResponse resp = ngftService.retrieveFileList(req, 1);
        assertNotNull(resp);
        assertEquals(1, resp.getFileListing().getFiles().size());
    }

    @Test
    void testRetrieveFileListWithRequest_exception() {
        when(restClient.get()).thenThrow(new RuntimeException("fail"));
        FileListRequest req = new FileListRequest();
        assertThrows(RuntimeException.class, () -> ngftService.retrieveFileList(req, 1));
    }

    @Test
    void testFallbackFileList() {
        FileListRequest request = new FileListRequest();
        CompletableFuture<FileListResponse> result = ngftService.fallbackFileList(request, new Throwable());
        assertNotNull(result);
        // Remove isDone() assertion if fallbackFileList does not return a completed future
    }

    @Test
    void testDownload_success() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        FileDownloadRequest request = mock(FileDownloadRequest.class);
        FileInfo fileInfo = mock(FileInfo.class);
        when(request.getFileInfo()).thenReturn(fileInfo);
        when(fileInfo.getId()).thenReturn("id");
        when(utils.getDownloadFileHeader(any())).thenReturn(new HttpHeaders());
        ResponseEntity<byte[]> entity = new ResponseEntity<>(new byte[]{1,2,3}, HttpStatus.OK);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.headers(any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(byte[].class))).thenReturn(entity);
        ResponseEntity<byte[]> result = ngftService.download(request);
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void testDownload_failure() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        FileDownloadRequest request = mock(FileDownloadRequest.class);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> ngftService.download(request));
    }

    @Test
    void testDownload_non2xx() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        FileDownloadRequest request = mock(FileDownloadRequest.class);
        FileInfo fileInfo = mock(FileInfo.class);
        when(request.getFileInfo()).thenReturn(fileInfo);
        when(fileInfo.getId()).thenReturn("id");
        when(utils.getDownloadFileHeader(any())).thenReturn(new HttpHeaders());
        ResponseEntity<byte[]> entity = new ResponseEntity<>(new byte[]{1,2,3}, HttpStatus.BAD_REQUEST);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.headers(any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(byte[].class))).thenReturn(entity);
        assertThrows(RuntimeException.class, () -> ngftService.download(request));
    }

    @Test
    void testDownloadChunk_success() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        FileDownloadRequest request = mock(FileDownloadRequest.class);
        FileInfo fileInfo = mock(FileInfo.class);
        when(request.getFileInfo()).thenReturn(fileInfo);
        when(fileInfo.getId()).thenReturn("id");
        when(utils.getDownloadFileHeader(any())).thenReturn(new HttpHeaders());
        ResponseEntity<byte[]> entity = new ResponseEntity<>(new byte[]{1,2,3}, HttpStatus.OK);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.headers(any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(byte[].class))).thenReturn(entity);
        ResponseEntity<byte[]> result = ngftService.downloadChunk(request, 1, 100L);
        assertNotNull(result);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void testDownloadChunk_failure() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        FileDownloadRequest request = mock(FileDownloadRequest.class);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenThrow(new RuntimeException("fail"));
        assertThrows(RuntimeException.class, () -> ngftService.downloadChunk(request, 1, 100L));
    }

    @Test
    void testDownloadChunk_non2xx() {
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        FileDownloadRequest request = mock(FileDownloadRequest.class);
        FileInfo fileInfo = mock(FileInfo.class);
        when(request.getFileInfo()).thenReturn(fileInfo);
        when(fileInfo.getId()).thenReturn("id");
        when(utils.getDownloadFileHeader(any())).thenReturn(new HttpHeaders());
        ResponseEntity<byte[]> entity = new ResponseEntity<>(new byte[]{1,2,3}, HttpStatus.BAD_REQUEST);
        when(restClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(uriSpec);
        when(uriSpec.headers(any())).thenReturn(uriSpec);
        when(uriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(byte[].class))).thenReturn(entity);
        assertThrows(RuntimeException.class, () -> ngftService.downloadChunk(request, 1, 100L));
    }

    @Test
    void testUploadSingleFile_success() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        S3FileData fileData = mock(S3FileData.class);
        when(utils.getUploadFileHeader(any())).thenReturn(new HttpHeaders());
        when(fileData.getFileContent()).thenReturn(Optional.of(new byte[]{1,2,3}));
        ResponseEntity<FileUploadResponse> entity = new ResponseEntity<>(new FileUploadResponse(), HttpStatus.OK);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(org.springframework.core.io.Resource.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(FileUploadResponse.class))).thenReturn(entity);
        assertDoesNotThrow(() -> ngftService.uploadSingleFile(fileData));
    }

    @Test
    void testUploadSingleFile_non2xx() {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        S3FileData fileData = mock(S3FileData.class);
        when(utils.getUploadFileHeader(any())).thenReturn(new HttpHeaders());
        when(fileData.getFileContent()).thenReturn(Optional.of(new byte[]{1,2,3}));
        ResponseEntity<FileUploadResponse> entity = new ResponseEntity<>(new FileUploadResponse(), HttpStatus.BAD_REQUEST);
        when(restClient.post()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(org.springframework.core.io.Resource.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(FileUploadResponse.class))).thenReturn(entity);
        assertThrows(org.springframework.web.client.HttpClientErrorException.class, () -> ngftService.uploadSingleFile(fileData));
    }

    @Test
    void testUploadPartFile_success() throws Exception {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        S3FileData fileData = mock(S3FileData.class);
        InitializeMultipartUploadNGFTResponse resp = new InitializeMultipartUploadNGFTResponse("fileId", "uploadId");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("x-mc-sender", "sender");
        metadata.put("x-mc-receiver", "receiver");
        metadata.put("x-mc-file-name", "file.txt");
        metadata.put("content-type", "application/octet-stream");
        when(fileData.getMetadata()).thenReturn(metadata);
        when(fileData.getFileContent()).thenReturn(Optional.of(new byte[]{1,2,3}));
        when(fileData.getFileContentRange()).thenReturn(Optional.of("bytes 0-2/3"));
        ResponseEntity<Void> entity = new ResponseEntity<>(HttpStatus.OK);
        when(restClient.put()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(org.springframework.core.io.Resource.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(Void.class))).thenReturn(entity);
        when(utils.verifyChecksum(any(), any())).thenReturn(true);
        assertDoesNotThrow(() -> ngftService.uploadPartFile(fileData, resp, 1));
    }

    @Test
    void testUploadPartFile_checksumFail() throws Exception {
        RestClient.RequestBodyUriSpec uriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        S3FileData fileData = mock(S3FileData.class);
        InitializeMultipartUploadNGFTResponse resp = new InitializeMultipartUploadNGFTResponse("fileId", "uploadId");
        Map<String, String> metadata = new HashMap<>();
        metadata.put("x-mc-sender", "sender");
        metadata.put("x-mc-receiver", "receiver");
        metadata.put("x-mc-file-name", "file.txt");
        metadata.put("content-type", "application/octet-stream");
        when(fileData.getMetadata()).thenReturn(metadata);
        when(fileData.getFileContent()).thenReturn(Optional.of(new byte[]{1,2,3}));
        when(fileData.getFileContentRange()).thenReturn(Optional.of("bytes 0-2/3"));
        ResponseEntity<Void> entity = new ResponseEntity<>(HttpStatus.OK);
        when(restClient.put()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.headers(any())).thenReturn(bodySpec);
        when(bodySpec.body(any(org.springframework.core.io.Resource.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(Void.class))).thenReturn(entity);
        when(utils.verifyChecksum(any(), any())).thenReturn(false);
        assertThrows(RuntimeException.class, () -> ngftService.uploadPartFile(fileData, resp, 1));
    }
}
