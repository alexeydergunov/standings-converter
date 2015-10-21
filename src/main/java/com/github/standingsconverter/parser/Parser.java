package com.github.standingsconverter.parser;

import com.github.standingsconverter.entity.Contest;

import java.io.IOException;

public interface Parser {
    Contest parse(String filename) throws IOException;
}
