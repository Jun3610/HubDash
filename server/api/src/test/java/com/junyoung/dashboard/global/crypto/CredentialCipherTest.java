package com.junyoung.dashboard.global.crypto;

import com.junyoung.dashboard.global.exception.IntegrationException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CredentialCipherTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @Test
    void roundTripsAndNeverStoresPlainText() {
        CredentialCipher cipher = new CredentialCipher(KEY);
        String secret = "ntn_1234567890abcdefSECRET6SK";

        String stored = cipher.encrypt(secret);

        assertThat(stored).startsWith("v1:").doesNotContain(secret).doesNotContain("SECRET");
        assertThat(cipher.decrypt(stored)).isEqualTo(secret);
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        CredentialCipher cipher = new CredentialCipher(KEY);
        assertThat(cipher.encrypt("같은 값")).isNotEqualTo(cipher.encrypt("같은 값"));
    }

    @Test
    void otherKeyOrTamperedValueCannotBeDecrypted() {
        String stored = new CredentialCipher(KEY).encrypt("abcd-efgh-ijkl-mnop");

        assertThatThrownBy(() -> new CredentialCipher(OTHER_KEY).decrypt(stored))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("다시 입력");
        String tampered = stored.substring(0, stored.length() - 4) + (stored.endsWith("AAAA") ? "BBBB" : "AAAA");
        assertThatThrownBy(() -> new CredentialCipher(KEY).decrypt(tampered))
                .isInstanceOf(IntegrationException.class);
    }

    @Test
    void missingOrWrongSizedKeyGivesClearErrorOnUse() {
        CredentialCipher missing = new CredentialCipher("");
        assertThat(missing.ready()).isFalse();
        assertThatThrownBy(() -> missing.encrypt("x"))
                .isInstanceOfSatisfying(IntegrationException.class,
                        e -> assertThat(e.getCode()).isEqualTo("CREDENTIAL_KEY_MISSING"))
                .hasMessageContaining("openssl rand -base64 32");

        CredentialCipher shortKey = new CredentialCipher(Base64.getEncoder().encodeToString(new byte[16]));
        assertThatThrownBy(() -> shortKey.encrypt("x")).hasMessageContaining("32바이트");

        assertThatThrownBy(() -> new CredentialCipher("not base64!!").encrypt("x")).hasMessageContaining("base64");
    }
}
