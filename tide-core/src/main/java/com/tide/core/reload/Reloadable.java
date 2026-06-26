package com.tide.core.reload;

public interface Reloadable {

    /** @return number of entries successfully (re)loaded */
    int reload();
}
