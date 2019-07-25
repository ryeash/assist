package vest.assist.util;

import sun.reflect.CallerSensitive;

import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * Represents a reflected field in a class. Provides some metadata and additional information for the field.
 */
public class FieldTarget implements AnnotatedElement {

    private final Field field;
    private final MethodHandle setter;
    private final boolean isInject;
    private final boolean isStatic;

    public FieldTarget(Field field) {
        this.field = field;
        try {
            Reflector.makeAccessible(field);
            this.setter = MethodHandles.lookup().unreflectSetter(field);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("unable to gather meta data for field " + Reflector.detailString(field), e);
        }
        this.isInject = field.isAnnotationPresent(Inject.class);
        this.isStatic = Modifier.isStatic(field.getModifiers());
    }

    public Field field() {
        return field;
    }

    public boolean isInject() {
        return isInject;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Class<?> getDeclaringClass() {
        return field.getDeclaringClass();
    }

    public String getName() {
        return field.getName();
    }

    public int getModifiers() {
        return field.getModifiers();
    }

    public boolean isEnumConstant() {
        return field.isEnumConstant();
    }

    public boolean isSynthetic() {
        return field.isSynthetic();
    }

    public Class<?> getType() {
        return field.getType();
    }

    public Type getGenericType() {
        return field.getGenericType();
    }

    @Override
    public boolean equals(Object obj) {
        return field.equals(obj);
    }

    @Override
    public int hashCode() {
        return field.hashCode();
    }

    @Override
    public String toString() {
        return field.toString();
    }

    public String toGenericString() {
        return field.toGenericString();
    }

    @CallerSensitive
    public Object get(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.get(obj);
    }

    @CallerSensitive
    public boolean getBoolean(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.getBoolean(obj);
    }

    @CallerSensitive
    public byte getByte(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.getByte(obj);
    }

    @CallerSensitive
    public char getChar(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.getChar(obj);
    }

    @CallerSensitive
    public short getShort(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.getShort(obj);
    }

    @CallerSensitive
    public int getInt(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.getInt(obj);
    }

    @CallerSensitive
    public long getLong(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.getLong(obj);
    }

    @CallerSensitive
    public float getFloat(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.getFloat(obj);
    }

    @CallerSensitive
    public double getDouble(Object obj) throws IllegalArgumentException, IllegalAccessException {
        return field.getDouble(obj);
    }

    @CallerSensitive
    public void set(Object obj, Object value) throws Throwable {
        if (isStatic) {
            setter.invoke(value);
        } else {
            setter.invoke(obj, value);
        }
    }

    @CallerSensitive
    public void setBoolean(Object obj, boolean z) throws Throwable {
        setter.invokeExact(obj, z);
    }

    @CallerSensitive
    public void setByte(Object obj, byte b) throws IllegalArgumentException, IllegalAccessException {
        field.setByte(obj, b);
    }

    @CallerSensitive
    public void setChar(Object obj, char c) throws IllegalArgumentException, IllegalAccessException {
        field.setChar(obj, c);
    }

    @CallerSensitive
    public void setShort(Object obj, short s) throws IllegalArgumentException, IllegalAccessException {
        field.setShort(obj, s);
    }

    @CallerSensitive
    public void setInt(Object obj, int i) throws IllegalArgumentException, IllegalAccessException {
        field.setInt(obj, i);
    }

    @CallerSensitive
    public void setLong(Object obj, long l) throws IllegalArgumentException, IllegalAccessException {
        field.setLong(obj, l);
    }

    @CallerSensitive
    public void setFloat(Object obj, float f) throws IllegalArgumentException, IllegalAccessException {
        field.setFloat(obj, f);
    }

    @CallerSensitive
    public void setDouble(Object obj, double d) throws IllegalArgumentException, IllegalAccessException {
        field.setDouble(obj, d);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return field.getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
        return field.getAnnotationsByType(annotationClass);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        return field.getDeclaredAnnotations();
    }

    public AnnotatedType getAnnotatedType() {
        return field.getAnnotatedType();
    }

    public static void setAccessible(AccessibleObject[] array, boolean flag) throws SecurityException {
        AccessibleObject.setAccessible(array, flag);
    }

    public void setAccessible(boolean flag) throws SecurityException {
        field.setAccessible(flag);
    }

    public boolean isAccessible() {
        return field.isAccessible();
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return field.isAnnotationPresent(annotationClass);
    }

    @Override
    public Annotation[] getAnnotations() {
        return field.getAnnotations();
    }

    @Override
    public <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
        return field.getDeclaredAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return field.getDeclaredAnnotationsByType(annotationClass);
    }
}
