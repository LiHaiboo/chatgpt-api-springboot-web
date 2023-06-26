package com.example.mywebuidemo.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class MyResponse implements Serializable {
    public String created;
    public String model;
    public String reply;
    public List<Choice> choices;
    public boolean input_sensitive;
    public boolean output_sensitive;
    public String id;
    public Usage usage;
    public BaseResp base_resp;

    public static class Choice {
        public int index;
        public double logprobes;
        public String finish_reason;
        public String delta;
    }

    public static class Usage {
        public String total_tokens;
    }

    public static class BaseResp {
        public int status_code;
        public String status_msg;
    }
}
