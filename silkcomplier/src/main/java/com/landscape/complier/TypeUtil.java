package com.landscape.complier;

import com.squareup.javapoet.ClassName;

/**
 * Created by landscape on 2016-08-17.
 */
public class TypeUtil {
    public static final ClassName SUBCRIBER = ClassName.get("com.landscape", "BeanSupcriber");
    public static final ClassName PUBLISHSUBJECT = ClassName.get("rx.subjects", "PublishSubject");
    public static final ClassName ANNOTATION = ClassName.get("com.landscape", "RxBean");
}
