package com.junyoung.dashboard.global.crypto;

import com.junyoung.dashboard.global.exception.IntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 연동 비밀값(노션 토큰, Apple 앱 암호)을 DB에 넣기 전에 AES-256-GCM으로 암호화한다 (이슈 #227).
 * 키는 환경변수 CREDENTIAL_KEY(32바이트를 base64로, `openssl rand -base64 32`) — DB가 새어도 키 없이는 못 푼다.
 * 저장 형식: "v1:" + base64(IV 12바이트 + 암호문 + 인증 태그)
 */
@Component
public class CredentialCipher {

    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    static final String KEY_HELP = "레포 루트 .env에 CREDENTIAL_KEY=(openssl rand -base64 32로 만든 값)를 넣고 API를 다시 띄워 주세요";

    private final SecretKey key;
    private final String keyProblem;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(@Value("${app.credential-key:}") String base64Key) {
        SecretKey parsed = null;
        String problem = null;
        if (base64Key == null || base64Key.isBlank()) {
            problem = "CREDENTIAL_KEY가 설정되지 않아 계정 정보를 저장할 수 없어요 — " + KEY_HELP;
        } else {
            try {
                byte[] raw = Base64.getDecoder().decode(base64Key.trim());
                if (raw.length != 32) {
                    problem = "CREDENTIAL_KEY는 32바이트여야 해요(지금 " + raw.length + "바이트) — " + KEY_HELP;
                } else {
                    parsed = new SecretKeySpec(raw, "AES");
                }
            } catch (IllegalArgumentException e) {
                problem = "CREDENTIAL_KEY가 올바른 base64가 아니에요 — " + KEY_HELP;
            }
        }
        this.key = parsed;
        this.keyProblem = problem;
    }

    public boolean ready() {
        return key != null;
    }

    public String encrypt(String plain) {
        requireReady();
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("계정 정보 암호화 실패", e);
        }
    }

    public String decrypt(String stored) {
        requireReady();
        if (stored == null || !stored.startsWith(PREFIX)) {
            throw undecryptable();
        }
        try {
            byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, IV_BYTES));
            byte[] plain = cipher.doFinal(in, IV_BYTES, in.length - IV_BYTES);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // 키가 바뀌었거나 값이 손상됨 — 원인(예외 메시지)에 비밀값이 섞이지 않도록 감싸지 않는다
            throw undecryptable();
        }
    }

    /** 키가 없으면 저장을 시작하기 전에(외부 로그인 확인 전에) 바로 알린다 */
    public void requireReady() {
        if (key == null) {
            throw new IntegrationException(HttpStatus.BAD_REQUEST, "CREDENTIAL_KEY_MISSING", keyProblem);
        }
    }

    private static IntegrationException undecryptable() {
        return new IntegrationException(HttpStatus.CONFLICT, "CREDENTIAL_UNREADABLE",
                "저장된 계정 정보를 풀 수 없어요 — CREDENTIAL_KEY가 바뀌었다면 설정에서 계정 정보를 다시 입력해 주세요");
    }
}
