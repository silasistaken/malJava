package main;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static main.REPLErrors.ApplyError;
import static main.REPLErrors.SyntaxError;
import static main.Types.*;

public class REPL {

    public static Environment globalEnv = Core.getNamespace();
    public static Pattern pSpecialForms = Pattern.compile("(def!|set!|begin|let\\*|cond|quote|quasiquote|quasiquoteexpand|lambda|defun|defmacro)");
    public static Pattern pConstants = Pattern.compile("(nil|true|false)");

    /**
     * Converts a string representation of an S-expression into internal representation
     *
     * @param input input string
     * @return Datatype parsed from input string
     */
    public static MyDataType read(String input) throws REPLErrors, ReaderErrors {
        return Reader.read_str(input);
    }

    /**
     * Takes an AST and determines whether it is calling a Macro. I.e. AST is a list starting with a symbol
     * that maps to a macro function in the given environment
     *
     * @return true if AST is a call to a macro
     */
    public static boolean is_macro_call(MyDataType ast, Environment env) {
        if (ast instanceof ListType astList) {
            var function = env.get(astList.get(0).toString());//can be null
            if (function instanceof MyFunction)
                return ((MyFunction) function).isMacro();
        }
        return false;
    }

    /**
     * Returns the macro body with the values substituted, return value should be evaluated instead of the macro call
     *
     * @return a new AST according to the macro
     */
    public static MyDataType macroexpand(MyDataType ast, Environment env) throws REPLErrors, ReaderErrors {
        MyDataType expandedAST = ast;
        while (is_macro_call(expandedAST, env)) {
            MyFunction macro = (MyFunction) env.get(((ListType) expandedAST).get(0).toString());
            macro.setEnvironment(env);
            expandedAST = macro.apply(((ListType) expandedAST).rest());
        }
        return expandedAST;
    }

    /**
     * Evaluates a given AST by switching on type and content of the AST. Handles macro expansion and function calls.
     *
     * @param ast unevaluated AST
     * @param env current environment
     * @return evaluated AST
     */
    public static MyDataType eval(MyDataType ast, Environment env) throws REPLErrors, ReaderErrors {
        //ast is not a list, i.e. is an atom
        if (!ast.isList())
            return evalAST(ast, env);
        //(List) apply section
        ListType astList = (ListType) ast;
        //ast is an empty list
        if (astList.size() == 0)
            return ast;
        else {
            //ast is a non-empty list
            MyDataType expanded = macroexpand(ast, env);
            if (!expanded.isList()) {
                return evalAST(expanded, env);
            }
            astList = (ListType) expanded;
            var a0 = astList.get(0);
            //ast is an empty list
            if (astList.size() == 0)
                return ast;
            //handle special forms
            if (a0 instanceof SymbolType)
                if (pSpecialForms.matcher(((SymbolType) a0).getValue()).matches())
                    return evalSpecialForm(astList, env);
            //eval/apply for function calls
            ListType list = (ListType) evalAST(astList, env);//evaluate all the elements in the list
            MyDataType first = list.first();//evaluates to a function
            ListType rest = list.rest();//arguments for function call
            if (first instanceof MyFunction)
                return ((MyFunction) first).apply(rest);//apply the function the arguments
            else
                throw new ApplyError("Function call failed, could not find a function called " + first.toString() + " to apply");
        }
    }


    /**
     * Symbols get looked up in the environment, lists have eval called on every element
     * and return a ListType holding the resulting values and self evaluating expressions (i.e. integers, strings, constants)
     * return themselves.
     *
     * @return value of AST in the current environment
     */
    public static MyDataType evalAST(MyDataType ast, Environment env) throws REPLErrors, ReaderErrors {
        if (ast instanceof SymbolType) {
            //symbol: lookup the symbol in the environment structure and return the value or raise an error if no value is found
            MyDataType fun = env.get(((SymbolType) ast).getValue());
            if (fun == null)
                throw new SyntaxError("Symbol " + ((SymbolType) ast).value + " not found in env");
            else return fun;
        } else if (ast.isList()) {
            //list: return a new list that is the result of calling EVAL on each of the members of the list
            ArrayList<MyDataType> evalList = new ArrayList<>();
            for (MyDataType entry : ((ListType) ast).values) {
                evalList.add(eval(entry, env));
            }
            return new ListType(evalList);
        } else
            //otherwise (self evaluating expression) i.e. integer or string
            return ast;
    }

    public static MyDataType evalSpecialForm(ListType ast, Environment env) throws REPLErrors, ReaderErrors {
        SymbolType a0 = (SymbolType) ast.get(0);
        MyDataType result = switch (a0.getValue()) {
            case "def!" -> {
                //define a variable in the global environment
                //required form: (def! var1 form1 var2 form2...) where var must be a symbol and form must be a (valid) expression
                if (ast.size() == 0 || ast.size() % 2 == 0)
                    throw new SyntaxError("Expected form: (def! var1 form1 var2 form2...) where var must be a symbol and" +
                            " form must be a (valid) expression");
                MyDataType lastResult = Nil;
                for (int i = 1; i < ast.size(); i += 2) {
                    if (ast.get(i) instanceof SymbolType name) {
                        if (pSpecialForms.matcher(name.getValue()).matches() || pConstants.matcher(name.getValue()).matches()
                                || Core.coreNames.contains(name.getValue())) {
                            throw new SyntaxError("cannot change constant variable " + name);
                        }
                        var form = ast.get(i + 1);
                        form = eval(form, env);
                        lastResult = form;
                        //add the variable to the global environment
                        globalEnv.put(name, form);
                    } else
                        throw new SyntaxError("variable name is not a symbol");
                }
                yield lastResult;
            }
            case "set!" -> {
                //change the value of an existing variable
                //required form: (set! var1 exp1) where var1 must be a symbol and exp1 can be any (valid) expression
                if (ast.size() == 0 || ast.size() % 2 == 0)
                    throw new SyntaxError("Expected form: (set! var1 form1 var2 form2...) where var must be a symbol and" +
                            " form can be any (valid) expression");
                MyDataType lastResult = Nil;
                for (int i = 1; i < ast.size(); i += 2) {
                    if (ast.get(i) instanceof SymbolType name) {
                        if (pSpecialForms.matcher(name.getValue()).matches() || pConstants.matcher(name.getValue()).matches() || Core.coreNames.contains(name.getValue()))
                            throw new SyntaxError("cannot change constant variable " + name);
                        var form = ast.get(i + 1);
                        //find env where var is defined and change its value
                        Environment varEnv = env.lookup(name);
                        if (varEnv != null) {
                            form = eval(form, env);//eval in current env
                            varEnv.put(name, form);//change value in env where var is assigned.
                            lastResult = form;
                        } else
                            throw new SyntaxError("cannot use set! on unassigned variable: " + name.getValue());
                    } else
                        throw new SyntaxError("variable name is not a symbol");
                }
                yield lastResult;
            }
            case "begin" -> {
                //eval all args and return the last value
                if (ast.size() == 1)
                    yield Nil;
                else {
                    for (int i = 1; i < ast.size() - 1; i++) {
                        eval(ast.get(i), env);
                    }
                    yield eval(ast.get(ast.size() - 1), env);
                }
            }
            case "let*" -> {
                //required form: (let* ((k1 v1)...(kn vn)) exp) or (let* (k v) exp)
                // where k must be a symbol and v and exp can be any (valid) expression, must have at least 1 pair
                var exp = ast.get(2);
                if (ast.get(1) instanceof ListType bindingList && exp != null) {
                    Environment tempEnv = new Environment(env);
                    MyDataType k, v;
                    if (bindingList.size() == 2 && !(bindingList.get(0) instanceof ListType)) {
                        //only one binding
                        k = bindingList.get(0);
                        v = bindingList.get(1);
                        if (!(k instanceof SymbolType name))
                            throw new SyntaxError(k.toString() + " not a symbol");
                        if (pSpecialForms.matcher(name.getValue()).matches() || pConstants.matcher(name.getValue()).matches() || Core.coreNames.contains(name.getValue()))
                            throw new SyntaxError("cannot change constant variable " + k);
                        tempEnv.put((String) k.getValue(), eval(v, tempEnv));
                    } else {
                        //list of bindings
                        //sequential evaluation of the local bindings, evaluates the first expression then stores it
                        //in the variable. Then evaluates the next expression etc. This allows bindings to refer to
                        //previous bindings
                        for (MyDataType binding : bindingList.getValue()) {
                            //the argument for the bindings is a "list of pairs"
                            if (!binding.isList() || ((ListType) binding).getValue().size() != 2)
                                throw new SyntaxError("bindings must be a list, required form: (let* ((k1 v1)...(kn vn)) exp) or (let* (k v) exp)");
                            k = ((ListType) binding).get(0);
                            v = ((ListType) binding).get(1);
                            if (!(k instanceof SymbolType name))
                                throw new SyntaxError("keys must be symbols, required form: (let* ((k1 v1)...(kn vn)) exp) or (let* (k v) exp)");
                            if (pSpecialForms.matcher(name.getValue()).matches() || pConstants.matcher(name.getValue()).matches() || Core.coreNames.contains(name.getValue()))
                                throw new SyntaxError("cannot change constant variable " + name);
                            tempEnv.put((String) k.getValue(), eval(v, tempEnv));
                        }
                    }
                    yield eval(exp, tempEnv);

                } else
                    throw new SyntaxError("missing list of assignments");
            }
            case "quote" -> {
                //form: (quote exp)
                //returns exp unevaluated
                if (ast.size() == 2)
                    yield ast.get(1);
                else
                    throw new SyntaxError("expected 1 argument but got: " + (ast.size() - 1));
            }
            case "quasiquote" -> {
                if (ast.size() != 2)
                    throw new SyntaxError("quasiquote requires 1 arg");
                MyDataType quote = quasiquote(ast.get(1));
                MyDataType eQuote = eval(quote, env);
                yield eQuote;
            }
            case "quasiquoteexpand" -> {
                if (ast.size() != 2)
                    throw new SyntaxError("quasiquoteexpand requires 1 arg");
                MyDataType quote = quasiquote(ast.get(1));
                yield quote;
            }
            case "cond" -> {
                //form: (cond (c1 e1)...(cn en))
                //eval every ci up until the first one returns True then return the value of ei as the return value of the cond expression
                for (int i = 1; i < ast.size(); i++) {
                    if (!(ast.get(i) instanceof ListType pair))
                        throw new SyntaxError("cond expects a list of pairs like: (cond (c1 e1)...(cn en))");
                    if (pair.size() != 2)
                        throw new SyntaxError("cond didnt get a pair of size 2, expected form: (cond (c1 e1)...(cn en))");
                    var a1value = eval(pair.get(0), env);
                    if (!a1value.equals(False) && !a1value.equals(Nil)) //anything other than false or nil counts as true
                        yield eval(pair.get(1), env);
                }//none of the clauses evaluated to true
                yield Nil;
            }
            case "lambda" -> {
                //form: (lambda (p1...pn) body)
                //calling a function: ((lambda (p1...pn) body) a1...an)
                if (!(ast.get(1) instanceof ListType params))
                    throw new SyntaxError("required form: (lambda (p1...pn) body)");//a1...an
                MyDataType lambdaBody = ast.get(2);
                yield new MyFunction("<lambda>", lambdaBody, env, params) {
                    @Override
                    public MyDataType apply(ListType args) throws REPLErrors, ReaderErrors {
                        Environment inner = new Environment(env);
                        if (args.size() != params.size())
                            throw new SyntaxError("Wrong number of arguments to call this function, expected " + params.size() + " but got " + args.size());//check if # of parameters match # of arguments passed
                        int i = 0;
                        for (MyDataType parameter : params.getValue()) {
                            if (!(parameter instanceof SymbolType))
                                throw new SyntaxError("Only symbols are viable parameters");
                            inner.put(((SymbolType) parameter).getValue(), args.get(i++));//bind arguments to the parameters in the local inner environment
                        }
                        return eval(lambdaBody, inner);//return the value of the expression of the lambda, given the new environment/local bindings of args and parameters
                    }
                };
            }
            case "defun" -> {
                //create a function/lambda and add it to the global environment under the given name
                //"syntactic sugar" for label forms (note: there is no label eval rule)
                //behaves like def! name (lambda (args) body)
                if (ast.size() < 4)//not enough args
                    throw new SyntaxError("missing arguments for defun. Syntax for defun:\n" +
                            "(defun name (parameters...) function-body)");
                SymbolType name = (SymbolType) ast.get(1);
                ListType params = (ListType) ast.get(2);
                MyDataType body = ast.get(3);
                MyFunction fun = new MyFunction(name.getValue(), body, env, params) {
                    @Override
                    public MyDataType apply(ListType args) throws REPLErrors, ReaderErrors {
                        Environment inner = new Environment(env);
                        assert args.size() == params.size();//check if # of parameters match # of arguments passed
                        int i = 0;
                        for (MyDataType parameter : params.getValue()) {
                            if (!(parameter instanceof SymbolType))
                                throw new SyntaxError("Only symbols are viable parameters");
                            inner.put(((SymbolType) parameter).getValue(), args.get(i++));
                        }
                        return eval(body, inner);
                    }
                };
                globalEnv.put(name.getValue(), fun);
                yield fun;

            }
            case "defmacro" -> {
                //define a macro in the global environment
                //simple macro definition where a symbol (name) gets mapped to a macro
                //required form: (defmacro name (args) body)
                if (!(ast.get(1) instanceof SymbolType name))
                    throw new SyntaxError("Expected form: (defmacro name (args) body) where name must be a symbol.");
                var p = ast.get(2);
                if (!(p instanceof ListType params))
                    throw new SyntaxError("expected a list of args, required form: (defmacro name (args) body)");
                var body = ast.get(3);
                MyFunction macro = new MyFunction(name.getValue(), body, params) {
                    @Override
                    public MyDataType apply(ListType args) throws REPLErrors, ReaderErrors {
                        Environment inner = new Environment(env);
                        //add params and args to inner environment
                        assert args.size() == params.size();//check if # of parameters match # of arguments passed
                        int i = 0;
                        for (MyDataType parameter : params.getValue()) {
                            if (!(parameter instanceof SymbolType))
                                throw new SyntaxError("Only symbols are viable parameters for macros");
                            inner.put(((SymbolType) parameter).getValue(), args.get(i++));
                        }
                        this.setEnvironment(inner);
                        this.lexical(args);//resolve lexical shadowing
                        return eval(this.body, inner);
                    }
                };
                macro.setMacro(true);
                //add the variable to the global environment
                globalEnv.put(name.getValue(), macro);
                yield macro;
            }
            default -> throw new Error("internal error with special form eval");
        };
        return result;
    }

    public static MyDataType quasiquote(MyDataType ast) throws SyntaxError {
        if (ast == null)
            throw new SyntaxError("AST is null in quasiquote");
        if (ast instanceof SymbolType symbol)
            return new ListType(new SymbolType("quote"), symbol);//no need for quasiquote
        if (ast instanceof ListType list) {
            if (list.size() == 0)//empty list
                return list;
            if (list.size() == 2 && list.get(0) instanceof SymbolType a0 && a0.getValue().equals("unquote"))
                //ast = (unquote X)
                return list.get(1);//return X
            else {
                //any other lists
                ListType result = new ListType();
                ListType temp;
                MyDataType element;
                for (int i = list.size() - 1; i >= 0; i--) {//reverse order
                    element = list.get(i);
                    if (element instanceof ListType eList && eList.get(0) instanceof SymbolType a0 && a0.getValue().equals("splice-unquote")) {
                        MyDataType spliceList = eList.get(1);
                        if (spliceList == null)//no argument given to splice-unquote
                            throw new SyntaxError("splice list didnt get an argument");//cant check if it evaluates to a list
                        else {
                            temp = new ListType(new SymbolType("concat"), spliceList, result);
                            result = new ListType(temp.getValue());
                        }
                    } else {
                        temp = new ListType(new SymbolType("cons"), quasiquote(element), result);
                        result = new ListType(temp.getValue());
                    }
                }
                return result;
            }
        } else
            return ast;//self evaluating
    }

    /**
     * Prints the given datatype to the console.
     * Calls the toString() (of the result of eval usually) which implements the details for each datatype
     */
    public static String print(MyDataType result) {
        return result.toString();
    }

    /**
     * Reads the input string, evaluates the expression and returns the resulting String
     *
     * @param userInput input string, S-expression
     * @return output string, evaluated S-expression
     */
    public static String rep(String userInput) throws REPLErrors, ReaderErrors {
        return print(eval(read(userInput), globalEnv));
    }

    /**
     * Reads the input string and returns the evaluated expression
     *
     * @param userInput input string, S-expression
     * @return output string, evaluated S-expression
     */
    public static MyDataType re(String userInput) throws REPLErrors, ReaderErrors {
        return eval(read(userInput), globalEnv);
    }
}
