package io.github.navms.web.agent;

import io.github.navms.application.artifact.ArtifactAppService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

/**
 * 导出文件下载。
 *
 * @author navms
 */
@RestController
public class ArtifactController {

    private final ArtifactAppService artifactAppService;

    /**
     * @param artifactAppService 文件服务
     */
    public ArtifactController(ArtifactAppService artifactAppService) {
        this.artifactAppService = artifactAppService;
    }

    /**
     * @param fileId 文件 ID
     * @return xlsx 流
     */
    @GetMapping("/files/{fileId}")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable String fileId) {
        ArtifactAppService.StoredArtifact artifact = artifactAppService.find(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "file not found"));
        StreamingResponseBody body = outputStream -> {
            try (var input = artifactAppService.open(artifact)) {
                input.transferTo(outputStream);
            }
        };
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(artifact.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(body);
    }
}
