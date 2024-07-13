package com.heig.documentation;

import org.reflections.Reflections;

import java.util.LinkedList;
import java.util.List;

public class Documentation {
    public static List<DocumentClass> getDocumentation() {
        var reflections = new Reflections("com.heig");
        var types = reflections.getTypesAnnotatedWith(Document.class);
        var docClasses = new LinkedList<DocumentClass>();
        for (var type : types) {
            var typeAnnotation = type.getAnnotation(Document.class);
            var docMethods = new LinkedList<DocumentMethod>();
            var docClass = new DocumentClass(type.getSimpleName(), docMethods, typeAnnotation.value());
            for (var method : type.getDeclaredMethods()) {
                var docParameters = new LinkedList<DocumentParameter>();
                if (method.isAnnotationPresent(Document.class)) {
                    var methodAnnotation = method.getAnnotation(Document.class);
                    var docMethod = new DocumentMethod(method.getName(), method.getReturnType().getSimpleName(), docParameters, methodAnnotation.value());
                    for (var param : method.getParameters()) {
                        var docParameter = new DocumentParameter(param.getName(), param.getType().getSimpleName());
                        docParameters.add(docParameter);
                    }
                    docMethods.add(docMethod);
                }
            }
            docClasses.add(docClass);
        }
        return docClasses;
    }
}
