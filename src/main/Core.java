package main;

import main.REPLErrors.SyntaxError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import static main.Types.*;

public class Core {
    private static final HashMap<String, MyDataType> env = new HashMap<>();
    public static List<String> coreNames;

    /**
     * Populates the global environment with the basic arithmetic operators, some primitive operators mentioned in
     * Grahams "Root of Lisp" as well as utility functions.
     *
     * @return The global environment.
     */
    public static Environment getNamespace() {
        initArithmetics();
        initPrimitives();
        initUtilities();
        coreNames = new ArrayList<>();
        coreNames = List.of("debug", "*", "+", "parse", "macroexpand", "concat", "eq", "list", "-", "/", "cdr", "print",
                "eval", "load", "car", "atom", "cons");
        return new Environment(env);
    }

    /**
     * Defines macros when starting the interpreter. Can be expanded with more macros at will. Has to be called from main to avoid problems with loading of static classes.
     */
    public static void initMacros() {
        ArrayList<String> macroList = new ArrayList<>();

        macroList.add("""
                (defmacro incf (place)
                    `(set! ,place (+ ,place 1))
                )
                """);

        macroList.add("""
                (defmacro decf (place)
                    `(set! ,place (- ,place 1))
                )
                """);

        macroList.add("""
                (defmacro if (test t-clause else)
                    `(cond (,test ,t-clause) ('default ,else))
                )
                """);
        for (String macro : macroList) {
            try {
                if (!macro.isEmpty())
                    REPL.rep(macro);
            } catch (REPLErrors | ReaderErrors replErrors) {
                replErrors.printStackTrace();
            }
        }
    }

    /**
     * Functions used for grahams eval implementations
     */
    private static void initPrimitives() {
        //cons takes 2 arguments a1 and a2 (a2 can be either a list or a self-eval type e.g. int, symbol)
        //returns a list consisting of a1 and the contents of a2
        env.put("cons", new MyFunction("cons") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() != 2)
                    throw new SyntaxError("Cons expects 2 arguments but got: " + args.size());
                var a1 = args.get(0);
                var a2 = args.get(1);
                ArrayList<MyDataType> l = new ArrayList<>();
                l.add(a1);
                if (a2.isList() && ((ListType) a2).size() != 0)//non-empty list
                    l.addAll(((ListType) a2).getValue());
                else if (!a2.isList())
                    l.add(a2);
                return new ListType(l);
            }
        }.setIs_core(true));

        //(car x) expects a non-empty list x
        //returns the first element of x
        env.put("car", new MyFunction("car") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() == 1 && args.get(0) instanceof ListType x)
                    if (x.size() != 0)//x is non-empty list
                        return x.first();
                throw new SyntaxError("Car expects 1 non-empty list argument, but got: " + args);
            }
        }.setIs_core(true));
        //(cdr x) expects a non-empty list x
        //returns a new list containing all but the 1st element of x
        env.put("cdr", new MyFunction("cdr") {
            @Override
            public ListType apply(ListType args) throws SyntaxError {
                if (args.size() == 1 && args.get(0) instanceof ListType x) {
                    if (x.size() != 0)//x is non-empty list
                        return x.rest();
                }
                throw new SyntaxError("cdr expects a non-empty list but got:" + args.get(0));
            }
        }.setIs_core(true));
        //(eq x y)
        //returns #true if the values of x and y are the same atom or both the empty list, and #false otherwise
        env.put("eq", new MyFunction("eq") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() != 2)
                    throw new SyntaxError("eq expects 2 arguments but got: " + args.size() + " " + args);
                return args.get(0).equals(args.get(1)) ? True : False;
            }
        }.setIs_core(true));
        //(atom x)
        //returns #false if the value of x is a non-empty list, #true otherwise (x is of atomic type)
        env.put("atom", new MyFunction("atom") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() != 1)
                    throw new SyntaxError("atom expects 1 argument but got: " + args.size() + " " + args);
                var a1 = args.get(0);
                if (!a1.isList() || ((ListType) a1).size() == 0)
                    return True;//Symbol, int, empty list or constant(true false nil)
                else
                    return False;

            }
        }.setIs_core(true));

        //(list a1 ... an)
        //returns a list containing the specified elements, can be empty
        env.put("list", new MyFunction("list") {
            @Override
            public MyDataType apply(ListType args) {
                return new ListType(args.getValue());
            }
        }.setIs_core(true));
    }

    /**
     * basic arithmetic functions on integers (+,-,*,/)
     */
    private static void initArithmetics() {
        //plus
        env.put("+", new MyFunction("+") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() != 2)
                    throw new SyntaxError("+ expects 2 args but got " + args.size());
                MyDataType a1, a2;
                a1 = args.get(0);
                a2 = args.get(1);
                if (a1 instanceof IntegerType && a2 instanceof IntegerType)
                    return new IntegerType(((IntegerType) a1).value + ((IntegerType) a2).value);
                else throw new SyntaxError("+ expects integers, but got: " + a1 + " and " + a2);
            }
        }.setIs_core(true));

        //minus
        env.put("-", new MyFunction("-") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() != 2)
                    throw new SyntaxError("Expected 2 args but got " + args.size());
                MyDataType a1, a2;
                a1 = args.get(0);
                a2 = args.get(1);
                if (a1 instanceof IntegerType && a2 instanceof IntegerType)
                    return new IntegerType(((IntegerType) a1).value - ((IntegerType) a2).value);
                else throw new SyntaxError("- expects integers, but got: " + a1 + " and " + a2);

            }
        }.setIs_core(true));

        //multiply
        env.put("*", new MyFunction("*") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() != 2)
                    throw new SyntaxError("Expected 2 args but got " + args.size());
                MyDataType a1, a2;
                a1 = args.get(0);
                a2 = args.get(1);
                if (a1 instanceof IntegerType && a2 instanceof IntegerType)
                    return new IntegerType(((IntegerType) a1).value * ((IntegerType) a2).value);
                else throw new SyntaxError("* expects integers, but got: " + a1 + " and " + a2);
            }
        }.setIs_core(true));

        //division
        env.put("/", new MyFunction("/") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() != 2)
                    throw new SyntaxError("Expected 2 args but got " + args.size());
                MyDataType a1, a2;
                a1 = args.get(0);
                a2 = args.get(1);
                if (a1 instanceof IntegerType && a2 instanceof IntegerType a2i) {
                    if (a2i.getValue() == 0)
                        throw new SyntaxError("division by 0");
                    return new IntegerType(((IntegerType) a1).value / ((IntegerType) a2).value);
                } else throw new SyntaxError("/ expects integers, but got: " + a1 + " and " + a2);
            }
        }.setIs_core(true));
    }

    /**
     * Utility functions
     */
    private static void initUtilities() {
        //parse
        //returns the parsed S-expression from  a string
        env.put("parse", new MyFunction("parse") {
            @Override
            public MyDataType apply(ListType args) throws REPLErrors, ReaderErrors {
                if (args.size() != 1)
                    throw new SyntaxError("Expected 1 arg but got " + args.size());
                MyDataType exp = args.get(0);
                if (exp instanceof StringType)
                    return REPL.read(((StringType) exp).value);
                else throw new SyntaxError("parse expects a string to parse");
            }
        }.setIs_core(true));

        //returns the evaluated S-expression
        env.put("eval", new MyFunction("eval") {
            @Override
            public MyDataType apply(ListType args) throws REPLErrors, ReaderErrors {
                if (args.size() != 1)
                    throw new SyntaxError("Expected 1 arg but got " + args.size());
                MyDataType exp = args.get(0);
                return REPL.eval(exp, REPL.globalEnv);
            }
        }.setIs_core(true));

        //loads a file (absolute or relative path passed as a StingType) and evaluates the content, returns Nil
        env.put("load", new MyFunction("load") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() != 1)
                    throw new SyntaxError("Expected 1 arg but got " + args.size());
                MyDataType exp = args.get(0);
                if ((exp instanceof StringType)) {
                    String path = ((StringType) exp).value;
                    Main.parseFile(path);
                    Main.scanner = new Scanner(System.in);
                    return Nil;
                } else
                    throw new SyntaxError("load expects a string with a filepath, but got: " + exp);
            }
        }.setIs_core(true));
        //expects 1 expression and prints information about it
        env.put("debug", new MyFunction("debug") {
            @Override
            public MyDataType apply(ListType args) throws REPLErrors, ReaderErrors {
                if (args.size() != 1) throw new SyntaxError("Expected 1 arg but got " + args.size());
                MyDataType exp = args.get(0);
                String info = debug(exp);
                return new StringType("\n" + info);
            }
        }.setIs_core(true));

        //expands macro with the given args, in global env
        env.put("macroexpand", new MyFunction("macroexpand") {
            @Override
            public MyDataType apply(ListType args) throws REPLErrors, ReaderErrors {
                if (args.size() != 1) throw new SyntaxError("Expected 1 arg but got " + args.size());
                return REPL.macroexpand(args.get(0), REPL.globalEnv);
            }
        }.setIs_core(true));

        //prints the value of a given expression, gets printed before the main loops print of input/output
        env.put("print", new MyFunction("print") {
            @Override
            public MyDataType apply(ListType args) throws REPLErrors {
                if (args.size() != 1) throw new SyntaxError("Expected 1 arg but got " + args.size());
                System.out.println("print: " + REPL.print(args.get(0)));
                return Nil;
            }
        }.setIs_core(true));

        //concatenates 0 or more lists aka append
        env.put("concat", new MyFunction("concat") {
            @Override
            public MyDataType apply(ListType args) throws SyntaxError {
                if (args.size() == 0)
                    return new ListType();
                else {
                    ArrayList<MyDataType> result = new ArrayList<>();
                    for (MyDataType list : args.getValue()) {
                        if (!list.isList())
                            throw new SyntaxError("Error: concat only takes lists as arguments");
                        else
                            result.addAll(((ListType) list).getValue());
                    }
                    return new ListType(result);
                }
            }
        }.setIs_core(true));

    }

    /**
     * Prints various bits of information collected during evaluation of an expression. work in progress.
     * @param exp
     * @param indent
     * @return
     * @throws REPLErrors
     * @throws ReaderErrors
     */
    private static String debug(MyDataType exp, int indent) throws REPLErrors, ReaderErrors {

        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("\t".repeat(indent++)).append("DEBUG INFO ABOUT ").append(exp.toString()).append("\n");
        //basic info about a function/macro
        if (exp instanceof MyFunction function) {
            debugInfo.append("\t".repeat(indent)).append("Java Class: ").append(function.getClass()).append("\n");
            if (function.isCore()) {
                debugInfo.append("\t".repeat(indent)).append("Is a core function").append("\n");
            }
            if (function.isMacro()) {
                debugInfo.append("\t".repeat(indent)).append("Is a MACRO with name: ").append(function.getValue()).append("\n");
            }//same for user defined functions and macros
            debugInfo.append("\t".repeat(indent)).append("Internal Parameters: ").append(((MyFunction) exp).getParameters()).append("\n");
            debugInfo.append("\t".repeat(indent)).append("Body: ").append(((MyFunction) exp).getBody()).append("\n");
        } else if (exp instanceof ListType list) {
            //basic info about the list and its elements along with more detailed info about function/macro calls
            debugInfo.append("\t".repeat(indent)).append("Is a List containing the values: ").append(list.getValue()).append("\n");
            debugInfo.append("\t".repeat(indent)).append("Size: ").append(list.size()).append("\n");

            if (list.get(0) instanceof SymbolType a0) {//first element is a symbol
                boolean isSpecial = Pattern.matches("(def!|let\\*|cond|quote|lambda|defun|defmacro)", a0.getValue());
                //case 1: special Form
                if (isSpecial) {
                    //defer until needed
                    if (a0.getValue().equals("quote")) {
                        debugInfo.setLength(0);
                        debugInfo.append("\t".repeat(indent++)).append("DEBUG INFO ABOUT ").append(exp).append("\n");
                        debugInfo.append("\t".repeat(indent)).append("Is a List quoting the exp: ").append(list.get(1)).append("\n");
                        return debugInfo.toString();
                    }
                    debugInfo.append("\t".repeat(indent)).append("List is a special form" + "\n");
                    return debugInfo.toString();//no further debugging of special forms
                    //case 2: function call
                } else {
                    //check what it evals to
                    MyDataType a0e = REPL.evalAST(a0, REPL.globalEnv);
                    if (a0e instanceof MyFunction fun) {
                        //case 1: evals to a function/macro
                        if (fun.isMacro()) {
                            if (list.size() == 1)
                                debugInfo.append("\t".repeat(indent)).append("List is a single Macro" + "\n");
                            else {
                                debugInfo.append("\t".repeat(indent)).append("List is a Macro call" + "\n");
                                debugInfo.append("\t".repeat(indent)).append("Expanding Macro..." + "\n");
                                debugInfo.append(expandDebug(exp, indent + 1) + "\n");
                            }
                            return debugInfo.toString();
                        } else {
                            debugInfo.append("\t".repeat(indent)).append("List is a function call" + "\n");
                            //todo add info about function calls
                        }

                    } else if (a0e instanceof SymbolType) {
                        //case 2: evals to another symbol
                        debugInfo.append("\t".repeat(indent)).append(a0 + " has function call form but a0 evals to symbol " + a0e).append("\n");
                    } else {
                        //case 3: evals to int, string, const or list (thats some wierd case where def was used to save a list)
                        debugInfo.append("\t".repeat(indent)).append(a0 + " has function call form but a0 evals to " + a0e).append("\n");

                    }
                }
            }
            //non macro call list
            debugInfo.append("\t".repeat(indent)).append("Elements:").append("\n");
            for (int i = 0; i < list.size(); i++) {
                debugInfo.append("\t".repeat(indent)).append(i).append(") ");
                debugInfo.append(debug(list.get(i), indent + 1).stripLeading());
            }
        } else if (exp instanceof SymbolType)
            debugInfo.append("\t".repeat(indent)).append("Is a symbol with value: ").append(exp.getValue()).append("\n");
        else if (exp instanceof IntegerType)
            debugInfo.append("\t".repeat(indent)).append("Is an integer with value: ").append(exp.getValue()).append("\n");
        else if (exp instanceof StringType)
            debugInfo.append("\t".repeat(indent)).append("Is a string with value: ").append(exp.getValue()).append("\n");
        else if (exp instanceof ConstType)
            debugInfo.append("\t".repeat(indent)).append("Is a constant with value: ").append(exp.getValue()).append("\n");
        return debugInfo.toString();
    }

    private static String expandDebug(MyDataType function, int indent) throws REPLErrors, ReaderErrors {

        MyDataType expandedAST = function;
        int step = 1;
        StringBuilder info = new StringBuilder();
        info.append("\t".repeat(indent)).append("Expanding macro " + function.getValue() + "\n");

        while (REPL.is_macro_call(expandedAST, REPL.globalEnv)) {
            info.append("\t".repeat(indent)).append("Step  " + step + "\n");

            MyFunction macro = (MyFunction) env.get(((ListType) expandedAST).get(0).toString());
            expandedAST = macro.apply(((ListType) expandedAST).rest());
            step++;
            info.append("\t".repeat(indent + 1)).append("resulting AST --> " + expandedAST + "\n");

        }
        if (step == 0)
            info.append("\t".repeat(indent)).append("Not Expanded since its not a macro " + "\n");
        else
            info.append("\t".repeat(indent)).append("Expanded to " + expandedAST.toString() + " in " + (step - 1) + " steps" + "\n");

        return info.toString();
    }

    private static String debug(MyDataType exp) throws REPLErrors, ReaderErrors {
        return debug(exp, 1);
    }
}
