package com.Author.Fitness.Security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "security.login-rate-limit")
public class LoginRateLimitProperties {

    @Min(1)
    private int maxAttemptsPerLoginIp = 5;

    @Min(1)
    private int maxAttemptsPerIp = 30;

    @NotNull
    private Duration window = Duration.ofMinutes(15);

    @Min(1)
    private int maxRegistrationAttemptsPerIp = 10;

    @NotNull
    private Duration registrationWindow = Duration.ofHours(1);

    @Min(1)
    private int maxFailedPasswordChangesPerUserIp = 5;

    @NotNull
    private Duration passwordChangeWindow = Duration.ofMinutes(15);

    @Min(1)
    private int maxAttemptsPerLogin = 30;

}
