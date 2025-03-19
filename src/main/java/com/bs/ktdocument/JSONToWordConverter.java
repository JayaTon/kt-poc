package com.bs.ktdocument;

import org.apache.poi.xwpf.usermodel.*;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;

public class JSONToWordConverter {

    public static void main(String[] args) {
        // Replace with your actual JSON response
       // String jsonResponse = "{ \"id\": \"chatcmpl-BAzdMwdi83joVRUQ6z7TJfsCMAndU\", \"object\": \"chat.completion\", \"created\": 1741959900, \"model\": \"gpt-4-0613\", \"choices\": [ { \"index\": 0, \"message\": { \"role\": \"assistant\", \"content\": \"### Overview\\nSAP (Systems, Applications, and Products in Data Processing), a leading ERP software solution globally, assists businesses in streamlining their operations.\\n\\n### Key Points\\n1. **What is SAP?**\\n   SAP stands for Systems, Applications, and Products in Data Processing.\\n\\n2. **Who are the primary users of SAP?**\\n   SAP is used by businesses worldwide looking to streamline their operations.\\n\\n3. **Why choose SAP?**\\n   Organizations choose SAP to gain efficiency and business insight.\\n\\n4. **Key features and benefits of SAP**\\n   SAP delivers integrated modules for finance, supply chain, HR, and more. Through these modules, businesses could gain data-driven insights and improve operational efficiency.\" } } ], \"usage\": { \"prompt_tokens\": 312, \"completion_tokens\": 454, \"total_tokens\": 766 } }";
        String jsonResponse =   "{   \"id\": \"chatcmpl-BAzdMwdi83joVRUQ6z7TJfsCMAndU\",   \"object\": \"chat.completion\",   \"created\": 1741959900,   \"model\": \"gpt-4-0613\",   \"choices\": [     {       \"index\": 0,       \"message\": {         \"role\": \"assistant\",         \"content\": \"### Knowledge Transfer Document: Introduction to SAP\\n\\n#### Overview\\nSAP (Systems, Applications, and Products in Data Processing), a leading ERP software solution globally, assists businesses in streamlining their operations. This powerful tool offers integrated modules for various areas like finance, supply chain, and human resources, enabling organizations to improve efficiency and gain valuable business insights. The introduction will delve into SAP's key features and benefits, followed by a detailed overview of SAP’s core modules.\\n\\n#### Key Points\\n1. **What is SAP?** \\n   SAP stands for Systems, Applications, and Products in Data Processing. \\n\\n2. **Who are the primary users of SAP?**\\n   SAP is used by businesses worldwide looking to streamline their operations.\\n\\n3. **Why choose SAP?**\\n   Organizations choose SAP to gain efficiency and business insight.\\n\\n4. **Key features and benefits of SAP**\\n   SAP delivers integrated modules for finance, supply chain, HR, and more. Through these modules, businesses could gain data-driven insights and improve operational efficiency.\\n\\n5. **Overview of SAP's core modules**\\n   SAP's core modules offer comprehensive business operation management, which will be discussed in detail in the upcoming sessions.\\n\\n#### Frequently Asked Questions (FAQ)\\n\\n1. **What does SAP stand for?**\\n   SAP stands for Systems, Applications, and Products in Data Processing.\\n\\n2. **What does SAP do?**\\n   SAP is a leading ERP (Enterprise Resource Planning) software solution used by businesses to streamline operations and gain insightful business knowledge.\\n\\n3. **Who uses SAP?**\\n   Businesses across various industries use SAP. Especially ones that need integrated modules to manage finance, supply chain, HR, and others.\\n\\n4. **Why use SAP?**\\n   SAP improves operational efficiency and provides valuable business insights, making it a preferred tool for many businesses.\\n\\n5. **What are the core modules of SAP?**\\n   SAP offers a wide variety of core modules, including those for finance, supply chain, and HR. Each of these modules plays a crucial role in the management and streamlining of business operations. The details of these modules will be explored in the following sessions. \\n\\nThank you for reading this SAP introduction. Stay tuned for the next segment dive into how SAP can transform business processes.\",         \"refusal\": null,         \"annotations\": []       },       \"logprobs\": null,       \"finish_reason\": \"stop\"     }   ],   \"usage\": {     \"prompt_tokens\": 312,     \"completion_tokens\": 454,     \"total_tokens\": 766,     \"prompt_tokens_details\": {       \"cached_tokens\": 0,       \"audio_tokens\": 0     },     \"completion_tokens_details\": {       \"reasoning_tokens\": 0,       \"audio_tokens\": 0,       \"accepted_prediction_tokens\": 0,       \"rejected_prediction_tokens\": 0     }   },   \"service_tier\": \"default\",   \"system_fingerprint\": null }";
        try {
            saveToWord(jsonResponse, "E:\\Learning\\SAP\\output\\KT_Document.docx");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveToWord(String jsonResponse, String fileName) throws IOException {
        // Parse JSON to extract the content
        JSONObject jsonObject = new JSONObject(jsonResponse);
        String content = jsonObject.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");

        // Create Word Document
        XWPFDocument document = new XWPFDocument();

        // Split content into lines
        String[] lines = content.split("\n");

        for (String line : lines) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();

            line = line.trim(); // Trim whitespace

            // Convert headings
            if (line.startsWith("###")) {
                paragraph.setStyle("Heading1");
                run.setBold(true);
                run.setFontSize(14);
                run.setText(line.replace("###", "").trim());
            } else if (line.startsWith("####")) {
                paragraph.setStyle("Heading2");
                run.setBold(true);
                run.setFontSize(12);
                run.setText(line.replace("####", "").trim());
            }
            // Convert numbered list while removing **
            else if (line.matches("^\\d+\\.\\s\\*\\*(.*?)\\*\\*.*$")) {
                paragraph.setSpacingAfter(200);
                line = line.replaceAll("\\*\\*(.*?)\\*\\*", "$1"); // Remove ** from bold text
                run.setBold(true);
                run.setText(line);
            }
            // Convert normal text and remove **
            else {
                run.setText(line.replaceAll("\\*\\*(.*?)\\*\\*", "$1")); // Remove ** from bold text
            }
        }

        // Save Word file
        try (FileOutputStream out = new FileOutputStream(fileName)) {
            document.write(out);
        }
        document.close();

        System.out.println("KT Document saved successfully as " + fileName);
    }
}

