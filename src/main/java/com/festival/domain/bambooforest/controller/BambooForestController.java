package com.festival.domain.bambooforest.controller;

import com.festival.common.util.ValidationUtils;
import com.festival.domain.bambooforest.dto.BamBooForestCreateReq;
import com.festival.domain.bambooforest.dto.BamBooForestRes;
import com.festival.domain.bambooforest.service.BamBooForestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/bambooforest")
public class BambooForestController {

    private final BamBooForestService bambooForestService;
    private final ValidationUtils validationUtils;

    @PostMapping
    public ResponseEntity<Long> createBamBooForest(@RequestBody @Valid BamBooForestCreateReq bamBooForestCreateReq) throws Exception {
        if (!validationUtils.isBamBooForestValid(bamBooForestCreateReq)) {
            throw new Exception();
        }
        return ResponseEntity.ok().body(bambooForestService.create(bamBooForestCreateReq));
    }

    @DeleteMapping("/{bamBooForestId}")
    public ResponseEntity<Void> deleteBamBooForest(@PathVariable Long bamBooForestId) {
        bambooForestService.delete(bamBooForestId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list")
    public ResponseEntity<Page<BamBooForestRes>> getList(String status, Pageable pageable) {
        return ResponseEntity.ok().body(bambooForestService.getBamBooForestList(status, pageable));
    }
}
