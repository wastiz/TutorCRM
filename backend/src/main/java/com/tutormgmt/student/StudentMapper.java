package com.tutormgmt.student;

import com.tutormgmt.student.schedule.StudentSchedule;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StudentMapper {

    @Mapping(target = "schedules", source = "schedules")
    StudentDto toDto(Student student);

    StudentScheduleDto toScheduleDto(StudentSchedule schedule);

    List<StudentScheduleDto> toScheduleDtos(List<StudentSchedule> schedules);

    @Mapping(target = "fullName", expression = "java((student.getFirstName() + \" \" + student.getLastName()).trim())")
    @Mapping(target = "nextLessonAt", ignore = true)
    StudentSummaryDto toSummary(Student student);

    List<StudentSummaryDto> toSummaries(List<Student> students);
}
