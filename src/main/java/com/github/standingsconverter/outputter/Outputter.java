package com.github.standingsconverter.outputter;

import com.github.standingsconverter.entity.Contest;

import java.io.IOException;

public interface Outputter {
    void output(Contest contest, String filename) throws IOException;
}
