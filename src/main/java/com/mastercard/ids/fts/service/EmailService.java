
package com.mastercard.ids.fts.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final SesClient sesClient;
    @Value("${aws.ses.sender-email:sender}")
    private String senderEmail;
    @Value("${aws.ses.receiver-email:receiver}")
    private String receiverEmail;

    public void sendEmail(String subject, String body) {
        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(receiverEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder().text(Content.builder().data(body).build()).build())
                            .build())
                    .source(senderEmail)
                    .build();

            sesClient.sendEmail(emailRequest);
            log.info("Email sent successfully to {}", receiverEmail);
        } catch (SesException e) {
            log.error("Error sending email: {}", e.getMessage());
        }
    }
}



