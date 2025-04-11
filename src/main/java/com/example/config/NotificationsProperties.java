package com.example.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class NotificationsProperties {

    @NotEmpty
    private String govNotifyApiKey;
    @NotEmpty
    private String portalUrl;

    @NotEmpty
    private String addNewUserEmailTemplate;

}
