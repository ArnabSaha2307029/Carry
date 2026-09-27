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
            String systemInstructions = "You are Carry1 Support Bot. Carry1 is a P2P delivery app. Answer clearly and concisely. Do not use markdown (no asterisks or bold text). If the prompt contains a [System hidden data] note, use that exact data to formulate your answer about the order status. User input: ";

            JSONObject requestBody = new JSONObject();
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();

            part.put("text", systemInstructions + "\n" + finalPrompt);
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