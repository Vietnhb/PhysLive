package com.example.backend.physics.validation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.backend.physics.model.ArchetypeCatalog;
import com.example.backend.physics.model.ComparisonTolerance;
import com.example.backend.physics.model.PhysicsArchetype;
import com.example.backend.physics.model.PhysicsQuantityDefinition;
import com.example.backend.physics.model.PhysicsValidationStatus;
import com.example.backend.physics.model.QuantityCardinality;
import com.example.backend.physics.model.SolverResult;
import com.example.backend.physics.model.ValidationResult;

@Component
public class ResultComparator {

    private static final MathContext MATH_CONTEXT = new MathContext(24, RoundingMode.HALF_EVEN);

    private final ArchetypeCatalog catalog;
    private final ValidationTolerancePolicy tolerancePolicy;

    public ResultComparator(ArchetypeCatalog catalog, ValidationTolerancePolicy tolerancePolicy) {
        this.catalog = catalog;
        this.tolerancePolicy = tolerancePolicy;
    }

    public ValidationResult compare(
            PhysicsArchetype archetype,
            SolverResult numerical,
            SolverResult reference) {
        Map<String, BigDecimal> absoluteErrors = new LinkedHashMap<>();
        Map<String, BigDecimal> relativeErrors = new LinkedHashMap<>();
        Map<String, ComparisonTolerance> tolerances = new LinkedHashMap<>();
        List<String> failures = new ArrayList<>();

        for (PhysicsQuantityDefinition output : catalog.require(archetype).outputs()) {
            ComparisonTolerance tolerance = tolerancePolicy.require(archetype, output.key());
            if (output.cardinality() == QuantityCardinality.SCALAR) {
                compareScalar(output.key(), numerical.outputs().get(output.key()),
                        reference.outputs().get(output.key()), tolerance,
                        absoluteErrors, relativeErrors, tolerances, failures);
            } else {
                compareSeries(output.key(), numerical.seriesOutputs().get(output.key()),
                        reference.seriesOutputs().get(output.key()), tolerance,
                        absoluteErrors, relativeErrors, tolerances, failures);
            }
        }

        PhysicsValidationStatus status = failures.isEmpty()
                ? PhysicsValidationStatus.PASSED
                : PhysicsValidationStatus.FAILED;
        return new ValidationResult(status, numerical, reference, absoluteErrors,
                relativeErrors, tolerances, failures.isEmpty() ? null : String.join("; ", failures));
    }

    private void compareSeries(
            String key,
            List<BigDecimal> actual,
            List<BigDecimal> expected,
            ComparisonTolerance tolerance,
            Map<String, BigDecimal> absoluteErrors,
            Map<String, BigDecimal> relativeErrors,
            Map<String, ComparisonTolerance> tolerances,
            List<String> failures) {
        tolerances.put(key, tolerance);
        if (actual == null || expected == null) {
            failures.add("Missing required series output: " + key);
            return;
        }
        if (actual.size() != expected.size()) {
            failures.add("Output series '" + key + "' has different lengths");
            return;
        }
        for (int index = 0; index < expected.size(); index++) {
            compareScalar(key + "[" + index + "]", actual.get(index), expected.get(index), tolerance,
                    absoluteErrors, relativeErrors, tolerances, failures);
        }
    }

    private void compareScalar(
            String key,
            BigDecimal actual,
            BigDecimal expected,
            ComparisonTolerance tolerance,
            Map<String, BigDecimal> absoluteErrors,
            Map<String, BigDecimal> relativeErrors,
            Map<String, ComparisonTolerance> tolerances,
            List<String> failures) {
        tolerances.put(key, tolerance);
        if (actual == null || expected == null) {
            failures.add("Missing required output: " + key);
            return;
        }
        BigDecimal absoluteError = actual.subtract(expected, MATH_CONTEXT).abs();
        BigDecimal denominator = expected.abs().max(tolerance.absolute());
        BigDecimal relativeError = denominator.signum() == 0
                ? BigDecimal.ZERO
                : absoluteError.divide(denominator, MATH_CONTEXT);
        absoluteErrors.put(key, clean(absoluteError));
        relativeErrors.put(key, clean(relativeError));

        boolean withinAbsolute = absoluteError.compareTo(tolerance.absolute()) <= 0;
        boolean withinRelative = relativeError.compareTo(tolerance.relative()) <= 0;
        if (!withinAbsolute && !withinRelative) {
            failures.add("Output '" + key + "' exceeded tolerance");
        }
    }

    private BigDecimal clean(BigDecimal value) {
        return value.signum() == 0 ? BigDecimal.ZERO : value.round(MATH_CONTEXT).stripTrailingZeros();
    }
}
