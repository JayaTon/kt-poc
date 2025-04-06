package com.bs.ktdocument;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CodeProcessor {
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    private static final String OUTPUT_FOLDER = "E:\\Learning\\SAP\\code_docs";
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    public void processCodeFile(String filePath) {
        try {
            String code = readFileWithRetry(filePath);
            if (code == null) {
                System.err.println("Failed to read file after multiple attempts: " + filePath);
                return;
            }

            String documentation = sendToChatGPT(code);
            if (documentation != null) {
                saveToWord(documentation, filePath);
            }
        } catch (IOException e) {
            System.err.println("Error processing code file: " + e.getMessage());
        }
    }

    /**
     * Reads a file with retry mechanism to handle file locking issues.
     */
    private String readFileWithRetry(String filePath) throws IOException {
        int attempts = 5; // Retry up to 5 times
        while (attempts > 0) {
            try {
                Path originalPath = Paths.get(filePath);
                Path tempPath = Files.createTempFile("code_temp", ".tmp");
                Files.copy(originalPath, tempPath, StandardCopyOption.REPLACE_EXISTING);
                return new String(Files.readAllBytes(tempPath)); // Read from temp file
            } catch (IOException e) {
                if (--attempts == 0) throw e;
                System.out.println("File is locked, retrying in 2 seconds...");
                try {
                    Thread.sleep(2000); // Wait 2 seconds before retrying
                } catch (InterruptedException ignored) {}
            }
        }
        return null;
    }

    private static String sendToChatGPT(String code) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // ✅ Construct JSON properly
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("model", "gpt-4");

        JsonArray messages = new JsonArray();

        // System Message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", "You are a senior developer. Document the functionality of the given code in a developer-friendly format. Ignore any import statements.");
        messages.add(systemMessage);

        // User Message (with properly formatted code)
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", "Analyze and document the following code (ignore import statements):\n\n" + code);
        messages.add(userMessage);

        jsonRequest.add("messages", messages);

        // Convert JSON to String
        String requestBody = new Gson().toJson(jsonRequest);

        Request request = new Request.Builder()
                .url(OPENAI_URL)
                .post(RequestBody.create(requestBody, MediaType.get("application/json; charset=utf-8")))
                .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            System.out.println("Response Code: " + response.code());
            System.out.println("Response Body:\n" + responseBody);

            if (!response.isSuccessful()) {
                return "ChatGPT API call failed: " + response.message();
            }

            return responseBody;
        } catch (IOException e) {
            return "Error calling ChatGPT API: " + e.getMessage();
        }
    }


    public static void saveToWord(String jsonResponse, String filePath) throws IOException {
        // ✅ Parse JSON to extract the actual documentation text
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray choices = jsonObject.getJSONArray("choices");

        if (choices.length() == 0) {
            System.err.println("No content received from ChatGPT.");
            return;
        }

        // Extract "content" from response
        String content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        // ✅ Create Word Document
        XWPFDocument document = new XWPFDocument();

        // ✅ Split content into sections
        String[] lines = content.split("\n");

        for (String line : lines) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();

            line = line.trim();

            // ✅ Format Headings & Lists
            if (line.startsWith("Package:") || line.startsWith("Class:") || line.startsWith("Method:")) {
                paragraph.setStyle("Heading1");
                run.setBold(true);
                run.setFontSize(14);
            } else if (line.startsWith("- ")) {
                paragraph.setSpacingAfter(200);
            }

            run.setText(line);
        }

        // ✅ Save to Word file
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String wordFileName = new java.io.File(filePath).getName().replace(".java", "_" + timestamp + "_Documentation.docx");
        String wordFilePath = "E:\\Learning\\SAP\\output\\" + wordFileName;

        try (FileOutputStream out = new FileOutputStream(wordFilePath)) {
            document.write(out);
        }
        document.close();

        System.out.println("Developer Documentation saved: " + wordFilePath);
    }
}
