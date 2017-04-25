/**
 *
 * Copyright (c) 2006-2017, Speedment, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); You may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.speedment.runtime.field.internal.predicate;

import com.speedment.runtime.field.predicate.CombinedPredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Aggregation of a number of {@link Predicate Predicates} of the same type
 * (e.g. AND or OR) that can be applied in combination.
 *
 * @param <ENTITY> the entity type
 *
 * @author Per Minborg
 * @author Emil Forslund
 * @since 2.2.0
 */
public abstract class AbstractCombinedPredicate<ENTITY> extends AbstractPredicate<ENTITY>
    implements CombinedPredicate<ENTITY> {

    private final List<Predicate<? super ENTITY>> predicates;
    private final Type type;

    private AbstractCombinedPredicate(Type type, Predicate<ENTITY> first, Predicate<? super ENTITY> second) {

        this.type = requireNonNull(type);
        this.predicates = new ArrayList<>();
        add(requireNonNull(first));
        add(requireNonNull(second));
    }

    /**
     * Adds the provided Predicate to this CombinedBasePredicate.
     *
     * @param <R> the Type of CombinedBasePredicate (AndCombinedBasePredicate or
     * OrCombinedBasePredicate)
     * @param predicate to add
     * @return a reference to a CombinedPredicate after the method has been
     * applied
     */
    protected final <R extends AbstractCombinedPredicate<ENTITY>> R add(Predicate<? super ENTITY> predicate) {
        requireNonNull(predicate);
        if (getClass().equals(predicate.getClass())) {
            @SuppressWarnings("unchecked")
            final AbstractCombinedPredicate<ENTITY> cbp = getClass().cast(predicate);
            cbp.stream().forEachOrdered(predicates::add);
        } else {
            predicates.add(predicate);
        }

        @SuppressWarnings("unchecked")
        final R self = (R) this;
        return self;
    }

    /**
     * Removes the provided Predicate from this CombinedBasePredicate.
     *
     * @param predicate to remove
     * @return a reference to a CombinedPredicate after the method has been
     * applied
     */
    protected AbstractCombinedPredicate<ENTITY> remove(Predicate<? super ENTITY> predicate) {
        requireNonNull(predicate);
        predicates.remove(predicate);
        return this;
    }

    @Override
    public Stream<Predicate<? super ENTITY>> stream() {
        return predicates.stream();
    }

    @Override
    public int size() {
        return predicates.size();
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public abstract AndCombinedBasePredicate<ENTITY> and(Predicate<? super ENTITY> other);

    @Override
    public abstract OrCombinedBasePredicate<ENTITY> or(Predicate<? super ENTITY> other);

    public static class AndCombinedBasePredicate<ENTITY> extends AbstractCombinedPredicate<ENTITY> {

        public AndCombinedBasePredicate(Predicate<ENTITY> first, Predicate<? super ENTITY> second) {
            super(Type.AND, first, second);
        }

        @Override
        protected boolean testWithoutNegation(ENTITY entity) {
            requireNonNull(entity);
            return stream().allMatch(p -> p.test(entity));
        }

        @Override
        public AndCombinedBasePredicate<ENTITY> and(Predicate<? super ENTITY> other) {
            requireNonNull(other);
            return add(other);
        }

        @Override
        public OrCombinedBasePredicate<ENTITY> or(Predicate<? super ENTITY> other) {
            requireNonNull(other);
            return new OrCombinedBasePredicate<>(this, other);
        }
    }

    public static class OrCombinedBasePredicate<ENTITY> extends AbstractCombinedPredicate<ENTITY> {

        public OrCombinedBasePredicate(Predicate<ENTITY> first, Predicate<? super ENTITY> second) {
            super(Type.OR, first, second);
        }

        @Override
        protected boolean testWithoutNegation(ENTITY entity) {
            requireNonNull(entity);
            return stream().anyMatch(p -> p.test(entity));
        }

        @Override
        public AndCombinedBasePredicate<ENTITY> and(Predicate<? super ENTITY> other) {
            requireNonNull(other);
            return new AndCombinedBasePredicate<>(this, other);
        }

        @Override
        public OrCombinedBasePredicate<ENTITY> or(Predicate<? super ENTITY> other) {
            requireNonNull(other);
            return add(other);
        }
    }

    @Override
    public String toString() {
        return "{type="
            + type.name()
            + ", predicates="
            + predicates.toString()
            + "}";
    }
}
