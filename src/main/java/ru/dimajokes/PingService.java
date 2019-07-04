package ru.dimajokes;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;

@Component
@Slf4j
public class PingService {

    @Value("${app.deploy.url}") private String deployUrl;

    public long ping(String ipAddress) {
        try {
            log.info("Sending Ping Request to " + ipAddress);

            URL siteURL = new URL(ipAddress);
            HttpURLConnection connection = (HttpURLConnection) siteURL.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            long cur = System.currentTimeMillis();
            connection.connect();
            long ping = System.currentTimeMillis() - cur;

            int code = connection.getResponseCode();
            if (code == 200) {
                return ping;
            } else {
                return -1;
            }
        } catch (Exception e) {
            log.error("Error on ping {}", e.getMessage());
            return -2;
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public long pingThemself() {
        return ping(deployUrl);
    }
}
