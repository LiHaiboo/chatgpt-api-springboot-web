package com.example.mywebuidemo.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.example.mywebuidemo.config.ApiKeyConfiguration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import com.example.mywebuidemo.entity.MyMessage;
import com.example.mywebuidemo.entity.MyResponse;
import com.example.mywebuidemo.entity.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
public class MinimaxService {
    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Gson gson = new GsonBuilder().setLenient().disableHtmlEscaping().create();

    public String getAnswerFromMessages(OkHttpClient client, String prompt, List<MyMessage> messages) throws IOException {
        String url = "https://api.minimax.chat/v1/text/chatcompletion?GroupId=" + ApiKeyConfiguration.group_id;


        Payload payload = new Payload();
        payload.model = "abab5.5-chat";
        payload.prompt = prompt;
        payload.role_meta = new Payload.RoleMeta();
        payload.role_meta.user_name = "用户";
        payload.role_meta.bot_name = "商家AI小助手";
        payload.stream = false;
        payload.use_standard_sse = true;
        payload.temperature = 0.1;
        payload.tokens_to_generate=128;
        payload.top_p=0.95;
        payload.beam_width=1;
        payload.messages = messages;

        //发起调用
        String jsonPayload = gson.toJson(payload);
        System.out.println(jsonPayload);

        RequestBody requestBody = RequestBody.create(jsonPayload, JSON);
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + ApiKeyConfiguration.api_key)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        Call call = client.newCall(request);
        Response response = call.execute();
        ResponseBody responseBody = response.body();
        assert responseBody != null;
        return parseChunkDelta(responseBody.string());
    }

    private String parseChunkDelta(String chunk) {
        String jsonStr;
        if (chunk.startsWith("data:")) {
            jsonStr = chunk.substring(6).trim();
        } else {
            jsonStr = chunk;
        }
        System.out.println(jsonStr);
        MyResponse response = gson.fromJson(jsonStr, MyResponse.class);

        return response.getReply();
    }

    public void startChat() throws IOException {
        String url = "https://api.minimax.chat/v1/text/chatcompletion?GroupId=" + ApiKeyConfiguration.group_id;

        OkHttpClient client = new OkHttpClient();

        Payload payload = new Payload();
        payload.model = "abab5-chat";
        payload.prompt = "你是一个电商服装店的客服，只能回答关于你店里商品的问题。当用户咨询你服装问题时，你需要理解询问的问题并只能从所有商品进行过滤查询找到答案回复给用户，不要杜撰。\\n----\\n" +
                "所有商品=[{名称:始祖鸟冲锋衣男春秋,颜色:蓝色,尺码:XL},{名称:小红书爆款夏日连衣裙小碎花女,颜色:绿色,尺码:M}]。\\n----";
        payload.role_meta = new Payload.RoleMeta();
        payload.role_meta.user_name = "用户";
        payload.role_meta.bot_name = "客服";
        payload.stream = false;
        payload.use_standard_sse = true;
        payload.messages = new ArrayList<>();
        payload.temperature = 0.1;
        //payload.tokens_to_generate=128;
        payload.top_p=0.95;
        //payload.beam_width=1;
        Scanner scanner = new Scanner(System.in);
        String userMessage = "如果用户的问题暗示多个选择，但实际只有一种或者没有选择，按照真实情况回复给用户，不要杜撰。\\n----\\n您好！";
        while (!userMessage.equalsIgnoreCase("exit")) {
            payload.messages.add(new MyMessage("USER", userMessage));

            //发起调用
            String jsonPayload = gson.toJson(payload);

            RequestBody requestBody = RequestBody.create(jsonPayload, JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + ApiKeyConfiguration.api_key)
                    .header("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            Call call = client.newCall(request);
            Response response = call.execute();
            ResponseBody responseBody = response.body();
            assert responseBody != null;
            String responseMessage = parseChunkDelta(responseBody.string());
            System.out.println("客服bot:");
            System.out.println(responseMessage);
            //将message加入请求中
            payload.messages.add(new MyMessage("BOT", responseMessage));

            System.out.println("我:");

//            userMessage = "如果用户的问题暗示多个选择，但实际只有一种或者没有选择，按照真实情况回复给用户，不要杜撰。\\n----\\n";
//            String input = scanner.nextLine();
//            userMessage += input;

            userMessage = scanner.nextLine();
        }

        scanner.close();
    }

}
