# Basic Syntax

The REPL will evaluate the 1st complete expression and ignore everything else afterward until it encounters a new line.
From the input `(eq 1 2) (eq 1 1)` will only evaluate `(eq 1 2)` return `#false` and ignore the second
expression `(eq 1 1)`.  
A singular atom can be used without brackets but more than one atom without surrounding brackets will give an error. `1`
will evaluate the atomic expression but `1 2` will result in an error.  
A special case is a quoted expression like `'a` which gets translated by the reader to a list containing `(quote a)`
which is means internally `'a` gets treated as a list.  
Lists are treated differently depending on the value of the 1st element (a0). If a0 is a symbol representing a special
form the list is evaluated depending on the rules of the special form. Otherwise, it is assumed that the list is a
function call.

# Special Forms:

- # def!
  Syntax: `(def! name1 exp1 ...)` where name must be a symbol type and expression can be any valid expression side
  effect: assign values to variables in the global env.  
  Usage:  
  ` (def! a 5)`  
  ` a ;--> 5`  
  ` (def! b (+ 2 4))`  
  ` b ;--> 6`  
  `(def! x 10 y 12 z 13)`  
  ` x ;-->10`
  ` y ;-->12`
  ` z ;-->13`
- # set!
  Changes the value of an existing variable. Throws an error when trying to change a variable that has not been
  initialized yet. When a variable is shadowing another variable set! changes the most local instance. Cannot change "
  const variables" like true, false, nil and special form names.  
  syntax: (set! var1 form1 var2 form2...), evaluates form1 then stores it in var1 etc. returns value of last form or nil
  if no value/form pairs are provided.
- # defun
  defines a function and assigns it to a variable in global env can technically be done by a macro
  Syntax: `(defun name (p1 p2 ...pn) exp)` where name must be a symbol type and expression can be any valid expression.
  The list `(p1 p2 ...pn)` holds the parameters, if the function takes no input this has to be an empty list. the number
  of parameters must match the number of arguments when calling the function.  
  Is syntactic sugar for `(def! name (lambda (p1 p2 ...pn) exp))`
- # defmacro
  Syntax: `(defmacro name (p1 p2 ...pn) exp)` where name must be a symbol type and expression a valid expression.
  defines a macro and assigns it to a variable in global env sets the functions macro flag to true. Before evaluating
  the macro gets expanded resulting in the expression which is then evaluated instead. The parameters get replaced with
  the arguments the macro was called with.
- # let*
  syntax: `(let* (clause_ 1 ...) body)` where each clause is of form `(variable exp)`. each exp in a clause is evaluated
  and bound (locally within the let*) to the variable. Clauses can refer to variables defined in earlier clauses.  
  e.g.
  ```
  (let* ( (a 1)  
          (b (+ a 1))
        )
    (list a b))
  ```
  returns `(1 2)`

- # cond

  Syntax: `(cond (c1 e1)...(cn en))`
  conditional statement, eval every ci up until the first one that returns True then return the value of ei as the return
  value of the cond expression. Nil otherwise.

- # lambda
  basic function type of lisp, can be used anonymously or bound to a variable(name). When called it creates an env
  pointing to the env where the lambda was created in

- # quasiquote
  syntax: (quasiquote exp)
  calls the internal quasiquote function, if exp is:
    - a list:
        - that is empty, return that empty list (exp)
        - first element is unquote, returns 2nd element of exp
        - else:
            - a list with all the elements of exp but
            - elements that are lists starting with splice-unquote are replaced with (concat element(1) (rest of exp))
            - elements that are lists starting with unquote are replaced with (cons quasiquote(element(1)) (rest of
              exp))
    - a symbol: returns (quote exp)
    - otherwise: returns exp (not affected by evaluation, ie self evaluating such as ints and strings)

  the return value is then evaluated in the current environment. use of unquote and splice-unquote outside of a
  quasiquote context throws an error since the auxiliary functions cons and concat get wrong arguments passed.

- ## Quasiquote
    - allows certain elements within the quoted exp (list) to be evaluated
      `(quasiquote (a lst d)) -> (a lst d)`
    - evaluates an expression and returns it
    - the following two special forms only have meaning within a quasiquote
    - without unquote it works the same as a normal quote
- ## Unquote
    - turns evaluation for its elements on  
      `(quasiquote (a (unquote lst) d)) -> (a (b c) d)`

- ## Splice unquote
    - The splice-unquote also turns evaluation back on for its argument, but the evaluated value must be a list which is
      then "spliced" into the quasiquoted list
      ` (quasiquote (a (splice-unquote lst) d)) -> (a b c d)`
- ## Quasiquoteexpand
    - for debugging, same as quaisquote but returns the UNEVALUATED result

# Function calls

Function calls are list where the 1st element is a function which will be applied to the rest of the list. The rest of
the list are the argument to the function. The whole list will be evaluated and only after that the function is applied.
That means the 1st element must evaluate to a function. Lets say we have defined a function `add2` which takes 2 integer
arguments and adds them together. We call this function by passing a list to the REPL where the 1st element evaluates to
the function `add2`. This is done by passing the function name we defined when creating the function.
`(defun add2 (x y) (+ x y))` creates the function with the body `(+ x y)` and maps it to the name `add2`
in the global environment. When we call the function with `(add2 1 2)` the whole list is evaluated and rsulting in a
list where the 1st element is a function with body `(+ x y)` and the arguments `1 and 2`
evaluate to themselves since they are atoms.

the list `(+ 2 3)` evals to a list where the 1st element is the arithmetic function `+`. this gets applied/called to the
rest of the list `(2 3)`.

# Core functions

they get treated/evaluated like a user defined function, but have functionalities implemented in java (unlike lambda
expressions). that means the arguments get evaluated before the function is applied to them.

# Macros

Creation syntax: (defmacro name (arg1 arg2 ...argN) body)  
Example: (defmacro if (test t-clause else) `(cond (,test ,t-clause) ('default ,else)))  
The body is quasiquoted and every occurence of an argument needs to be unquoted. This means that when the macro is
created the syntax is checked and the body and the parameters are stored for later use in a function object.    
Calling a macro: (if (atom 'a) "a_IsAnAtom" "a_IsNotAnAtom")  
A macro call consists of the name followed by the arguments. This is then expanded before being evaluated.  
Macro expansion:  
During expansion the current environment is passed to the macro and the apply function of the macro is called with the
arguments passed by the call. The apply function creates an internal environment, with the passed environment as a
parent, which maps the parameters of the macro to the arguments. The macro body is then evaluated in that environment
and the result is returned. This step basically replaces the macro call with new code which is then evaluated in place
of the original code (the macro call). This effectively replaces the macros parameters with the arguments passed during
the macro call. To avoid evaluation of the macro body at this point, it is necessary to **quote the body and unquote all
occurrences of the parameters in the body.**  
Expansion is repeated until the resulting code is no longer a macro call.