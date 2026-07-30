// backend/src/main/java/com/dbtraining/tradeflow/TradeflowApplication.java
package com.dbtraining.tradeflow;

import com.dbtraining.tradeflow.model.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
public class TradeflowApplication {

    public static void main(String[] args) {
        printBanner();
        SpringApplication.run(TradeflowApplication.class, args);
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  Deutsche Bank — TDI 2026 Graduate Technical Training");
        System.out.println("  Intermediate Track — Case Study: Trade Reconciliation");
        System.out.println();
    }


}