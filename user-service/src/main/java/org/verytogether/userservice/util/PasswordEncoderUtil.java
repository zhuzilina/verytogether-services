package org.verytogether.userservice.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordEncoderUtil {

    private final PasswordEncoder passwordEncoder;

    public PasswordEncoderUtil() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String encodedPassword) {
        System.out.println("PasswordEncoderUtil.matches() called:");
        System.out.println("  Raw password: " + rawPassword);
        System.out.println("  Encoded password: " + encodedPassword);
        System.out.println("  PasswordEncoder class: " + passwordEncoder.getClass().getName());

        boolean result = passwordEncoder.matches(rawPassword, encodedPassword);
        System.out.println("  Match result: " + result);

        return result;
    }
}