package com.competition.hub.service.impl;

import com.competition.hub.service.EmailCodeService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailCodeServiceImpl implements EmailCodeService {
    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate stringRedisTemplate;
    private final JavaMailSender mailSender;
    private final String mailUsername;

    public EmailCodeServiceImpl(StringRedisTemplate stringRedisTemplate, JavaMailSender mailSender, @Value("${spring.mail.username:}") String mailUsername) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.mailSender = mailSender;
        this.mailUsername = mailUsername;
    }

    @Override
    public void sendRegisterCode(String email) {
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        stringRedisTemplate.opsForValue().set(codeKey(email), code, CODE_TTL);
        CompletableFuture.runAsync(() -> sendHtmlMail(email, code));
    }

    @Override
    public void verifyCode(String email, String code) {
        String cached = stringRedisTemplate.opsForValue().get(codeKey(email));
        if (cached == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱验证码已过期");
        }
        if (!cached.equals(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱验证码错误");
        }
        stringRedisTemplate.delete(codeKey(email));
    }

    private void sendHtmlMail(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            if (mailUsername != null && !mailUsername.isBlank()) {
                helper.setFrom(mailUsername);
            }
            helper.setTo(email);
            helper.setSubject("学科竞赛服务平台注册验证码");
            helper.setText("""
                    <div style="font-family:Arial,'Microsoft YaHei',sans-serif;padding:24px;background:#f6f9fc">
                      <div style="max-width:520px;margin:auto;background:white;border-radius:12px;padding:28px;border:1px solid #e5edf5">
                        <h2 style="margin:0;color:#102033">邮箱注册验证码</h2>
                        <p style="color:#607084">欢迎加入学科竞赛服务平台。请在 5 分钟内使用以下验证码完成注册：</p>
                        <div style="font-size:34px;font-weight:800;letter-spacing:8px;color:#2563eb;background:#eff6ff;border-radius:10px;padding:16px;text-align:center">%s</div>
                        <p style="color:#94a3b8;font-size:13px">如果不是你本人操作，请忽略这封邮件。</p>
                      </div>
                    </div>
                    """.formatted(code), true);
            mailSender.send(message);
        } catch (MessagingException error) {
            // Async mail errors are intentionally isolated from the HTTP request.
        }
    }

    private String codeKey(String email) {
        return "auth:email:code:" + email;
    }
}
