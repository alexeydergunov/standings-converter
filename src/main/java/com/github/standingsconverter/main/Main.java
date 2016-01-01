package com.github.standingsconverter.main;

import com.github.standingsconverter.entity.Contest;
import com.github.standingsconverter.outputter.Outputter;
import com.github.standingsconverter.parser.Parser;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 4) {
            System.out.println("Usage: java -jar [jarFile] [parserClass] [outputterClass] [inputFile] [outputFile]");
            return;
        }
        Parser parser = ClassFactory.createInstance(Parser.class, args[0]);
        Outputter outputter = ClassFactory.createInstance(Outputter.class, args[1]);
        String inputFile = args[2];
        String outputFile = args[3];
        System.out.printf("Use parser = %s, outputter = %s\n", parser.getClass().getSimpleName(), outputter.getClass().getSimpleName());
        long t1 = System.currentTimeMillis();
        Contest contest = parser.parse(inputFile);
        outputter.output(contest, outputFile);
        long t2 = System.currentTimeMillis();
        System.out.printf("Parsing completed in %d ms.\n", t2 - t1);
    }
}
