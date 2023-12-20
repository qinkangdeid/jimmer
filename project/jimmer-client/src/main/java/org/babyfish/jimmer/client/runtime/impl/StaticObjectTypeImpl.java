package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.*;
import org.babyfish.jimmer.client.runtime.FetchByInfo;
import org.babyfish.jimmer.client.runtime.ObjectType;
import org.babyfish.jimmer.client.runtime.Property;
import org.babyfish.jimmer.client.runtime.Type;
import org.babyfish.jimmer.meta.ImmutableType;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class StaticObjectTypeImpl extends Graph implements ObjectType {

    private final Class<?> javaType;

    private final List<String> simpleNames;

    private List<Type> arguments;

    @Nullable
    private Doc doc;

    @Nullable
    private TypeDefinition.Error error;

    private Map<String, Property> properties;

    public StaticObjectTypeImpl(Class<?> javaType) {
        this.javaType = javaType;
        List<String> simpleNames = new ArrayList<>();
        for (Class<?> type = javaType; type != null; type = type.getDeclaringClass()) {
            simpleNames.add(0, type.getSimpleName());
        }
        this.simpleNames = Collections.unmodifiableList(simpleNames);
    }

    void init(TypeName typeName, List<Type> arguments, TypeContext ctx) {
        TypeDefinition definition = ctx.definition(typeName);
        if (arguments.isEmpty() && javaType.getTypeParameters().length != 0) {
            arguments = Arrays.stream(javaType.getTypeParameters())
                    .map(it -> new TypeVariableImpl(typeName.typeVariable(it.getName())))
                    .collect(Collectors.toList());
        }
        this.arguments = arguments;
        Map<String, Property> properties = new LinkedHashMap<>();
        collectProperties(definition, ctx, properties);
        this.doc = definition.getDoc();
        this.error = definition.getError();
        this.properties = Collections.unmodifiableMap(properties);
    }

    private void collectProperties(TypeDefinition definition, TypeContext ctx, Map<String, Property> properties) {
        for (Prop prop : definition.getPropMap().values()) {
            if (!properties.containsKey(prop.getName())) {
                properties.put(
                        prop.getName(),
                        new PropertyImpl(
                                prop.getName(),
                                ctx.parseType(prop.getType()),
                                prop.getDoc()
                        )
                );
            }
        }
        for (TypeRef superType : definition.getSuperTypes()) {
            TypeDefinition superDefinition = ctx.definition(superType.getTypeName());
            collectProperties(superDefinition, ctx, properties);
        }
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Nullable
    @Override
    public ImmutableType getImmutableType() {
        return null;
    }

    @Override
    public Kind getKind() {
        return error != null ? Kind.ERROR : Kind.STATIC;
    }

    @Override
    public List<String> getSimpleNames() {
        return simpleNames;
    }

    @Nullable
    @Override
    public FetchByInfo getFetchByInfo() {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Type> getArguments() {
        return arguments;
    }

    @Override
    @Nullable
    public Doc getDoc() {
        return doc;
    }

    @Override
    @Nullable
    public TypeDefinition.Error getError() {
        return error;
    }

    @Override
    public Map<String, Property> getProperties() {
        return properties;
    }

    @Override
    public boolean isRecursiveFetchedType() {
        return false;
    }

    @Override
    public ObjectType unwrap() {
        return null;
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        if (arguments.isEmpty()) {
            return javaType.getName() +
                    '{' +
                    properties.values().stream().map(it -> string(it, stack)).collect(Collectors.joining(", ")) +
                    '}';
        }
        return javaType.getName() +
                '<' +
                arguments.stream().map(it -> string(it, stack)).collect(Collectors.joining(", ")) +
                "> {" +
                properties.values().stream().map(it -> string(it, stack)).collect(Collectors.joining(", ")) +
                '}';
    }
}