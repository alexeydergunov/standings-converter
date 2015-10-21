package com.github.standingsconverter.main;

import com.github.standingsconverter.entity.Contest;
import com.github.standingsconverter.outputter.Outputter;
import com.github.standingsconverter.outputter.TestsysOutputter;
import com.github.standingsconverter.parser.EjudgeParser;
import com.github.standingsconverter.parser.Parser;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        // TODO support multiple parsers and outputters
        if (args.length != 2) {
            System.out.println("Usage: java -jar [jarFile] [ejudgeInputFile] [testsysOutputFile]");
            return;
        }
        String inputFile = args[0];
        String outputFile = args[1];
        Parser parser = new EjudgeParser();
        Outputter outputter = new TestsysOutputter();
        long t1 = System.currentTimeMillis();
        Contest contest = parser.parse(inputFile);
        outputter.output(contest, outputFile);
        long t2 = System.currentTimeMillis();
        System.out.printf("Parsing completed in %d ms.\n", t2 - t1);
    }
}
