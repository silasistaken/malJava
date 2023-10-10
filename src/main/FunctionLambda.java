package main;

import static main.Types.ListType;
import static main.Types.MyDataType;

/**
 * Interface for function types
 */
public interface FunctionLambda {
    MyDataType apply(ListType args) throws REPLErrors, ReaderErrors;
}