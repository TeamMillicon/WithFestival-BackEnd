package com.festival.domain.guide.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GuideReq {

    @NotBlank(message = "제목을 입력 해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력 해주세요.")
    private String content;

    @NotNull(message = "안내사항 타입을 입력 해주세요.")
    private String type;

    @NotNull(message = "상태값을 입력해주세요")
    private String status;

    private MultipartFile mainFile;

    private List<MultipartFile> subFiles;

    @Builder
    private GuideReq(String title, String content, String type, String status, MultipartFile mainFile, List<MultipartFile> subFiles) {
        this.title = title;
        this.content = content;
        this.type = type;
        this.status = status;
        this.mainFile = mainFile;
        this.subFiles = subFiles;
    }
}
