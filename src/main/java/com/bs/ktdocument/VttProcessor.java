package com.bs.ktdocument;


import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.*; // For HTTP requests to OpenAI API
import org.apache.poi.xwpf.usermodel.*;

public class VttProcessor {
    private static final String OPENAI_API_KEY = System.getenv("OPENAI_API_KEY");
    // Replace with your OpenAI API key
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OUTPUT_FOLDER = "E:\\Learning\\SAP\\output";  // Change this path

    public void processVTTFile(String filePath) {
        try {
            String transcript = new String(Files.readAllBytes(Paths.get(filePath)));
            String ktContent = sendToChatGPT(transcript);

            if (ktContent != null) {
                saveToWord(ktContent, filePath);
            }
        } catch (IOException e) {
            System.err.println("Error reading VTT file: " + e.getMessage());
        }
    }

    private static String sendToChatGPT(String transcript) {
        //OkHttpClient client = new OkHttpClient();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();


        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // Escape special characters in transcript
        String escapedTranscript = transcript
                .replace("\\", "\\\\")  // Escape backslashes
                .replace("\"", "\\\"")  // Escape double quotes
                .replace("\n", "\\n")   // Escape newlines
                .replace("\r", "\\r")   // Escape carriage returns
                .replace("\t", "\\t");  // Escape tabs

        String requestBody = "{"
                + "\"model\": \"gpt-4\","
                + "\"messages\": ["
                + " {\"role\": \"system\", \"content\": \"You are an expert in creating structured Knowledge Transfer (KT) documents with an FAQ section.\"},"
                + " {\"role\": \"user\", \"content\": \"Create a well-structured KT document with an FAQ section from this transcript:\\n\\n"
                + escapedTranscript
                + "\"}"
                + "]"
                + "}";

        System.out.println("Sending JSON to OpenAI:\n" + requestBody); // Debugging

        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(RequestBody.create(requestBody, JSON))
                .addHeader("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY")) // Ensure API Key is set
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();

            System.out.println("Response Code: " + response.code());  // Print HTTP Status Code
            System.out.println("Response Body:\n" + responseBody);   // Print Full Response

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





    private String extractContent(String responseBody) {
        int startIndex = responseBody.indexOf("\"content\":\"") + 10;
        int endIndex = responseBody.indexOf("\"}", startIndex);
        if (startIndex > 10 && endIndex > startIndex) {
            return responseBody.substring(startIndex, endIndex).replace("\\n", "\n");
        }
        return "Error extracting content.";
    }

    private void saveToWord(String content, String vttFilePath) {
        try (XWPFDocument document = new XWPFDocument()) {
            String[] sections = content.split("FAQs:", 2);
            String ktSection = sections[0].trim();
            String faqSection = sections.length > 1 ? sections[1].trim() : "No FAQs provided.";

            // Add KT section
            XWPFParagraph ktPara = document.createParagraph();
            XWPFRun ktRun = ktPara.createRun();
            ktRun.setText(ktSection);
            ktRun.setBold(true);

            // Add FAQ section
            document.createParagraph().createRun().addBreak();
            XWPFParagraph faqHeader = document.createParagraph();
            XWPFRun faqHeaderRun = faqHeader.createRun();
            faqHeaderRun.setText("Frequently Asked Questions (FAQs)");
            faqHeaderRun.setBold(true);
            faqHeaderRun.setFontSize(14);

            XWPFParagraph faqPara = document.createParagraph();
            XWPFRun faqRun = faqPara.createRun();
            faqRun.setText(faqSection);

            // Save file
            String wordFileName = new File(vttFilePath).getName().replace(".vtt", "_KT_Document.docx");
            String wordFilePath = OUTPUT_FOLDER + "/" + wordFileName;

            try (FileOutputStream out = new FileOutputStream(wordFilePath)) {
                document.write(out);
            }

            System.out.println("KT document saved: " + wordFilePath);
        } catch (IOException e) {
            System.err.println("Error saving KT document: " + e.getMessage());
        }
    }
}
