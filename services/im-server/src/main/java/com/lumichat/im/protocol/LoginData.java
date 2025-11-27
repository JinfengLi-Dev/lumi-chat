package com.lumichat.im.protocol;

import lombok.Data;

@Data
public class LoginData {
    private String token;
    private String deviceId;
    private String deviceType;
}
