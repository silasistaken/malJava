package main;

/**
 * Errors thrown during parsing of the input string by the reader.
 */
public class ReaderErrors extends Throwable {
    public ReaderErrors(String message) {
        super(message);
    }


    /**
     * Thrown to signal let the main loop to continue reading lines as the currently read input is not a complete expression
     * i.e. it doesnt contain enough closing brackets.
     */
    public static class ParseError extends ReaderErrors {
        public ParseError(String message) {
            super(message);
        }
    }

    /**
     * Thrown when a token is either null or not recognized
     */
    public static class ParenMismatchError extends REPLErrors {

        public ParenMismatchError(String message) {
            super(message);
        }

    }

    /**
     * Thrown when a comment token (;) is encountered at the start of a line. Tells the main loop to ignore this line completely.
     */
    public static class CommentError extends REPLErrors {
        public CommentError(String message) {
            super(message);
        }
    }

    /**
     * Thrown when an empty line is encountered. Tells the main loop to ignore this line completely.
     */
    public static class EmptyLineError extends REPLErrors {
        public EmptyLineError(String message) {
            super(message);
        }
    }
}
