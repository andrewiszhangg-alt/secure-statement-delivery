package com.chitsa.accountstatement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.statement")
public class AccountStatementProperties {

    private int pageSize = 5;

    /** Storage context (bucket/folder) used for every statement media resource. */
    private String mediaContext = "account-statements";
}
