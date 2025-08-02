package com.mastercard.ids.fts;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class SampleFileGenerator {
    private static final Map<String, List<String>> statesByCountry = new HashMap<>();
    private static String[] countries = new String[] {"CA", "IN", "US"};
    private static String[] userAgents = new String[] {"IOS", "Chrome", "Microsoft Edge", "Mozilla", "Firefox"};
    static {
        statesByCountry.put("US", Arrays.asList("CA", "NY", "TX", "FL", "IL"));
        statesByCountry.put("CA", Arrays.asList("ON", "QC", "BC", "AB", "MB"));
        statesByCountry.put("IN", Arrays.asList("MH", "DL", "KA", "TN", "UP"));
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> row = new TreeMap<>();
//        String r = "%s,%s,2,USD,%s,%s,%s,%s,255.255.255.255,,e1af96a2e8af1115,wg1-ce04533340ea94cf,-5:00UTC,1792x828,TRUE,en,24,Google Inc,Unknown,2024.01,Unknown,Unknown,Unknown,Unknown,Unknown,Unknown,2024.02,2024.01,860,Verizon USA Inc,12345667,12314342342,test,test,test,test,d1e8a70b5ccab1dc2f56bbf7e99f064a660c08e361a3,123456789,TRUE,,,,John William Test,John William Test jr,john.william.1984@mastercard.com,john.william.1984@mastercard.com,16367220000,16367220000,16367220000,1111222233334444,4444,1250,111122,1,6367220000,1,6367220000,1,6367220000,2200 Mastercard Blvd,Test 123,O’Fallon,MO,63368,USA,2200 Mastercard Blvd,Test 123,O’Fallon,MO,63368,USA \n";
//        String h = "purchaseDate,purchaseAmount,purchaseCurrencyExponent,purchaseCurrency,cardHolderName,merchantName,merchantCountryCode,mcc,ipAddress,userAgent,canvas,webGl,timeZone,screenResolution,localStorage,language,colorDepth,buildManufacturer,placement,androidBuildVersion,androidSdkInt,buildFingerprint,buildBrand,supportedFeatures,model,sdkUserAgent,majorOsVersion,minorOsVersion,simCountryIso,simOperator,primaryStorage,systemMemory,buildProduct,vendorAppId,storedDeviceId,androidId,hashedAccountId,endCustomerRefId,cookiesEnabled,plugins,acquirerMerchantId,acquirerBin,cardHolderName,secondaryCardholderName,primaryEmail,secondaryEmail,homePhone,workPhone,mobilePhone,accountNumber,cardLast4,cardExpiryDate,cardBin,homephone.cc,homephone.subscriber,workphone.cc,workphone.subscriber,mobilephone.cc,mobilephone.subscriber,shipping.addressLine1,shipping.addressLine2,shipping.addressCity,shipping.addressState,shipping..addressPostCode,shipping.addressCountry,billing.addressLine1,billing.addressLine2,billing.addressCity,billing.addressState,billing.addressPostCode,billing.addressCountry \n";

        File f = new File("FDS-Test-File-1GB.csv");
        FileOutputStream fos = new FileOutputStream(f);
        fos.write(getHeaderRow().getBytes());
        for (int i=0; i < 6000000; i++) {
            String r = getDataRow();
//            String r1 = String.format(r, purchaseDate, purchaseAmt, cardHolderName, merchantName, merchantCountry, mcc);
            fos.write(r.getBytes());
        }
        fos.flush();
        fos.close();
        System.out.println("Created a file @" + f.getAbsolutePath());
    }

    private static String getMCC() {
        int mcc = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.valueOf(mcc);
    }

    private static String getCountryCode() {
        int c = ThreadLocalRandom.current().nextInt(0, 2);
        return countries[c];
//        String[] countryCodes = Locale.getISOCountries();
//        Random random = new Random();
//        String randomCountryCode = countryCodes[random.nextInt(countryCodes.length)];
//        return randomCountryCode;
    }

    private static String generateName(int length) {
        char[] alphabetsS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        char[] alphabetsL = "abcdefghijklmnopqrstuvwxyz".toUpperCase().toCharArray();

        StringBuilder sb = new StringBuilder();
        int l = ThreadLocalRandom.current().nextInt(0, 25);
        sb.append(alphabetsL[l]);

        for (int i=0; i < length; i++) {
            int s = ThreadLocalRandom.current().nextInt(0, 25);
            sb.append(alphabetsS[s]);
            if (i == (length/2)) {
                sb.append(" ");
                l = ThreadLocalRandom.current().nextInt(0, 25);
                sb.append(alphabetsL[l]);
            }
        }
        return sb.toString();
    }

    private static String generateUnknown(int length) {
        char[] alphabetsL = "abcdefghijklmnopqrstuvwxyz".toUpperCase().toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i < length; i++) {
            int l = ThreadLocalRandom.current().nextInt(0, 25);
            sb.append(alphabetsL[l]);
        }
        return sb.toString();
    }

    private static String getDataRow() {
        StringBuilder sb = new StringBuilder();
        Map<String, String> row = getRow();
        row.entrySet().forEach(entry -> {
            sb.append(entry.getValue()).append(",");
        });
        sb.append("\n");
        return sb.toString();
    }
    private static String getHeaderRow() {
        StringBuilder sb = new StringBuilder();
        Map<String, String> row = getRow();
        row.entrySet().forEach(entry -> {
            sb.append(entry.getKey()).append(",");
        });
        sb.append("\n");
        return sb.toString();
    }
    private static String getCurrencyCode() {
        Set<Currency> currencies = Currency.getAvailableCurrencies();
        List<Currency> currencyList = new ArrayList<>(currencies);
        Random random = new Random();
        Currency randomCurrency = currencyList.get(random.nextInt(currencyList.size()));
        return randomCurrency.getCurrencyCode();
    }
    private static Map<String, String> getRow() {
        int date = ThreadLocalRandom.current().nextInt(1, 28);
        int month = ThreadLocalRandom.current().nextInt(1, 12);
        int year = ThreadLocalRandom.current().nextInt(2020, 2025);

        String purchaseDate = String.format("%s/%s/%s", month, date, year);
        int purchaseAmt = ThreadLocalRandom.current().nextInt(1, 100000);

        Map<String, String> row = new TreeMap<>();
        row.put("purchaseDate", purchaseDate);
        row.put("purchaseAmount", String.valueOf(purchaseAmt));
        row.put("purchaseCurrencyExponent", "2");
        row.put("purchaseCurrency", getCurrencyCode());
        row.put("cardHolderName", generateName(30));
        row.put("merchantName", generateName(35));
        row.put("merchantCountryCode", getCountryCode());
        row.put("mcc", getMCC());
        row.put("ipAddress", "255.255.255.255");
        row.put("userAgent", getUserAgent());
        row.put("canvas", "e1af96a2e8af1115");
        row.put("webGl", "wg1-ce04533340ea94cf");
        row.put("timeZone", getTimezone());//"-5:00UTC");
        row.put("screenResolution", "1792x828");
        row.put("localStorage","TRUE");
        row.put("language","en");
        row.put("colorDepth","24");
        row.put("buildManufacturer","Google Inc");
        row.put("placement","Unknown");
        row.put("androidBuildVersion","2024.01");
        row.put("androidSdkInt", getUnknown());
        row.put("buildFingerprint", getUnknown());
        row.put("buildBrand", getUnknown());
        row.put("supportedFeatures", getUnknown());
        row.put("model", getUnknown());
        row.put("sdkUserAgent", getUnknown());
        row.put("majorOsVersion", getUnknown());
        row.put("minorOsVersion", getUnknown());
        row.put("simCountryIso", getUnknown());
        row.put("simOperator", getUnknown());
        row.put("primaryStorage", getUnknown());
        row.put("systemMemory", getUnknown());
        row.put("buildProduct", getUnknown());
        row.put("vendorAppId", getUnknown());
        row.put("storedDeviceId", getUnknown());
        row.put("androidId", getUnknown());
        row.put("hashedAccountId", getUnknown());
        row.put("endCustomerRefId", getUnknown());
        row.put("cookiesEnabled", getUnknown());
        row.put("plugins", getUnknown());
        row.put("acquirerMerchantId", "");
        row.put("acquirerBin", "");
        row.put("secondaryCardholderName", generateName(20));
        row.put("primaryEmail", getEmail());
        row.put("secondaryEmail", getEmail());
        row.put("accountNumber","");
        row.put("cardLast4","");
        row.put("cardExpiryDate","");
        row.put("cardBin","");
        row.put("homephone.cc", getPhone_CountryCode());
        row.put("homephone.subscriber", getPhone());
        row.put("workphone.cc", getPhone_CountryCode());
        row.put("workphone.subscriber", getPhone());
        row.put("mobilephone.cc", getPhone_CountryCode());
        row.put("mobilephone.subscriber", getPhone());

        String shippingAddressCC = getCountryCode();
        row.put("shipping.addressLine1", getHouseNumber() + " Mastercard Blvd");
        row.put("shipping.addressLine2", "Apt# 123");
        row.put("shipping.addressCity", "O’Fallon");
        row.put("shipping.addressState", getRandomState(shippingAddressCC));
        row.put("shipping.addressPostCode", getZipcode());
        row.put("shipping.addressCountry", shippingAddressCC);

        String billingAddressCC = getCountryCode();
        row.put("billing.addressLine1", getHouseNumber() + " Mastercard Blvd");
        row.put("billing.addressLine2","Apt# 123");
        row.put("billing.addressCity","O’Fallon");
        row.put("billing.addressState", getRandomState(billingAddressCC));
        row.put("billing.addressPostCode", getZipcode());
        row.put("billing.addressCountry", billingAddressCC);
        return row;
    }

    private static String getHouseNumber() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
    }

    private static String getZipcode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(60000, 69999));
    }

    private static String getPhone() {
        String areaCode = String.valueOf(ThreadLocalRandom.current().nextInt(600, 610));
        String number = String.valueOf(ThreadLocalRandom.current().nextInt(1000000, 2000000));
        return areaCode.concat(number);
    }

    public static String getRandomState(String countryCode) {
        List<String> states = statesByCountry.get(countryCode);
        if (states != null) {
            return states.get(new Random().nextInt(states.size()));
        }
        return "N/A"; // Or return null, or a default value
    }

    private static String getPhone_CountryCode() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(1, 10));
    }

    private static String getEmail() {
        return generateName(10) + "@mastercard.com";
    }

    private static String getUnknown() {
        return generateUnknown(10);
    }
    public static String getTimezone() {
        List<String> zoneIds = new ArrayList<>(ZoneId.getAvailableZoneIds());
        Random random = new Random();

        String randomZoneId = zoneIds.get(random.nextInt(zoneIds.size()));
        ZoneId zoneId = ZoneId.of(randomZoneId);

        ZonedDateTime now = ZonedDateTime.now(zoneId);
        int totalSeconds = now.getOffset().getTotalSeconds();
        int hours = totalSeconds / 3600;
        int minutes = Math.abs((totalSeconds % 3600) / 60);

        String offsetStr = String.format("%+d:%02dUTC", hours, minutes);
        return offsetStr;
    }
    private static String getUserAgent() {
        int i = ThreadLocalRandom.current().nextInt(0, userAgents.length - 1 );
        return userAgents[i];
    }


}
