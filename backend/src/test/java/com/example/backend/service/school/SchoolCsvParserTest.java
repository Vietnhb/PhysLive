package com.example.backend.service.school;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.backend.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchoolCsvParserTest {
    private static List<com.example.backend.dto.school.SchoolImport.Row> parse(String csv) {
        return SchoolCsvParser.parse(csv.getBytes(StandardCharsets.UTF_8),
                List.of("fullName", "email", "initialPassword"), List.of("fullName", "email"));
    }

    @Test void readsBomQuotedCommasEscapedQuotesAndMultilineCells() {
        var rows = parse("\uFEFFfull_name,email,password\r\n\"Nguyen, \"\"An\"\"\nMinh\",an@example.edu.vn,12345678\r\n");
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().data().get("fullName")).isEqualTo("Nguyen, \"An\"\nMinh");
        assertThat(rows.getFirst().data().get("initialPassword")).isEqualTo("12345678");
    }

    @Test void preservesPasswordWhitespaceButTrimsProfileFields() {
        var row = parse("fullName,email,password\n\"  Nguyen An  \",\" an@example.edu.vn \",\"  Abcd1234!  \"\n").getFirst();
        assertThat(row.data().get("fullName")).isEqualTo("Nguyen An");
        assertThat(row.data().get("email")).isEqualTo("an@example.edu.vn");
        assertThat(row.data().get("initialPassword")).isEqualTo("  Abcd1234!  ");
    }

    @Test void rejectsMalformedUnknownDuplicateAndOversizedInput() {
        assertThatThrownBy(() -> parse("fullName,email\n\"An,an@example.edu.vn")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> parse("fullName,FULL_NAME,email\nAn,An,a@example.edu.vn")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> parse("fullName,email,schoolId\nAn,a@example.edu.vn,id")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> SchoolCsvParser.parse(new byte[SchoolCsvParser.MAX_BYTES + 1], List.of("email"), List.of("email")))
                .isInstanceOf(ApiException.class);
    }

    @Test void limitsEachImportToTwoHundredRows() {
        assertThat(parse("fullName,email\n" + "An,a@example.edu.vn\n".repeat(200))).hasSize(200);
        assertThatThrownBy(() -> parse("fullName,email\n" + "An,a@example.edu.vn\n".repeat(201)))
                .isInstanceOf(ApiException.class);
    }
}
