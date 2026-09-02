package com.teachquest.repository;

import com.teachquest.model.StudyResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyResourceRepository extends JpaRepository<StudyResource, Long> {
    List<StudyResource> findByDepartment(String department);

    List<StudyResource> findByCourseCode(String courseCode);

    @Query("SELECT DISTINCT s.courseCode, s.department, s.semester FROM StudyResource s")
    List<Object[]> findDistinctCourses();

    List<StudyResource> findByCourseCodeAndCategory(String courseCode, String category);
}
