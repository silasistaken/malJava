package Test;

import main.REPLErrors;
import main.REPLErrors.SyntaxError;
import main.Reader;
import main.ReaderErrors;
import org.junit.Test;

import static main.Types.*;
import static org.junit.Assert.*;

public class PrinterTest {

    @Test
    public void quoteTest() throws REPLErrors, ReaderErrors {
        //quoting
        String in = "'a";
        assertEquals(in, Reader.read_str(in).toString());

        in = "`a";
        assertEquals(in, Reader.read_str(in).toString());

        in = ",a";
        assertEquals(in, Reader.read_str(in).toString());

        in = ",@a";
        assertEquals(in, Reader.read_str(in).toString());

        in = "(quote a)";
        assertEquals("'a", Reader.read_str(in).toString());

        in = "(quasiquote a)";
        assertEquals("`a", Reader.read_str(in).toString());

        in = "(unquote a)";
        assertEquals(",a", Reader.read_str(in).toString());

        in = "(splice-unquote a)";
        assertEquals(",@a", Reader.read_str(in).toString());
    }

    @Test
    public void listTest() throws REPLErrors, ReaderErrors {

        String in = "(1 2 4)";
        assertEquals(in, Reader.read_str(in).toString());

        in = "(a b 2 +)";
        assertEquals(in, Reader.read_str(in).toString());

        in = "(a b (nil) d)";
        assertEquals("(a b (#nil) d)", Reader.read_str(in).toString());

        in = "((a b c))";
        assertEquals(in, Reader.read_str(in).toString());

        in = "()";
        assertEquals(in, Reader.read_str(in).toString());
    }

    @Test
    public void constantTest() throws REPLErrors, ReaderErrors {

        MyDataType result = Reader.read_str("nil");
        assertTrue(Nil.equals(result));
        assertEquals("#nil", result.toString());


        result = Reader.read_str("true");
        assertTrue(True.equals(result));
        assertEquals("#true", result.toString());

        result = Reader.read_str("false");
        assertTrue(False.equals(result));
        assertEquals("#false", result.toString());

        result = Reader.read_str("Nil");
        assertFalse(Nil.equals(result));
        assertEquals("Nil", result.toString());

        result = Reader.read_str("True");
        assertFalse(True.equals(result));
        assertEquals("True", result.toString());

        result = Reader.read_str("False");
        assertFalse(False.equals(result));
        assertEquals("False", result.toString());

    }

    @Test
    public void integerTest() throws REPLErrors, ReaderErrors {
        String in = "+1";
        assertEquals("1", Reader.read_str(in).toString());

        in = "-1";
        assertEquals("-1", Reader.read_str(in).toString());

        assertThrows(SyntaxError.class, () -> Reader.read_str("- 1"));
        assertThrows(SyntaxError.class, () -> Reader.read_str("+ 1"));
    }

    @Test
    public void stringTest() throws REPLErrors, ReaderErrors {

        MyDataType result = Reader.read_str("\"foo\"");
        assertTrue(new StringType("foo").equals(result));
        assertEquals("\"foo\"", result.toString());

        //special characters within a string
        result = Reader.read_str("\"foo,\"");
        assertTrue(new StringType("foo,").equals(result));
        assertEquals("\"foo,\"", result.toString());

        result = Reader.read_str("\",foo\"");
        assertTrue(new StringType(",foo").equals(result));
        assertEquals("\",foo\"", result.toString());

        result = Reader.read_str("\"'foo\"");
        assertTrue(new StringType("'foo").equals(result));
        assertEquals("\"'foo\"", result.toString());

        result = Reader.read_str("\"f,@oo\"");
        assertTrue(new StringType("f,@oo").equals(result));
        assertEquals("\"f,@oo\"", result.toString());

        result = Reader.read_str("\"`foo\"");
        assertTrue(new StringType("`foo").equals(result));
        assertEquals("\"`foo\"", result.toString());

        result = Reader.read_str("\"`foo\"");
        assertTrue(new StringType("`foo").equals(result));
        assertEquals("\"`foo\"", result.toString());

        result = Reader.read_str("\"false\"");
        assertTrue(new StringType("false").equals(result));
        assertEquals("\"false\"", result.toString());

        assertThrows("missing closing quote or empty quotes/string", SyntaxError.class, () -> Reader.read_str("\"\"foo\"\""));
    }

}
