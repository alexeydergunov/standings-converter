package com.github.standingsconverter.main;

import com.github.standingsconverter.outputter.EjudgeOutputter;
import com.github.standingsconverter.outputter.TestsysOutputter;
import com.github.standingsconverter.parser.CodeforcesAPIParser;
import com.github.standingsconverter.parser.EjudgeParser;
import com.github.standingsconverter.parser.PCMSParser;
import com.github.standingsconverter.parser.TestsysParser;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ClassFactory {
    private static final List<Class<?>> SUPPORTED_CLASSES = Collections.unmodifiableList(Arrays.asList(
            CodeforcesAPIParser.class,
            EjudgeParser.class,
            PCMSParser.class,
            TestsysParser.class,
            EjudgeOutputter.class,
            TestsysOutputter.class
    ));

    private ClassFactory() {
    }

    public static <T> T createInstance(Class<T> iface, String neededName) {
        Class<?> neededClass = null;
        for (Class<?> subclass : SUPPORTED_CLASSES) {
            if (iface.isAssignableFrom(subclass)) {
                String subclassName = subclass.getSimpleName();
                if (subclassName.equalsIgnoreCase(neededName) || subclassName.equalsIgnoreCase(neededName + iface.getSimpleName())) {
                    if (neededClass != null) {
                        throw new IllegalArgumentException("Duplicate children classes found for interface = " + iface + ", name = " + neededName);
                    }
                    neededClass = subclass;
                }
            }
        }
        if (neededClass == null) {
            throw new IllegalArgumentException("No children classes found for interface = " + iface + ", name = " + neededName);
        }
        try {
            //noinspection unchecked
            return (T) neededClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
