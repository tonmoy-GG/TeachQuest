package com.teachquest.config;

import com.teachquest.model.PracticeCourse;
import com.teachquest.repository.PracticeCourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PracticeCourseInitializer implements CommandLineRunner {
    private final PracticeCourseRepository courseRepository;

    public PracticeCourseInitializer(PracticeCourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) {
        List<String[]> courses = List.of(
                new String[]{"DSA", "Data Structures and Algorithms"},
                new String[]{"ML", "Machine Learning"},
                new String[]{"AI", "Artificial Intelligence"},
                new String[]{"OOP", "Object-Oriented Programming"});
        for (String[] course : courses) {
            if (courseRepository.findByCodeIgnoreCase(course[0]).isEmpty()) {
                PracticeCourse record = new PracticeCourse();
                record.setCode(course[0]);
                record.setName(course[1]);
                courseRepository.save(record);
            }
        }
    }
}