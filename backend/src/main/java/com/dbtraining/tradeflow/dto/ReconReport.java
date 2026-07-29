package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.BaseTrade;

import java.util.List;

public record ReconReport(int internalSize, int externalSize, List<BaseTrade> matched,
                          List<Discrepancy> discrepancies) {
}
