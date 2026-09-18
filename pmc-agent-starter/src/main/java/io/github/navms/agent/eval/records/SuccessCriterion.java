package io.github.navms.agent.eval.records;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 一条可验证成功标准。
 *
 * @param id    稳定主键，用于失败分类
 * @param kind  required_agent / forbidden_agent / required_write_tool / forbidden_write_tool / interrupt / clarify_only / required_clarify
 * @param value 字符串或布尔；interrupt 用 true/false
 * @author navms
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SuccessCriterion(String id, String kind, Object value) {

    /**
     * @return 规范化 kind
     */
    public String normalizedKind() {
        return kind == null ? "" : kind.trim().toLowerCase();
    }

    /**
     * @return 字符串值
     */
    public String stringValue() {
        return value == null ? "" : String.valueOf(value).trim();
    }

    /**
     * @return 布尔值，无法解析时为 null
     */
    public Boolean boolValue() {
        if (value instanceof Boolean flag) {
            return flag;
        }
        String raw = stringValue();
        if (raw.isEmpty()) {
            return null;
        }
        if ("true".equalsIgnoreCase(raw) || "1".equals(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw) || "0".equals(raw)) {
            return false;
        }
        return null;
    }

}
