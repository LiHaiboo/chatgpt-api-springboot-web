package com.example.mywebuidemo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MyMessage {
    public String sender_type;
    public String text;
}
