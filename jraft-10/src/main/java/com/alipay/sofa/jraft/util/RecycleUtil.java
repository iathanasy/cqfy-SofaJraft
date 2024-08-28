package com.alipay.sofa.jraft.util;


public final class RecycleUtil {


    public static boolean recycle(final Object obj) {
        return obj instanceof Recyclable && ((Recyclable) obj).recycle();
    }

    private RecycleUtil() {
    }
}
