package com.festival.old.domain.foodTruck.service.impl;

import com.festival.old.common.base.CommonIdResponse;
import com.festival.old.common.utils.ImageServiceUtils;
import com.festival.old.common.vo.SearchCond;
import com.festival.old.domain.admin.data.entity.Admin;
import com.festival.old.domain.admin.exception.AdminNotFoundException;
import com.festival.old.domain.admin.exception.AdminNotMatchException;
import com.festival.old.domain.admin.repository.AdminRepository;
import com.festival.old.domain.foodTruck.data.dto.request.FoodTruckRequest;
import com.festival.old.domain.foodTruck.data.dto.response.FoodTruckListResponse;
import com.festival.old.domain.foodTruck.data.dto.response.FoodTruckResponse;
import com.festival.old.domain.foodTruck.data.entity.FoodTruck;
import com.festival.old.domain.foodTruck.data.entity.FoodTruckImage;
import com.festival.old.domain.foodTruck.repository.FoodTruckImageRepository;
import com.festival.old.domain.foodTruck.repository.FoodTruckRepository;
import com.festival.old.domain.foodTruck.service.FoodTruckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class FoodTruckServiceImpl implements FoodTruckService {

    private final FoodTruckRepository foodTruckRepository;
    private final FoodTruckImageRepository foodTruckImageRepository;

    private final AdminRepository adminRepository;

    @Value("${file.path}")
    private String filePath;

    @Override
    public CommonIdResponse createFoodTruck(FoodTruckRequest foodTruckRequest, MultipartFile mainFile, List<MultipartFile> subFiles) throws Exception {
        log.debug("Start : FoodTruckServiceImpl : createFoodTruck");
        if (foodTruckRequest == null)
            throw new Exception("FoodTruckRequest Is Null");
        if (mainFile.getName().isBlank())
            throw new Exception("Main Image File Is Null");
        try {
            String name = SecurityContextHolder.getContext().getAuthentication().getName();
            Admin admin = adminRepository.findByUsername(name).orElseThrow(() -> new AdminNotFoundException("관리자를 찾을 수 없습니다."));

            FoodTruck foodTruck = new FoodTruck(foodTruckRequest);
            foodTruck.connectAdmin(admin);
            foodTruckRepository.save(foodTruck);

            String mainFileName = saveMainFile(mainFile);
            FoodTruckImage foodTruckImage = new FoodTruckImage(mainFileName, foodTruck);
            foodTruckImageRepository.save(foodTruckImage);

            saveSubFiles(subFiles, foodTruckImage);
            foodTruck.connectFoodTruckImage(foodTruckImage);

            return new CommonIdResponse(foodTruck.getId());
        } catch (RuntimeException re) {
            log.error("### FoodTruckServiceImpl createFoodTruck: RuntimeException occur ###");
            throw new RuntimeException("CreateFoodTruck Exception");
        }
    }

    @Override
    public FoodTruckResponse getFoodTruck(Long foodTruckId) {
        log.debug("Start : FoodTruckServiceImpl : getFoodTruck");
        //TODO: JWT_USER_PARSER_FOR_ADMIN
        FoodTruck foodTruck = foodTruckRepository.findById(foodTruckId).orElseThrow(() -> new IllegalArgumentException("해당 푸드트럭 게시글이 존재하지 않습니다."));
        return FoodTruckResponse.of(foodTruck, filePath);
    }

    @Override
    public Page<FoodTruckListResponse> getFoodTruckList(int offset, boolean state) {

        Pageable pageable = PageRequest.of(offset, 20);
        SearchCond cond = new SearchCond(state);

        Page<FoodTruck> foodTruckList = foodTruckRepository.findByIdTrucks(cond, pageable);
        return foodTruckList.map(foodTruck -> FoodTruckListResponse.of(foodTruck, filePath));
    }

    @Override
    public CommonIdResponse updateFoodTruck(Long foodTruckId, FoodTruckRequest foodTruckRequest, MultipartFile mainFile, List<MultipartFile> subFiles) throws IOException {

        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        Admin admin = adminRepository.findByUsername(name).orElseThrow(() -> new AdminNotFoundException("관리자를 찾을 수 없습니다."));

        FoodTruck foodTruck = foodTruckRepository.findById(foodTruckId).orElseThrow(() -> new IllegalArgumentException("해당 푸드트럭 게시글이 존재하지 않습니다."));

        if (foodTruck.getAdmin().equals(admin)) {

            FoodTruckImage foodTruckImage = foodTruck.getFoodTruckImage();
            foodTruckImage.modifyMainFileName(filePath, ImageServiceUtils.createStoreFileName(mainFile.getOriginalFilename()), mainFile);

            if (!subFiles.isEmpty()) {
                List<String> list = ImageServiceUtils.saveSubImages(filePath, subFiles);
                foodTruckImage.modifySubFileNames(filePath, list);
            }
            foodTruck.modify(foodTruckRequest);


            return new CommonIdResponse(foodTruck.getId());
        } else {
            throw new AdminNotMatchException("권한이 없습니다.");
        }
    }

    @Override
    public CommonIdResponse deleteFoodTruck(Long foodTruckId) {
        //TODO: JWT_USER_PARSER_FOR_ADMIN(어드민 임시 데이터 1L)
        String name = SecurityContextHolder.getContext().getAuthentication().getName();
        Admin admin = adminRepository.findByUsername(name).orElseThrow(() -> new AdminNotFoundException("관리자를 찾을 수 없습니다."));

        FoodTruck foodTruck = foodTruckRepository.findById(foodTruckId).orElseThrow(() -> new IllegalArgumentException("해당 푸드트럭 게시글이 존재하지 않습니다."));

        if (foodTruck.getAdmin().equals(admin)) {
            foodTruck.getFoodTruckImage().deleteFile(filePath);
            foodTruckRepository.delete(foodTruck);

            return new CommonIdResponse(foodTruck.getId());
        } else {
            throw new AdminNotMatchException("권한이 없습니다.");
        }
    }

    private String saveMainFile(MultipartFile mainFile) throws IOException {
        String mainFileName = ImageServiceUtils.createStoreFileName(mainFile.getOriginalFilename());
        mainFile.transferTo(new File(filePath + mainFileName));
        return mainFileName;
    }

    private void saveSubFiles(List<MultipartFile> subFiles, FoodTruckImage foodTruckImage) throws IOException {
        List<String> subFilePaths = ImageServiceUtils.saveSubImages(filePath, subFiles);
        foodTruckImage.saveSubFileNames(subFilePaths);
    }
}