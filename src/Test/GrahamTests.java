package Test;

import main.REPL;
import main.REPLErrors;
import main.ReaderErrors;
import main.Types;
import org.junit.Test;

import static org.junit.Assert.*;

public class GrahamTests {
    @Test
    public void init() {
        nullTest();
        andTest();
        notTest();
        appendTest();
        pairTest();
        cxrTest();
        assocTest();
        evalTest();
    }

    public void nullTest() {
        String output;
        String input;
        try {
            //init
            input = "(defun null. (x)\n" +
                    "    (eq x '()))";
            output = REPL.rep(input);
            assertEquals("null.", output);
            //test
            input = "(null. 'a)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(null. '())";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(null. 1)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(null. \"a\")";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(null. 't)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(null. 'false)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(null. false)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(null. nil)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(null. true)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(null. ())";
            output = REPL.rep(input);
            assertEquals(output, "#true");

        } catch (REPLErrors | ReaderErrors emptyLineError) {
            emptyLineError.printStackTrace();
        }
    }

    public void andTest() {
        String output;
        String input;
        try {

//            returns #true if both its arguments do and #false otherwise
            input = """
                    (defun and. (x y)
                        (cond   (x  (cond (y  true)
                                    (true  false)))
                                (true false)));default""";
            output = REPL.rep(input);
            assertEquals("and.", output);


            input = "(and. (atom 'a) (eq 'a 'b))";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(and. (atom 'a) (eq 'a 'a))";
            output = REPL.rep(input);
            assertEquals("#true", output);


            input = "(and. 't '())"; //anything other than false, 'false, nil or 'nil evaluates to #true
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(and. 'false '())";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(and. false '())";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(and. 'alse '())";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(and. true false)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(and. false false)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(and. true true)";
            output = REPL.rep(input);
            assertEquals("#true", output);

        } catch (REPLErrors | ReaderErrors emptyLineError) {
            emptyLineError.printStackTrace();
        }
    }

    public void notTest() {
        String output;
        String input;
        try {
            //init
            input = "(defun not. (x)" +
                    "   (cond (x false)" +
                    "   ('else true)))";
            output = REPL.rep(input);
            assertEquals("not.", output);
            assertTrue(REPL.re(input) instanceof Types.MyFunction);

            input = "(not. (eq 'a 'a))";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(not. (eq 'a 'b))";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. true)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(not. 'true)";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(not. false)";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. 'false)";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. nil)";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. (not. nil))";
            output = REPL.rep(input);
            assertEquals("#false", output);

            input = "(not. (not. true))";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. (not. false))";
            output = REPL.rep(input);
            assertEquals("#false", output);
            //anything thats not explicitly #false or #nil evaluates as true
            input = "(not. (not. '(a b c)))";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. (not. 12))";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. (not. '()))";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. (not. ()))";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. (not. '(nil)))";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(not. (not. \"HI\"))";
            output = REPL.rep(input);
            assertEquals("#true", output);

        } catch (REPLErrors | ReaderErrors emptyLineError) {
            emptyLineError.printStackTrace();
            fail();
        }
    }

    public void appendTest() {
        String output;
        String input;
        try {
            //init
            input = """
                    (defun append. (x y)
                      (cond   ((null. x) y)
                              ('else (cons (car x) (append. (cdr x) y)))))""";
            output = REPL.rep(input);
            assertEquals("append.", output);

            input = " (append. '(a b) '(c d))";
            output = REPL.rep(input);
            assertEquals("(a b c d)", output);

            input = "(append. '() '(c d))";
            output = REPL.rep(input);
            assertEquals("(c d)", output);

            input = "(append. '((a) b) '(c d))";
            output = REPL.rep(input);
            assertEquals("((a) b c d)", output);


        } catch (REPLErrors | ReaderErrors emptyLineError) {
            emptyLineError.printStackTrace();
            fail();
        }
    }

    public void pairTest() {
        String output;
        String input;
        try {
            //takes two lists of the same length and returns a list of two-element lists
            // containing successive pairs of an element from each
            //init
            input = "(defun pair. (x y)\n" +
                    "    (cond   ((and. (null. x) (null. y)) '())\n" +
                    "            ((and. (not. (atom x)) (not. (atom y)))\n" +
                    "                (cons   (list (car x) (car y))\n" +
                    "                        (pair. (cdr x) (cdr y))))))";
            output = REPL.rep(input);
            assertEquals("pair.", output);

            input = "(pair. '(a b c) '(x y z))";
            output = REPL.rep(input);
            assertEquals("((a x) (b y) (c z))", output);

            input = "(pair. '(a b) '(x y z))";
            output = REPL.rep(input);
            assertNotEquals("((a x) (b y))", output);

            input = "(pair. '(a b c) '(x y))";
            output = REPL.rep(input);
            assertNotEquals("((a x) (b y))", output);

            input = "(pair. '() '())";
            output = REPL.rep(input);
            assertEquals("()", output);


        } catch (REPLErrors | ReaderErrors emptyLineError) {
            emptyLineError.printStackTrace();
            fail();
        }
    }

    public void cxrTest() {
        String output;
        String input;
        try {
            //init
            input = "(defun cadar (x)\n" +
                    "    (car (cdr (car x))))";//second item in the first list
            output = REPL.rep(input);
            assertEquals("cadar", output);

            input = "(cadar '((a x) (b y) (c z)))";
            output = REPL.rep(input);
            assertEquals("x", output);

            input = "(cadar '((1 2)) )";
            output = REPL.rep(input);
            assertEquals("2", output);

            //init
            input = "(defun caar (x)\n" +
                    "    (car (car x)))\n"; //first item in the first list
            output = REPL.rep(input);
            assertEquals("caar", output);

            input = "(caar '((a x) (b y) (c z)))";
            output = REPL.rep(input);
            assertEquals("a", output);

            input = "(caar '((1 2)) )";
            output = REPL.rep(input);
            assertEquals("1", output);

            //init
            input = "(defun cadr (x)\n" +
                    "            (car (cdr x)))"; //second list
            output = REPL.rep(input);
            assertEquals("cadr", output);

            input = "(cadr '((a x) (b y) (c z)))";
            output = REPL.rep(input);
            assertEquals("(b y)", output);

            //init
            input = "(defun caddr (x)\n" +
                    "    (car (cdr (cdr x))))"; //the 3rd list
            output = REPL.rep(input);
            assertEquals("caddr", output);


            input = "(caddr '((a x) (b y) (c z)))";
            output = REPL.rep(input);
            assertEquals("(c z)", output);

            //init
            input = "(defun caddar (x)\n" +
                    "    (car (cdr (cdr (car x)))))"; //3rd item in 1st list
            output = REPL.rep(input);
            assertEquals("caddar", output);

            input = "(caddar '((a x d) (b y) (c z)))";
            output = REPL.rep(input);
            assertEquals("d", output);


        } catch (REPLErrors | ReaderErrors emptyLineError) {
            emptyLineError.printStackTrace();
            fail();
        }
    }

    public void assocTest() {
        String output;
        String input;
        try {
            //takes an atom x and a list y of the form created by pair.
            //returns the 2nd element of 1st list in y whose 1st element is x
            //i.e. lookup symbol (x) in env (y)
            //init
            input = "(defun assoc. (x y)\n" +
                    "    (cond   ((eq (caar y) x) (cadar y))\n" +
                    "            ('default (assoc. x (cdr y)))))";
            output = REPL.rep(input);
            assertEquals("assoc.", output);

            input = "(assoc. 'x '((x a) (y b)))";
            output = REPL.rep(input);
            assertEquals("a", output);

            input = "(assoc. 'x '((x new) (y b)))";
            output = REPL.rep(input);
            assertEquals("new", output);

        } catch (REPLErrors | ReaderErrors emptyLineError) {
            emptyLineError.printStackTrace();
            fail();
        }
    }

    public void evalTest() {
        String output;
        String input;
        try {
            //init
            input = "(defun evcon. (c a)\n" +
                    "    (cond   ((eval. (caar c) a)\n" +
                    "             (eval. (cadar c) a))\n" +
                    "            ('default (evcon. (cdr c) a))))";
            output = REPL.rep(input);
            assertEquals("evcon.", output);
            //init
            input = "(defun evlis. (m a)\n" +
                    "    (cond   ((null. m) '())\n" +
                    "            ('default (cons (eval. (car m) a)\n" +
                    "                            (evlis. (cdr m) a)))))";
            output = REPL.rep(input);
            assertEquals("evlis.", output);
            //init
            input = """
                    (defun eval. (e a)
                        (cond
                            ((atom e) (assoc. e a))
                            ((atom (car e)) (cond
                                ((eq (car e) 'quote)    (cadr e))
                                ((eq (car e) 'atom)     (atom   (eval. (cadr e) a)))
                                ((eq (car e) 'eq)       (eq     (eval. (cadr e) a)
                                                                (eval. (caddr e) a)))
                                ((eq (car e) 'car)      (car    (eval. (cadr e) a)))
                                ((eq (car e) 'cdr)      (cdr    (eval. (cadr e) a)))
                                ((eq (car e) 'cons)     (cons   (eval. (cadr e) a)
                                                                (eval. (caddr e) a)))
                                ((eq (car e) 'cond)     (evcon. (cdr e) a))
                                ('t (eval. (cons    (assoc. (car e) a)
                                                    (cdr e))
                                            a))))
                                        
                            ((eq (caar e) 'label)   (eval.  (cons (caddar e) (cdr e))
                                                            (cons (list (cadar e) (car e)) a)))
                            ((eq (caar e) 'lambda)  (eval.  (caddar e)
                                                            (append. (pair. (cadar e) (evlis. (cdr e) a)) a)))))
                    """;
            output = REPL.rep(input);
            assertEquals("eval.", output);


            input = "(eval. ''1 '((1 2)))";
            output = REPL.rep(input);
            assertEquals("1", output);

            input = "(eval. '1 '((1 2)))";
            output = REPL.rep(input);
            assertEquals("2", output);

            input = "(eval. 1 '((1 2)))";
            output = REPL.rep(input);
            assertEquals("2", output);

            input = "(eval. '(eq 'a 'a) '())";
            output = REPL.rep(input);
            assertEquals("#true", output);

            input = "(eval. 'x '((x a) (y b)))";
            output = REPL.rep(input);
            assertEquals("a", output);

            input = "(eval. '(cons x '(b c))\n" +
                    "        '((x a) (y b)))";
            output = REPL.rep(input);
            assertEquals("(a b c)", output);

            input = "(eval. '(cond ((atom x) 'atom)\n" +
                    "                ('default. 'list))\n" +
                    "        '((x '(a b))))";
            output = REPL.rep(input);
            assertEquals("list", output);

            input = "(eval. '(cond ((atom x) 'nil)\n" +
                    "                ('default. 'list))\n" +
                    "        '((x 2)))";
            output = REPL.rep(input);
            assertEquals("#nil", output);

            input = "(eval. '(cond ((atom x) 'nil)\n" +
                    "                ('default. 'list))\n" +
                    "        '((x 2)))";
            output = REPL.rep(input);
            assertEquals("#nil", output);

            input = "(eval. '((lambda (x) (cons 'a x)) '(b c))\n" +
                    "        '())";
            output = REPL.rep(input);
            assertEquals("(a b c)", output);

            input = "(eval. '((lambda (x) (cons 'a x)) '(b c))\n" +
                    "        '((f (lambda (x) (cons 'a x)))))";
            output = REPL.rep(input);
            assertEquals("(a b c)", output);

            input = "(eval. '(f '(b c))\n" +
                    "        '((f (lambda (x) (cons 'a x)))))";
            output = REPL.rep(input);
            assertEquals("(a b c)", output);

        } catch (REPLErrors | ReaderErrors emptyLineError) {
            emptyLineError.printStackTrace();
            fail();
        }
    }
}