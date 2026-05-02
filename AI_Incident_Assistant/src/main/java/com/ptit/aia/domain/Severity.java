package com.ptit.aia.domain;

public enum Severity {
    P1,
    P2,
    P3;

    public Severity escalate() {
        return switch (this) {
            case P3 -> P2;
            case P2, P1 -> P1;
        };
    }
}
