 A JAR file of the project can be found in \out\artifacts\LispInterpreter_jar.
 To start the REPL start the JAR with

  	java -jar LispInterpreter.jar optional_arg

 With the optional_arg argument a file can be passed to be evaluated before the REPL accepts user input.
 For example if we run

      java -jar LispInterpreter.jar interpreter.txt

 Grahams eval. functions is in the global environment, so we can type

 	    (eval. '(eq 'a 'a) '())

 into the terminal which evaluates to #true using the eval. which is implemented in the interpreters lisp syntax.
 Alternatively if we were to type

 	    (eval '(eq 'a 'a))

 into the terminal, it also evaluates to #true but using the interpreter directly.
 Files can either be passed as the optional_arg when executing the JAR or they can be loaded at any point from the REPL. This is done using the core function "load" which expects a single string
 as an argument containing a valid filepath
      
       (load "interpreter.lisp")