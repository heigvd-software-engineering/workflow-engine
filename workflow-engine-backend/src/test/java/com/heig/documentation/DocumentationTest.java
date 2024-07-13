package com.heig.documentation;

import com.heig.entities.workflow.execution.NodeArguments;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.Objects;

@QuarkusTest
public class DocumentationTest {
    @Test
    public void documentation() {
        var classes = Documentation.getDocumentation();
        assert !classes.isEmpty();
        assert classes.stream().anyMatch(c -> Objects.equals(c.name(), NodeArguments.class.getSimpleName()));
    }
}
