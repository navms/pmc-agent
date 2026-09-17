package io.github.navms.agent.prompt;

/**
 * 一次从 Langfuse 取回（或 fallback）的 prompt 快照。
 *
 * @param name Langfuse prompt 名
 * @param version 版本号，fallback 为 null
 * @param template 未 compile 的正文
 * @param fallback 是否未命中远端
 * @author navms
 */
public record LangfusePromptSnapshot(String name, Integer version, String template, boolean fallback) {
}
