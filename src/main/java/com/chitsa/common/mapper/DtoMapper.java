package com.chitsa.common.mapper;

/** Maps between entity, request DTO, and response DTO. */
public interface DtoMapper<Entity, RequestDto, ResponseDto> {

    /** Builds a new entity from a create/update request. */
    Entity convertDtoToEntity(RequestDto dto);

    /** Builds the API response for a persisted entity. */
    ResponseDto convertEntityToResponseDto(Entity entity);

    /** Copies updatable fields from {@code dto} onto the managed {@code entity}. */
    void updateEntityFromDto(RequestDto dto, Entity entity);
}
