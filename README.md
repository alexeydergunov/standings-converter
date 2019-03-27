# Standings Converter

[![Build Status](https://travis-ci.org/alexeydergunov/standings-converter.svg?branch=master)]
(https://travis-ci.org/alexeydergunov/standings-converter)

Standings Converter is an utility for converting submission logs of programming contests. Different testing systems use
their own format of these logs, and that causes troubles when you try to copy a contest from one testing system to
another. Usually people just don't import monitor and submission logs. This project is to help you to import them.

## Supported testing systems

### As input

- [Codeforces API](http://codeforces.com/api/help) (only ghosts and official participants, for unfreezing)
- Ejudge (open-source testing system, [https://ejudge.ru](https://ejudge.ru))
- PCMS (used in University ITMO)
- Testsys (used in Codeforces Gyms)
- Yandex.Contest

### As output

- Ejudge (very useful for unfreezing by [OSt's S4RiS-StanD](https://github.com/OStrekalovsky/S4RiS-StanD))
- Testsys

## Usage

1. Download and install Maven, enter the root directory of this project and execute `mvn clean install`.
2. Obtain a submission log you want to convert. The examples of the submissions logs for different testing systems can
be found in tests.
3. Execute `java -jar [jarFile] [parserClass] [outputterClass] [inputFile] [outputFile]`. Parameter `jarFile` is the
name of the project's jar in the target directory, including the extension, `parserClass` is the name of the class to
process the input file, `outputterClass` is the name of the class to produce the output file, and the names `inputFile`
and `outputFile` speak for themselves. For CodeforcesAPIParser, `inputFile` is a properties file which must contain a
property `contestId`, and may contains properties `key` and `secret` if you are going to parse a private contest.
