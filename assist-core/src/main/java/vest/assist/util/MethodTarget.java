package vest.assist.util;

import jakarta.inject.Inject;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Represents a reflected method in a class. Provides some metadata and additional information for the method.
 */
public final class MethodTarget implements AnnotatedElement {

    private final Method method;
    private final Parameter[] parameters;
    private final Class<?>[] parameterTypes;
    private final MethodHandle handle;
    private final boolean isInject;
    private final boolean isStatic;

    public MethodTarget(Method method) {
        this.method = method;
        this.parameters = method.getParameters();
        this.parameterTypes = method.getParameterTypes();
        this.isStatic = Modifier.isStatic(method.getModifiers());
        try {
//            Reflector.makeAccessible(null, method);
            this.handle = MethodHandles.lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("error creating method handle", e);
        }
        this.isInject = method.isAnnotationPresent(Inject.class);
    }

    public Method getMethod() {
        return method;
    }

    public boolean isInject() {
        return isInject;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Class<?> getDeclaringClass() {
        return method.getDeclaringClass();
    }

    public String getName() {
        return method.getName();
    }

    public int getModifiers() {
        return method.getModifiers();
    }

    public TypeVariable<Method>[] getTypeParameters() {
        return method.getTypeParameters();
    }

    public Class<?> getReturnType() {
        return method.getReturnType();
    }

    public Type getGenericReturnType() {
        return method.getGenericReturnType();
    }

    public Class<?>[] getParameterTypes() {
        return parameterTypes;
    }

    public int getParameterCount() {
        return method.getParameterCount();
    }

    public Type[] getGenericParameterTypes() {
        return method.getGenericParameterTypes();
    }

    public Class<?>[] getExceptionTypes() {
        return method.getExceptionTypes();
    }

    public Type[] getGenericExceptionTypes() {
        return method.getGenericExceptionTypes();
    }

    public String toGenericString() {
        return method.toGenericString();
    }

    public Object invoke(Object obj, Object... args) throws Throwable {
        if (isStatic) {
            return Reflector.invoke(handle, null, args);
        } else {
            Reflector.makeAccessible(obj, method);
            return Reflector.invoke(handle, obj, args);
        }
    }

    public boolean isBridge() {
        return method.isBridge();
    }

    public boolean isVarArgs() {
        return method.isVarArgs();
    }

    public boolean isSynthetic() {
        return method.isSynthetic();
    }

    public boolean isDefault() {
        return method.isDefault();
    }

    public Object getDefaultValue() {
        return method.getDefaultValue();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return method.getAnnotation(annotationClass);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return method.getDeclaredAnnotations();
    }

    public Annotation[][] getParameterAnnotations() {
        return method.getParameterAnnotations();
    }

    public AnnotatedType getAnnotatedReturnType() {
        return method.getAnnotatedReturnType();
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return method.getAnnotationsByType(annotationClass);
    }

    public AnnotatedType getAnnotatedReceiverType() {
        return method.getAnnotatedReceiverType();
    }

    public AnnotatedType[] getAnnotatedParameterTypes() {
        return method.getAnnotatedParameterTypes();
    }

    public AnnotatedType[] getAnnotatedExceptionTypes() {
        return method.getAnnotatedExceptionTypes();
    }

    public static void setAccessible(AccessibleObject[] array, boolean flag) throws SecurityException {
        AccessibleObject.setAccessible(array, flag);
    }

    public void setAccessible(boolean flag) throws SecurityException {
        method.setAccessible(flag);
    }

    public boolean isAccessible() {
        return method.canAccess(this);
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return method.isAnnotationPresent(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return method.getAnnotations();
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        return method.getDeclaredAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return method.getDeclaredAnnotationsByType(annotationClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodTarget um = (MethodTarget) o;
        Method other = um.method;
        return Objects.equals(method.getName(), other.getName())
                && Objects.equals(method.getReturnType(), other.getReturnType())
                && Arrays.equals(parameterTypes, um.parameterTypes);
    }

    @Override
    public int hashCode() {
        return method.getName().hashCode()
                + 31 * method.getReturnType().toString().hashCode()
                + 31 * Arrays.hashCode(parameterTypes);
    }
}
