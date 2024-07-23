package com.heig.entities.documentation;

import org.reflections.Reflections;

import java.util.LinkedList;
import java.util.List;

/**
 * Static class used to generate the dynamic documentation
 */
public class Documentation {
    /**
     * Generates the dynamic documentation for classes annotated with {@link Document}
     * @return The dynamic documentation
     */
    public static List<DocumentClass> getDocumentation() {
        var reflections = new Reflections("com.heig");
        var types = reflections.getTypesAnnotatedWith(Document.class);
        var docClasses = new LinkedList<DocumentClass>();

        //Loop over each class annotated with Document
        for (var type : types) {
            var typeAnnotation = type.getAnnotation(Document.class);
            var docMethods = new LinkedList<DocumentMethod>();
            var docClass = new DocumentClass(getTypeName(type), docMethods, typeAnnotation.value());

            //Loop over each method
            for (var method : type.getDeclaredMethods()) {
                var docParameters = new LinkedList<DocumentParameter>();

                //Only the methods annotated with Document are useful
                if (method.isAnnotationPresent(Document.class)) {
                    var methodAnnotation = method.getAnnotation(Document.class);
                    var docMethod = new DocumentMethod(method.getName(), getTypeName(method.getReturnType()), docParameters, methodAnnotation.value());

                    //Loop over each parameter of the method
                    for (var param : method.getParameters()) {
                        var docParameter = new DocumentParameter(param.getName(), getTypeName(param.getType()));
                        docParameters.add(docParameter);
                    }
                    docMethods.add(docMethod);
                }
            }
            docClasses.add(docClass);
        }
        return docClasses;
    }

    /**
     * Returns the name to show for the type specified
     * @param paramType The type
     * @return The name to show
     */
    private static String getTypeName(Class<?> paramType) {
        return paramType.getSimpleName();
    }
}
