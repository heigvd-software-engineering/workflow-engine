package com.heig.entities.workflow.execution;

import com.heig.documentation.Document;
import jakarta.annotation.Nonnull;

import java.util.*;

@Document("""
    This class is used to pass data from and to the Code node executor.
    You can use :
    - "inputs" to access the node parameters
    - "outputs" to specify the values of each node parameter to return
    Example :
    The node has 1 input named "number" and an output named "squared"
    Code (in JS here) :
    let n = inputs.get("number");
    outputs.put("squared", n * n);
    """)
public class NodeArguments {
    private final Map<String, Object> arguments = new HashMap<>();
    public void putArgument(@Nonnull String name, @Nonnull Object value) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(name);
        arguments.put(name, value);
    }

    public Optional<Object> getArgument(@Nonnull String name) {
        Objects.requireNonNull(name);
        return Optional.ofNullable(arguments.get(name));
    }

    public Map<String, Object> getArguments() {
        return Collections.unmodifiableMap(arguments);
    }

    @Document("""
        Used to set the value of a parameter
        If name or value is null, fails the node execution
        """)
    public void put(String name, Object value) {
        if (name == null || value == null) {
            throw new RuntimeException("The argument name or value cannot be null");
        }
        putArgument(name, value);
    }

    @Document("""
        Used to get the value of a parameter
        If name is null or if the argument is not found, fails the node execution
        """)
    public Object get(String name) {
        if (name == null) {
            throw new RuntimeException("The argument name cannot be null");
        }
        return getArgument(name).orElseThrow(() -> new RuntimeException("The argument " + name + " is not present"));
    }
}
