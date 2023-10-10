package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static main.REPLErrors.ApplyError;
import static main.REPLErrors.SyntaxError;
import static main.ReaderErrors.*;

/**
 * A JAR file of the project can be found in \out\artifacts\LispInterpreter_jar along with
 * the file containing the code for Grahams eval. function (as a .txt and .lisp file).
 * To start the REPL start the JAR with
 * <p>
 * java -jar LispInterpreter.jar optional_arg
 * <p>
 * With the optional_arg argument a file can be passed to be evaluated before the REPL accepts user input.
 * For example if we run
 * <p>
 * java -jar LispInterpreter.jar interpreter.txt
 * <p>
 * Grahams eval. functions is in the global environment, so we can type
 * <p>
 * (eval. '(eq 'a 'a) '())
 * <p>
 * into the terminal which evaluates to #true using the eval. which is implemented in the interpreters lisp syntax.
 * Alternatively if we were to type
 * <p>
 * (eval '(eq 'a 'a))
 * <p>
 * into the terminal, it also evaluates to #true but using the interpreter directly.
 * Files can either be passed as the optional_arg when executing the JAR or the source code (from an IDE for example)
 * or they can be loaded at any point during the REP-Loop. This is done using the core function "load" which expects a single string
 * as an argument containing a valid filepath
 * (load "interpreter.lisp")
 */
public class Main {
    static Scanner scanner = new Scanner(System.in);

    /**
     * Reads input from cmd line and passes it to the REPL. Supports multi line input by appending input lines until a
     * valid expression is found.
     */
    public static void parseUserInput() {
        String input = "", output;
        boolean addLine = false;
        scanner.useDelimiter("\\n");
        while (true) {
            try {
                if (addLine) {//multiline keyboard input
                    System.out.print("... ");
                    input += scanner.nextLine();
                    addLine = false;
                } else {
                    System.out.print("user> ");
                    input = scanner.nextLine();
                }

                output = REPL.rep(input);
                System.out.println("input>" + input);
                System.out.println("    output> " + output);
            } catch (ParenMismatchError | EmptyLineError e) {
                //missing a closing parenthesis or empty line, append the next line from reader to current input and try parsing again
                addLine = true;
                input = Reader.commentFreeInput;//removes comments from end of line, allowing to read multiple lines with comments ending some or all of them
            } catch (CommentError ignored) {
                //this way we can ignore full lines of comments
            } catch (SyntaxError | ApplyError | ParseError customError) {
                System.out.println(customError.getMessage());
            } catch (REPLErrors | ReaderErrors replErrors) {
                replErrors.printStackTrace();
            }
        }
    }

    /**
     * Attempts to read and evaluate the contents of the file line by line. Supports multi line input. If a line doesn't
     * contain a valid expression then the next line is added and the resulting string is evaluated again. This repeats
     * until a valid expression is found or EOF is reached.
     *
     * @param filename path of file to read from.
     */
    public static void parseFile(String filename) {
        String input = "", output;
        boolean addLine = false;
        try {
            scanner = new Scanner(new File(filename));
        } catch (FileNotFoundException e) {
            System.out.println("could not find the file: " + filename);
            return;
        }
        System.out.println(">parsing file " + filename + "...");
        scanner.useDelimiter("\\n");
        while (scanner.hasNext()) {
            if (addLine) {
                input += scanner.nextLine();
                addLine = false;
            } else
                input = scanner.nextLine();
            try {
                output = REPL.rep(input);

                System.out.println("input>" + Reader.commentFreeInput.stripTrailing());
                System.out.println("    output> " + output.stripTrailing());

            } catch (ParenMismatchError | EmptyLineError e) {
                //missing a closing parenthesis or empty line, append the next line from reader to current input and try parsing again
                addLine = true;
                input = Reader.commentFreeInput;//Remove comments from end of line, allowing to read multiple lines with comments ending some or all of them
            } catch (CommentError ignored) {
                //this way we can ignore full lines of comments in the code
            } catch (SyntaxError | ApplyError | ParseError myError) {
                System.out.println("Error with input: " + input);
                System.out.println(myError.getMessage());
            } catch (REPLErrors | ReaderErrors replErrors) {
                replErrors.printStackTrace();
            }
        }//Reached EOF
        System.out.println("Finished parsing file.");
    }

    /**
     * Starts the REPL, can take a filepath as argument and tries to evaluate the contents. Afterwards continually reads
     * user input from the cmd line.
     *
     * @param args path to a file containing lisp code.
     */
    public static void main(String[] args) {
        Core.initMacros();
        if (args.length > 0)
            parseFile(args[0]);
        scanner = new Scanner(System.in);
        parseUserInput();
    }
}