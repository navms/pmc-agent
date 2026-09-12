package io.github.navms.web.agent.vo;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

/**
 * AG-UI Message。
 *
 * @param fields 协议字段
 * @author navms
 */
public record ChatMessageVO(@JsonValue Map<String, Object> fields) {
}
