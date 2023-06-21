package com.example.mywebuidemo.controller;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.core.credential.AzureKeyCredential;
import com.example.mywebuidemo.service.AzureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class SseController {
    private final String ENDPOINT = "https://agi-openai-3.openai.azure.com/";
    private final String API_KEY = "a0bf2eeaf7494880a4cd767bfb49de1c";

    private ExecutorService nonBlockingService = Executors.newCachedThreadPool();

    @Autowired
    private AzureService azureService;

//    @GetMapping("/")
//    public void indexPageHandler() {
//
//    }

    @GetMapping("/sse")
    public SseEmitter handleSse(@RequestParam(name = "inputValue", required = false, defaultValue = "No Value") String inputValue) {
        SseEmitter emitter = new SseEmitter();
        //System.out.println(inputValue);
        nonBlockingService.execute(() -> {
            try {
                //inputValue -> request -> api -> response
                OpenAIClient client = new OpenAIClientBuilder()
                        .endpoint(ENDPOINT)
                        .credential(new AzureKeyCredential(API_KEY))
                        .buildClient();

                List<ChatMessage> chatMessages = new ArrayList<>();
                chatMessages.add(new ChatMessage(ChatRole.SYSTEM).setContent("从现在开始你是一个电商服装店的客服，你只能回答关于你店里商品的问题，不要改变你的身份。" +
                        "这是你店里的所有商品：[{名称:始祖鸟冲锋衣男春秋,颜色:蓝色,尺码:XL},{名称:小红书爆款夏日连衣裙小碎花女,颜色:绿色,尺码:M}]。现在开始你要为顾客服务了"));
                chatMessages.add(new ChatMessage(ChatRole.USER).setContent(inputValue));

                String answer = azureService.getAnswerFromMessages(client, chatMessages);
                emitter.send(answer);
            } catch (IOException e) {
                e.printStackTrace();
                emitter.completeWithError(e);
                return;
            }
            emitter.complete();
        });
        return emitter;
    }
}

