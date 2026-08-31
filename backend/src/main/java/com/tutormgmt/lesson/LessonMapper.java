package com.tutormgmt.lesson;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LessonMapper {

    @Mapping(target = "studentName", ignore = true)
    LessonDto toDto(Lesson lesson);

    List<LessonDto> toDtos(List<Lesson> lessons);
}
