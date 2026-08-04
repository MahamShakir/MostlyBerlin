// backend/src/main/java/com/dbtraining/tradeflow/TradeflowApplication.java
package com.dbtraining.tradeflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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