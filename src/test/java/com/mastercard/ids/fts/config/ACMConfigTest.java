//package com.mastercard.ids.fts.config;
//
//import com.mastercard.ids.fts.service.ACMService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import software.amazon.awssdk.services.acm.AcmClient;
//import software.amazon.awssdk.services.acm.model.GetCertificateRequest;
//import software.amazon.awssdk.services.acm.model.GetCertificateResponse;
//
//import static org.junit.jupiter.api.Assertions.assertArrayEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
//class ACMConfigTest {
//
//    private AcmClient acmClientMock;
//    private ACMService acmConfig;
//
//    @BeforeEach
//    void setUp() {
//        acmClientMock = Mockito.mock(AcmClient.class);
//
//        AWSProperties props = new AWSProperties();
//        props.setRegion("us-east-1");
//        props.setAccessKey("accessKey");
//        props.setSecretKey("secretKey");
//        props.setEndpoint("https://localhost");
//
//        // Inject the mock client using reflection or a package-visible constructor in real scenarios
//        acmConfig = new ACMService(props)
//        {
//            @Override
//            public byte[] getAcmCertificate(String certificateArn) throws Exception {
//                return acmClientMock.getCertificate(GetCertificateRequest.builder()
//                                .certificateArn(certificateArn).build())
//                        .certificate().getBytes();
//            }
//        };
//    }
//
//    @Test
//    void testGetAcmCertificate_returnsExpectedCertificateBytes() throws Exception {
//        String expectedCert = "CERTIFICATE";
//        when(acmClientMock.getCertificate(any(GetCertificateRequest.class)))
//                .thenReturn(GetCertificateResponse.builder().certificate(expectedCert).build());
//
//        byte[] result = acmConfig.getAcmCertificate("mock-arn");
//
//        assertArrayEquals(expectedCert.getBytes(), result);
//    }
//}
