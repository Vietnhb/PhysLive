package com.example.backend.system.simulation.service;

import com.fasterxml.jackson.databind.JsonNode;

/** Where the creation flow reports what the law catalogues could not check, for the reviewers who maintain them. */
public interface LawGapSink {
    /** Records the gaps of one checked model; returns how many kinds of gap it had. Never throws. */
    int record(String schemaId, String topic, String description, JsonNode spec, JsonNode validation);
}
