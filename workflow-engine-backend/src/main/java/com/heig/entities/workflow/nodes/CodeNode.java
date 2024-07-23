package com.heig.entities.workflow.nodes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.Workflow;
import jakarta.annotation.Nonnull;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.wildfly.common.annotation.NotNull;

import java.io.*;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * The code node
 */
public class CodeNode extends ModifiableNode {
    /**
     * Used to convert a json representation to a {@link CodeNode}
     */
    public static class Deserializer extends Node.NodeDeserializer<CodeNode> {
        public Deserializer(int id, Workflow workflow) {
            super(id, workflow);
        }

        @Override
        public CodeNode deserialize(JsonElement value) throws JsonParseException {
            var obj = value.getAsJsonObject();

            var code = obj.get("code").getAsString();
            var language = Language.valueOf(obj.get("language").getAsString());

            var codeNode = new CodeNode(id, workflow);

            codeNode.code = code;
            codeNode.language = language;

            return codeNode;
        }
    }

    /**
     * Represents the language options
     */
    public enum Language {
        /**
         * Javascript
         */
        JS("js", "function main(inputs, outputs){%s}");

        /**
         * The language code used by GraalVM
         */
        private final String graalLanguageCode;

        /**
         * The template containing one %s to place the code written by the user
         */
        private final String mainCodeTemplate;

        Language(@Nonnull String graalLanguageCode, @Nonnull String mainCodeTemplate) {
            this.graalLanguageCode = Objects.requireNonNull(graalLanguageCode);
            this.mainCodeTemplate = Objects.requireNonNull(mainCodeTemplate);
        }

        public String completeMain(@NotNull String code) {
            return mainCodeTemplate.formatted(Objects.requireNonNull(code));
        }
    }

    private static final Engine engine = Engine.newBuilder()
        .option("engine.WarnInterpreterOnly", "false")
        .build();
    private static final Context.Builder contextBuilder = Context
        .newBuilder()
        .engine(engine)
        .allowHostAccess(HostAccess.ALL);

    /**
     * The code
     */
    private String code = "";

    /**
     * The language
     */
    private Language language = Language.JS;

    /**
     * The current context
     */
    private Context context = null;

    protected CodeNode(int id, @Nonnull Workflow workflow) {
        super(id, workflow);
    }

    @Override
    public NodeArguments execute(@Nonnull NodeArguments inputs, @Nonnull Consumer<String> logLine) {
        Objects.requireNonNull(inputs);

        //Creates a new context that writes its standard output to the logLine Consumer
        context = contextBuilder.out(new OutputStream() {
            private String current = "";
            @Override
            public synchronized void write(int b) {
                current += new String(new byte[] {(byte) b}, 0, 1);
                if (current.endsWith("\n")) {
                    logLine.accept(current);
                    current = "";
                }
            }
        }).build();
        var bindings = context.getBindings(language.graalLanguageCode);
        var outputs = new NodeArguments();

        context.eval(language.graalLanguageCode, language.completeMain(code));
        var mainFunc = bindings.getMember("main");
        //Executes the code
        mainFunc.execute(inputs, outputs);

        return outputs;
    }

    @Override
    public void cancel() {
        super.cancel();
        if (context != null) {
            try {
                context.close(true);
            } catch (Exception ignored) { }
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(@Nonnull String code) {
        if (!Objects.equals(this.code, code)) {
            this.code = Objects.requireNonNull(code);
            getWorkflow().nodeModified(this);
        }
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(@Nonnull Language language) {
        if (this.language != language) {
            this.language = Objects.requireNonNull(language);;
            getWorkflow().nodeModified(this);
        }
    }

    @Override
    public String toString() {
        return "Code" + super.toString();
    }

    @Override
    public JsonObject toJson() {
        var obj = super.toJson();
        obj.addProperty("code", code);
        obj.addProperty("language", language.name());
        return obj;
    }
}
