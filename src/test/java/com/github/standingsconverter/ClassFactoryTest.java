package com.github.standingsconverter;

import com.github.standingsconverter.main.ClassFactory;
import com.github.standingsconverter.outputter.Outputter;
import com.github.standingsconverter.parser.EjudgeParser;
import com.github.standingsconverter.parser.Parser;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ClassFactoryTest {
    @Test
    public void testCreatingClasses() {
        assertClass(ClassFactory.createInstance(Parser.class, "EjudgeParser"), EjudgeParser.class);
        assertClass(ClassFactory.createInstance(Parser.class, "ejudgeparser"), EjudgeParser.class);
        assertClass(ClassFactory.createInstance(Parser.class, "EJUDGEPARSER"), EjudgeParser.class);
        assertClass(ClassFactory.createInstance(Parser.class, "EjUdGePaRsEr"), EjudgeParser.class);
        assertClass(ClassFactory.createInstance(Parser.class, "Ejudge"), EjudgeParser.class);
        assertClass(ClassFactory.createInstance(Parser.class, "ejudge"), EjudgeParser.class);
        assertClass(ClassFactory.createInstance(Parser.class, "EJUDGE"), EjudgeParser.class);
        assertClass(ClassFactory.createInstance(Parser.class, "EjUdGe"), EjudgeParser.class);
        Assert.assertThrows(() -> ClassFactory.createInstance(Parser.class, "Parser"));
        Assert.assertThrows(() -> ClassFactory.createInstance(Parser.class, "ParserEjudge"));
        Assert.assertThrows(() -> ClassFactory.createInstance(Parser.class, "Ejudge-Parser"));
        Assert.assertThrows(() -> ClassFactory.createInstance(Parser.class, "Ejudge_Parser"));
        Assert.assertThrows(() -> ClassFactory.createInstance(Parser.class, "String"));
        Assert.assertThrows(() -> ClassFactory.createInstance(String.class, "EjudgeParser"));
        Assert.assertThrows(() -> ClassFactory.createInstance(Outputter.class, "EjudgeParser"));
    }

    private static <T> void assertClass(T actualObject, Class<? extends T> expectedClass) {
        Assert.assertEquals(actualObject.getClass(), expectedClass);
    }
}
