package Test;

import main.REPLErrors;
import main.Reader;
import main.ReaderErrors;
import org.junit.Test;

import java.util.ArrayList;

import static main.REPLErrors.SyntaxError;
import static main.Types.*;
import static org.junit.Assert.*;

public class ReaderTest {
    @Test
    public void TestTokenize() {
        try {
            //whitespaces are ignored
            ArrayList<String> out = Reader.tokenize("foo 123 ");
            assertArrayEquals(out.toArray(new String[0]), new String[]{"foo", "123"});

            //list of single special chars ()'`, and the 2 special chars ,@ captured together
            out = Reader.tokenize("()'`,@@");
            assertArrayEquals(new String[]{"(", ")", "'", "`", ",@", "@"}, out.toArray(new String[0]));

            // double quotes
            out = Reader.tokenize("\"first\"\"second \\\"double quotes \\\"\\\"\\\"");
            assertArrayEquals(out.toArray(new String[0]), new String[]{"\"first\"", "\"second \\\"double quotes \\\"\\\"\\\""});

            //expect syntax error to bw thrown since no closing quotes
            assertThrows("missing closing quote or empty quotes/string", SyntaxError.class, () -> Reader.tokenize("\"first\"\"second \\\"double quotes \\\"\\\"\\\" ,,, \"\"open"));

        } catch (REPLErrors syntaxError) {
            syntaxError.printStackTrace();
        }
    }

    @Test
    public void testReadForm() {
        try {
            //QUOTES

            //quotes without brackets, must start with some sort of quote
            MyDataType result = Reader.read_str("'a");
            assertEquals(Reader.read_str("(quote a)").toString(), result.toString());

            result = Reader.read_str("''a");
            assertEquals(Reader.read_str("(quote (quote a))").toString(), result.toString());

            result = Reader.read_str(",a");
            assertEquals(Reader.read_str("(unquote a)").toString(), result.toString());

            result = Reader.read_str(",,a");
            assertEquals(Reader.read_str("(unquote (unquote a))").toString(), result.toString());

            result = Reader.read_str(",@a");
            assertEquals(Reader.read_str("(splice-unquote a)").toString(), result.toString());

            result = Reader.read_str(",@,@a");
            assertEquals(Reader.read_str("(splice-unquote (splice-unquote a))").toString(), result.toString());


            //anything after the first valid expression('a) is ignored, regardless of what that exp is
            result = Reader.read_str("'a b");
            assertEquals(Reader.read_str("(quote a)").toString(), result.toString());

            result = Reader.read_str("'a 'b");
            assertEquals(Reader.read_str("(quote a)").toString(), result.toString());

            result = Reader.read_str("`a b");
            assertEquals(Reader.read_str("(quasiquote a)").toString(), result.toString());

            result = Reader.read_str("`a 'b");
            assertEquals(Reader.read_str("(quasiquote a)").toString(), result.toString());

            result = Reader.read_str(",a b");
            assertEquals(Reader.read_str("(unquote a)").toString(), result.toString());

            result = Reader.read_str(",a ,b");
            assertEquals(Reader.read_str("(unquote a)").toString(), result.toString());

            result = Reader.read_str(",@a b");
            assertEquals(Reader.read_str("(splice-unquote a)").toString(), result.toString());

            result = Reader.read_str(",@a 'b");
            assertEquals(Reader.read_str("(splice-unquote a)").toString(), result.toString());

            result = Reader.read_str("(+ 1 2) b");
            assertEquals(Reader.read_str("(+ 1 2)").toString(), result.toString());
            //exp here is "foo 'b" which needs surrounding brackets
            assertThrows("wrong syntax, expected a list for more than 1 argument", SyntaxError.class, () -> Reader.read_str("foo 'b"));
            //errors when nothing to quote
            assertThrows("wrong syntax, nothing to quote", SyntaxError.class, () -> Reader.read_str("'"));
            assertThrows("wrong syntax, nothing to quote", SyntaxError.class, () -> Reader.read_str("`"));
            assertThrows("wrong syntax, nothing to quote", SyntaxError.class, () -> Reader.read_str(","));
            assertThrows("wrong syntax, nothing to quote", SyntaxError.class, () -> Reader.read_str(",@"));

            //list quoting
            result = Reader.read_str("'(+ 1 2 3 3)");
            assertEquals(Reader.read_str("(quote (+ 1 2 3 3))").toString(), result.toString());

            result = Reader.read_str("'()");
            assertEquals(Reader.read_str("(quote ())").toString(), result.toString());


            //CONSTANTS
            //true, false ,nil
            result = Reader.read_str("nil");
            assertTrue(Nil.equals(result));

            result = Reader.read_str("true");
            assertTrue(True.equals(result));

            result = Reader.read_str("false");
            assertTrue(False.equals(result));

            result = Reader.read_str("Nil");
            assertFalse(Nil.equals(result));
            assertTrue(result instanceof SymbolType);

            result = Reader.read_str("True");
            assertFalse(True.equals(result));
            assertTrue(result instanceof SymbolType);

            result = Reader.read_str("False");
            assertFalse(False.equals(result));
            assertTrue(result instanceof SymbolType);

            //INTEGERS
            result = Reader.read_str("+3");
            assertTrue(new IntegerType(3).equals(result));

            result = Reader.read_str("-3");
            assertTrue(new IntegerType(-3).equals(result));

            result = Reader.read_str("0");
            assertTrue(new IntegerType(0).equals(result));

            //SYMBOLS
            result = Reader.read_str("-");
            assertTrue(new SymbolType("-").equals(result));

            result = Reader.read_str("  asd");
            assertTrue(new SymbolType("asd").equals(result));

            result = Reader.read_str(" asd  ");
            assertTrue(new SymbolType("asd").equals(result));

            result = Reader.read_str("[");
            assertTrue(new SymbolType("[").equals(result));

            result = Reader.read_str("{");
            assertTrue(new SymbolType("{").equals(result));

            result = Reader.read_str("}");
            assertTrue(new SymbolType("}").equals(result));

            result = Reader.read_str("~");
            assertTrue(new SymbolType("~").equals(result));

            result = Reader.read_str("@");
            assertTrue(new SymbolType("@").equals(result));
            //strings
            result = Reader.read_str("\"foo\"");
            assertTrue(new StringType("foo").equals(result));

            //malformated inputs
            assertThrows(SyntaxError.class, () -> {//thrown by tokenize
                Reader.read_str("\"foo");
            });

            assertThrows(SyntaxError.class, () -> {//thrown by tokenize
                Reader.read_str("\"\"");
            });

            assertThrows(SyntaxError.class, () -> {//thrown by tokenize
                Reader.read_str("\"");
            });

            assertThrows(SyntaxError.class, () -> {//thrown by tokenize, parses empty string, bar, empty string
                Reader.read_str("\"\"bar\"\"");
            });

            assertThrows(SyntaxError.class, () -> {//thrown by tokenize, parses empty string, bar, empty string
                Reader.read_str("\"\"bar\"");
            });

            assertThrows(ReaderErrors.ParseError.class, () -> {//thrown by read_atom
                Reader.read_str("0.9");
            });

        } catch (REPLErrors | ReaderErrors syntaxError) {
            syntaxError.printStackTrace();
        }
    }
}
