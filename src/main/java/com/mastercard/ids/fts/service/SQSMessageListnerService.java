package com.mastercard.ids.fts.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile("!local")
public class SQSMessageListnerService {

    private final OutboundFileListProcessor outboundFileListProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SqsListener(value = "${spring.cloud.aws.sqs.queue-name}", acknowledgementMode = "ON_SUCCESS")
    public void receiveMessage(String message) {
        try {
            log.info("Received message: " + message);
            JsonNode rootNode = objectMapper.readTree(message);

            if ("s3:TestEvent".equals(rootNode.path("Event").asText())) {
                return;
            }

            JsonNode recordsNode = rootNode.get("Records");
            List<JsonNode> recordList = new ArrayList<>();

            if (recordsNode != null && recordsNode.isArray()) {
                recordsNode.forEach(recordList::add);
            }
            recordList.forEach(record -> {
                JsonNode s3Node = record.path("s3");
                String objectKey = s3Node.path("object").path("key").asText();
                long fileSize = s3Node.path("object").path("size").asLong();
                log.info("S3 object available for transfer to NGFT : {} , File Size : {}", objectKey, fileSize);
                outboundFileListProcessor.processOutboundFiles(objectKey, fileSize);
            });

        } catch (IOException e) {
            log.error("Error processing message: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error: " + e.getMessage());
        }
    }

}
