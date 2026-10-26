/*
 * Student Grade Manager (Java 17+, console-based)
 * ------------------------------------------------
 * - Add / update / remove students and their subject marks
 * - Average, highest and lowest scores (per student, per subject, whole class)
 * - Data stored in ArrayList / arrays
 * - Summary report with totals, average, grade and rank
 * - Auto-saves to grades.csv so data persists between runs
 *
 * Run:  java StudentGradeManager.java
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

// ============================================================================
// Model
// ============================================================================
class Student {
    private final int rollNo;
    private String name;
    private double[] marks; // one mark per subject

    Student(int rollNo, String name, double[] marks) {
        this.rollNo = rollNo;
        this.name = name;
        this.marks = marks;
    }

    int getRollNo()        { return rollNo; }
    String getName()       { return name; }
    double[] getMarks()    { return marks; }
    void setMarks(double[] marks) { this.marks = marks; }

    double total()   { return Arrays.stream(marks).sum(); }
    double average() { return marks.length == 0 ? 0 : total() / marks.length; }
    double highest() { return Arrays.stream(marks).max().orElse(0); }
    double lowest()  { return Arrays.stream(marks).min().orElse(0); }

    String grade() {
        double a = average();
        if (a >= 90) return "A+";
        if (a >= 80) return "A";
        if (a >= 70) return "B";
        if (a >= 60) return "C";
        if (a >= 50) return "D";
        return "F";
    }
}

// ============================================================================
// Data + calculations + file storage
// ============================================================================
class GradeBook {
    private final ArrayList<String> subjects = new ArrayList<>();
    private final ArrayList<Student> students = new ArrayList<>();
    private final Path file;

    GradeBook(String fileName) {
        this.file = Paths.get(fileName);
        load();
    }

    List<String> getSubjects()   { return subjects; }
    List<Student> getStudents()  { return students; }
    boolean hasSubjects()        { return !subjects.isEmpty(); }

    void setSubjects(List<String> names) {
        subjects.clear();
        subjects.addAll(names);
        save();
    }

    Student find(int rollNo) {
        for (Student s : students) if (s.getRollNo() == rollNo) return s;
        return null;
    }

    boolean add(Student s) {
        if (find(s.getRollNo()) != null) return false;
        students.add(s);
        save();
        return true;
    }

    boolean remove(int rollNo) {
        Student s = find(rollNo);
        if (s == null) return false;
        students.remove(s);
        save();
        return true;
    }

    /** Students sorted by average, best first (used for ranking). */
    List<Student> ranked() {
        List<Student> copy = new ArrayList<>(students);
        copy.sort(Comparator.comparingDouble(Student::average).reversed());
        return copy;
    }

    // ---- per-subject statistics (index = subject position) ----
    double subjectAverage(int i) {
        double sum = 0;
        for (Student s : students) sum += s.getMarks()[i];
        return students.isEmpty() ? 0 : sum / students.size();
    }

    Student subjectTopper(int i) {
        Student best = null;
        for (Student s : students) if (best == null || s.getMarks()[i] > best.getMarks()[i]) best = s;
        return best;
    }

    Student subjectLowest(int i) {
        Student low = null;
        for (Student s : students) if (low == null || s.getMarks()[i] < low.getMarks()[i]) low = s;
        return low;
    }

    double classAverage() {
        double sum = 0;
        for (Student s : students) sum += s.average();
        return students.isEmpty() ? 0 : sum / students.size();
    }

    // ---- persistence (CSV) ----
    void save() {
        List<String> lines = new ArrayList<>();
        lines.add("SUBJECTS," + String.join(",", subjects));
        for (Student s : students) {
            StringBuilder sb = new StringBuilder(s.getRollNo() + "," + s.getName().replace(",", " "));
            for (double m : s.getMarks()) sb.append(',').append(m);
            lines.add(sb.toString());
        }
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            System.out.println("Warning: could not save data: " + e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            List<String> lines = Files.readAllLines(file);
            if (lines.isEmpty()) return;
            String[] head = lines.get(0).split(",");
            subjects.addAll(Arrays.asList(head).subList(1, head.length));
            for (int i = 1; i < lines.size(); i++) {
                String[] f = lines.get(i).split(",");
                if (f.length < 2 + subjects.size()) continue;
                double[] marks = new double[subjects.size()];
                for (int j = 0; j < marks.length; j++) marks[j] = Double.parseDouble(f[2 + j]);
                students.add(new Student(Integer.parseInt(f[0]), f[1], marks));
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Warning: could not read saved data (" + e.getMessage() + "). Starting fresh.");
            subjects.clear();
            students.clear();
        }
    }
}

// ============================================================================
// Console UI
// ============================================================================
public class StudentGradeManager {
    private static final Scanner in = new Scanner(System.in);
    private static GradeBook book;

    public static void main(String[] args) {
        book = new GradeBook("grades.csv");
        if (!book.hasSubjects()) setupSubjects();

        while (true) {
            System.out.println("\n===== STUDENT GRADE MANAGER =====");
            System.out.println("1. Add student");
            System.out.println("2. Update student's marks");
            System.out.println("3. Remove student");
            System.out.println("4. View a student");
            System.out.println("5. Class statistics (average / highest / lowest)");
            System.out.println("6. Summary report of all students");
            System.out.println("7. Exit");
            switch (readLine("Select: ")) {
                case "1" -> addStudent();
                case "2" -> updateStudent();
                case "3" -> removeStudent();
                case "4" -> viewStudent();
                case "5" -> showStatistics();
                case "6" -> showReport();
                case "7" -> {
                    System.out.println("Data saved. Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid option.");
            }
        }
    }

    // ---- actions ----
    private static void setupSubjects() {
        int n = readInt("How many subjects? ", 1, 15);
        List<String> names = new ArrayList<>();
        for (int i = 1; i <= n; i++) names.add(readLine("Name of subject " + i + ": ").replace(",", " "));
        book.setSubjects(names);
    }

    private static void addStudent() {
        int roll = readInt("Roll number: ", 1, Integer.MAX_VALUE);
        if (book.find(roll) != null) {
            System.out.println("A student with that roll number already exists.");
            return;
        }
        String name = readLine("Name: ");
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        book.add(new Student(roll, name, readMarks()));
        System.out.println("Student added.");
    }

    private static void updateStudent() {
        Student s = book.find(readInt("Roll number: ", 1, Integer.MAX_VALUE));
        if (s == null) {
            System.out.println("Student not found.");
            return;
        }
        s.setMarks(readMarks());
        book.save();
        System.out.println("Marks updated.");
    }

    private static void removeStudent() {
        boolean ok = book.remove(readInt("Roll number: ", 1, Integer.MAX_VALUE));
        System.out.println(ok ? "Student removed." : "Student not found.");
    }

    private static void viewStudent() {
        Student s = book.find(readInt("Roll number: ", 1, Integer.MAX_VALUE));
        if (s == null) {
            System.out.println("Student not found.");
            return;
        }
        System.out.printf("%nRoll %d - %s%n", s.getRollNo(), s.getName());
        for (int i = 0; i < book.getSubjects().size(); i++)
            System.out.printf("  %-15s %6.1f%n", book.getSubjects().get(i), s.getMarks()[i]);
        System.out.printf("  Total %.1f | Average %.2f | Highest %.1f | Lowest %.1f | Grade %s%n",
                s.total(), s.average(), s.highest(), s.lowest(), s.grade());
    }

    private static void showStatistics() {
        if (book.getStudents().isEmpty()) {
            System.out.println("No students yet.");
            return;
        }
        System.out.println("\n--- Subject-wise statistics ---");
        System.out.printf("%-15s %8s  %-22s %-22s%n", "Subject", "Average", "Highest", "Lowest");
        for (int i = 0; i < book.getSubjects().size(); i++) {
            Student top = book.subjectTopper(i), low = book.subjectLowest(i);
            System.out.printf("%-15s %8.2f  %-22s %-22s%n", book.getSubjects().get(i), book.subjectAverage(i),
                    String.format("%.1f (%s)", top.getMarks()[i], top.getName()),
                    String.format("%.1f (%s)", low.getMarks()[i], low.getName()));
        }
        List<Student> ranked = book.ranked();
        Student best = ranked.get(0), worst = ranked.get(ranked.size() - 1);
        System.out.println("\n--- Overall (by student average) ---");
        System.out.printf("Class average : %.2f%n", book.classAverage());
        System.out.printf("Highest       : %.2f (%s)%n", best.average(), best.getName());
        System.out.printf("Lowest        : %.2f (%s)%n", worst.average(), worst.getName());
    }

    private static void showReport() {
        List<Student> ranked = book.ranked();
        if (ranked.isEmpty()) {
            System.out.println("No students yet.");
            return;
        }
        List<String> subs = book.getSubjects();
        String line = "-".repeat(36 + 9 * subs.size() + 30);
        System.out.println("\n" + "=".repeat(line.length()));
        System.out.println(" SUMMARY REPORT  (" + ranked.size() + " students)");
        System.out.println("=".repeat(line.length()));
        StringBuilder head = new StringBuilder(String.format("%-5s %-6s %-18s", "Rank", "Roll", "Name"));
        for (String s : subs) head.append(String.format("%9s", s.length() > 8 ? s.substring(0, 8) : s));
        head.append(String.format("%9s%9s%7s%6s", "Total", "Avg", "High", "Grd"));
        System.out.println(head);
        System.out.println(line);
        int rank = 1;
        for (Student s : ranked) {
            StringBuilder row = new StringBuilder(String.format("%-5d %-6d %-18s", rank++, s.getRollNo(),
                    s.getName().length() > 17 ? s.getName().substring(0, 17) : s.getName()));
            for (double m : s.getMarks()) row.append(String.format("%9.1f", m));
            row.append(String.format("%9.1f%9.2f%7.1f%6s", s.total(), s.average(), s.highest(), s.grade()));
            System.out.println(row);
        }
        System.out.println(line);
        System.out.printf(" Class average: %.2f | Topper: %s (%.2f) | Lowest: %s (%.2f)%n",
                book.classAverage(), ranked.get(0).getName(), ranked.get(0).average(),
                ranked.get(ranked.size() - 1).getName(), ranked.get(ranked.size() - 1).average());
        long passed = ranked.stream().filter(s -> !s.grade().equals("F")).count();
        System.out.printf(" Pass: %d | Fail: %d%n", passed, ranked.size() - passed);
        System.out.println("=".repeat(line.length()));
    }

    // ---- input helpers ----
    private static double[] readMarks() {
        double[] marks = new double[book.getSubjects().size()];
        for (int i = 0; i < marks.length; i++) marks[i] = readDouble(book.getSubjects().get(i) + " (0-100): ");
        return marks;
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "7"; // end of input -> behave like Exit
    }

    private static int readInt(String prompt, int min, int max) {
        while (true) {
            try {
                int v = Integer.parseInt(readLine(prompt));
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Enter a whole number between " + min + " and " + (max == Integer.MAX_VALUE ? "..." : max) + ".");
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            try {
                double v = Double.parseDouble(readLine(prompt));
                if (v >= 0 && v <= 100) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  Enter a number between 0 and 100.");
        }
    }
}