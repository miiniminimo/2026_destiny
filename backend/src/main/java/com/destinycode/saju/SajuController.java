package com.destinycode.saju;

import com.destinycode.common.ApiResponse;
import com.destinycode.saju.dto.SajuRequest;
import com.destinycode.saju.dto.SajuResponse;
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

    @PostMapping
    public ResponseEntity<ApiResponse<SajuResponse>> saveSaju(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SajuRequest request) {
        SajuResponse response = sajuService.saveSaju(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<SajuResponse>> getMySaju(
            @AuthenticationPrincipal UserDetails userDetails) {
        SajuResponse response = sajuService.getSaju(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
