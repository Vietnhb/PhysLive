package com.example.backend.integration.ai;

/** Where AI usage is counted against an allowance: asked before every provider call, told what each call used. */
public interface AiUsageMeter {
    /** Refuses the call when the caller's allowance for the period is spent. */
    void requireAllowance();

    /** What one provider call used, in provider tokens, and what it was for. */
    void record(String operation, long tokens);
}
