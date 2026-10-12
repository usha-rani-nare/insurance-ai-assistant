package com.insurance.assistant.dto;


public record LoginResponse(String token, Long userId, String name, String email) {
}