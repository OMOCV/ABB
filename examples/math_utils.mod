MODULE MathUtils
    !***********************************************************
    ! Math Utilities Module
    ! 数学工具模块 - 包含常用数学函数
    !***********************************************************
    
    ! 常量定义
    CONST num PI := 3.14159265359;
    CONST num E := 2.71828182846;
    
    !***********************************************************
    ! 计算两点之间的距离
    !***********************************************************
    FUNC num calculate_distance(robtarget p1, robtarget p2)
        VAR num dx;
        VAR num dy;
        VAR num dz;
        VAR num distance;
        
        dx := p2.trans.x - p1.trans.x;
        dy := p2.trans.y - p1.trans.y;
        dz := p2.trans.z - p1.trans.z;
        
        distance := Sqrt(dx * dx + dy * dy + dz * dz);
        
        RETURN distance;
    ENDFUNC
    
    !***********************************************************
    ! 将角度转换为弧度
    !***********************************************************
    FUNC num deg_to_rad(num degrees)
        RETURN degrees * PI / 180;
    ENDFUNC
    
    !***********************************************************
    ! 将弧度转换为角度
    !***********************************************************
    FUNC num rad_to_deg(num radians)
        RETURN radians * 180 / PI;
    ENDFUNC
    
    !***********************************************************
    ! 计算平均值
    !***********************************************************
    FUNC num calculate_average(num values{*})
        VAR num sum := 0;
        VAR num count := 0;
        VAR num i;
        
        FOR i FROM 1 TO Dim(values, 1) DO
            sum := sum + values{i};
            count := count + 1;
        ENDFOR
        
        IF count > 0 THEN
            RETURN sum / count;
        ELSE
            RETURN 0;
        ENDIF
    ENDFUNC
    
    !***********************************************************
    ! 线性插值
    !***********************************************************
    FUNC num lerp(num start_val, num end_val, num t)
        ! t 应该在 0 到 1 之间
        VAR num clamped_t;
        
        ! 限制 t 的范围
        IF t < 0 THEN
            clamped_t := 0;
        ELSEIF t > 1 THEN
            clamped_t := 1;
        ELSE
            clamped_t := t;
        ENDIF
        
        RETURN start_val + (end_val - start_val) * clamped_t;
    ENDFUNC
    
    !***********************************************************
    ! 检查数值是否在范围内
    !***********************************************************
    FUNC bool is_in_range(num value, num min_val, num max_val)
        RETURN value >= min_val AND value <= max_val;
    ENDFUNC
    
    !***********************************************************
    ! 限制数值在指定范围内
    !***********************************************************
    FUNC num clamp(num value, num min_val, num max_val)
        VAR num result;
        
        result := value;
        IF result < min_val THEN
            result := min_val;
        ENDIF
        IF result > max_val THEN
            result := max_val;
        ENDIF
        
        RETURN result;
    ENDFUNC
    
ENDMODULE
