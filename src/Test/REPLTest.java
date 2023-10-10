package Test;


import main.REPL;
import main.REPLErrors;
import main.ReaderErrors;
import org.junit.Test;

import static main.REPLErrors.SyntaxError;
import static main.Types.*;
import static org.junit.Assert.*;

public class REPLTest {


    @Test
    public void CoreTests() throws REPLErrors, ReaderErrors {

        //list vs cons
        String output = REPL.rep("(list 1 2 3)");
        assertEquals(output, "(1 2 3)");

        output = REPL.rep("(list 1 2 3)");
        String output2 = REPL.rep("(cons 1 (cons 2 (cons 3 '())))");
        assertEquals(output, output2);

        output = REPL.rep("(list '(1 2) 3)");
        output2 = REPL.rep("(cons (cons 1 (cons 2 '())) (cons 3 '()))");
        assertEquals(output, output2);

        output = REPL.rep("(list '(1 2) 3)");
        output2 = REPL.rep("(cons  '(1 2) 3)");
        assertEquals(output, output2);

        output = REPL.rep("(list '(1 2) '(3 4))");
        output2 = REPL.rep("(cons (cons 1 (cons 2 '())) (cons (cons 3 (cons 4 '())) '()))");
        assertEquals(output, output2);

        output = REPL.rep("(list '(1 2) '(3 4))");
        output2 = REPL.rep("(cons '(1 2) (cons '(3 4) '()))");
        assertEquals(output, output2);

        output = REPL.rep("(list)");
        assertEquals("()", output);

        output = REPL.rep("(list ())");
        assertEquals("(())", output);

        output = REPL.rep("(list '())");
        assertEquals("(())", output);

        output = REPL.rep("(car '('(a b) c))");
        assertEquals("'(a b)", output);

        //car and cdr on list of size 0
        assertThrows(SyntaxError.class, () -> {
            REPL.rep("(car '())");//uses first()
        });
        assertThrows(SyntaxError.class, () -> {
            REPL.rep("(cdr '())");//uses rest()
        });
        //car
        assertNull(new ListType().first());
        assertNotNull(new ListType(new IntegerType(1)).first());
        assertEquals("1", new ListType(new IntegerType(1)).first().toString());
        assertNotNull(new ListType(new IntegerType(1), new SymbolType("sup")).first());
        //cdr
        assertNotNull(new ListType().rest());
        assert (new ListType().rest().size() == 0);
        assertNotNull(new ListType(new IntegerType(1)).rest());
        assertEquals("()", new ListType(new IntegerType(1)).rest().toString());
        assertNotNull(new ListType(new IntegerType(1), new SymbolType("sup")).rest());
        assertEquals("(sup)", new ListType(new IntegerType(1), new SymbolType("sup")).rest().toString());

        //car and cdr on list of size 1
        output = REPL.rep("(car '(1))");
        assertEquals("1", output);

        output = REPL.rep("(cdr '(1))");
        assertEquals("()", output);

        //car and cdr on list of size 2
        output = REPL.rep("(car '(1 2))");
        assertEquals("1", output);

        output = REPL.rep("(cdr '(1 2))");
        assertEquals("(2)", output);

        assertThrows(SyntaxError.class, () -> {
            REPL.rep("(car 1)");//uses first()
        });
        assertThrows(SyntaxError.class, () -> {
            REPL.rep("(cdr 2)");//uses rest()
        });
        //todo cond and some empty list/nil/false test

    }

    @Test
    public void LambdaTests() throws REPLErrors, ReaderErrors {
        String output;

        output = REPL.rep("((lambda (f) (f '(b c))) (lambda (x) (cons 'a x))))");//unquoted args cus lambda is special form
        System.out.println("lambda test: " + output);
        assertEquals("(a b c)", output);


    }

    @Test
    public void beginTest() throws REPLErrors, ReaderErrors {
        String in = """
                (begin 2 4 'a (+ 2 4))
                """;
        String result = REPL.rep(in);
        assertEquals("6", result);

        in = """
                (begin (def! a 2) 4 a (+ a 4))
                """;
        result = REPL.rep(in);
        assertEquals("6", result);

        in = "(begin)";
        result = REPL.rep(in);
        assertEquals(REPL.print(Nil), result);

    }

    @Test
    public void setTest() throws REPLErrors, ReaderErrors {
        assertThrows(SyntaxError.class, () -> REPL.rep("(set! a 1 b 2 c 3)"));
        assertThrows(SyntaxError.class, () -> REPL.rep("(set! cond 2)"));
        assertThrows(SyntaxError.class, () -> REPL.rep("(set! lambda 2)"));
        REPL.rep("(def! a 0)");
        REPL.rep("(def! b 0)");
        REPL.rep("(def! c 0)");
        String in = """
                (set! a 1 b 2 c 3)
                    """;
        String result = REPL.rep(in);
        assertEquals("3", result);

        in = "a";
        result = REPL.rep(in);
        assertEquals("1", result);

        in = "b";
        result = REPL.rep(in);
        assertEquals("2", result);

        in = "c";
        result = REPL.rep(in);
        assertEquals("3", result);

        in = "(set! a (+ 1 b) b (+ 1 a) c (+ a b))";
        result = REPL.rep(in);
        assertEquals("7", result);

        in = "a";
        result = REPL.rep(in);
        assertEquals("3", result);

        in = "b";
        result = REPL.rep(in);
        assertEquals("4", result);

        in = "c";
        result = REPL.rep(in);
        assertEquals("7", result);

        REPL.rep("(def! x 10)");
        in = """
                (let* (x 2)
                    (begin
                        (set! x 5)
                        x))
                    """;
        result = REPL.rep(in);
        assertEquals("5", result);
    }

    @Test
    public void defTest() throws REPLErrors, ReaderErrors {
        assertThrows(SyntaxError.class, () -> REPL.rep("(def! cond 2)"));
        assertThrows(SyntaxError.class, () -> REPL.rep("(def! lambda 2)"));
        assertThrows(SyntaxError.class, () -> REPL.rep("(def! true 2)"));
        assertThrows(SyntaxError.class, () -> REPL.rep("(def! nil 2)"));

        String in = """
                (def! a 4 b (+ 2 4))
                """;
        assertEquals("6", REPL.rep(in));
        assertEquals("4", REPL.rep("a"));
        assertEquals("6", REPL.rep("b"));


        assertEquals("0", REPL.rep("(set! a 0)"));
        assertEquals("0", REPL.rep("a"));
        assertEquals("6", REPL.rep("b"));


    }

    @Test
    public void scopeTest() throws REPLErrors, ReaderErrors {
        //todo
        String in = """
                (let* (x 2)
                        (begin
                            (def! x 5)
                            x))
                """;
        REPL.rep("(def! x 12)");
        assertEquals("2", REPL.rep(in));

        in = """
                (defmacro decf (place)
                    (list 'set! place (list '- place 1))
                    )
                """;
        REPL.rep(in);

        in = """
                (defmacro incf (place)
                    (list 'set! place (list '+ place 1))
                    )
                """;
        REPL.rep(in);

        in = """
                (def! *op-list*
                    (let* ((count 0))
                        (list   (lambda () (incf count))
                                (lambda () (decf count))
                                (lambda () (set! count 0)))))
                """;
        REPL.rep(in);
        assertEquals("1", REPL.rep("((car *op-list*))"));
        assertEquals("2", REPL.rep("((car *op-list*))"));
        assertEquals("1", REPL.rep("((car (cdr *op-list*)))"));
        assertEquals("0", REPL.rep("((car (cdr (cdr  *op-list*))))"));

    }


}