package com.example.backend.system.school.dataio.service;

import com.example.backend.system.school.dto.SchoolClassContracts.ClassDetail;
import com.example.backend.system.school.dto.SchoolClassContracts.Person;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/** Flat roster for Excel: each student row includes its class and assigned teacher details. */
public final class SchoolClassCsv {
    private SchoolClassCsv() { }

    public static byte[] students(ClassDetail classroom) {
        StringBuilder csv = new StringBuilder("\uFEFFSTT,Lớp,Khối,Năm học,Môn học,Giáo viên,Email giáo viên,Họ tên học sinh,Email học sinh\r\n");
        String teacherNames = classroom.teachers().stream().map(Person::fullName).collect(Collectors.joining("; "));
        String teacherEmails = classroom.teachers().stream().map(Person::email).collect(Collectors.joining("; "));
        int index = 0;
        for (Person student : classroom.students()) {
            csv.append(++index).append(',').append(cell(classroom.name())).append(',').append(classroom.gradeLevel()).append(',')
                    .append(cell(classroom.schoolYear())).append(',').append(cell(classroom.subject())).append(',')
                    .append(cell(teacherNames)).append(',').append(cell(teacherEmails)).append(',')
                    .append(cell(student.fullName())).append(',').append(cell(student.email())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String cell(String value) {
        String text = value == null ? "" : value;
        String leading = text.stripLeading();
        if ((!text.isEmpty() && "\t\r\n".indexOf(text.charAt(0)) >= 0)
                || (!leading.isEmpty() && "=+@-".indexOf(leading.charAt(0)) >= 0)) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
