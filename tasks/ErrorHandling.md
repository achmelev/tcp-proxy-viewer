**Kind (Bug, Feature)

Feature

**Short Description

Error handling implementation

**Text

Currently the tool doesn't have any user-friendly error handling. If there ist a non-recoverable error in the application
, both due to a programming error or due to an external condition, the corresponding exception gets logged but otherwise 
the user doesn't get any feedback. Worse, in some cases the application remains in a faulty state and doesn't work anymore
Implement a  user-friendly error handling according to the following guidelines:

* Monitor all non-recoverable errors, that means only RuntimeExceptions but also Throwables like OutOfMemoryErrors
* React to non recorevable errors as follows:
  * Show the user an error-specific message
  * Decide whether the application can proceed after this error or is now in a faulty state.
  * If the appication is a faulty state, stop it.

**Status (Open, Done, Canceled)
Done