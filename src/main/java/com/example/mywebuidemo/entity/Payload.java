package com.example.mywebuidemo.entity;

import lombok.Data;

import java.util.List;

@Data
public class Payload {
    public String model;
    public String prompt;
    public RoleMeta role_meta;
    public boolean stream;
    public boolean use_standard_sse;
    public List<MyMessage> messages;
    public double temperature;
    public int tokens_to_generate;
    public double top_p;
    public int beam_width;

    public static class RoleMeta {
        public String user_name;
        public String bot_name;
    }
}
