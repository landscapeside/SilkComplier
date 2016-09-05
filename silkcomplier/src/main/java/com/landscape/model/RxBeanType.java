package com.landscape.model;

import com.landscape.annotation.RxBean;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;

/**
 * Created by landscape on 2016-08-16.
 */
public class RxBeanType {
    private TypeElement element;
    private List<ExecutableElement> mMethodElement = new ArrayList<>();

    public RxBeanType(Element element) throws IllegalArgumentException {
        if (element.getKind() != ElementKind.CLASS) {
            throw new IllegalArgumentException(
                String.format("Only class can be annotated with @%s", RxBean.class.getSimpleName()));
        }
        this.element = (TypeElement) element;
        List<? extends Element> allMembers = this.element.getEnclosedElements();
        if (allMembers.size() > 0) {
            mMethodElement.addAll(ElementFilter.methodsIn(allMembers));
        }
    }

    public List<ExecutableElement> getMethodElement() {
        return mMethodElement;
    }

    public Name getFieldName() {
        return element.getSimpleName();
    }

    public TypeMirror getFieldType() {
        return element.asType();
    }
}
