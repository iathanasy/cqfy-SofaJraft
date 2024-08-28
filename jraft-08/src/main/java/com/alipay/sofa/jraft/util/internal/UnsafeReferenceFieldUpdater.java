package com.alipay.sofa.jraft.util.internal;

import sun.misc.Unsafe;

import java.lang.reflect.Field;


@SuppressWarnings("unchecked")
final class UnsafeReferenceFieldUpdater<U, W> implements ReferenceFieldUpdater<U, W> {

    private final long   offset;
    private final Unsafe unsafe;

    UnsafeReferenceFieldUpdater(Unsafe unsafe, Class<? super U> tClass, String fieldName) throws NoSuchFieldException {
        final Field field = tClass.getDeclaredField(fieldName);
        if (unsafe == null) {
            throw new NullPointerException("unsafe");
        }
        this.unsafe = unsafe;
        this.offset = unsafe.objectFieldOffset(field);
    }

    @Override
    public void set(final U obj, final W newValue) {
        this.unsafe.putObject(obj, this.offset, newValue);
    }

    @Override
    public W get(final U obj) {
        return (W) this.unsafe.getObject(obj, this.offset);
    }
}
