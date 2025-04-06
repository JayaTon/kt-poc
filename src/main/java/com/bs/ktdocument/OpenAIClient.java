package com.bs.ktdocument;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OpenAIClient {

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY"); // Ensure this is set

    public static String sendToChatGPT(String code) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // ✅ Construct JSON correctly
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("model", "gpt-4");

        JsonArray messages = new JsonArray();

        // System Message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a senior developer. Document the functionality of the given code in a developer-friendly format. Ignore any import statements.");
        messages.add(systemMessage);

        // User Message (with properly escaped code)
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Analyze and document the following code (ignore import statements):\n\n" + code);
        messages.add(userMessage);

        jsonRequest.add("messages", messages);

        // Convert JSON to String
        String requestBody = new Gson().toJson(jsonRequest);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(requestBody, MediaType.get("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            System.out.println("Response Code: " + response.code());
            System.out.println("Response Body:\n" + responseBody);

            if (!response.isSuccessful()) {
                System.err.println("ChatGPT API call failed: " + response.message());
                return null;
            }

            return responseBody;
        } catch (IOException e) {
            System.err.println("Error calling ChatGPT API: " + e.getMessage());
            return null;
        }
    }
}
