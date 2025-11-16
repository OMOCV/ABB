MODULE MainModule
! 测试文件：基于问题陈述中的示例
! 这个文件包含了从问题陈述中提到的所有关键字/指令的不完整版本

! 变量声明 - 测试不完整的声明关键字

! 正确的声明（不应报错）
VAR robtarget Target_10;
VAR robtarget Target_20;
VAR robtarget Target_30;
PERS speeddata v1000:=[1000,500,5000,1000];
CONST num MaxSpeed:=1000;

! 错误的声明（应该检测到）
! VA robtarget Target_40;
! PER speeddata v2000:=[2000,500,5000,1000];
! CONS num MinSpeed:=100;

PROC Main()
    ! 正确的指令
    WHILE TRUE DO
        WaitTime 0.5;
    ENDWHILE
ENDPROC

PROC Initialize()
    ! 设置数字输出
    SetDO DO_Gripper, 0;
    SetDO DO_Conveyor, 0;
    
    ! 移动到初始位置
    MoveAbsJ Home\NoEOffs, v1000, fine, tool0;
    
    TPWrite "系统初始化完成";
ENDPROC

FUNC num CheckDistance(robtarget p1, robtarget p2)
    VAR num distance;
    
    distance := Distance(p1.trans, p2.trans);
    
    RETURN distance;
ENDFUNC

! 测试区域：包含不完整关键字的版本（这些应该被检测到）

PROC TestIncompleteKeywords()
    ! 测试不完整的 VAR
    VA num test1;
    
    ! 测试不完整的 PERS
    PER num test2;
    
    ! 测试不完整的 CONST
    CONS num test3 := 10;
    
    ! 测试不完整的 WaitTime
    WaitTim 1.0;
    
    ! 测试不完整的 TPWrite
    TPWrit "测试";
    
    ! 测试不完整的 MoveAbsJ
    MoveAbs Home\NoEOffs, v1000, fine, tool0;
    
    ! 测试不完整的 RETURN (在函数中)
    ! （注意：这个在PROC中不会报RETURN缺少返回值的错误，但会报关键字不完整）
    RETUR;
    
    ! 测试不完整的 WHILE
    WHIL test1 < 10 DO
        test1 := test1 + 1;
    ENDWHILE
    
ENDPROC

ENDMODULE
