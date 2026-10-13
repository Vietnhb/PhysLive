package com.example.backend.system.simulation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeacherSimulationPresentationTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void emptyBackendTimelineCannotProduceAFallbackContract() {
        ObjectNode timeline = json.createObjectNode();
        timeline.putArray("frames");
        assertThrows(com.example.backend.exception.ApiException.class,
                () -> TeacherSimulationPresentation.create(timeline, json.createObjectNode(), json.createObjectNode(), null));
    }

    @Test
    void serverSuppliesEffectiveParametersMetadataRangesAndSampleIntervals() throws Exception {
        ObjectNode timeline = (ObjectNode) json.readTree("""
                {"durationSeconds":2,"frames":[
                {"t":0,"values":{"opaque":5.04e-29,"unknown":7}},
                {"t":1,"values":{"opaque":-2,"unknown":8}},
                {"t":1,"values":{"opaque":9,"unknown":6}}]}
                """);
        var originalValues = timeline.path("frames").path(0).path("values").deepCopy();
        var spec = json.readTree("""
                {"parameters":[{"name":"m","value":2,"label":"  Exact (kg)  ","unit":"kg"}],
                 "observables":[{"field":"opaque","label":"Exact caption"}]}
                """);
        var metadata = json.readTree("""
                {"opaque":{"participantId":"body","participantLabel":"Original body",
                "label":"  Original label  ","symbol":"Q","unit":"J","quantity":"declared"}}
                """);
        var contract = TeacherSimulationPresentation.create(timeline, spec, metadata, json.readTree("{\"m\":3.123456789}"));
        assertEquals(3.123456789, contract.path("parameters").path("m").asDouble());
        assertEquals(originalValues, contract.path("initialFrame").path("values"));
        assertEquals(originalValues, timeline.path("frames").path(0).path("values"));
        assertEquals(0, timeline.path("frames").path(0).path("dt").asDouble());
        assertEquals(1, timeline.path("frames").path(1).path("dt").asDouble());
        assertEquals(0, timeline.path("frames").path(2).path("dt").asDouble());
        assertEquals(-2, contract.path("fieldRanges").path("opaque").path("min").asDouble());
        assertEquals(9, contract.path("fieldRanges").path("opaque").path("max").asDouble());
        assertEquals("  Original label  ", contract.path("scene").path("fields").path("opaque").path("label").asText());
        assertEquals("declared", contract.path("scene").path("fields").path("opaque").path("quantity").asText());
        assertTrue(contract.path("scene").path("fields").has("unknown"));
        assertEquals("  Exact (kg)  ", contract.path("scene").path("parameters").path("m").path("label").asText());
        assertTrue(contract.path("frameResolver").asText().startsWith("function"));
    }
}
