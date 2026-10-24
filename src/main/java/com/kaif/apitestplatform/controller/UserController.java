package com.kaif.apitestplatform.controller;

import com.kaif.apitestplatform.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<Map<String,Object>> me(@AuthenticationPrincipal User user){
        return ResponseEntity.ok(Map.of(
                "id",user.getId(),
                "email",user.getEmail(),
                "fullName",user.getFullName(),
                "role",user.getRole()
        ));
    }

    @GetMapping("/admin-check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String,String>> adminCheck(){
        return ResponseEntity.ok(Map.of("message","You are an admin"));
    }
}
