package com.bezpredel.classgendispatcher;

import com.google.common.collect.ImmutableMap;
import javassist.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: option for try/catch
public enum InterfaceMultiplexerClassGenerator {
    INSTANCE;

    private final ClassPool _pool;
    private final CtClass _void;
    private final CtClass _exceptionHandlerClass;
    private final Map<CtClass, String> _primitiveClassToDefaultReturnValueMap;

    InterfaceMultiplexerClassGenerator() {
        try {
            this._pool = ClassPool.getDefault();
            this._void = _pool.get(void.class.getName());
            this._exceptionHandlerClass = _pool.get(DispatchExceptionHandler.class.getName());

            _primitiveClassToDefaultReturnValueMap = buildPrimitiveDefaultValuesMap();

        } catch (NotFoundException e) {
            throw new Error();
        }
    }

    public <T> Class<T> create(Class<T> clazz, boolean needExceptionHandler, T... delegates) throws Exception {
        assert clazz != null;
        assert delegates != null;
        assert clazz.isInterface();

        if (!clazz.isInterface()) {
            throw new UnsupportedOperationException();
        }

        return generateClass(clazz, delegates, needExceptionHandler);
    }

    private <T> Class<T> generateClass(Class<T> clazz, T[] delegates, boolean needExceptionHandler) throws NotFoundException, CannotCompileException {
        CtClass argClass = _pool.getCtClass(clazz.getName());
        CtClass argArrayClass = _pool.getCtClass(delegates.getClass().getName());

        String proxyClassName = NameGenerator.INSTANCE.generateProxyClassName(clazz);
        CtClass newClass = _pool.makeClass(proxyClassName);
        newClass.addInterface(argClass);


        List<FieldRec> fieldNames = addDelegateFields(newClass, clazz, delegates);

        if ( needExceptionHandler) {
            addExceptionHandlerField(newClass);
        }

        addConstructor(newClass, argArrayClass, clazz, fieldNames, needExceptionHandler);
        addMethods(newClass, argClass, clazz, fieldNames, needExceptionHandler);

        Class newJVMClass = newClass.toClass();

        return newJVMClass;
    }


    private <T> List<FieldRec> addDelegateFields(CtClass newClass, Class<T> type, T[] delegates) throws CannotCompileException {
        ArrayList<FieldRec> fieldNames = new ArrayList<>();
        for (int i = 0; i < delegates.length; i++) {
            T listener = delegates[i];
            String fieldType;
            if (isPublicEnough(listener)) {
                fieldType = listener.getClass().getTypeName();
            } else {
                fieldType = type.getTypeName();
            }

            String fieldName = "_delegate" + i;

            newClass.addField(
                    CtField.make(
                            String.format("private final %s %s;", fieldType, fieldName),
                            newClass
                    )
            );
            fieldNames.add(new FieldRec(fieldName, fieldType));
        }
        return fieldNames;
    }

    private void addExceptionHandlerField(CtClass newClass) throws CannotCompileException {
        newClass.addField(
                CtField.make(
                        String.format("private final %s %s;", DispatchExceptionHandler.class.getTypeName(), "_exceptionHandler"),
                        newClass
                )
        );
    }


    private <T> void addMethods(CtClass newClass, CtClass argClass, Class<T> clazz, List<FieldRec> fieldNames,  boolean needExceptionHandler) throws CannotCompileException, NotFoundException {
        CtMethod[] methods = argClass.getMethods();

        for (CtMethod method : methods) {
            if(!method.getDeclaringClass().isInterface()) {
                // skip methods inherited from Object etc
                continue;
            }

            addMethod(newClass, method, clazz, fieldNames, needExceptionHandler);
        }
    }

    private <T> void addConstructor(CtClass newClass, CtClass argArrayClass, Class<T> clazz, List<FieldRec> fieldNames, boolean needExceptionHandler) throws NotFoundException, CannotCompileException {
        CtConstructor constructor = CtNewConstructor.make(new CtClass[]{argArrayClass, _exceptionHandlerClass}, new CtClass[]{}, newClass);

        StringBuilder sb = new StringBuilder();
        sb.append("\n{");
        for (int i=0; i<fieldNames.size(); i++) {
            FieldRec field = fieldNames.get(i);
            sb.append("\n  " + field.name + " = (" + field.type +") $1[" + i + "];");
            //                                  ^this is a cast;    ^method parameter #1;
        }
        if (needExceptionHandler) {
            sb.append("\n  _exceptionHandler = $2;");
            //                                 ^method parameter #2;
        }

        sb.append("\n}");

        constructor.setBody(sb.toString());
        newClass.addConstructor(constructor);
    }

    private <T> void addMethod(CtClass newClass, CtMethod method, Class<T> clazz, List<FieldRec> fieldNames,  boolean needExceptionHandler) throws CannotCompileException, NotFoundException {
        final CtMethod newMethod = CtNewMethod.copy(
                method,
                newClass,
                null
        );

        final String methodName = method.getName();
        final CtClass methodReturnType = newMethod.getReturnType();

        StringBuilder methodBody = new StringBuilder();
        methodBody.append("\n{");

        for (FieldRec fieldName : fieldNames) {
            if (needExceptionHandler) {
                methodBody.append("\n\ttry{");
            }

            appendDelegateMethodCall(fieldName, methodName, methodBody);

            if (needExceptionHandler) {
                methodBody
                    .append("\n\t} catch (Exception e){")
                    .append("\n\t\t")
                    .append( String.format("_exceptionHandler.errorWhileDispatching(e, %s, \"%s\");", fieldName.name, methodName) )
                    .append("\n\t}");
            }
        }

        appendReturnStatement(methodReturnType, methodBody);

        methodBody.append("\n}");
        newMethod.setBody(methodBody.toString());

        newClass.addMethod(newMethod);
    }

    private void appendDelegateMethodCall(FieldRec delegateFieldName, String methodName, StringBuilder methodBody) {
        methodBody.append("\n\t").append(delegateFieldName.name).append(".").append(methodName).append("($$);");
    }

    private void appendReturnStatement(CtClass methodReturnType, StringBuilder methodBody) {
        final boolean isVoidMethod = methodReturnType == _void;
        if (!isVoidMethod) {
            // need to return something
            methodBody.append("\n\t").append("return ");
            if (methodReturnType.isPrimitive()) {
                String defaultVal = _primitiveClassToDefaultReturnValueMap.get(methodReturnType);

                methodBody.append(defaultVal).append(";");
            } else {
                methodBody.append("null;");
            }
        }
    }

    private boolean isPublicEnough(Object delegate) {
        Class<?> clazz = delegate.getClass();

        while (clazz != null) {
            boolean isPublic = java.lang.reflect.Modifier.isPublic(clazz.getModifiers());
            if (!isPublic) return false;

            clazz = clazz.getEnclosingClass();
        }
        return true;
    }

    private ImmutableMap<CtClass, String> buildPrimitiveDefaultValuesMap() {
        return ImmutableMap.<CtClass, String>builder()
                .put(CtClass.booleanType, "false")
                .put(CtClass.charType, "'\0'")
                .put(CtClass.byteType, "(byte)0")
                .put(CtClass.shortType, "(short)0")
                .put(CtClass.intType, "0")
                .put(CtClass.longType, "0L")
                .put(CtClass.floatType, "Float.NaN")
                .put(CtClass.doubleType, "Double.NaN")
                .build();
    }

    private static class FieldRec {
        final String name;
        final String type;

        public FieldRec(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }


    private enum NameGenerator {
        INSTANCE;

        private final AtomicInteger cnt = new AtomicInteger();

        private <T> String generateProxyClassName(Class<T> clazz) {
            return clazz.getName() + "$" + (cnt.getAndIncrement());
        }
    }

}
