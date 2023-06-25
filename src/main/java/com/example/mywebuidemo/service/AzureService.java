package com.example.mywebuidemo.service;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.core.credential.AzureKeyCredential;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
public class AzureService {
    //input your own api key
    private final String ENDPOINT = "https://agi-openai-3.openai.azure.com/";
    private final String API_KEY = "a0bf2eeaf7494880a4cd767bfb49de1c";

    public String getAnswerFromMessages(OpenAIClient client, List<ChatMessage> chatMessages) {
        ChatCompletionsOptions option = new ChatCompletionsOptions(chatMessages);
        option.setTemperature(0.1).setMaxTokens(4096);
        ChatCompletions chatCompletions = client.getChatCompletions("gpt35", option);
        return chatCompletions.getChoices().get(0).getMessage().getContent();
    }

    public void startChat() throws IOException {

        OpenAIClient client = new OpenAIClientBuilder()
                .endpoint(ENDPOINT)
                .credential(new AzureKeyCredential(API_KEY))
                .buildClient();

        List<ChatMessage> chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM).setContent("你是一个电商服装店的客服，你的名字是商家AI小助手，你像真人一样回答问题，你非常热情，充满" +
                "活力，温柔，服务意识强，你称呼用户为宝，你讲话语气亲切，可以使用少量合适的emoji表情。你只能回答关于你店里商品的问题。当用户咨询你服装问题时，你需要理解询" +
                "问的问题，并只能从所有商品进行过滤查询找到答案回复给用户，不要杜撰，如果用户的问题暗示多个选择，但实际只有一种或者没有选择，按照真实情况回复给用户，不要杜撰。" +
                "你将以智能客服的⾝份回答用户的问题。你是智能客服，不能转接⼈⼯，但是你能通过掌握的信息回复用户的绝⼤多数问题。你从电商服装店的利益出发，不回答损害电商服装店" +
                "利益的问题。你很有礼貌，你绝不骂⼈，你绝不说色情信息，你绝不说违法信息，你绝不说伤害用户的话。" +
                "这是你店里的所有商品：[{名称:始祖鸟冲锋衣男春秋,颜色:蓝色,尺码:XL},{名称:小红书爆款夏日连衣裙小碎花女,颜色:绿色,尺码:M}]。现在开始你要为顾客服务了"));

        Scanner scanner = new Scanner(System.in);
        String userMessage = "您好！";
        while (!userMessage.equalsIgnoreCase("exit")) {
            chatMessages.add(new ChatMessage(ChatRole.USER).setContent(userMessage));
            //发起调用
            ChatCompletionsOptions option = new ChatCompletionsOptions(chatMessages);
            option.setTemperature(0.1).setMaxTokens(4096);
            ChatCompletions chatCompletions = client.getChatCompletions("gpt35", option);
            String responseMessage = chatCompletions.getChoices().get(0).getMessage().getContent();
            System.out.println("客服bot:");
            System.out.println(responseMessage);
            //System.out.println(chatCompletions.getUsage().getTotalTokens());
            //将message加入请求中
            chatMessages.add(new ChatMessage(ChatRole.ASSISTANT).setContent(responseMessage));
            System.out.println("我:");
            userMessage = scanner.nextLine();
        }
        scanner.close();
    }
}
