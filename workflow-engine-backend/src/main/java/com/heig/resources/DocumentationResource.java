package com.heig.resources;

import com.google.gson.*;
import com.heig.entities.documentation.DocumentClass;
import com.heig.entities.documentation.DocumentMethod;
import com.heig.entities.documentation.DocumentParameter;
import com.heig.entities.documentation.Documentation;
import com.heig.helpers.ResultOrStringError;
import io.smallrye.mutiny.tuples.Tuple2;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/documentation")
public class DocumentationResource {
    private static final Map<String, DocumentClass> documentation =
        Documentation
            .getDocumentation()
            .stream()
            .collect(
                Collectors.toMap(DocumentClass::name, Function.identity())
            );

    /**
     * Returns the json representation of the data to send to the client. The type will be "error" if the value is an error.
     * @param value The value
     * @return The json representation to send to the client
     */
    private static String toJson(ResultOrStringError<Tuple2<String, JsonElement>> value) {
        var gson = new Gson();
        var obj = new JsonObject();
        value.execute(v -> {
            obj.addProperty("type", v.getItem1());
            obj.add("value", v.getItem2());
        }, e -> {
            obj.addProperty("type", "error");
            obj.addProperty("value", e);
        });
        return gson.toJson(obj);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String documentation() {
        var arr = new JsonArray();
        for (var name: documentation.keySet()) {
            arr.add(name);
        }

        return toJson(ResultOrStringError.result(Tuple2.of("list", arr)));
    }

    @Path("{type}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String documentation(@PathParam("type") String type) {
        if (!documentation.containsKey(type)) {
            return toJson(ResultOrStringError.error("%s not found".formatted(type)));
        }
        var classDoc = documentation.get(type);
        return toJson(ResultOrStringError.result(Tuple2.of("class", toJson(classDoc))));
    }

    private static JsonObject toJson(DocumentParameter documentParameter) {
        var methodParam = new JsonObject();
        methodParam.addProperty("name", documentParameter.name());
        methodParam.addProperty("type", documentParameter.type());
        return methodParam;
    }

    private static JsonObject toJson(DocumentMethod documentMethod) {
        var classMethod = new JsonObject();
        classMethod.addProperty("name", documentMethod.name());
        classMethod.addProperty("type", documentMethod.type());
        classMethod.addProperty("comment", documentMethod.comment());

        var methodParams = new JsonArray();
        for (var param: documentMethod.parameters()) {
            methodParams.add(toJson(param));
        }
        classMethod.add("params", methodParams);
        return classMethod;
    }

    private static JsonObject toJson(DocumentClass documentClass) {
        var classObj = new JsonObject();
        classObj.addProperty("name", documentClass.name());
        classObj.addProperty("comment", documentClass.comment());

        var classMethods = new JsonArray();
        for (var method: documentClass.methods()) {
            var classMethod = toJson(method);

            classMethods.add(classMethod);
        }
        classObj.add("methods", classMethods);
        return classObj;
    }
}
