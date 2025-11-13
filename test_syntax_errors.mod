MODULE TestSyntaxErrors
    ! This file tests various syntax error detection capabilities
    
    ! Test 1: Unclosed string
    PROC test_unclosed_string()
        TPWrite "This string is not closed;
        TPWrite "This is fine";
    ENDPROC
    
    ! Test 2: Unmatched parenthesis
    PROC test_unclosed_paren()
        VAR num result;
        result := myFunction(param1, param2;
        TPWrite "Done";
    ENDPROC
    
    ! Test 3: IF missing THEN
    PROC test_if_missing_then()
        IF counter > 10
            TPWrite "Too many";
        ENDIF
    ENDPROC
    
    ! Test 4: Incomplete PROC declaration
    PROC
        TPWrite "Missing procedure name";
    ENDPROC
    
    ! Test 5: FUNC missing return value
    FUNC num calculate_total()
        TPWrite "Calculating";
        RETURN
    ENDFUNC
    
    ! Test 6: Incomplete assignment - missing left side
    PROC test_assignment_error()
        := 10
    ENDPROC
    
    ! Test 7: Incomplete assignment - missing right side
    PROC test_assignment_error2()
        VAR num x;
        x :=
    ENDPROC
    
    ! Test 8: Invalid variable name
    PROC test_invalid_varname()
        VAR num 123invalid;
    ENDPROC
    
    ! Test 9: WHILE missing DO
    PROC test_while_missing_do()
        WHILE counter < 100
            counter := counter + 1;
        ENDWHILE
    ENDPROC
    
    ! Test 10: FOR missing TO
    PROC test_for_missing_to()
        FOR i FROM 1 DO
            TPWrite "Loop";
        ENDFOR
    ENDPROC
    
    ! Test 11: Unclosed bracket
    PROC test_unclosed_bracket()
        VAR num myArray[5;
        myArray[1 := 10;
    ENDPROC
    
    ! Test 12: Semicolon at end (RAPID doesn't use semicolons)
    PROC test_semicolon()
        VAR num x;
        x := 10;
    ENDPROC
    
    ! Test 13: Incomplete variable declaration
    PROC test_incomplete_var()
        VAR
        VAR num
    ENDPROC
    
    ! Test 14: Incomplete MODULE declaration
MODULE

ENDMODULE

! Test 15: Unclosed PROC block
PROC test_unclosed()
    TPWrite "This PROC is never closed";
