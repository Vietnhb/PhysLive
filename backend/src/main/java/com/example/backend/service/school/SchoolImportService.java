package com.example.backend.service.school;

import com.example.backend.dto.admin.CreateManagedUserRequest;
import com.example.backend.dto.school.SchoolImport;
import com.example.backend.entity.User;
import com.example.backend.entity.enums.PermissionCode;
import com.example.backend.entity.enums.RoleName;
import com.example.backend.entity.school.School;
import com.example.backend.entity.school.SchoolClass;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.school.ClassEnrollmentRepository;
import com.example.backend.repository.school.ClassTeacherAssignmentRepository;
import com.example.backend.repository.school.SchoolClassRepository;
import com.example.backend.repository.school.SchoolRepository;
import com.example.backend.security.PasswordPolicy;
import com.example.backend.service.CurrentUserService;
import com.example.backend.service.account.AccountAccessService;
import com.example.backend.service.account.RoleValidationService;
import com.example.backend.service.admin.AdminService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Previews bounded school imports and revalidates them inside one commit transaction. */
@Service
@RequiredArgsConstructor
public class SchoolImportService {
    private final SchoolRepository schools;
    private final SchoolClassRepository classes;
    private final ClassEnrollmentRepository enrollments;
    private final ClassTeacherAssignmentRepository assignments;
    private final UserRepository users;
    private final CurrentUserService currentUser;
    private final RoleValidationService roles;
    private final AccountAccessService accountAccess;
    private final LicenseCheckService license;
    private final AdminService admin;
    private final SchoolClassService classService;
    private final EntityManager entityManager;
    private final byte[] previewSecret = randomBytes(32);

    private static final List<String> STAFF_PERMISSIONS = Arrays.stream(PermissionCode.values())
            .filter(code -> code.role() == RoleName.STAFF).map(Enum::name).toList();

    public static List<String> columns(SchoolImport.Kind kind) {
        if (kind == null) throw bad("Select an import type");
        return switch (kind) {
            case USERS -> List.of("fullName", "dateOfBirth", "email", "initialPassword", "role", "permissions", "classCode", "schoolYear", "avatarUrl");
            case CLASSES -> List.of("classCode", "gradeLevel", "schoolYear", "subject");
            case ENROLLMENTS -> List.of("studentEmail", "classCode", "schoolYear");
            case TEACHER_ASSIGNMENTS -> List.of("teacherEmail", "classCode", "schoolYear");
        };
    }

    private static List<String> required(SchoolImport.Kind kind) {
        return switch (kind) {
            case USERS -> List.of("fullName", "dateOfBirth", "email", "initialPassword", "role");
            case CLASSES -> List.of("classCode", "gradeLevel", "schoolYear");
            case ENROLLMENTS -> List.of("studentEmail", "classCode", "schoolYear");
            case TEACHER_ASSIGNMENTS -> List.of("teacherEmail", "classCode", "schoolYear");
        };
    }

    @Transactional(readOnly = true)
    public byte[] template(UUID schoolId, SchoolImport.Kind kind) {
        requireAccess(schoolId, kind);
        int year = LocalDate.now().getMonthValue() >= 8 ? LocalDate.now().getYear() : LocalDate.now().getYear() - 1;
        String schoolYear = year + "-" + (year + 1);
        List<String> example = switch (kind) {
            case USERS -> List.of("Nguyen Minh An", "2010-09-15", "minhan@example.edu.vn", "ChangeMe2026!", "STUDENT", "", "", "", "");
            case CLASSES -> List.of("10A1", "10", schoolYear, "Physics");
            case ENROLLMENTS -> List.of("minhan@example.edu.vn", "10A1", schoolYear);
            case TEACHER_ASSIGNMENTS -> List.of("teacher@example.edu.vn", "10A1", schoolYear);
        };
        String teacher = kind == SchoolImport.Kind.USERS
                ? csv(List.of("Tran Thu Ha", "1988-04-23", "teacher@example.edu.vn", "ChangeMe2026!", "STAFF", "TEACH", "", "", "")) + "\r\n" : "";
        return ("\uFEFF" + csv(columns(kind)) + "\r\n" + csv(example) + "\r\n" + teacher).getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public SchoolImport.Preview previewFile(UUID schoolId, SchoolImport.Kind kind, byte[] bytes) {
        requireAccess(schoolId, kind);
        return preview(schoolId, new SchoolImport.Request(kind, SchoolCsvParser.parse(bytes, columns(kind), required(kind))));
    }

    @Transactional(readOnly = true)
    public SchoolImport.Preview preview(UUID schoolId, SchoolImport.Request request) {
        checkRequest(request);
        User actor = requireAccess(schoolId, request.kind());
        School school = schools.findById(schoolId).orElseThrow(() -> ApiException.notFound("School not found"));
        requireActiveSchool(school);
        return validate(schoolId, actor, school, request);
    }

    @Transactional
    public SchoolImport.Result commit(UUID schoolId, SchoolImport.Request request) {
        checkRequest(request);
        User actor = requireAccess(schoolId, request.kind());
        School school = schools.findByIdForUpdate(schoolId).orElseThrow(() -> ApiException.notFound("School not found"));
        entityManager.refresh(school, LockModeType.PESSIMISTIC_WRITE);
        requireActiveSchool(school);
        if (!hasRole(actor, RoleName.MANAGER)) license.requireWriteAccess(actor);
        requirePreview(schoolId, actor, request);
        lockReferences(schoolId, request);
        SchoolImport.Preview preview = validate(schoolId, actor, school, request);
        if (!preview.canCommit()) throw bad("Data changed or contains errors; preview it again");
        List<SchoolImport.Credential> credentials = new ArrayList<>();
        for (SchoolImport.PreviewRow row : preview.rows()) {
            Map<String, String> data = row.data();
            switch (request.kind()) {
                case USERS -> {
                    var created = admin.createUser(new CreateManagedUserRequest(data.get("email"), data.get("initialPassword"),
                            data.get("fullName"), data.get("role"), schoolId.toString(), LocalDate.parse(data.get("dateOfBirth")),
                            blankToNull(data.get("avatarUrl")), permissionCodes(data.get("permissions"))));
                    if (!data.get("classCode").isBlank()) {
                        SchoolClass target = findClass(schoolId, data.get("classCode"), data.get("schoolYear"));
                        if (RoleName.STUDENT.matches(data.get("role"))) classService.enrollStudent(schoolId, target.getId(), created.id());
                        else classService.assignTeacher(schoolId, target.getId(), created.id());
                    }
                    credentials.add(new SchoolImport.Credential(data.get("fullName"), LocalDate.parse(data.get("dateOfBirth")),
                            data.get("email"), data.get("initialPassword"), data.get("role"), data.get("classCode"), data.get("schoolYear")));
                }
                case CLASSES -> classService.create(schoolId, new SchoolClassService.ClassRequest(data.get("classCode"),
                        Integer.valueOf(data.get("gradeLevel")), data.get("schoolYear"), data.get("subject")));
                case ENROLLMENTS -> classService.enrollStudent(schoolId, findClass(schoolId, data.get("classCode"), data.get("schoolYear")).getId(),
                        users.findFirstByEmailIgnoreCase(data.get("studentEmail")).orElseThrow().getId());
                case TEACHER_ASSIGNMENTS -> classService.assignTeacher(schoolId, findClass(schoolId, data.get("classCode"), data.get("schoolYear")).getId(),
                        users.findFirstByEmailIgnoreCase(data.get("teacherEmail")).orElseThrow().getId());
            }
        }
        return new SchoolImport.Result(request.kind(), preview.rows().size(), List.copyOf(credentials));
    }

    private SchoolImport.Preview validate(UUID schoolId, User actor, School school, SchoolImport.Request request) {
        List<SchoolClass> schoolClasses = classes.findBySchoolIdAndIsActiveTrue(schoolId);
        List<User> schoolUsers = request.kind() == SchoolImport.Kind.USERS ? users.findBySchoolId(schoolId) : List.of();
        List<SchoolImport.Row> normalized = request.rows().stream().map(row -> normalize(request.kind(), row)).toList();
        Map<String, Long> emailCounts = new HashMap<>(), identityCounts = new HashMap<>(), classCounts = new HashMap<>();
        for (SchoolImport.Row row : normalized) {
            Map<String, String> data = row.data();
            String emailKey = switch (request.kind()) {
                case USERS -> data.get("email");
                case ENROLLMENTS -> data.get("studentEmail") + "|" + data.get("schoolYear");
                case TEACHER_ASSIGNMENTS -> data.get("teacherEmail") + "|" + classKey(data.get("classCode"), data.get("schoolYear"));
                case CLASSES -> "";
            };
            emailCounts.merge(emailKey, 1L, Long::sum);
            identityCounts.merge(identity(data.get("fullName"), data.get("dateOfBirth")), 1L, Long::sum);
            classCounts.merge(classKey(data.get("classCode"), data.get("schoolYear")), 1L, Long::sum);
        }
        long newStudents = normalized.stream().filter(row -> "STUDENT".equals(row.data().get("role"))).count();
        boolean userQuota = request.kind() == SchoolImport.Kind.USERS && school.getStudentQuota() != null
                && users.countActiveStudents(schoolId) + newStudents > school.getStudentQuota();
        long incomingEnrollments = request.kind() == SchoolImport.Kind.ENROLLMENTS ? normalized.size()
                : request.kind() == SchoolImport.Kind.USERS ? normalized.stream()
                        .filter(row -> "STUDENT".equals(row.data().get("role")) && !row.data().get("classCode").isBlank()).count() : 0;
        boolean enrollmentQuota = incomingEnrollments > 0 && school.getStudentQuota() != null
                && enrollments.countActiveStudentsBySchoolId(schoolId) + incomingEnrollments > school.getStudentQuota();
        List<SchoolImport.PreviewRow> result = new ArrayList<>();
        for (SchoolImport.Row row : normalized) {
            Map<String, String> data = row.data();
            List<String> errors = new ArrayList<>(), warnings = new ArrayList<>();
            List<SchoolImport.Match> matches = new ArrayList<>();
            if (request.kind() == SchoolImport.Kind.USERS) {
                requiredText(data.get("fullName"), 120, "Full name", errors);
                LocalDate dob = date(data.get("dateOfBirth"), errors);
                validEmail(data.get("email"), errors);
                if (emailCounts.get(data.get("email")) > 1) errors.add("Email is duplicated in the file");
                if (users.findFirstByEmailIgnoreCase(data.get("email")).isPresent()) errors.add("Email is already in use");
                if (!Set.of("STAFF", "STUDENT").contains(data.get("role"))) errors.add("Role must be STAFF or STUDENT");
                try { PasswordPolicy.requireValid(data.get("initialPassword")); }
                catch (ApiException ex) { errors.add("Initial password must satisfy the password policy"); }
                List<String> codes = permissionCodes(data.get("permissions"));
                if (RoleName.STAFF.matches(data.get("role"))) {
                    if (codes == null || !STAFF_PERMISSIONS.containsAll(codes)) errors.add("Invalid STAFF permissions");
                } else if (!data.get("permissions").isBlank()) errors.add("Permissions apply only to STAFF");
                if (!data.get("avatarUrl").isBlank() && (data.get("avatarUrl").length() > 2000
                        || !data.get("avatarUrl").matches("https?://[^\\s]+"))) errors.add("Avatar URL is invalid");
                if (dob != null) for (User existing : schoolUsers) {
                    if (dob.equals(existing.getDateOfBirth()) && identity(data.get("fullName"), "").equals(identity(existing.getFullName(), "")))
                        matches.add(new SchoolImport.Match(existing.getId(), existing.getEmail(), existing.getFullName(), existing.getDateOfBirth()));
                }
                if (!matches.isEmpty()) warnings.add("Another school account has the same name and date of birth");
                if (identityCounts.get(identity(data.get("fullName"), data.get("dateOfBirth"))) > 1) warnings.add("Another row has the same name and date of birth");
                if (userQuota && RoleName.STUDENT.matches(data.get("role"))) errors.add("Student quota would be exceeded");
                if (data.get("classCode").isBlank() != data.get("schoolYear").isBlank()) errors.add("classCode and schoolYear must be supplied together");
                if (RoleName.STAFF.matches(data.get("role")) && !data.get("classCode").isBlank()
                        && (codes == null || !codes.contains(PermissionCode.TEACH.name())))
                    errors.add("STAFF assigned to a class must have TEACH permission");
            }
            if (request.kind() == SchoolImport.Kind.CLASSES) {
                requiredText(data.get("classCode"), 100, "Class code", errors);
                validYear(data.get("schoolYear"), errors);
                if (!Set.of("10", "11", "12").contains(data.get("gradeLevel"))) errors.add("Grade level must be 10, 11 or 12");
                if (data.get("subject").length() > 50) errors.add("Subject must not exceed 50 characters");
                if (classCounts.get(classKey(data.get("classCode"), data.get("schoolYear"))) > 1) errors.add("Class is duplicated in the file");
                if (classes.existsBySchoolIdAndNameIgnoreCaseAndSchoolYear(schoolId, data.get("classCode"), data.get("schoolYear"))) errors.add("Class already exists");
            } else if (!data.get("classCode").isBlank() || request.kind() != SchoolImport.Kind.USERS) {
                requiredText(data.get("classCode"), 100, "Class code", errors); validYear(data.get("schoolYear"), errors);
                SchoolClass target = schoolClasses.stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())
                        && item.getName().equalsIgnoreCase(data.get("classCode")) && item.getSchoolYear().equals(data.get("schoolYear"))).findFirst().orElse(null);
                if (target == null) errors.add("Active class was not found in this school and year");
                if (request.kind() == SchoolImport.Kind.ENROLLMENTS || request.kind() == SchoolImport.Kind.TEACHER_ASSIGNMENTS) {
                    boolean student = request.kind() == SchoolImport.Kind.ENROLLMENTS;
                    String email = data.get(student ? "studentEmail" : "teacherEmail"); validEmail(email, errors);
                    User person = users.findFirstByEmailIgnoreCase(email).orElse(null);
                    if (!validMember(person, schoolId, student ? RoleName.STUDENT : RoleName.STAFF)) errors.add(student ? "Student is not valid in this school" : "Teacher is not valid in this school");
                    else if (target != null) {
                        if (student && enrollments.findActiveEnrollment(person.getId(), data.get("schoolYear")).isPresent()) errors.add("Student already has a class in this school year");
                        if (!student && !accountAccess.canTeach(person)) errors.add("Teacher does not have TEACH permission");
                        if (!student && assignments.existsBySchoolClassIdAndTeacherIdAndIsActiveTrue(target.getId(), person.getId())) errors.add("Teacher is already assigned to this class");
                    }
                    String key = student ? email + "|" + data.get("schoolYear") : email + "|" + classKey(data.get("classCode"), data.get("schoolYear"));
                    if (emailCounts.get(key) > 1) errors.add("Assignment is duplicated in the file");
                }
            }
            if (enrollmentQuota && (request.kind() == SchoolImport.Kind.ENROLLMENTS
                    || "STUDENT".equals(data.get("role")) && !data.get("classCode").isBlank()))
                errors.add("Student enrollment quota would be exceeded");
            result.add(new SchoolImport.PreviewRow(row.row(), data, List.copyOf(errors), List.copyOf(warnings), List.copyOf(matches)));
        }
        int invalid = (int) result.stream().filter(row -> !row.errors().isEmpty()).count();
        String token = invalid == 0 ? signPreview(schoolId, actor, new SchoolImport.Request(request.kind(), normalized), Instant.now().getEpochSecond()) : null;
        return new SchoolImport.Preview(request.kind(), columns(request.kind()), List.copyOf(result), result.size() - invalid, invalid, invalid == 0, token);
    }

    private User requireAccess(UUID schoolId, SchoolImport.Kind kind) {
        columns(kind);
        User actor = currentUser.requireCurrentUser();
        boolean head = accountAccess.isDepartmentHead(actor) && actor.getSchool() != null && schoolId.equals(actor.getSchool().getId());
        if (!roles.canManageSchool(actor, schoolId) && !head) throw ApiException.forbidden("School data access denied");
        if (head && (kind == SchoolImport.Kind.USERS || kind == SchoolImport.Kind.CLASSES))
            throw ApiException.forbidden("Department heads can import enrollments and teacher assignments only");
        return actor;
    }

    private static void checkRequest(SchoolImport.Request request) {
        if (request == null || request.kind() == null || request.rows() == null || request.rows().isEmpty()) throw bad("Import contains no rows");
        if (request.rows().size() > SchoolCsvParser.MAX_ROWS) throw bad("Each import supports at most 200 rows");
        Set<Integer> numbers = new HashSet<>(); long bytes = 0;
        for (SchoolImport.Row row : request.rows()) {
            if (row == null || row.row() < 1 || !numbers.add(row.row()) || row.data() == null) throw bad("Invalid import row");
            for (Map.Entry<String, String> cell : row.data().entrySet()) {
                if (!columns(request.kind()).contains(cell.getKey()) || cell.getValue() == null || cell.getValue().length() > 10_000
                        || cell.getValue().indexOf('\0') >= 0) throw bad("Invalid import column or cell");
                bytes += cell.getValue().getBytes(StandardCharsets.UTF_8).length;
            }
        }
        if (bytes > SchoolCsvParser.MAX_BYTES) throw bad("Import data must not exceed 1 MB");
    }

    private static SchoolImport.Row normalize(SchoolImport.Kind kind, SchoolImport.Row row) {
        Map<String, String> data = new LinkedHashMap<>();
        for (String column : columns(kind)) {
            String value = row.data().getOrDefault(column, "");
            data.put(column, column.equals("initialPassword") ? value : value.trim());
        }
        for (String column : List.of("email", "studentEmail", "teacherEmail")) if (data.containsKey(column)) data.put(column, data.get(column).toLowerCase(Locale.ROOT));
        if (kind == SchoolImport.Kind.USERS) {
            data.put("role", data.get("role").toUpperCase(Locale.ROOT));
            List<String> codes = permissionCodes(data.get("permissions"));
            data.put("permissions", codes == null ? "" : String.join("|", codes));
            if (RoleName.STAFF.matches(data.get("role")) && codes == null) data.put("permissions", PermissionCode.TEACH.name());
            if (data.get("initialPassword").isBlank()) data.put("initialPassword", "Pl!" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(12)));
        }
        return new SchoolImport.Row(row.row(), data);
    }

    private void lockReferences(UUID schoolId, SchoolImport.Request request) {
        if (request.kind() == SchoolImport.Kind.CLASSES) return;
        Set<String> classKeys = new HashSet<>(), emails = new HashSet<>();
        for (SchoolImport.Row row : request.rows()) {
            Map<String, String> data = row.data();
            if (!data.getOrDefault("classCode", "").isBlank()) classKeys.add(classKey(data.get("classCode"), data.get("schoolYear")));
            if (request.kind() == SchoolImport.Kind.ENROLLMENTS) emails.add(data.get("studentEmail"));
            if (request.kind() == SchoolImport.Kind.TEACHER_ASSIGNMENTS) emails.add(data.get("teacherEmail"));
        }
        classes.findBySchoolIdAndIsActiveTrue(schoolId).stream().filter(item -> classKeys.contains(classKey(item.getName(), item.getSchoolYear())))
                .sorted(Comparator.comparing(SchoolClass::getId)).forEach(item -> entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE));
        emails.stream().map(users::findFirstByEmailIgnoreCase).flatMap(Optional::stream)
                .filter(item -> item.getSchool() != null && schoolId.equals(item.getSchool().getId()))
                .sorted(Comparator.comparing(User::getId)).forEach(item -> entityManager.refresh(item, LockModeType.PESSIMISTIC_WRITE));
    }

    private SchoolClass findClass(UUID schoolId, String code, String year) {
        return classes.findBySchoolIdAndIsActiveTrue(schoolId).stream().filter(item -> Boolean.TRUE.equals(item.getIsActive())
                && item.getName().equalsIgnoreCase(code) && item.getSchoolYear().equals(year)).findFirst()
                .orElseThrow(() -> bad("Class changed; preview the import again"));
    }
    private static List<String> permissionCodes(String cell) {
        if (cell == null || cell.isBlank()) return null;
        List<String> codes = Arrays.stream(cell.toUpperCase(Locale.ROOT).split("[|;+,\\s]+"))
                .filter(code -> !code.isBlank()).distinct().toList();
        return codes.isEmpty() ? null : codes;
    }
    private static boolean validMember(User user, UUID schoolId, RoleName role) {
        return user != null && user.isEnabled() && Boolean.TRUE.equals(user.getActive()) && user.getSchool() != null
                && schoolId.equals(user.getSchool().getId()) && user.getRole() != null && role.matches(user.getRole().getName());
    }
    private static void requireActiveSchool(School school) { if (!school.isActive()) throw ApiException.forbidden("School is suspended"); }
    private static boolean hasRole(User user, RoleName role) { return user != null && user.getRole() != null && role.matches(user.getRole().getName()); }
    private static String identity(String name, String dob) { return Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFC).trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT) + "|" + (dob == null ? "" : dob); }
    private static String classKey(String code, String year) { return (code == null ? "" : code.toLowerCase(Locale.ROOT)) + "|" + year; }
    private static void requiredText(String value, int max, String label, List<String> errors) { if (value.isBlank() || value.length() > max) errors.add(label + " must contain 1 to " + max + " characters"); }
    private static void validEmail(String value, List<String> errors) { if (value.length() > 254 || !value.matches("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+")) errors.add("Email is invalid"); }
    private static LocalDate date(String value, List<String> errors) {
        try {
            if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) throw new DateTimeParseException("format", value, 0);
            LocalDate date = LocalDate.parse(value);
            if (date.isAfter(LocalDate.now()) || date.isBefore(LocalDate.of(1900, 1, 1))) throw new DateTimeParseException("range", value, 0);
            return date;
        } catch (DateTimeParseException ex) { errors.add("Date of birth must be yyyy-MM-dd and not in the future"); return null; }
    }
    private static void validYear(String value, List<String> errors) {
        if (!value.matches("\\d{4}-\\d{4}") || Integer.parseInt(value.substring(5)) != Integer.parseInt(value.substring(0, 4)) + 1)
            errors.add("School year must contain consecutive years, for example 2026-2027");
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private static String csv(List<String> fields) { return String.join(",", fields.stream().map(value -> "\"" + value.replace("\"", "\"\"") + "\"").toList()); }
    private static byte[] randomBytes(int size) { byte[] bytes = new byte[size]; new SecureRandom().nextBytes(bytes); return bytes; }

    private String signPreview(UUID schoolId, User actor, SchoolImport.Request request, long issued) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(previewSecret, "HmacSHA256"));
            mac.update((schoolId + "|" + actor.getId() + "|" + request.kind() + "|" + issued).getBytes(StandardCharsets.UTF_8));
            for (SchoolImport.Row row : request.rows()) {
                mac.update(("|" + row.row()).getBytes(StandardCharsets.UTF_8));
                for (String column : columns(request.kind())) {
                    byte[] value = row.data().getOrDefault(column, "").getBytes(StandardCharsets.UTF_8);
                    mac.update(("|" + value.length + ":").getBytes(StandardCharsets.UTF_8)); mac.update(value);
                }
            }
            return issued + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal());
        } catch (java.security.GeneralSecurityException ex) { throw new IllegalStateException("Cannot sign import preview", ex); }
    }
    private void requirePreview(UUID schoolId, User actor, SchoolImport.Request request) {
        try {
            String token = request.previewToken();
            if (token == null || token.length() > 100) throw new IllegalArgumentException();
            long issued = Long.parseLong(token.split("\\.", 2)[0]);
            long age = Instant.now().getEpochSecond() - issued;
            if (age < 0 || age > 900 || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                    signPreview(schoolId, actor, request, issued).getBytes(StandardCharsets.UTF_8))) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) { throw bad("Preview the data before committing; previews expire after 15 minutes"); }
    }
    private static ApiException bad(String message) { return new ApiException(HttpStatus.BAD_REQUEST, message); }
}
