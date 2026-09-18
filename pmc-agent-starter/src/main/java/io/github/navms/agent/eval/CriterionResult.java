package io.github.navms.agent.eval;

/**
 * 单条成功标准的判定。
 *
 * @param criterionId 标准 id
 * @param passed      是否通过
 * @param score       0 或 1
 * @param reason      一句依据
 * @param category    失败现象；通过时为空
 * @author navms
 */
public record CriterionResult(String criterionId, boolean passed, double score, String reason, String category) {
}
