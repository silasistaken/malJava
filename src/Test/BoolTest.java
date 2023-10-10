package Test;

import main.REPL;
import main.REPLErrors;
import main.ReaderErrors;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BoolTest {


    @Test
    public void boolsTests() throws REPLErrors, ReaderErrors {

        //Atom
        String output = REPL.rep("(atom (atom (quote a)))");
        assertEquals(output, "#true");

        output = REPL.rep("(atom (quote (atom (quote a))))");
        assertEquals(output, "#false");

        output = REPL.rep("(atom ())");//empty list evaluates to itself
        assertEquals(output, "#true");

        output = REPL.rep("(atom (quote ()))");
        assertEquals(output, "#true");

        output = REPL.rep("(eq (quote a) (quote a))");
        assertEquals(output, "#true");

        output = REPL.rep("(eq 12 1)");
        assertEquals(output, "#false");

        output = REPL.rep("(atom (quote (a d 3)))");
        assertEquals(output, "#false");

        output = REPL.rep("(atom 3)");
        assertEquals(output, "#true");


        output = REPL.rep("(cond (2 (quote two)))");
        assertEquals(output, "two");

        output = REPL.rep("(eq 'a 'a)");
        assertEquals(output, "#true");

        output = REPL.rep("(eq '() '())");
        assertEquals(output, "#true");

        output = REPL.rep("(eq '(a) 'a)");
        assertEquals(output, "#false");

        output = REPL.rep("(eq '(a b c) '(a b c))");
        assertEquals(output, "#false");

        output = REPL.rep("(eq 1 1)");
        assertEquals(output, "#true");

        output = REPL.rep("(atom (eq 1 1))");
        assertEquals(output, "#true");

    }

}
