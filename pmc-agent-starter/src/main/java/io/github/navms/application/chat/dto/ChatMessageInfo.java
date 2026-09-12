package io.github.navms.application.chat.dto;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

/**
 * AG-UI Message。
 *
 * @param fields 协议字段
 * @author navms
 */
public record ChatMessageInfo(@JsonValue Map<String, Object> fields) {
}
