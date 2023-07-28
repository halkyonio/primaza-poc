package io.halkyon.utils;

import org.htmlunit.IncorrectnessListener;

public class SilentIncorrectnessListener implements IncorrectnessListener {
    @Override
    public void notify(String s, Object o) {
        // do nothing
    }
}
