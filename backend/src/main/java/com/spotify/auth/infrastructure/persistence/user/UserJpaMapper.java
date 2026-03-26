package com.spotify.auth.infrastructure.persistence.user;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.valueobject.Email;
import com.spotify.auth.domain.valueobject.Password;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserJpaMapper {

    @Mapping(target = "email", source = "email", qualifiedByName = "mapEmailToString")
    @Mapping(target = "password", source = "password", qualifiedByName = "mapPasswordToString")
    @Mapping(target = "_isNew", ignore = true)
    @Mapping(target = "verified", source = "verified")
    UserJpaEntity toJpaEntity(User user);

    @Mapping(target = "email", source = "email", qualifiedByName = "mapStringToEmail")
    @Mapping(target = "password", source = "password", qualifiedByName = "mapStringToPassword")
    @Mapping(target = "isVerified", source = "verified")
    User toDomainEntity(UserJpaEntity entity);

    @Named("mapEmailToString")
    default String mapEmailToString(Email email) {
        return email != null ? email.value() : null;
    }

    @Named("mapStringToEmail")
    default Email mapStringToEmail(String email) {
        return email != null ? new Email(email) : null;
    }

    @Named("mapPasswordToString")
    default String mapPasswordToString(Password password) {
        return password != null ? password.value() : null;
    }

    @Named("mapStringToPassword")
    default Password mapStringToPassword(String password) {
        return password != null ? new Password(password) : null;
    }
}
