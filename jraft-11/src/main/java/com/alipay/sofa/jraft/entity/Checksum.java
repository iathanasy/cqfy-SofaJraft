package com.alipay.sofa.jraft.entity;

import java.util.Collection;



public interface Checksum {


    long checksum();


    default long checksum(final long v1, final long v2) {
        return v1 ^ v2;
    }


    default long checksum(final Collection<? extends Checksum> factors, long v) {
        if (factors != null && !factors.isEmpty()) {
            for (final Checksum factor : factors) {
                v = checksum(v, factor.checksum());
            }
        }
        return v;
    }

}
