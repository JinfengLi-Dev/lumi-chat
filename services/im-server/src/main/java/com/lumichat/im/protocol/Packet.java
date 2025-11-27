package com.lumichat.im.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Packet {

    private int type;
    private String seq;  // Sequence ID for request-response matching
    private Object data;
    private Long timestamp;

    public static Packet of(int type, Object data) {
        return Packet.builder()
                .type(type)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static Packet response(int type, String seq, Object data) {
        return Packet.builder()
                .type(type)
                .seq(seq)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
