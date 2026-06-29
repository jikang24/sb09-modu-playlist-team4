package com.mopl.infra.s3;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

//Todo: s3관련 구현 진행
@Service
// @Profile("dev")
public class LocalS3Service implements S3Service {
    @Override
    public String upload(MultipartFile file) {
        return null;
    }

    @Override
    public void delete(String imageUrl) {

    }
}