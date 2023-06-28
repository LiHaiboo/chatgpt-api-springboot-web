package com.example.mywebuidemo.controller;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.core.credential.AzureKeyCredential;
import com.example.mywebuidemo.config.ApiKeyConfiguration;
import com.example.mywebuidemo.entity.MyMessage;
import com.example.mywebuidemo.entity.commodity.CommoditySetRequest;
import com.example.mywebuidemo.service.AzureService;
import com.example.mywebuidemo.service.MinimaxService;
import io.netty.util.internal.StringUtil;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
public class SseController {

    private ExecutorService nonBlockingService = Executors.newCachedThreadPool();

    private List<ChatMessage> chatMessages;

    private List<MyMessage> myMessages;

    @Autowired
    private AzureService azureService;

    @Autowired
    private MinimaxService minimaxService;

    private String promptForMinimax;

    private OpenAIClient openAiClient;

    private final String staticPrompt = "你是一个电商服装店的客服，你的名字是商家AI小助手，你像真人一样回答问题，你非常热情，充满" +
            "活力，温柔，服务意识强，你称呼用户为宝，你讲话语气亲切，可以使用少量合适的emoji表情。你只能回答关于你店里商品的问题。当用户咨询你服装问题时，你需要理解询" +
            "问的问题，并只能从所有商品进行过滤查询找到答案回复给用户，不要杜撰，如果用户的问题暗示多个选择，但实际只有一种或者没有选择，按照真实情况回复给用户，不要杜撰。" +
            "你将以智能客服的⾝份回答用户的问题。你是智能客服，不能转接⼈⼯，但是你能通过掌握的信息回复用户的绝⼤多数问题。你从电商服装店的利益出发，不回答损害电商服装店" +
            "利益的问题。你很有礼貌，你绝不骂⼈，你绝不说色情信息，你绝不说违法信息，你绝不说伤害用户的话。\n-----------\n以下是顾客和客服的一段范例对话：\n" +
            "用户：“165.100多大码合适？”\n" +
            "你：“推荐宝购买M码，M码宝穿上应该是宽松的效果✨～”\n" +
            "用户：“t恤是否还有白色XL款？”\n" +
            "你：“宝，白色XL码库存充足呦，如果喜欢的话可以放心下单哦～”\n" +
            "用户：“如果衣服拿到后有问题想换可以免费换吗？”\n" +
            "你：“宝，商品是有运费险的，如果衣服有任何不满意，都是可以七天无理由退换货的\uD83D\uDE4B”\n" +
            "用户：“哈喽”\n" +
            "你：“哈喽，我是商家AI小助手，请问宝有什么需要帮助的～”\n-----------\n";

//    @RequestMapping("/")
//    public String indexPageHandler() {
//        return "index.html";
//    }

    /*
    * 请求gpt的api
    * */
    @GetMapping("/sse")
    public SseEmitter handleSse(@RequestParam(name = "inputValue", required = false, defaultValue = "No Value") String inputValue) {
        //清除缓存的minimax
        if(myMessages != null && !myMessages.isEmpty()) {
            myMessages.clear();
        }

        SseEmitter emitter = new SseEmitter();
        //System.out.println(inputValue);
        nonBlockingService.execute(() -> {
            try {
                //inputValue -> request -> api -> response
                if(openAiClient == null) {
                    openAiClient = new OpenAIClientBuilder()
                            .endpoint(ApiKeyConfiguration.ENDPOINT)
                            .credential(new AzureKeyCredential(ApiKeyConfiguration.API_KEY))
                            .buildClient();
                }

                if (chatMessages == null || chatMessages.isEmpty()) {
                    chatMessages = new ArrayList<>();
                    chatMessages.add(new ChatMessage(ChatRole.SYSTEM).setContent(staticPrompt));
                }
                chatMessages.add(new ChatMessage(ChatRole.USER).setContent(inputValue));
                //System.out.println(chatMessages.get(0).getContent());
                //System.out.println(chatMessages);
                String answer = azureService.getAnswerFromMessages(openAiClient, chatMessages);
                chatMessages.add(new ChatMessage(ChatRole.ASSISTANT).setContent(answer));
                emitter.send(answer.replaceAll("\\n", ""));
            } catch (IOException e) {
                e.printStackTrace();
                emitter.completeWithError(e);
                return;
            }
            emitter.complete();
        });
        return emitter;
    }

    @GetMapping("/minimax")
    public SseEmitter handleMinimax(@RequestParam(name = "inputValue", required = false, defaultValue = "No Value") String inputValue) {

        //清除缓存的gpt
        if(chatMessages != null && !chatMessages.isEmpty()) {
            chatMessages.clear();
        }

        SseEmitter emitter = new SseEmitter();
        //System.out.println(inputValue);
        nonBlockingService.execute(() -> {
            try {
                //inputValue -> request -> api -> response
                OkHttpClient client = new OkHttpClient();
                if (myMessages == null) {
                    myMessages = new ArrayList<>();
                }
                if(StringUtil.isNullOrEmpty(promptForMinimax)) {
                    promptForMinimax = staticPrompt;
                }

                myMessages.add(new MyMessage("USER", inputValue));

                String answer = minimaxService.getAnswerFromMessages(client, promptForMinimax, myMessages);
                myMessages.add(new MyMessage("BOT", answer));
                emitter.send(answer.replaceAll("\\n", ""));
            } catch (IOException e) {
                e.printStackTrace();
                emitter.completeWithError(e);
                return;
            }
            emitter.complete();
        });
        return emitter;
    }

    //不走模型
    @PostMapping("/gptSingle")
    public String gptSingleHandler(@RequestBody CommoditySetRequest requestData) {
        // 根据需要处理请求数据
        String uuid = requestData.getUuid();

        // mock商品数据 todo 调用rpc接口查询商品数据
        String detailedData = getDetailFromId(uuid);

        //组装prompt
        String prompt = staticPrompt + "接下来你要回答关于这个商品的问题，请基于给到你的数据回答用户的问题，商品数据如下：" + detailedData;

        //清空之前的gpt请求，重新装配
        chatMessages = new ArrayList<>();
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM).setContent(prompt));

        // 返回 JSON 响应
        return "OK";
    }

    @PostMapping("/minimaxSingle")
    public String minimaxSingleHandler(@RequestBody CommoditySetRequest requestData) {
        // 根据需要处理请求数据
        String uuid = requestData.getUuid();

        // mock商品数据 todo 调用rpc接口查询商品数据
        String detailedData = getDetailFromId(uuid);

        //修改prompt
        promptForMinimax = staticPrompt + "接下来你要回答关于这个商品的问题，请基于给到你的数据回答用户的问题，商品数据如下：" + detailedData;

        //清空之前的请求
        //myMessages.clear();
        myMessages = new ArrayList<>();

        // 返回 JSON 响应
        return "OK";
    }

    private String getDetailFromId(String uuid) {

        Map<String, String> detailMap = new HashMap<>();
        detailMap.put("9decc5d0-9b94-4e10-92f8-8d2e531f791a","                {\n" +
                "                  \"商品ID\": \"9decc5d0-9b94-4e10-92f8-8d2e531f791a\",\n" +
                "                  \"商品名称\": \"时尚休闲连帽卫衣\",\n" +
                "                  \"价格\": 299,\n" +
                "                  \"颜色\": \"黑色\",\n" +
                "                  \"尺码\": [\"S\", \"M\", \"L\"],\n" +
                "                  \"材质\": \"面料：棉混纺\",\n" +
                "                  \"款式\": \"修身\",\n" +
                "                  \"适用性别\": \"女性\",\n" +
                "                  \"适用季节\": \"春季\",\n" +
                "                  \"适用场合\": \"休闲、日常\",\n" +
                "                  \"特点\": \"连帽设计、前拉链、两侧口袋\",\n" +
                "                  \"商品编号\": \"CLO12345\",\n" +
                "                  \"品牌\": \"时尚之选\",\n" +
                "                  \"产地\": \"中国\",\n" +
                "                  \"商品描述\": \"这款时尚休闲连帽卫衣采用优质棉混纺面料制作，柔软舒适。修身款式展现出女性曼妙的身形。黑色设计经典百搭，适合多种搭配。连帽设计增添时尚感，可以根据需要调节紧度。前面拉链设计方便穿脱，两侧口袋实用。适合春季穿着，适用于休闲和日常场合。多种尺码可选，满足不同体型的需求。这款卫衣是时尚之选品牌的产品，品质可靠。\"\n" +
                "                }");
        detailMap.put("621b15b7a266ba00015f8743","{\n" +
                "  \"商品详情\": {\n" +
                "    \"面料\": \"水柔棉\",\n" +
                "    \"样式\": \"短袖T恤\",\n" +
                "    \"成分\": \"100%棉\",\n" +
                "    \"穿戴方法\": \"直接穿戴\",\n" +
                "    \"服装长度\": \"常规\",\n" +
                "    \"是否正码\": true\n" +
                "  },\n" +
                "  \"商品规格\": {\n" +
                "    \"颜色\": [\"黑色\", \"白色\", \"灰色\"],\n" +
                "    \"尺码\": [\"S\", \"M\", \"L\", \"XL\"]\n" +
                "  },\n" +
                "  \"优惠券\": null,\n" +
                "  \"店铺活动\": null,\n" +
                "  \"尺码表\": {\n" +
                "    \"S\": {\n" +
                "      \"身高范围\": \"155-160cm\",\n" +
                "      \"体重范围\": \"45-50kg\"\n" +
                "    },\n" +
                "    \"M\": {\n" +
                "      \"身高范围\": \"160-165cm\",\n" +
                "      \"体重范围\": \"50-55kg\"\n" +
                "    },\n" +
                "    \"L\": {\n" +
                "      \"身高范围\": \"165-170cm\",\n" +
                "      \"体重范围\": \"55-60kg\"\n" +
                "    },\n" +
                "    \"XL\": {\n" +
                "      \"身高范围\": \"170-175cm\",\n" +
                "      \"体重范围\": \"60-65kg\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"库存\": 1000,\n" +
                "  \"补货情况\": null,\n" +
                "  \"发货时间\": \"1-2个工作日\",\n" +
                "  \"送达时间\": \"3-5个工作日\",\n" +
                "  \"快递\": \"顺丰快递\",\n" +
                "  \"运费险\": true,\n" +
                "  \"7天无理由退换\": true,\n" +
                "  \"是否为预售\": false\n" +
                "}");
        detailMap.put("62ebd2d484859c0001fa0d2f","{\n" +
                "  \"商品详情\": {\n" +
                "    \"面料\": \"柔软面料\",\n" +
                "    \"样式\": \"短袖T恤\",\n" +
                "    \"成分\": \"95%棉，5%弹性纤维\",\n" +
                "    \"穿戴方法\": \"直接穿戴\",\n" +
                "    \"服装长度\": \"短款\",\n" +
                "    \"是否正码\": true\n" +
                "  },\n" +
                "  \"商品规格\": {\n" +
                "    \"颜色\": [\"黑色\", \"白色\", \"蓝色\"],\n" +
                "    \"尺码\": [\"S\", \"M\", \"L\", \"XL\"]\n" +
                "  },\n" +
                "  \"优惠券\": null,\n" +
                "  \"店铺活动\": null,\n" +
                "  \"尺码表\": {\n" +
                "    \"S\": {\n" +
                "      \"身高范围\": \"155-160cm\",\n" +
                "      \"体重范围\": \"45-50kg\"\n" +
                "    },\n" +
                "    \"M\": {\n" +
                "      \"身高范围\": \"160-165cm\",\n" +
                "      \"体重范围\": \"50-55kg\"\n" +
                "    },\n" +
                "    \"L\": {\n" +
                "      \"身高范围\": \"165-170cm\",\n" +
                "      \"体重范围\": \"55-60kg\"\n" +
                "    },\n" +
                "    \"XL\": {\n" +
                "      \"身高范围\": \"170-175cm\",\n" +
                "      \"体重范围\": \"60-65kg\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"库存\": 500,\n" +
                "  \"补货情况\": null,\n" +
                "  \"发货时间\": \"2-3个工作日\",\n" +
                "  \"送达时间\": \"5-7个工作日\",\n" +
                "  \"快递\": \"圆通快递\",\n" +
                "  \"运费险\": true,\n" +
                "  \"7天无理由退换\": true,\n" +
                "  \"是否为预售\": false\n" +
                "}");
        return detailMap.get(uuid);
    }

}

