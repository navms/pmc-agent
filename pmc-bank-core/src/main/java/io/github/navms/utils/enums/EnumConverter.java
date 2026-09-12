package io.github.navms.utils.enums;

import org.mapstruct.TargetType;

/**
 * 枚举转换器
 *
 * @author navms
 */
public class EnumConverter {

    public <E extends StringEnum> E string2Enum(String code, @TargetType Class<E> enumType) {
        if (code == null) return null;
        return BaseEnum.of(enumType, code);
    }

    public <E extends StringEnum> String enum2String(E enumValue) {
        return enumValue == null ? null : enumValue.getCode();
    }

    public <E extends IntegerEnum> E integer2Enum(Integer code, @TargetType Class<E> enumType) {
        if (code == null) return null;
        return BaseEnum.of(enumType, code);
    }

    public <E extends IntegerEnum> Integer enum2Integer(E enumValue) {
        return enumValue == null ? null : enumValue.getCode();
    }

}
