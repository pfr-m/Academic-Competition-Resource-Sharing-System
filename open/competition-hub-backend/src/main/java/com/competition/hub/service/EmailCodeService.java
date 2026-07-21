package com.competition.hub.service;

public interface EmailCodeService {
    void sendRegisterCode(String email);

    void verifyCode(String email, String code);
}
