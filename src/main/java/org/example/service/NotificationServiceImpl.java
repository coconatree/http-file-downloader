package org.example.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.example.model.UsageNotification;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Date;

public class NotificationServiceImpl implements NotificationServiceI {
    @Override
    public void sendNotification() {

        OkHttpClient client = new OkHttpClient();

        String usedBy = "";

        try {
            InetAddress localMachine = InetAddress.getLocalHost();
            usedBy = localMachine.getHostName();
        }
        catch (UnknownHostException e) {
            usedBy = System.getProperty("user.name");
            if (usedBy.isEmpty()) {
                usedBy = "Unknown user";
            }
        }

        UsageNotification usageNotification = new UsageNotification(
                usedBy,
                new Date(System.currentTimeMillis())
        );

        Gson gson = new GsonBuilder().create();

        RequestBody body = RequestBody.create(
                gson.toJson(usageNotification),
                MediaType.get("application/json")
        );

        Request request = new Request.Builder()
                .url("http://localhost:8080/api/v1/notification/monk-http")
                .post(body)
                .build();

        Response response = null;

        try {
            response = client.newCall(request).execute();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
