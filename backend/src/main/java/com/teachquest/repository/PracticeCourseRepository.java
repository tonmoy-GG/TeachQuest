package com.teachquest.repository;

import com.teachquest.model.PracticeCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PracticeCourseRepository extends JpaRepository<PracticeCourse, Long> {
    List<PracticeCourse> findByActiveTrueOrderByNameAsc();
    Optional<PracticeCourse> findByCodeIgnoreCase(String code);
}