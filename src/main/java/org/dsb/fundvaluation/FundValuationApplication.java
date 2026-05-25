package org.dsb.fundvaluation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FundValuationApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundValuationApplication.class, args);
    }

}
