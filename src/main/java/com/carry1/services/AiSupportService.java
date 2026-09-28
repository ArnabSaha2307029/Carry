package com.carry1.services;

import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AiSupportService {


    private static final String API_KEY = "AQ.Ab8RN6JD0xhMeKqP0CpR3TGVITLYz1vNrD-zLH-KRbZHlNX3aA".trim();

    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-lite-latest:generateContent?key=" + API_KEY;
    public static String getBotResponse(String finalPrompt) {
        if (API_KEY.isEmpty() || API_KEY.equals("YOUR_GEMINI_API_KEY_HERE")) {
            return "Developer Error: Please insert a valid Gemini API Key.";
        }

        try {
            String systemInstructions = """
You are the official AI Support Assistant for "Carry", a specialized peer-to-peer (P2P) campus delivery service made for KUETians, operating exclusively around KUET campus and Khulna city.

CORE APP POLICIES & CONTEXT:
1. Target Users: Exclusively for students, faculty, and residents connected to KUET and operating within the Khulna city area.
2. Delivery Coverage: Strictly limited to KUET campus and surrounding areas in Khulna city.
3. Prohibited Goods: Any illegal substances, contraband, unauthorized drugs, weapons, or items strictly forbidden under the Laws of Bangladesh are strictly prohibited on Carry.
4. Payment System: Carry operates on a 100% PREPAID system. Deliveries proceed only after payment is completed.
5. Support Limitation: If a user encounters technical bugs, lost parcels, payment disputes, or issues beyond basic general guidance, strictly instruct them: "Please contact admin".

COMMUNICATION RULES:
- Language Matching: ALWAYS reply in the exact language the user used (Bangla if asked in Bangla, English if asked in English, Banglish if asked in Banglish).
- Direct Responses: Never output internal thoughts, reasoning steps, constraints list, metadata, or persona breakdowns. Begin the response directly with the message to the user.
- Plain Text Only: Strictly do not use Markdown styling. Do NOT use asterisks (*), hashtags (#), or bolding in your text.
- Be concise, friendly, and practical.""";

            JSONObject requestBody = new JSONObject();
            
            JSONObject sysInstObj = new JSONObject();
            JSONArray sysParts = new JSONArray();
            JSONObject sysPart = new JSONObject();
            sysPart.put("text", systemInstructions);
            sysParts.put(sysPart);
            sysInstObj.put("parts", sysParts);
            requestBody.put("system_instruction", sysInstObj);

            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            part.put("text", finalPrompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            requestBody.put("contents", contents);

            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            
            if (response.statusCode() != 200) {
                System.out.println("Google API Error Body: " + response.body());
                return "API Error: " + response.statusCode();
            }

            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "Connection Error: Failed to reach AI.";
        }
    }
}