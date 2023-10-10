package Test;

import main.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static main.Types.*;
import static org.junit.Assert.*;


public class CoreTest {
    Environment env = Core.getNamespace();
    ListType args;
    MyFunction function;

    @Test
    public void initTest() {
        assertNotNull(env);
    }

    @Test
    public void primitivesTest() throws REPLErrors, ReaderErrors {
        //cons
        function = (MyFunction) env.get("cons");
        args = new ListType(new IntegerType(-20), new IntegerType(-6));
        ListType resultList = (ListType) function.apply(args);
        assertArrayEquals(args.getValue().toArray(), resultList.getValue().toArray());

        args = new ListType(new IntegerType(1), new ListType());// (1 ())
        resultList = (ListType) function.apply(args);
        ListType expectedList = new ListType(new IntegerType(1));
        assertEquals(expectedList.toString(), resultList.toString());

        args = new ListType(new IntegerType(-20), new IntegerType(-6));
        resultList = (ListType) function.apply(args);
        assertArrayEquals(args.getValue().toArray(), resultList.getValue().toArray());
    }

    @Test
    public void mathTests() throws REPLErrors, ReaderErrors {
        ListType args1, args2, args3, args4, args5, args6;

        MyFunction plus, minus, times, divide;
        args1 = new ListType(new IntegerType(0), new IntegerType(1));
        args2 = new ListType(new IntegerType(1), new IntegerType(0));
        args3 = new ListType(new IntegerType(0), new IntegerType(0));
        args4 = new ListType(new IntegerType(3), new IntegerType(-5));
        args5 = new ListType(new IntegerType(-3), new IntegerType(5));
        args6 = new ListType(new IntegerType(-5), new IntegerType(-3));
        //simple expressions
        plus = (MyFunction) env.get("+");
        minus = (MyFunction) env.get("-");
        times = (MyFunction) env.get("*");
        divide = (MyFunction) env.get("/");
        List<MyFunction> funList = Arrays.asList(plus, minus, times);
        List<ListType> argsList = Arrays.asList(args1, args2, args3, args4, args5, args6);
        Integer[] expectedResults = {
                1, 1, 0, -2, 2, -8,
                -1, 1, 0, 8, -8, -2,
                0, 0, 0, -15, -15, 15,
                0, 0, 0, 1
        };
        ArrayList<Integer> actualResults = new ArrayList<>();
        for (MyFunction function : funList) {
            for (ListType arg : argsList) {
                actualResults.add((Integer) function.apply(arg).getValue());
            }
        }
        actualResults.add((Integer) divide.apply(args1).getValue());
        actualResults.add((Integer) divide.apply(args4).getValue());
        actualResults.add((Integer) divide.apply(args5).getValue());
        actualResults.add((Integer) divide.apply(args6).getValue());
        assertArrayEquals(expectedResults, actualResults.toArray(new Integer[0]));

        assertThrows(REPLErrors.SyntaxError.class, () -> divide.apply(args2));
        assertThrows(REPLErrors.SyntaxError.class, () -> divide.apply(args3));

    }

    @Test
    public void coreTest() throws REPLErrors, ReaderErrors {
        assertTrue(Core.coreNames.contains("+"));
        assertFalse(Core.coreNames.contains("add"));
        REPL.rep("(def! add 2)");
        assertFalse(Core.coreNames.contains("add"));
        assertEquals("2", REPL.rep("add"));

        //override protection of core functions and special forms
        assertThrows(REPLErrors.SyntaxError.class, () -> REPL.rep("(def! + 2)"));
        assertThrows(REPLErrors.SyntaxError.class, () -> REPL.rep("(set! + 2)"));
        assertThrows(REPLErrors.SyntaxError.class, () -> REPL.rep("(defun + 2)"));
        assertThrows(REPLErrors.SyntaxError.class, () -> REPL.rep("(let* (+ 2) 1)"));

        //Macro init tests
        Core.initMacros();
        REPL.rep("(def! x 2)");
        assertEquals("3", REPL.rep("(incf x)"));

    }

    @Test
    public void concatTest() throws REPLErrors, ReaderErrors {
        String in = "(concat)";
        assertEquals("()", REPL.rep(in));
        in = "(concat '(a b c))";
        assertEquals("(a b c)", REPL.rep(in));

        assertThrows(REPLErrors.SyntaxError.class, () -> REPL.rep("(concat '(a b c) 1)"));

        in = "(concat '(a b c) '(x y z))";
        assertEquals("(a b c x y z)", REPL.rep(in));

        in = "(concat '(a b c) '(x y z) '(1 2))";
        assertEquals("(a b c x y z 1 2)", REPL.rep(in));
    }
}
