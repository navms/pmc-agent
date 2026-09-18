package io.github.navms.agent.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Golden Task 的一轮用户输入。
 *
 * @param role    角色，评测只发送 user
 * @param content 正文
 * @author navms
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoldenTurn(String role, String content) {
}
