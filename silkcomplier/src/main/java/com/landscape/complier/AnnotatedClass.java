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
        MethodSpec.Builder sendSilkMethodBuilder = MethodSpec.methodBuilder("sendModeNotify")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeName.get(mClassElement.asType()), "bean")
                .addStatement("if(silkTrigger != null){silkTrigger.onNext(bean);}");

        // add notifyNode
        MethodSpec.Builder notifyNodeMethodBuilder = MethodSpec.methodBuilder("notifyNode")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(String.class, "tag")
                .addParameter(Object.class, "bean")
                .addStatement("if(nodeTrigger != null){nodeTrigger.onNext(new SilkMsg(preTag+$S+tag,bean));}", "::");

        // trigger setter
        MethodSpec.Builder triggerSetterBuilder = MethodSpec.methodBuilder("setSilkTrigger")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeUtil.PUBLISHSUBJECT, "silkTrigger")
                .addStatement("this.silkTrigger = silkTrigger");

        // trigger setter
        MethodSpec.Builder nodeTriggerSetterBuilder = MethodSpec.methodBuilder("setNodeTrigger")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(TypeUtil.PUBLISHSUBJECT, "nodeTrigger")
                .addStatement("this.nodeTrigger = nodeTrigger");

        // add sendtrigger()
        for (int i = 0; i < mType.getMethodElement().size(); i++) {
            ExecutableElement element = mType.getMethodElement().get(i);
            if (element.getSimpleName().toString().startsWith("set")) {
                List<VariableElement> params = (List<VariableElement>) mType.getMethodElement().get(i).getParameters();
                if (params.size() > 0) {
                    String getterName = element.getSimpleName().toString().replace("set", "");
                    boolean find = false;
                    for (int j = 0; j < mType.getMethodElement().size(); j++) {
                        ExecutableElement element1 = mType.getMethodElement().get(j);
                        if ((element1.getSimpleName().toString().startsWith("is") ||
                                element1.getSimpleName().toString().startsWith("get")) &&
                                element1.getSimpleName().toString()
                                        .replace("is", "")
                                        .replace("get", "").equals(getterName)) {
                            getterName = element1.getSimpleName().toString();
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
                                        getterName, params.get(0).getSimpleName().toString(),
                                        getterName, params.get(0).getSimpleName().toString())
                                .addStatement("super.$N($N)", element.getSimpleName().toString(), params.get(0).getSimpleName().toString())
                                .addStatement("if(silkNotify){notifyNode($S,$N);sendModeNotify(this);}",
                                        params.get(0).getSimpleName().toString(),
                                        params.get(0).getSimpleName().toString());
                    } else {
                        setterMethod
                                .addStatement("super.$N($N)", element.getSimpleName().toString(), params.get(0).getSimpleName().toString())
                                .addStatement("notifyNode($S,$N)",
                                        params.get(0).getSimpleName().toString(),
                                        params.get(0).getSimpleName().toString())
                                .addStatement("sendModeNotify(this)");
                    }
                    injectMethods.add(
                            setterMethod.build());
                }
            }
        }

        // add setPreTag
        MethodSpec.Builder setPreTagBuilder = MethodSpec.methodBuilder("setPreTag")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(String.class, "preTag")
                .addStatement("this.preTag = preTag");

        // add getPreTag
        MethodSpec.Builder getPreTagBuilder = MethodSpec.methodBuilder("getPreTag")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(String.class)
                .addStatement("return preTag");

        // generate whole class
        TypeSpec.Builder finderClassBuilder = TypeSpec.classBuilder(mClassElement.getSimpleName() + "$$Subcriber")
                .superclass(TypeName.get(mClassElement.asType()))
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(ParameterizedTypeName.get(TypeUtil.SUBCRIBER, TypeName.get(mClassElement.asType())))
                .addMethod(sendSilkMethodBuilder.build())
                .addMethod(notifyNodeMethodBuilder.build())
                .addMethod(triggerSetterBuilder.build())
                .addMethod(nodeTriggerSetterBuilder.build())
                .addMethod(setPreTagBuilder.build())
                .addMethod(getPreTagBuilder.build())
                .addField(PublishSubject.class, "silkTrigger")
                .addField(PublishSubject.class, "nodeTrigger")
                .addField(TypeUtil.SILK_MSG, "silkMsg")//just for import the package,fuck!
                .addField(String.class, "preTag")
                .addField(Boolean.class, "silkNotify");
        for (MethodSpec injectMethodSpec : injectMethods) {
            finderClassBuilder.addMethod(injectMethodSpec);
        }
        TypeSpec finderClass = finderClassBuilder.build();
        String packageName = mElementUtils.getPackageOf(mClassElement).getQualifiedName().toString();
        return JavaFile.builder(packageName, finderClass)
                .build();
    }
}
