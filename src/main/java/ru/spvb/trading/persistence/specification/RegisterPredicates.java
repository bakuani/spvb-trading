package ru.spvb.trading.persistence.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import ru.spvb.trading.api.dto.common.RegisterFilter;

import java.util.ArrayList;
import java.util.List;

final class RegisterPredicates {
    private RegisterPredicates() {
    }

    static List<Predicate> from(RegisterFilter filter, Root<?> root, String timeField, CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();
        if (filter.getFrom() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get(timeField), filter.getFrom()));
        }
        if (filter.getTo() != null) {
            predicates.add(builder.lessThan(root.get(timeField), filter.getTo()));
        }
        return predicates;
    }
}
