package main;

/**
 * Errors that occur during the REPL, mainly during the eval and apply stages
 */
public class REPLErrors extends Throwable {
    public REPLErrors(String message) {
        super(message);
    }

    /**
     * Thrown whenever the given syntax for an expression is wrong i.e. when the input of the user doesnt conform
     * to the syntax of this LISP.
     */
    public static class SyntaxError extends REPLErrors {
        public SyntaxError(String message) {
            super(message);
        }
    }

    /**
     * Thrown by eval if something that is not a function is found during a function call i.e. the first list element didnt evaluate to a function
     */
    public static class ApplyError extends REPLErrors {
        public ApplyError(String message) {
            super(message);
        }
    }

    public static class TestError extends REPLErrors {
        public TestError(String message) {
            super(message);
        }
    }

}
