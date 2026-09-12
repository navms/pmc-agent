package io.github.navms.utils.enums;

/**
 * BaseEnum
 *
 * @author navms
 */
public interface BaseEnum<C> {

    /**
     * 获取枚举值
     */
    C getCode();

    /**
     * 获取枚举显示名称
     */
    String getName();

    static <E extends BaseEnum<C>, C> E of(Class<E> enumType, C code) {
        if (code == null) return null;
        for (E e : enumType.getEnumConstants()) {
            if (e.getCode().equals(code)) return e;
        }
        return null;
    }

}