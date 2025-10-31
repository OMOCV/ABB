MODULE MainModule
    !***********************************************************
    ! Sample ABB RAPID Program
    ! Description: This is a demonstration program showing
    !              various RAPID programming features
    !***********************************************************
    
    ! Global variable declarations
    VAR num counter := 0;
    VAR bool running := TRUE;
    PERS robtarget home := [[600, 0, 600], [1, 0, 0, 0], [0, 0, 0, 0], [9E9, 9E9, 9E9, 9E9, 9E9, 9E9]];
    PERS robtarget target1 := [[400, 200, 300], [0.707, 0, 0.707, 0], [0, 0, 1, 0], [9E9, 9E9, 9E9, 9E9, 9E9, 9E9]];
    PERS speeddata fast_speed := [1000, 500, 5000, 1000];
    PERS zonedata fine_zone := [FALSE, 0, 0, 0, 0, 0, 0];
    
    !***********************************************************
    ! Main procedure - Entry point
    !***********************************************************
    PROC main()
        ! Initialize the robot
        TPWrite "Starting ABB Robot Program";
        initialize_robot;
        
        ! Main loop
        counter := 0;
        WHILE counter < 5 DO
            ! Move to home position
            MoveJ home, fast_speed, fine_zone, tool0;
            WaitTime 0.5;
            
            ! Move to target
            MoveL target1, fast_speed, fine_zone, tool0;
            WaitTime 0.5;
            
            ! Perform operation
            perform_operation counter;
            
            counter := counter + 1;
        ENDWHILE
        
        ! Finish
        MoveJ home, fast_speed, fine_zone, tool0;
        TPWrite "Program completed successfully";
    ENDPROC
    
    !***********************************************************
    ! Initialize robot systems
    !***********************************************************
    PROC initialize_robot()
        ! Set acceleration
        AccSet 50, 50;
        
        ! Configure speed override
        VelSet 100, 100;
        
        TPWrite "Robot initialized";
    ENDPROC
    
    !***********************************************************
    ! Perform operation at target position
    !***********************************************************
    PROC perform_operation(num cycle_count)
        VAR num result;
        
        ! Print current cycle
        TPWrite "Executing cycle: " \Num:=cycle_count;
        
        ! Simulate work
        SetDO DO_Gripper, 1;
        WaitTime 1.0;
        SetDO DO_Gripper, 0;
        
        ! Calculate result
        result := calculate_value(cycle_count, 10);
        TPWrite "Result: " \Num:=result;
    ENDPROC
    
    !***********************************************************
    ! Calculate a value based on inputs
    !***********************************************************
    FUNC num calculate_value(num input1, num input2)
        VAR num temp;
        
        temp := input1 * input2;
        IF temp > 100 THEN
            temp := 100;
        ENDIF
        
        RETURN temp;
    ENDFUNC
    
    !***********************************************************
    ! Emergency stop handler
    !***********************************************************
    TRAP emergency_stop
        TPWrite "EMERGENCY STOP ACTIVATED!";
        Stop;
        
        ! Reset all outputs
        SetDO DO_Gripper, 0;
        SetDO DO_Valve, 0;
    ENDTRAP
    
    !***********************************************************
    ! Test various control structures
    !***********************************************************
    PROC test_control_structures()
        VAR num i;
        VAR num test_val := 5;
        
        ! IF-ELSEIF-ELSE
        IF test_val < 5 THEN
            TPWrite "Value is less than 5";
        ELSEIF test_val = 5 THEN
            TPWrite "Value is equal to 5";
        ELSE
            TPWrite "Value is greater than 5";
        ENDIF
        
        ! FOR loop
        FOR i FROM 1 TO 10 STEP 2 DO
            TPWrite "FOR loop iteration: " \Num:=i;
        ENDFOR
        
        ! TEST-CASE-DEFAULT
        TEST test_val
            CASE 1:
                TPWrite "Case 1";
            CASE 5:
                TPWrite "Case 5";
            DEFAULT:
                TPWrite "Default case";
        ENDTEST
    ENDPROC
    
ENDMODULE
