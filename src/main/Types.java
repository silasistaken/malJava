package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Datatypes used, encapsulated to get around the troubles of static types
public class Types {

    public static abstract class MyDataType {
        public abstract Object getValue();

        public abstract boolean equals(MyDataType other);

        public boolean isList() {
            return false;
        }

        public String toString() {
            return "override me";
        }

    }

    /**
     * Abstract class used to implement functions, lambdas and macros.
     */
    public static abstract class MyFunction extends MyDataType implements FunctionLambda {
        String value;//name of the function that implements the apply method, for convenience
        private boolean isMacro = false;

        public MyDataType body = null;
        private Environment environment = null;//inner/local env
        private ListType parameters = null;
        private boolean is_core = false;

        public MyFunction(String name, MyDataType body, Environment environment, ListType parameters) {
            this.value = name;
            this.body = body;
            this.environment = environment;
            this.parameters = parameters;
        }

        public MyFunction(String name, MyDataType body, ListType parameters) {
            this.value = name;
            this.body = body;
            this.parameters = parameters;
        }

        public MyFunction(String value) {
            this.value = value;
        }


        public void lexical(ListType args) throws REPLErrors, ReaderErrors {
            if (!isMacro)
                return;
            //STEP 1: check for conflicts between args and body
            ArrayList<String> argList = new ArrayList<>();//candidates for conflicts
            ArrayList<String> paramList = new ArrayList<>();//strings to be removed from body before conflict checking
            //populate argList and paramList with symbol types, ignore other datatypes
            for (int i = 0; i < args.size(); i++) {
                if (args.get(i) instanceof SymbolType as)
                    argList.add(as.getValue());
                if (parameters.get(i) instanceof SymbolType ps)
                    paramList.add(ps.getValue());
            }
            if (argList.isEmpty())//no conflicts possible since none of the args are symbols
                return;
            String bodyWithoutParams = body.toString();//used to determine conflicts
            String bodyReplacement = bodyWithoutParams.replaceAll("\\(", "( ");//used to resolve conflicts, passed to reader to make new body
            bodyReplacement = bodyReplacement.replaceAll("\\)", " )");//pad brackets
            //remove params, quotes and brackets from body
            for (String p : paramList)
                bodyWithoutParams = bodyWithoutParams.replaceAll(p, "");
            bodyWithoutParams = bodyWithoutParams.replaceAll("(\\)|\\(|,|'|`|@)", "");
            bodyWithoutParams = bodyWithoutParams.replaceAll("(\s+)", " ");

            //determine if conflicts exist
            ArrayList<String> bodySplit = new ArrayList<>(Arrays.asList(bodyWithoutParams.split("\s")));//holds all words except params

            ArrayList<String> conflictList = new ArrayList<>();
            for (String split : bodySplit) {
                if (argList.contains(split) && !conflictList.contains(split)) {
                    conflictList.add(split);
                }
            }
            //STEP 2: resolve conflicts by renaming vars in body
            //replace conflicting arg names in body
            String replacement = "foo";
            int appendix;
            boolean hasConflict;
            bodySplit.addAll(argList);
            if (!conflictList.isEmpty()) {
                for (String conflictingVar : conflictList) {
                    //find a new name
                    hasConflict = true;
                    appendix = 1;
                    while (hasConflict) {
                        replacement = conflictingVar + "_" + appendix++;
                        if (!bodySplit.contains(replacement)) {
                            //name is not taken yet
                            //add new name to bodySplit
                            //this now keeps track of any name that can lead to conflicts
                            bodySplit.add(replacement);
                            hasConflict = false;
                        }
                    }
                    bodyReplacement = bodyReplacement.replaceAll("\s" + conflictingVar + "\s", "\s" + replacement + "\s");
                }
            }
            bodyReplacement = bodyReplacement.replaceAll("\\(\s", "(");
            bodyReplacement = bodyReplacement.replaceAll("\s\\)", ")");
            this.body = Reader.read_str(bodyReplacement, false);//transform string back into ast
        }


        @Override
        public boolean equals(MyDataType other) {
            return false;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public String getValue() {
            return value;
        }

        public boolean isMacro() {
            return isMacro;
        }

        public MyDataType getBody() {
            return body;
        }

        public void setBody(MyDataType body) {
            this.body = body;
        }

        public Environment getEnvironment() {
            return environment;
        }

        public void setEnvironment(Environment environment) {
            this.environment = environment;
        }

        public void setMacro(boolean macro) {
            isMacro = macro;
        }

        public ListType getParameters() {
            return parameters;
        }

        public MyFunction setParameters(ListType parameters) {
            this.parameters = parameters;
            return this;
        }

        public boolean isCore() {
            return is_core;
        }

        public MyFunction setIs_core(boolean is_core) {
            this.is_core = is_core;
            return this;
        }
    }

    //wrapper class for Lists
    public static class ListType extends MyDataType {
        ArrayList<MyDataType> values;

        public ListType(List<MyDataType> list) {
            values = new ArrayList<>();
            values.addAll(list);
        }

        public ListType(MyDataType... args) {
            this.values = new ArrayList<>();
            values.addAll(Arrays.asList(args));
        }

        @Override
        public ArrayList<MyDataType> getValue() {
            return values;
        }

        /**
         * Compare this list to any other datatype, returns False unless both are empty lists.
         *
         * @param other MyDataType to compare
         * @return boolean value indicating equality in a LISP sense
         */
        @Override
        public boolean equals(MyDataType other) {
            if (!other.isList())
                return false;
            ListType otherL = (ListType) other;
            return this.size() == 0 && otherL.size() == 0;
        }

        @Override
        public boolean isList() {
            return true;
        }

        @Override
        public String toString() {
            if (values == null || values.size() < 1)
                return "()";
            StringBuilder s;
            //call toString on every element of the list other than quote and put some brackets around it
            if (values.get(0) instanceof SymbolType a0 &&
                    (a0.getValue().equals("quote") || a0.getValue().equals("quasiquote") || a0.getValue().equals("unquote")
                            || a0.getValue().equals("splice-unquote"))) {
                //Make quotes readable again
                String quoteType = a0.getValue();
                if (quoteType.equals("quote"))
                    s = new StringBuilder("'");
                else if (quoteType.equals("quasiquote"))
                    s = new StringBuilder("`");
                else if (quoteType.equals("unquote"))
                    s = new StringBuilder(",");
                else if (quoteType.equals("splice-unquote"))
                    s = new StringBuilder(",@");
                else
                    s = new StringBuilder("quote error");

                if (values.size() > 2)
                    return "more than 1 exp in a quoted list: " + values;
                else
                    return s.append(values.get(1)).toString().stripTrailing();
            } else {
                s = new StringBuilder("(");
                for (MyDataType e : values)
                    s.append(e.toString()).append(" ");
//                s.replace(s.length() - 1, s.length(), ")");
                return s.toString().stripTrailing() + ")";
            }
        }

        public int size() {
            return values.size();
        }

        /**
         * Returns the item at the specified position, or null if index i is out of range.
         */
        public MyDataType get(int i) {
            if (i >= values.size())
                return null;
            return values.get(i);
        }

        /**
         * Gets the first element
         *
         * @return the first element of the list, null if list is empty
         */
        public MyDataType first() {
            if (values.size() < 1)
                return null;
            else
                return values.get(0);
        }

        /**
         * Gets everything except the first element of the list. If the list is empty it returns a (new) empty list.
         *
         * @return a list containing everything except the first element
         */
        public ListType rest() {
            if (values.size() > 0)
                return new ListType(values.subList(1, values.size()));
            else
                return new ListType();//empty list
        }
    }

    //wrapper class for integers
    public static class IntegerType extends MyDataType {
        Integer value;

        public IntegerType(Integer value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }

        public Integer getValue() {
            return value;
        }

        /**
         * Returns true if both are of IntegerType and hold the same value
         *
         * @return true if this.value == other.value i.e. both integers have the same value
         */
        @Override
        public boolean equals(MyDataType other) {
            if (other instanceof IntegerType)
                return this.value.equals(((IntegerType) other).getValue());
            else
                return false;
        }
    }

    //wrapper class for Symbols
    public static class SymbolType extends MyDataType {
        String value;

        public SymbolType(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(MyDataType other) {
            if (other instanceof SymbolType)
                return this.value.equals(((SymbolType) other).getValue());
            else
                return false;

        }

        @Override
        public String toString() {
            return value;
        }
    }

    //wrapper class for Strings
    public static class StringType extends MyDataType {

        String value;

        public StringType(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(MyDataType other) {
            if (other instanceof StringType)
                return this.value.equals(((StringType) other).getValue());
            else
                return false;

        }

        @Override
        public String toString() {
            return "\"" + value + "\"";
        }
    }

    public static class ConstType extends MyDataType {

        String value;

        public ConstType(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "#" + value;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public boolean equals(MyDataType other) {
            return (other instanceof ConstType && this.value == other.getValue());
        }

    }

    //constants used to represent true, false and null
    public static ConstType True = new ConstType("true");
    public static ConstType False = new ConstType("false");
    public static ConstType Nil = new ConstType("nil");


}

