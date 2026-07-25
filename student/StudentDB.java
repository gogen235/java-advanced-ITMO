package info.kgeorgiy.ja.goge.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;


public class StudentDB implements AdvancedQuery {
    private record MyPair<U, E>(U first, E second) {
    }

    private static final Comparator<Student> COMPARATOR_BY_NAME = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Comparator.reverseOrder());

    private static final Comparator<MyPair<GroupName, List<Student>>> PAIR_COMPARATOR =
            Comparator.comparing(MyPair::first);

    private static final Comparator<MyPair<GroupName, Set<String>>> PAIR_COMPARATOR_REVERSED =
            Comparator.<MyPair<GroupName, Set<String>>, GroupName>comparing(MyPair::first).reversed();

    private static final Comparator<MyPair<String, Integer>> PAIR_SECOND_COMPARATOR_REVERSED =
            Comparator.<MyPair<String, Integer>, Integer>comparing(MyPair::second).reversed();

    private static final Comparator<MyPair<String, Integer>> PAIR_SECOND_COMPARATOR =
            Comparator.comparing(MyPair::second);

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroupsBy(students, COMPARATOR_BY_NAME);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroupsBy(students, Comparator.naturalOrder());
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> students) {
        return getMaxGroup(getGroups(students).map(e -> new MyPair<>(e.getKey(), e.getValue())),
                PAIR_COMPARATOR);
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> students) {
        return getMaxGroup(getGroups(students)
                        .map(e -> new MyPair<>(e.getKey(), getDistinctFirstNames(e.getValue()))),
                PAIR_COMPARATOR_REVERSED);
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return getFields(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return getFields(students, Student::getLastName);
    }

    @Override
    public List<GroupName> getGroups(final List<Student> students) {
        return getFields(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(final List<Student> students) {
        return getFields(students, StudentDB::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return students.stream().distinct().map(Student::getFirstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return students.stream().max(Student::compareTo).map(Student::getFirstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return sortStudentsBy(students, Comparator.naturalOrder());
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortStudentsBy(students, COMPARATOR_BY_NAME);
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return findStudentsBy(students, name, Student::getFirstName);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return findStudentsBy(students, name, Student::getLastName);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final GroupName group) {
        return findStudentsBy(students, group, Student::getGroup);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final GroupName group) {
        return findStudents(students, group, Student::getGroup)
                .collect(Collectors.toMap(Student::getLastName, Student::getFirstName, StudentDB::min));
    }

    @Override
    public String getMostPopularName(final Collection<Student> students) {
        return getPopularName(students, PAIR_SECOND_COMPARATOR_REVERSED);
    }

    @Override
    public String getLeastPopularName(final Collection<Student> students) {
        return getPopularName(students, PAIR_SECOND_COMPARATOR);
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, Student::getLastName);

    }

    @Override
    public List<GroupName> getGroups(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, Student::getGroup);

    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] indices) {
        return getByIndices(students, indices, StudentDB::getFullName);
    }

    private String getPopularName(final Collection<Student> students, final Comparator<MyPair<String, Integer>> comparator) {
        return collect(students, Comparator.naturalOrder(),
                groupingBy(Student::getFirstName, groupingBy(Student::getGroup)))
                .map(x -> new MyPair<>(x.getKey(), x.getValue().size()))
                .min(comparator.thenComparing(MyPair::first))
                .map(MyPair::first).orElse("");
    }

    private <T> List<T> getByIndices(final List<Student> students, final int[] indices, Function<Student, T> function) {
        return Arrays.stream(indices).mapToObj(x -> function.apply(students.get(x))).toList();
    }

    private <T> List<T> getByIndices(final Collection<Student> students, final int[] indices, Function<Student, T> function) {
        return getByIndices(new ArrayList<>(students), indices, function);
    }

    private Stream<Map.Entry<GroupName, List<Student>>> getGroups(final Collection<Student> students) {
        return students.stream().collect(groupingBy(Student::getGroup)).entrySet().stream();
    }

    private <S, T extends Collection<S>> GroupName getMaxGroup(
            final Stream<MyPair<GroupName, T>> students,
            Comparator<MyPair<GroupName, T>> comparator
    ) {
        return students
                .max(Comparator.<MyPair<GroupName, T>>comparingInt(e -> e.second().size()).thenComparing(comparator))
                .map(MyPair::first).orElse(null);
    }

    private List<Group> getGroupsBy(final Collection<Student> students, final Comparator<Student> comparator) {
        return collect(students, comparator, groupingBy(Student::getGroup))
                .sorted(Map.Entry.comparingByKey())
                .map(x -> new Group(x.getKey(), x.getValue()))
                .toList();
    }

    private <T, E> Stream<Map.Entry<T, E>> collect(
            final Collection<Student> students,
            final Comparator<Student> comparator,
            final Collector<Student, ?, Map<T, E>> collector
    ) {
        return students.stream().sorted(comparator).collect(collector).entrySet().stream();
    }

    private <T> Stream<Student> findStudents(
            final Collection<Student> students,
            final T field,
            final Function<Student, T> function
    ) {
        return students.stream().filter(student -> Objects.equals(function.apply(student), field));
    }

    private <T> List<Student> findStudentsBy(
            final Collection<Student> students,
            final T field,
            final Function<Student, T> function
    ) {
        return findStudents(students, field, function).sorted(COMPARATOR_BY_NAME).toList();
    }

    private List<Student> sortStudentsBy(final Collection<Student> students, final Comparator<Student> comparator) {
        return students.stream().sorted(comparator).toList();
    }

    private <T> List<T> getFields(final List<Student> students, final Function<Student, T> function) {
        return students.stream().map(function).toList();
    }

    private static String min(final String str1, final String str2) {
        return str1.compareTo(str2) > 0 ? str2 : str1;
    }

    private static String getFullName(final Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }
}
