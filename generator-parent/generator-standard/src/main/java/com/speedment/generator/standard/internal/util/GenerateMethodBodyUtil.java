/**
 *
 * Copyright (c) 2006-2016, Speedment, Inc. All Rights Reserved.
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
package com.speedment.generator.standard.internal.util;

import com.speedment.common.codegen.constant.DefaultType;
import com.speedment.common.codegen.constant.SimpleParameterizedType;
import com.speedment.common.codegen.model.File;
import com.speedment.common.codegen.model.Import;
import com.speedment.common.codegen.model.Method;
import com.speedment.generator.translator.TranslatorSupport;
import com.speedment.runtime.config.Column;
import com.speedment.runtime.config.Table;
import com.speedment.runtime.config.trait.HasEnabled;
import com.speedment.runtime.core.exception.SpeedmentException;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.speedment.common.codegen.constant.DefaultAnnotationUsage.OVERRIDE;
import static com.speedment.common.codegen.internal.util.Formatting.*;
import static com.speedment.generator.standard.manager.GeneratedManagerImplTranslator.ENTITY_CREATE_METHOD_NAME;
import static java.util.stream.Collectors.joining;

/**
 *
 * @author  Emil Forslund
 * @since   3.0.0
 */
public final class GenerateMethodBodyUtil {

    public static Method generateFields(TranslatorSupport<Table> support, File file, String methodName, Supplier<Stream<? extends Column>> columnsSupplier) {
        return Method.of(methodName, 
                DefaultType.stream(
                    SimpleParameterizedType.create(
                        com.speedment.runtime.core.field.Field.class,
                        support.entityType()
                    )
                )
            )
            .public_().add(OVERRIDE)
            .add(generateFieldsBody(support, file, columnsSupplier));
    }

    public static String[] generateFieldsBody(TranslatorSupport<Table> support, File file, Supplier<Stream<? extends Column>> columnsSupplier) {
        file.add(Import.of(Stream.class));
        final List<String> rows = new LinkedList<>();

        rows.add("return Stream.of(");
        rows.add(indent(columnsSupplier.get()
            .filter(HasEnabled::isEnabled)
            .map(Column::getJavaName)
            .map(support.namer()::javaStaticFieldName)
            .map(field -> support.typeName() + "." + field)
            .collect(joining("," + nl()))
        ));
        rows.add(");");

        return rows.toArray(new String[rows.size()]);
    }
    
    public static Method generateNewEmptyEntity(TranslatorSupport<Table> support, File file, Supplier<Stream<? extends Column>> columnsSupplier) {
        return Method.of(ENTITY_CREATE_METHOD_NAME, support.entityType())
            .public_().add(OVERRIDE)
            .add(generateNewEmptyEntityBody(support, file, columnsSupplier));

    }

    public static String[] generateNewEmptyEntityBody(TranslatorSupport<Table> support, File file, Supplier<Stream<? extends Column>> columnsSupplier) {
        file.add(Import.of(support.entityImplType()));
        return new String[] {
            "return new " + support.entityImplName() + "();"
        };
    }
    
    @FunctionalInterface
    public interface ReadFromResultSet {
        String readFromResultSet(File file, Column c, AtomicInteger position);
    }

    public static String[] generateApplyResultSetBody(ReadFromResultSet readFromResultSet, TranslatorSupport<Table> support, File file, Supplier<Stream<? extends Column>> columnsSupplier) {

        file.add(Import.of(SQLException.class));
        
        final List<String> rows = new LinkedList<>();
        rows.add("final " + support.entityName() + " entity = manager." + ENTITY_CREATE_METHOD_NAME + "();");

        final Stream.Builder<String> streamBuilder = Stream.builder();

        final AtomicInteger position = new AtomicInteger(1);
        columnsSupplier.get()
            .filter(HasEnabled::isEnabled)
            .forEachOrdered(c -> {
                streamBuilder.add("entity.set" + support.namer().javaTypeName(c.getJavaName()) + "(" + readFromResultSet.readFromResultSet(file, c, position) + ");");
            });

        rows.add("try " + block(streamBuilder.build()));
        rows.add("catch (final " + SQLException.class.getSimpleName() + " sqle) " + block(
            "throw new " + SpeedmentException.class.getSimpleName() + "(sqle);"
        ));
        rows.add("return entity;");

        return rows.toArray(new String[rows.size()]);
    }
    
    private GenerateMethodBodyUtil() {}
}
