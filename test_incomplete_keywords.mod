MODULE TestIncompleteKeywords
    ! Test file for incomplete keyword detection
    ! This file intentionally contains errors to test the syntax checker
    
    ! Test 1: Incomplete VAR keyword
    VA robtarget Target_10;
    
    ! Test 2: Incomplete PERS keyword
    PER speeddata v1000:=[1000,500,5000,1000];
    
    ! Test 3: Incomplete CONST keyword
    CONS num MaxSpeed:=1000;
    
    ! Test 4: Correct declarations (should not error)
    VAR robtarget Target_20;
    PERS speeddata v2000:=[2000,500,5000,1000];
    CONST num MinSpeed:=100;
    
    PROC Main()
        ! Test 5: Incomplete WaitTime
        WaitTim 0.5;
        
        ! Test 6: Incomplete TPWrite
        TPWrit "Hello";
        
        ! Test 7: Incomplete MoveAbsJ (missing letter 'J')
        MoveAbs Home\NoEOffs, v1000, fine, tool0;
        
        ! Test 8: Correct instructions (should not error)
        WaitTime 1.0;
        TPWrite "Test complete";
        MoveAbsJ Home\NoEOffs, v1000, fine, tool0;
    ENDPROC
    
    FUNC num TestFunction()
        VAR num result;
        result := 10;
        
        ! Test 9: Incomplete RETURN
        RETUR result;
    ENDFUNC
    
    ! Test 10: Incomplete WHILE
    PROC TestWhile()
        WHIL TRUE DO
            WaitTime 0.1;
        ENDWHILE
    ENDPROC
    
    ! Test 11: Incomplete ENDPROC
    PROC TestEndProc()
        TPWrite "Test";
    ENDPRO
    
ENDMODULE
