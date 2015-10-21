package com.github.standingsconverter;

import com.github.standingsconverter.entity.Contest;
import com.github.standingsconverter.outputter.Outputter;
import com.github.standingsconverter.outputter.TestsysOutputter;
import com.github.standingsconverter.parser.EjudgeParser;
import com.github.standingsconverter.parser.Parser;
import org.apache.commons.io.FileUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class StandingsConverterTest {
    private File testDirectory;

    @BeforeClass
    public void beforeClass() {
        String uuid = UUID.randomUUID().toString();
        testDirectory = new File(this.getClass().getSimpleName() + "_" + uuid);
        if (!testDirectory.mkdir()) {
            Assert.fail("Can't create test directory " + testDirectory);
        }
    }

    @AfterClass
    public void afterClass() {
        if (!FileUtils.deleteQuietly(testDirectory)) {
            Assert.fail("Can't delete test directory " + testDirectory);
        }
    }

    private class TestInfo {
        private final Parser parser;
        private final Outputter outputter;
        private final File inputFile;
        private final File expectedOutputFile;

        private TestInfo(Parser parser, Outputter outputter, File inputFile, File expectedOutputFile) {
            this.parser = parser;
            this.outputter = outputter;
            this.inputFile = inputFile;
            this.expectedOutputFile = expectedOutputFile;
        }

        private void runTest() throws IOException {
            Contest contest = parser.parse(inputFile.getAbsolutePath());
            File actualOutputFile = new File(testDirectory, "actual_" + expectedOutputFile.getName());
            outputter.output(contest, actualOutputFile.getAbsolutePath());
            String actualContent = FileUtils.readFileToString(actualOutputFile);
            String expectedContent = FileUtils.readFileToString(expectedOutputFile);
            Assert.assertEquals(actualContent, expectedContent);
        }
    }

    @Test
    public void test1() throws IOException {
        // TODO add other tests
        TestInfo testInfo = new TestInfo(
                new EjudgeParser(),
                new TestsysOutputter(),
                new File("src/test/resources/ejudge-to-testsys/ejudge-log-ssau-qual-2015.xml"),
                new File("src/test/resources/ejudge-to-testsys/testsys-log-ssau-qual-2015.dat")
        );
        testInfo.runTest();
    }
}