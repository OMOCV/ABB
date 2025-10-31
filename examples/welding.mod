MODULE Welding
    !***********************************************************
    ! Welding Application Example
    ! 焊接应用示例程序
    !***********************************************************
    
    ! 焊接参数
    PERS num weld_speed := 5.0;  ! mm/s
    PERS num weld_current := 180; ! A
    PERS bool weld_active := FALSE;
    
    ! 焊接位置
    PERS robtarget weld_start := [[400, 0, 200], [1, 0, 0, 0], [0, 0, 0, 0], [9E9, 9E9, 9E9, 9E9, 9E9, 9E9]];
    PERS robtarget weld_end := [[600, 0, 200], [1, 0, 0, 0], [0, 0, 0, 0], [9E9, 9E9, 9E9, 9E9, 9E9, 9E9]];
    
    !***********************************************************
    ! 主焊接程序
    !***********************************************************
    PROC main()
        TPWrite "开始焊接程序";
        
        ! 准备焊接
        prepare_welding;
        
        ! 执行焊接
        FOR i FROM 1 TO 3 DO
            TPWrite "焊接道次: " \Num:=i;
            weld_seam;
            IF i < 3 THEN
                WaitTime 2.0;  ! 冷却时间
            ENDIF
        ENDFOR
        
        ! 结束
        finish_welding;
        TPWrite "焊接完成";
    ENDPROC
    
    !***********************************************************
    ! 准备焊接
    !***********************************************************
    PROC prepare_welding()
        ! 移动到起始位置上方
        MoveJ Offs(weld_start, 0, 0, 100), v500, z10, tool0;
        
        ! 设置焊接参数
        set_weld_parameters weld_speed, weld_current;
        
        TPWrite "焊接准备就绪";
    ENDPROC
    
    !***********************************************************
    ! 焊接焊缝
    !***********************************************************
    PROC weld_seam()
        ! 移动到起始位置
        MoveL weld_start, v100, fine, tool0;
        
        ! 开始焊接
        start_weld;
        
        ! 焊接过程
        MoveL weld_end, v50, fine, tool0;
        
        ! 停止焊接
        stop_weld;
        
        ! 移动到安全位置
        MoveL Offs(weld_end, 0, 0, 100), v200, z10, tool0;
    ENDPROC
    
    !***********************************************************
    ! 设置焊接参数
    !***********************************************************
    PROC set_weld_parameters(num speed, num current)
        ! 这里设置焊接机的参数
        SetAO AO_WeldSpeed, speed;
        SetAO AO_WeldCurrent, current;
        
        TPWrite "焊接速度: " \Num:=speed;
        TPWrite "焊接电流: " \Num:=current;
    ENDPROC
    
    !***********************************************************
    ! 开始焊接
    !***********************************************************
    PROC start_weld()
        SetDO DO_WeldStart, 1;
        weld_active := TRUE;
        WaitTime 0.5;  ! 等待电弧稳定
        TPWrite "焊接开始";
    ENDPROC
    
    !***********************************************************
    ! 停止焊接
    !***********************************************************
    PROC stop_weld()
        SetDO DO_WeldStart, 0;
        weld_active := FALSE;
        WaitTime 0.3;  ! 等待电弧熄灭
        TPWrite "焊接停止";
    ENDPROC
    
    !***********************************************************
    ! 结束焊接
    !***********************************************************
    PROC finish_welding()
        ! 确保焊接已停止
        IF weld_active THEN
            stop_weld;
        ENDIF
        
        ! 移动到安全位置
        MoveJ home, v500, z10, tool0;
    ENDPROC
    
    !***********************************************************
    ! 紧急停止陷阱
    !***********************************************************
    TRAP emergency_stop
        ! 立即停止焊接
        IF weld_active THEN
            SetDO DO_WeldStart, 0;
            weld_active := FALSE;
        ENDIF
        
        TPWrite "紧急停止触发!";
        Stop;
    ENDTRAP
    
ENDMODULE
