package com.courtly.api.auth;

import com.courtly.api.auth.dto.AuthResponse;
import com.courtly.api.auth.dto.LoginRequest;
import com.courtly.api.auth.dto.RegisterRequest;
import com.courtly.api.auth.dto.UserResponse;
import com.courtly.service.auth.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 2.1.1 dang ky, 2.1.2 dang nhap, 2.1.4 dang xuat. */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 2.1.1 - dang ky tai khoan nguoi choi. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /** 2.1.2 - dang nhap bang email hoac so dien thoai. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * 2.1.4 - dang xuat.
     *
     * <p>Phien dang dung access token khong luu o server (quyet dinh cua du an), nen viec
     * thu hoi thuc te la frontend xoa token. Endpoint nay ton tai de frontend co mot diem
     * goi thong nhat va de sau nay them thu hoi token ma khong phai doi hop dong API.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    /** Thong tin tai khoan dang dang nhap, dung de dung lai header sau khi tai lai trang. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.currentUser(UUID.fromString(jwt.getSubject())));
    }
}
