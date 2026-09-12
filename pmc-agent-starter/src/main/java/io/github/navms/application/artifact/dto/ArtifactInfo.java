package io.github.navms.application.artifact.dto;

/**
 * 导出文件元数据。
 *
 * @param success     是否成功
 * @param message     说明
 * @param fileId      文件 ID
 * @param fileName    下载文件名
 * @param downloadUrl 下载地址
 * @param rowCount    写出行数
 * @param truncated   是否因上限截断
 * @author navms
 */
public record ArtifactInfo(
        boolean success,
        String message,
        String fileId,
        String fileName,
        String downloadUrl,
        int rowCount,
        boolean truncated
) {
}
