package com.heig.entities.workflow.nodes;

import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.Workflow;
import jakarta.annotation.Nonnull;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;

import java.util.Objects;

public class CodeNode extends ModifiableNode {
    public enum Language {
        JS("js", "function main(arguments, returnArguments){%s}");

        private final String graalLanguageCode;
        private final String mainCodeTemplate;
        Language(String graalLanguageCode, String mainCodeTemplate) {
            this.graalLanguageCode = graalLanguageCode;
            this.mainCodeTemplate = mainCodeTemplate;
        }

        public String getGraalLanguageCode() {
            return graalLanguageCode;
        }

        public String completeMain(String code) {
            return mainCodeTemplate.formatted(code);
        }
    }

    private String code = "";
    private Language language = Language.JS;

    protected CodeNode(int id, @Nonnull Workflow workflow) {
        super(id, workflow);
    }

    @Nonnull
    @Override
    public NodeArguments execute(@Nonnull NodeArguments arguments) {
        Objects.requireNonNull(arguments);
        var engine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        var contextBuilder = Context
                .newBuilder()
                .engine(engine)
                .allowHostAccess(HostAccess.ALL);
        try (var context = contextBuilder.build()) {
            var bindings = context.getBindings(language.graalLanguageCode);
            var returnArguments = new NodeArguments();

            context.eval(language.graalLanguageCode, language.completeMain(code));

            var mainFunc = bindings.getMember("main");
            mainFunc.execute(arguments, returnArguments);

            return returnArguments;
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(@Nonnull String code) {
        this.code = Objects.requireNonNull(code);
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(@Nonnull Language language) {
        this.language = Objects.requireNonNull(language);;
    }
}
