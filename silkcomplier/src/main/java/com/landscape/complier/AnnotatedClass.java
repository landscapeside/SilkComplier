package com.landscape.complier;

import com.landscape.model.RxBeanType;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

import rx.subjects.PublishSubject;

public class AnnotatedClass {

    public TypeElement mClassElement;
    public RxBeanType mType;
    public Elements mElementUtils;

    public AnnotatedClass(TypeElement classElement, Elements elementUtils) {
        this.mClassElement = classElement;
        this.mElementUtils = elementUtils;
    }

    public String getFullClassName() {
        return mClassElement.getQualifiedName().toString();
    }

    public void addClass(RxBeanType type) {
        mType = type;
    }

    public JavaFile generateFinder() throws Exception {
        List<MethodSpec> injectMethods = new ArrayList<>();
        // method inject(final T host, Object source, Provider provider)
        MethodSpec.Builder sendSilkMethodBuilder = MethodSpec.methodBuilder("sendTrigger")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.get(mClassElement.asType()), "bean")
                .addStatement("if(silkTrigger != null){silkTrigger.onNext(bean);}");

        // trigger setter
        MethodSpec.Builder triggerSetterBuilder = MethodSpec.methodBuilder("setSilkTrigger")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeUtil.PUBLISHSUBJECT, "silkTrigger")
                .addStatement("this.silkTrigger = silkTrigger");

        // add sendtrigger()
        for (int i = 0; i < mType.getMethodElement().size(); i++) {
            ExecutableElement element = mType.getMethodElement().get(i);
            if (element.getSimpleName().toString().startsWith("set")) {
                List<VariableElement> params = (List<VariableElement>) mType.getMethodElement().get(i).getParameters();
                if (params.size() > 0) {
                    String paramName = element.getSimpleName().toString().replace("set", "");
                    boolean find = false;
                    for (int j = 0; j < mType.getMethodElement().size(); j++) {
                        ExecutableElement element1 = mType.getMethodElement().get(j);
                        System.out.println("scan:" + element.getSimpleName());
                        if ((element1.getSimpleName().toString().startsWith("is") ||
                                element1.getSimpleName().toString().startsWith("get")) &&
                                element1.getSimpleName().toString()
                                        .replace("is", "")
                                        .replace("get", "").equals(paramName)) {
                            paramName = element1.getSimpleName().toString();
                            find = true;
                            break;
                        }
                    }
                    MethodSpec.Builder setterMethod = MethodSpec.methodBuilder(element.getSimpleName().toString())
                            .addModifiers(Modifier.PUBLIC)
                            .addAnnotation(Override.class)
                            .returns(TypeName.VOID)
                            .addParameter(TypeName.get(params.get(0).asType()), params.get(0).getSimpleName().toString());
                    if (find) {
                        setterMethod
                                .addStatement("silkNotify = ($N() != $N || ((Object)$N()).hashCode() != ((Object)$N).hashCode())",
                                        paramName, params.get(0).getSimpleName().toString(),
                                        paramName, params.get(0).getSimpleName().toString())
                                .addStatement("super.$N($N)", element.getSimpleName().toString(), params.get(0).getSimpleName().toString())
                                .addStatement("if(silkNotify){sendTrigger(this);}");
                    } else {
                        setterMethod
                                .addStatement("super.$N($N)", element.getSimpleName().toString(), params.get(0).getSimpleName().toString())
                                .addStatement("sendTrigger(this)");
                    }
                    injectMethods.add(
                            setterMethod.build());
                }
            }
        }
        // generate whole class
        TypeSpec.Builder finderClassBuilder = TypeSpec.classBuilder(mClassElement.getSimpleName() + "$$Subcriber")
                .superclass(TypeName.get(mClassElement.asType()))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(TypeUtil.SUBCRIBER, TypeName.get(mClassElement.asType())))
                .addMethod(sendSilkMethodBuilder.build())
                .addMethod(triggerSetterBuilder.build())
                .addField(PublishSubject.class, "silkTrigger")
                .addField(Boolean.class, "silkNotify");
        for (MethodSpec injectMethodSpec : injectMethods) {
            finderClassBuilder.addMethod(injectMethodSpec);
        }
        TypeSpec finderClass = finderClassBuilder.build();
        String packageName = mElementUtils.getPackageOf(mClassElement).getQualifiedName().toString();
        return JavaFile.builder(packageName, finderClass).build();
    }
}
