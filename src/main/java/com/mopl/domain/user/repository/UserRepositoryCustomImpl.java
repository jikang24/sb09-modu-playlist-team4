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

        if (request.cursor() != null && request.idAfter() != null) {
            var sortField = switch (request.sortBy()) {
                case name -> u.name;
                case email -> u.email;
                case createdAt -> u.createdAt.stringValue();
                case isLocked -> u.locked.stringValue();
                case role -> u.role.stringValue();
            };

            builder.and(
                    sortField.gt(request.cursor())
                            .or(sortField.eq(request.cursor())
                                    .and(u.id.gt(request.idAfter())))
            );
        }

        OrderSpecifier<?> order = switch (request.sortBy()) {
            case name -> request.sortDirection() == SortDirection.ASCENDING
                    ? u.name.asc() : u.name.desc();
            case email -> request.sortDirection() == SortDirection.ASCENDING
                    ? u.email.asc() : u.email.desc();
            case createdAt -> request.sortDirection() == SortDirection.ASCENDING
                    ? u.createdAt.asc() : u.createdAt.desc();
            case isLocked -> request.sortDirection() == SortDirection.ASCENDING
                    ? u.locked.asc() : u.locked.desc();
            case role -> request.sortDirection() == SortDirection.ASCENDING
                    ? u.role.asc() : u.role.desc();
        };

        return queryFactory.selectFrom(u)
                .where(builder)
                .orderBy(order, u.id.asc())
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

        if (request.emailLike() != null)
            builder.and(u.email.containsIgnoreCase(request.emailLike()));
        if (request.roleEqual() != null)
            builder.and(u.role.eq(request.roleEqual()));
        if (request.isLocked() != null)
            builder.and(u.locked.eq(request.isLocked()));

        return builder;
    }
}
