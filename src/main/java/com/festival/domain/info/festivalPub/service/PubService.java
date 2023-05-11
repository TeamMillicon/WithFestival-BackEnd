package com.festival.domain.info.festivalPub.service;

import com.festival.common.utils.ImageServiceUtils;
import com.festival.common.vo.SearchCond;
import com.festival.domain.admin.data.entity.Admin;
import com.festival.domain.admin.exception.AdminException;
import com.festival.domain.admin.exception.AdminNotMatchException;
import com.festival.domain.admin.repository.AdminRepository;
import com.festival.domain.info.festivalPub.data.dto.request.PubRequest;
import com.festival.domain.info.festivalPub.data.dto.response.PubResponse;
import com.festival.domain.info.festivalPub.data.entity.file.PubImage;
import com.festival.domain.info.festivalPub.data.entity.pub.Pub;
import com.festival.domain.info.festivalPub.exception.PubNotFoundException;
import com.festival.domain.info.festivalPub.repository.PubImageRepository;
import com.festival.domain.info.festivalPub.repository.PubRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PubService {

    private final PubRepository pubRepository;
    private final PubImageRepository pubImageRepository;

    private final AdminRepository adminRepository;
    private final EntityManager em;
    private final ImageServiceUtils utils;

    @Value("${file.path}")
    private String filePath;

    public PubResponse create(Long adminId, PubRequest pubRequest, MultipartFile mainFile, List<MultipartFile> subFiles) throws IOException {

        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new AdminException("관리자를 찾을 수 없습니다."));

        Pub pub = new Pub(pubRequest, admin);
        pubRepository.save(pub);
        admin.addPub(pub);

        String mainFileName = utils.createStoreFileName(mainFile.getOriginalFilename());
        mainFile.transferTo(new File(filePath + mainFileName));
        PubImage pubImage = new PubImage(mainFileName, pub);
        pubImageRepository.save(pubImage);

        saveSubFiles(subFiles, pubImage);
        pub.setPubImage(pubImage);

        return PubResponse.of(pub, filePath);
    }

    public PubResponse modify(Long adminId, Long pubId, PubRequest pubRequest, MultipartFile mainFile, List<MultipartFile> subFiles) throws IOException {

        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new AdminException("관리자를 찾을 수 없습니다."));
        Pub pub = pubRepository.findById(pubId).orElseThrow(() -> new PubNotFoundException("주점을 찾을 수 없습니다."));

        if (pub.getAdmin().equals(admin)) {

            PubImage pubImage = pub.getPubImage();
            pubImage.modifyMainFileName(filePath, utils.createStoreFileName(mainFile.getOriginalFilename()), mainFile);

            if (!subFiles.isEmpty()) {
                List<String> list = utils.saveSubImages(filePath, subFiles);
                pubImage.modifySubFileNames(filePath, list);
            }
            pub.modify(pubRequest);

            em.flush();
            em.clear();

            return PubResponse.of(pub, filePath);
        } else {
            throw new AdminNotMatchException("권한이 없습니다.");
        }
    }

    public PubResponse delete(Long adminId, Long pubId) {

        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new AdminException("관리자를 찾을 수 없습니다."));
        Pub pub = pubRepository.findById(pubId).orElseThrow(() -> new PubNotFoundException("주점을 찾을 수 없습니다."));

        if (pub.getAdmin().equals(admin)) {

            pub.getPubImage().deleteFile(filePath);
            pubRepository.delete(pub);

            return PubResponse.of(pub, filePath);
        } else {
            throw new AdminNotMatchException("권한이 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public PubResponse getPub(Long adminId, Long pubId) {

        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new AdminException("관리자를 찾을 수 없습니다."));
        Pub pub = pubRepository.findById(pubId).orElseThrow(() -> new PubNotFoundException("주점을 찾을 수 없습니다."));

        if (pub.getAdmin().equals(admin)) {
            return PubResponse.of(pub, filePath);
        } else {
            throw new AdminNotMatchException("권한이 없습니다.");
        }
    }

    @Transactional(readOnly = true)
    public Page<PubResponse> getPubs(Long adminId, int offset) {

        Pageable pageable = PageRequest.of(offset, 6);
        SearchCond cond = new SearchCond(adminId);

        Page<Pub> findPubs = pubRepository.findByIdPubs(cond, pageable);
        return findPubs.map(pub -> PubResponse.of(pub, filePath));
    }

    @Transactional(readOnly = true)
    public Page<PubResponse> getPubsForState(Long adminId, int offset, Boolean state) {

        Pageable pageable = PageRequest.of(offset, 6);
        SearchCond cond = new SearchCond(adminId, state);

        Page<Pub> findPubs = pubRepository.findByIdPubsWithState(cond, pageable);
        return findPubs.map(pub -> PubResponse.of(pub, filePath));
    }

    private void saveSubFiles(List<MultipartFile> subFiles, PubImage pubImage) throws IOException {
        List<String> subFilePaths = utils.saveSubImages(filePath, subFiles);
        pubImage.saveSubFileNames(subFilePaths);
    }
}
