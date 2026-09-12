package io.github.navms.application.artifact;

import io.github.navms.application.artifact.dto.ArtifactInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导出文件落盘与下载。
 *
 * @author navms
 */
@Service
public class ArtifactAppService {

    private final Path exportDir;

    private final Map<String, StoredArtifact> artifacts = new ConcurrentHashMap<>();

    /**
     * @param exportDir 导出目录
     */
    public ArtifactAppService(@Value("${pmc.artifact.export-dir:./data/exports}") String exportDir) {
        this.exportDir = Path.of(exportDir).toAbsolutePath().normalize();
    }

    /**
     * @param fileName 下载文件名
     * @return 待写入路径与 fileId
     */
    public PreparedFile prepare(String fileName) throws IOException {
        Files.createDirectories(exportDir);
        String fileId = UUID.randomUUID().toString().replace("-", "");
        Path path = exportDir.resolve(fileId + ".xlsx");
        artifacts.put(fileId, new StoredArtifact(fileName, path));
        return new PreparedFile(fileId, path);
    }

    /**
     * @param fileId    文件 ID
     * @param fileName  文件名
     * @param rowCount  行数
     * @param truncated 是否截断
     * @return 下载元数据
     */
    public ArtifactInfo toInfo(String fileId, String fileName, int rowCount, boolean truncated) {
        String message = truncated
                ? "导出成功，已按上限截断"
                : "导出成功";
        return new ArtifactInfo(true, message, fileId, fileName, "/files/" + fileId, rowCount, truncated);
    }

    /**
     * @param fileId 文件 ID
     * @return 已存文件
     */
    public Optional<StoredArtifact> find(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return Optional.empty();
        }
        StoredArtifact stored = artifacts.get(fileId.trim());
        if (stored == null || !Files.isRegularFile(stored.path())) {
            return Optional.empty();
        }
        return Optional.of(stored);
    }

    /**
     * @param artifact 文件
     * @return 输入流
     */
    public InputStream open(StoredArtifact artifact) throws IOException {
        return Files.newInputStream(artifact.path());
    }

    /**
     * @param fileId 文件 ID
     * @param path   路径
     * @author navms
     */
    public record PreparedFile(String fileId, Path path) {
    }

    /**
     * @param fileName 下载名
     * @param path     路径
     * @author navms
     */
    public record StoredArtifact(String fileName, Path path) {
    }
}
