package main;

import main.REPLErrors.SyntaxError;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static main.ReaderErrors.*;
import static main.Types.*;

public class Reader {


    public Reader(ArrayList<String> tokens) {
        this.tokens = tokens;
    }

    public static String commentFreeInput = "";

    ArrayList<String> tokens;
    int position = 0;

    //Object functions

    /**
     * Peek at the token in current position and return it without changing the position
     *
     * @return returns the Token at the current position, null if there are no more tokens
     */
    public String peek() {
        if (position >= tokens.size())
            return null;
        else
            return tokens.get(position);
    }

    /**
     * Retrieves the token in current position and increments the position by 1
     *
     * @return returns the Token at the current position, null if there are no more tokens
     */
    public String next() {
        if (position >= tokens.size())
            return null;
        else
            return tokens.get(position++);
    }

    /**
     * Takes a string and returns the corresponding S-expression. It first splits the input string into tokens and
     * then converts them into their respective datatype (e.g. symbol, list, integer etc.).
     *
     * @param input string of an s-expression
     * @return S-expression of the input string in the proper datatypes
     * @throws SyntaxError
     * @throws EmptyLineError Lets the main loop ignore empty lines
     */
    public static MyDataType read_str(String input, boolean storeInput) throws REPLErrors, ReaderErrors {
        //call tokenize on input
        ArrayList<String> tokens = tokenize(input);
        if (storeInput) {
            //remove comments from input string to represent only what ends up being evaluated
            StringBuilder sb = new StringBuilder();
            for (String token : tokens)
                sb.append(token).append(" ");

            commentFreeInput = sb.toString();
            commentFreeInput = commentFreeInput.replaceAll("\\( ", "(");
            commentFreeInput = commentFreeInput.replaceAll(" \\)", ")");
            commentFreeInput = commentFreeInput.replaceAll("' ", "'");
            commentFreeInput = commentFreeInput.replaceAll("` ", "`");
            commentFreeInput = commentFreeInput.replaceAll(", ", ",");
            commentFreeInput = commentFreeInput.replaceAll(",@ ", ",@");
        }
        if (tokens.size() == 0)
            throw new EmptyLineError("empty tokens");//empty line
        //corner case: 'a b c...-->silently ignores everything following 'a
        //input is more than 1 token and is neither quoted nor a list
        if (tokens.size() > 1 && !tokens.get(0).equals("(") && !isQuote(tokens.get(0)))
            throw new SyntaxError("wrong syntax, expected a list for more than 1 argument");
        if (tokens.size() < 2 && isQuote(tokens.get(0)))
            throw new SyntaxError("wrong syntax, nothing to quote");

        //create Reader obj holding the tokens
        Reader reader = new Reader(tokens);
        return readForm(reader);
    }

    public static MyDataType read_str(String input) throws REPLErrors, ReaderErrors {
        return read_str(input, true);
    }


    /**
     * Takes the input read from the user as one string and splits it into the tokens that make up the input, e.g. (+ 2 (- 1 2)) will return the tokens [+, 2, (- 1 2)]
     *
     * @param input Some string that represents some sort of (partial) expression
     * @return List of tokens
     * @throws CommentError bubbles up to main loop to tell it to ignore commented lines
     * @throws SyntaxError  empty quotes or unclosed quotes, rejects input
     */
    public static ArrayList<String> tokenize(String input) throws REPLErrors {
        //split the string into its tokens, regex from mal github
        Pattern pattern = Pattern.compile("[\\s]*(,@|[()'`,]|\"(?:[\\\\].|[^\\\\\"])*\"?|;.*|[^\\s ()'`\";]*)");
        //the first part matches any number of whitespaces and doesn't capture them -> [\\s]*
        //the expression in round brackets is captured as group 1
        //,@ captures the 2 special characters (splice unquote)
        //[()'`,@] matches any 1 of the special characters
        //"(?:[\\].|[^\\"])*"?  Starts capturing at a double-quote and stops at the next double-quote unless it was preceded by a backslash
        // in which case it includes it until the next double-quote. It will also match unbalanced strings (no ending double-quote)
        //;.*  matches a semicolon and anything after that
        //[^\s ()'`";]*  matches a sequence of zero or more non-special characters i.e. anything that is allowed as normal symbols, atm @ and
        //other brackets are allowed as part of symbol names (non-round brackets have no special meaning)
        Matcher m = pattern.matcher(input);
        String token;
        ArrayList<String> tokens = new ArrayList<>();
        //loop over the string and match the regex
        while (m.find()) {
            token = m.group(1);
            //potentially viable token to be added
            if (token != null && !token.isEmpty() && !token.isBlank()) {
                if (token.startsWith(";"))//handle comments (anything after a semicolon ';')
                    if (tokens.size() == 0)
                        throw new CommentError("comment at start of line");//comment at start of a line, tells main loop to ignore this line completely
                    else
                        break;//comment not at start of the line, ignore rest of the line i.e. stop tokenizing
                if (token.startsWith("\"") && (!token.endsWith("\"") || token.length() < 3))//doesn't allow non closed "foo or empty "" strings
                    throw new SyntaxError("missing closing quote or empty quotes/string");
                tokens.add(token);
            }
        }
        return tokens;
    }

    /**
     * Peek at token and depending on 1st char of that token call either:
     * -readList() if its a left parenthesis
     * -readAtom() otherwise
     * or substitute the shorthand quote symbol ' with parenthesis quote syntax
     *
     * @param reader Reader object holding the tokens
     * @return either a ListType or an atom (Integer, Symbol or String)
     * @throws ParseError if there is some internal error and we attempt to read a null token. This happens when one of
     *                    the delegate methods (readList or readAtom) doesnt check for null values properly
     */
    public static MyDataType readForm(Reader reader) throws ParseError, ParenMismatchError {
        //peek at token
        String token = reader.peek();
        if (token == null)//java code error, should be caught before it is passed to this method
            throw new ParseError("Token is null");
        if (token.startsWith("(")) {
            reader.next();
            return readList(reader);
        //READER MACROS
        } else if (token.equals("'")) {//quote
            reader.next();
            return new ListType(new SymbolType("quote"), readForm(reader));//converts 'exp into (quote exp)
        } else if (token.equals("`")) {//quasi quote
            reader.next();
            return new ListType(new SymbolType("quasiquote"), readForm(reader));//converts `exp into (quasiquote exp)
        } else if (token.equals(",")) {//unquote
            reader.next();
            return new ListType(new SymbolType("unquote"), readForm(reader));//converts ,exp into (unquote exp)
        } else if (token.equals(",@")) {//splice-unquote
            reader.next();
            return new ListType(new SymbolType("splice-unquote"), readForm(reader));//converts ,@exp into (splice-unquote exp)
        } else {
            return readAtom(reader);
        }
    }

    /**
     * Repeatedly calls readForm until closing parenthesis is reached
     * or throws error if EOF is reached before that
     *
     * @param reader
     * @return ListType containing everything between the opening and closing brackets
     * @throws ParenMismatchError
     * @throws ParseError
     */
    public static MyDataType readList(Reader reader) throws ParenMismatchError, ParseError {
        ArrayList<MyDataType> list = new ArrayList<>();
        String token;
        while ((token = reader.peek()) != null) {
            if (token.equals(")")) {//end of a list reached
                reader.position++;//advance position since we only peeked at the token
                return new ListType(list);
            } else
                list.add(readForm(reader));//read next list element and add it to the parsed list
        }
        //EOF reached without encountering a closing paren
        throw new ParenMismatchError("no closing parenthesis");
    }

    /**
     * Converts an atom from its string representation to an internal datatype. Does not treat empty list as a datatype
     *
     * @param reader the reader object holding the tokens
     * @return MyDataType representing the token
     * @throws ParseError if the token is null or doesn't match any datatype
     */
    public static MyDataType readAtom(Reader reader) throws ParseError {
        String token = reader.next();
        if (token == null)
            throw new ParseError("Token is null in read_Atom");//should never happen
        if (token.startsWith("\""))
            return new StringType(token.replaceAll("^\"|\"$", ""));
        Pattern p3 = Pattern.compile("(^[+-]?[0-9]+$)|(^-?[0-9][0-9.]*$)|(^nil$)|(^true$)|(^false$)|^\"((?:[\\\\].|[^\\\\\"])*)\"$|^\"(.*)$|(^[^\"]*$)");//ty mal
        Matcher matcher = p3.matcher(token);
        if (!matcher.find()) {
            throw new ParseError("unrecognized token '" + token + "'");
        }
        if (matcher.group(1) != null) {
            return new IntegerType(Integer.parseInt(matcher.group(1)));
        } else if (matcher.group(3) != null) {
            return Nil;
        } else if (matcher.group(4) != null) {
            return True;
        } else if (matcher.group(5) != null) {
            return False;
        } else if (matcher.group(6) != null) {//normal strings, matched quotes
            return new StringType((matcher.group(6)));
        } else if (matcher.group(7) != null) {//unmatched quotes
            throw new ParseError("expected '\"', got EOF");//should be caught by tokenize before ever getting here
        } else if (matcher.group(8) != null) {
            return new SymbolType(matcher.group(8));
        } else {
            throw new ParseError("unrecognized '" + matcher.group(0) + "'");
        }
    }

    public static boolean isQuote(String token) {
        return (token.equals("'") || token.equals("`") || token.equals(",") || token.equals(",@"));
    }
}
