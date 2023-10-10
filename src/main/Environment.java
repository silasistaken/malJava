package main;


import java.util.HashMap;

import static main.Types.MyDataType;
import static main.Types.SymbolType;

/***
 * Environment
 */
public class Environment {
    HashMap<String, MyDataType> env;
    Environment outer;


    public Environment(HashMap<String, MyDataType> map) {
        outer = null;
        env = map;
    }

    public Environment(Environment outer) {
        this.outer = outer;
        env = new HashMap<>();
    }

    /**
     * Tries to recursively get the expression associated with the key in the environment "chain". First looks in this
     * environment and if the key was not found it looks in the outer environment until it reaches the global environment.
     * Returns the expression when found or null if key is not present in any of the outer environments.
     *
     * @param key Symbol key to look up (variable name)
     * @return the expression mapped to the key or null if not found in this or any of the outer envs.
     */
    public MyDataType get(String key) {
        MyDataType fun = env.get(key);
        if (fun != null)
            return fun;
        else if (outer != null)
            return outer.get(key);
        else
            return null;
    }

    public MyDataType get(SymbolType key) {
        return this.get(key.getValue());
    }

    /**
     * Associates the specified variable with the expression in this env.
     * If this environment already contains a mapping for the key, the old value is overridden.
     *
     * @param key string representation of the symbol
     * @param exp an expression
     */
    public void put(String key, MyDataType exp) {
        this.env.put(key, exp);
    }

    public void put(SymbolType key, MyDataType exp) {
        this.env.put(key.getValue(), exp);
    }

    /**
     * Finds the first environment that has a mapping for the key/symbol, null if none found.
     *
     * @param key symbol/variable name
     * @return env with holding a value for the key, null if none found
     */
    public Environment lookup(SymbolType key) {
        MyDataType fun = env.get(key.getValue());
        if (fun != null)
            return this;
        else if (outer != null)
            return outer.lookup(key);
        else
            return null;
    }
}
