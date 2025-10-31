MODULE PickAndPlace
    !***********************************************************
    ! Pick and Place Example
    ! 这是一个简单的拾取和放置示例程序
    !***********************************************************
    
    ! 工具数据
    PERS tooldata gripper := [TRUE, [[0, 0, 200], [1, 0, 0, 0]], [5, [0, 0, 50], [1, 0, 0, 0], 0, 0, 0]];
    
    ! 位置数据
    PERS robtarget pick_pos := [[500, 200, 300], [0, 0, 1, 0], [0, 0, 0, 0], [9E9, 9E9, 9E9, 9E9, 9E9, 9E9]];
    PERS robtarget place_pos := [[500, -200, 300], [0, 0, 1, 0], [0, 0, 0, 0], [9E9, 9E9, 9E9, 9E9, 9E9, 9E9]];
    PERS robtarget safe_height := [[500, 0, 500], [0, 0, 1, 0], [0, 0, 0, 0], [9E9, 9E9, 9E9, 9E9, 9E9, 9E9]];
    
    ! 运动参数
    CONST speeddata v_fast := [1000, 500, 5000, 1000];
    CONST speeddata v_slow := [200, 100, 1000, 200];
    CONST zonedata z_fine := fine;
    CONST zonedata z_smooth := z10;
    
    !***********************************************************
    ! 主程序 - 执行拾取和放置循环
    !***********************************************************
    PROC main()
        VAR num cycle_count := 0;
        
        ! 初始化
        TPWrite "启动拾取和放置程序";
        init_system;
        
        ! 移动到安全位置
        MoveJ safe_height, v_fast, z_smooth, gripper;
        
        ! 执行 10 次拾取和放置
        WHILE cycle_count < 10 DO
            TPWrite "执行循环 " \Num:=cycle_count;
            
            ! 拾取
            pick_part;
            
            ! 放置
            place_part;
            
            ! 增加计数
            cycle_count := cycle_count + 1;
        ENDWHILE
        
        ! 返回安全位置
        MoveJ safe_height, v_fast, z_fine, gripper;
        TPWrite "程序完成!";
    ENDPROC
    
    !***********************************************************
    ! 初始化系统
    !***********************************************************
    PROC init_system()
        ! 设置加速度
        AccSet 50, 50;
        
        ! 打开夹具
        open_gripper;
        
        TPWrite "系统初始化完成";
    ENDPROC
    
    !***********************************************************
    ! 拾取零件
    !***********************************************************
    PROC pick_part()
        ! 移动到拾取位置上方
        MoveJ RelTool(pick_pos, 0, 0, 100), v_fast, z_smooth, gripper;
        
        ! 下降到拾取位置
        MoveL pick_pos, v_slow, z_fine, gripper;
        
        ! 关闭夹具
        close_gripper;
        WaitTime 0.5;
        
        ! 提升
        MoveL RelTool(pick_pos, 0, 0, 100), v_slow, z_smooth, gripper;
    ENDPROC
    
    !***********************************************************
    ! 放置零件
    !***********************************************************
    PROC place_part()
        ! 移动到放置位置上方
        MoveJ RelTool(place_pos, 0, 0, 100), v_fast, z_smooth, gripper;
        
        ! 下降到放置位置
        MoveL place_pos, v_slow, z_fine, gripper;
        
        ! 打开夹具
        open_gripper;
        WaitTime 0.5;
        
        ! 提升
        MoveL RelTool(place_pos, 0, 0, 100), v_slow, z_smooth, gripper;
    ENDPROC
    
    !***********************************************************
    ! 打开夹具
    !***********************************************************
    PROC open_gripper()
        SetDO DO_Gripper, 0;
        WaitTime 0.2;
    ENDPROC
    
    !***********************************************************
    ! 关闭夹具
    !***********************************************************
    PROC close_gripper()
        SetDO DO_Gripper, 1;
        WaitTime 0.2;
    ENDPROC
    
    !***********************************************************
    ! 计算相对工具坐标位置
    !***********************************************************
    FUNC robtarget RelTool(robtarget point, num dx, num dy, num dz)
        VAR robtarget new_point;
        
        new_point := point;
        new_point.trans.x := point.trans.x + dx;
        new_point.trans.y := point.trans.y + dy;
        new_point.trans.z := point.trans.z + dz;
        
        RETURN new_point;
    ENDFUNC
    
ENDMODULE
