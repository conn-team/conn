package com.github.connteam.conn.core;

import java.util.Comparator;

public class ByteArrayComparator implements Comparator<byte[]> {
    @Override
    public int compare(byte[] l, byte[] r) {
        for (int i = 0; i < l.length && i < r.length; i++) {
            if (l[i] < r[i]) {
                return -1;
            } else if (l[i] > r[i]) {
                return 1;
            }
        }

        if (l.length != r.length) {
            return (l.length < r.length ? -1 : 1);
        }
        return 0;
    }
}
