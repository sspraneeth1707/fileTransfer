package com.mastercard.ids.fts.service;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class MeterRegistryService {


    private static final Meter meter = GlobalOpenTelemetry.getMeter("com.mastercard.ids.fts");

    private static final LongCounter counter =
            meter
                    .counterBuilder("fts.outbound.files.total")
                    .setDescription("Counts the number of outbound files")
                    .build();


    public void recordOutboundFileProcessed() {
        try {
            counter.add(
                    1,
                    Attributes.of(
                            AttributeKey.stringKey("client.name"), "FTSService")
            );

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }
}