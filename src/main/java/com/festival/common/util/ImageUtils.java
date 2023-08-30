package com.festival.common.util;

import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class ImageUtils {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.bucketName}")
    private String bucketName;

    @Value("${cloud.aws.folderName}")
    private String folderName;


    public String upload(MultipartFile file, String kind) {
        try {
            String fileName =  createFileName(file.getOriginalFilename(), kind);
            ObjectMetadata om = new ObjectMetadata();
            om.setContentLength(file.getInputStream().available());
            om.setContentType(file.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, folderName + fileName, file.getInputStream(), om);
            // 객체의 권한을 공개로 설정(버킷에서 확인 가능)
            putObjectRequest.withCannedAcl(CannedAccessControlList.PublicRead);
            // 파일 업로드
            amazonS3.putObject(putObjectRequest);
            return fileName;
        } catch (AmazonS3Exception e) {
            e.printStackTrace();
        } catch(SdkClientException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<String> uploadMulti(List<MultipartFile> files, String kind) {
        return files.stream()
                .map(file -> upload(file, kind))
                .collect(Collectors.toList());
    }

    public String createFileName(String originalFilename, String kind) {
        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        return kind + "_" + UUID.randomUUID() + ext;
    }
}