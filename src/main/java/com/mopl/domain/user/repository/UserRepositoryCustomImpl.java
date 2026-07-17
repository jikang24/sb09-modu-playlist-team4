package com.mopl.domain.user.repository;

import com.mopl.domain.user.domain.QUser;
import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.UserSearchRequest;
import com.mopl.global.dto.SortDirection;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryCustomImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<User> findAllWithCursor(UserSearchRequest request) {
        QUser u = QUser.user;
        BooleanBuilder builder = buildFilter(u, request);

        boolean ascending = request.sortDirection() == SortDirection.ASCENDING;

        if (request.cursor() != null && request.idAfter() != null) {
            var sortField = switch (request.sortBy()) {
                case NAME -> u.name;
                case EMAIL -> u.email;
                case CREATEDAT -> u.createdAt.stringValue();
                case ISLOCKED -> u.locked.stringValue();
                case ROLE -> u.role.stringValue();
            };

            builder.and(
                    (ascending ? sortField.gt(request.cursor()) : sortField.lt(request.cursor()))
                            .or(sortField.eq(request.cursor())
                                    .and(ascending ? u.id.gt(request.idAfter()) : u.id.lt(request.idAfter())))
            );
        }

        OrderSpecifier<?> order = switch (request.sortBy()) {
            case NAME -> ascending ? u.name.asc() : u.name.desc();
            case EMAIL -> ascending ? u.email.asc() : u.email.desc();
            case CREATEDAT -> ascending ? u.createdAt.asc() : u.createdAt.desc();
            case ISLOCKED -> ascending ? u.locked.asc() : u.locked.desc();
            case ROLE -> ascending ? u.role.asc() : u.role.desc();
        };

        return queryFactory.selectFrom(u)
                .where(builder)
                .orderBy(order, ascending ? u.id.asc() : u.id.desc())
                .limit(request.limit() + 1L)
                .fetch();
    }

    @Override
    public long countAll(UserSearchRequest request) {
        QUser u = QUser.user;
        BooleanBuilder builder = buildFilter(u, request);

        Long count = queryFactory
                .select(u.count())
                .from(u)
                .where(builder)
                .fetchOne();

        return count != null ? count : 0L;

    }
    private BooleanBuilder buildFilter(QUser u, UserSearchRequest request) {
        BooleanBuilder builder = new BooleanBuilder();

        if (request.nameLike() != null)
            builder.and(u.name.containsIgnoreCase(request.nameLike()));
        if (request.emailLike() != null)
            builder.and(u.email.containsIgnoreCase(request.emailLike()));
        if (request.roleEqual() != null)
            builder.and(u.role.eq(request.roleEqual()));
        if (request.isLocked() != null)
            builder.and(u.locked.eq(request.isLocked()));

        return builder;
    }
}
