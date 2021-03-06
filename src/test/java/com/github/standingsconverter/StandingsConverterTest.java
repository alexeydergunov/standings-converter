package com.github.standingsconverter;

import com.github.standingsconverter.entity.Contest;
import com.github.standingsconverter.outputter.EjudgeOutputter;
import com.github.standingsconverter.outputter.Outputter;
import com.github.standingsconverter.outputter.TestsysOutputter;
import com.github.standingsconverter.parser.*;
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
        TestInfo testInfo = new TestInfo(
                new EjudgeParser(),
                new TestsysOutputter(),
                new File("src/test/resources/ejudge-to-testsys/ejudge-log-ssau-qual-2015.xml"),
                new File("src/test/resources/ejudge-to-testsys/testsys-log-ssau-qual-2015.dat")
        );
        testInfo.runTest();
    }

    @Test
    public void test2() throws IOException {
        TestInfo testInfo = new TestInfo(
                new EjudgeParser(),
                new TestsysOutputter(),
                new File("src/test/resources/ejudge-to-testsys/ejudge-log-ssau-contest-2015.xml"),
                new File("src/test/resources/ejudge-to-testsys/testsys-log-ssau-contest-2015.dat")
        );
        testInfo.runTest();
    }

    @Test
    public void test3() throws IOException {
        TestInfo testInfo = new TestInfo(
                new EjudgeParser(),
                new TestsysOutputter(),
                new File("src/test/resources/ejudge-to-testsys/ejudge-log-ssau-qual-2014.xml"),
                new File("src/test/resources/ejudge-to-testsys/testsys-log-ssau-qual-2014.dat")
        );
        testInfo.runTest();
    }

    @Test
    public void test4() throws IOException {
        TestInfo testInfo = new TestInfo(
                new EjudgeParser(),
                new TestsysOutputter(),
                new File("src/test/resources/ejudge-to-testsys/ejudge-log-ssau-contest-2014.xml"),
                new File("src/test/resources/ejudge-to-testsys/testsys-log-ssau-contest-2014.dat")
        );
        testInfo.runTest();
    }

    @Test
    public void test5() throws IOException {
        TestInfo testInfo = new TestInfo(
                new EjudgeParser(),
                new TestsysOutputter(),
                new File("src/test/resources/ejudge-to-testsys/ejudge-log-ssau-qual-2013.xml"),
                new File("src/test/resources/ejudge-to-testsys/testsys-log-ssau-qual-2013.dat")
        );
        testInfo.runTest();
    }

    @Test
    public void test6() throws IOException {
        TestInfo testInfo = new TestInfo(
                new PCMSParser(),
                new TestsysOutputter(),
                new File("src/test/resources/pcms-to-testsys/pcms-log-neerc-2007.xml"),
                new File("src/test/resources/pcms-to-testsys/testsys-log-neerc-2007.dat")
        );
        testInfo.runTest();
    }

    @Test
    public void test7() throws IOException {
        TestInfo testInfo = new TestInfo(
                new PCMSParser(),
                new TestsysOutputter(),
                new File("src/test/resources/pcms-to-testsys/pcms-log-neerc-northern-2013.xml"),
                new File("src/test/resources/pcms-to-testsys/testsys-log-neerc-northern-2013.dat")
        );
        testInfo.runTest();
    }

    @Test
    public void test8() throws IOException {
        TestInfo testInfo = new TestInfo(
                new YandexContestParser(),
                new TestsysOutputter(),
                new File("src/test/resources/yandexcontest-to-testsys/yandexcontest-log-ptz-yandex-cup-2018.xml"),
                new File("src/test/resources/yandexcontest-to-testsys/testsys-log-ptz-yandex-cup-2018.dat")
        );
        testInfo.runTest();
    }

    @Test
    public void test9() throws IOException {
        TestInfo testInfo = new TestInfo(
                new YandexContestParser(),
                new TestsysOutputter(),
                new File("src/test/resources/yandexcontest-to-testsys/yandexcontest-log-ptz-yandex-cup-2019.xml"),
                new File("src/test/resources/yandexcontest-to-testsys/testsys-log-ptz-yandex-cup-2019.dat")
        );
        testInfo.runTest();
    }

    @Test(enabled = false) // disabled for auto-testing as Codeforces may be unavailable
    public void remoteTest1() throws IOException {
        TestInfo testInfo = new TestInfo(
                new CodeforcesAPIParser(),
                new EjudgeOutputter(),
                new File("src/test/resources/codeforces-to-ejudge/codeforces-api-ssau-noob-contest-2015.properties"),
                new File("src/test/resources/codeforces-to-ejudge/ejudge-log-ssau-noob-contest-2015.xml")
        );
        testInfo.runTest();
    }
}
