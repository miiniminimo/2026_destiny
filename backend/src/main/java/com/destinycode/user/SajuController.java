package com.destinycode.user;

import com.destinycode.user.dto.SajuRequest;
import com.destinycode.user.dto.SajuResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/saju")
@RequiredArgsConstructor
public class SajuController {

    private final SajuService sajuService;

    // 사주 입력 등록 및 업데이트 (동시에 캐릭터 생성)
    @PostMapping
    public ResponseEntity<SajuResponse> saveSaju(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SajuRequest request) {
        SajuResponse response = sajuService.saveSajuInfo(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    // 내 사주 정보 및 캐릭터 요약 가져오기
    @GetMapping("/me")
    public ResponseEntity<SajuResponse> getMySaju(
            @AuthenticationPrincipal UserDetails userDetails) {
        SajuResponse response = sajuService.getSajuInfo(userDetails.getUsername());
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}
