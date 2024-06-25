package com.heig.cache;

import com.google.common.collect.Sets;
import com.heig.entities.workflow.WorkflowManager;
import com.heig.entities.workflow.cache.Cache;
import com.heig.entities.workflow.execution.NodeArguments;
import com.heig.entities.workflow.nodes.PrimitiveNode;
import com.heig.entities.workflow.types.WCollection;
import com.heig.entities.workflow.types.WMap;
import com.heig.entities.workflow.types.WObject;
import com.heig.entities.workflow.types.WPrimitive;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.*;

@QuarkusTest
public class CacheTest {
    @BeforeAll
    public static void init() {
        //Delete everything if there was still a cache directory
        Cache.clearAll();
    }

    @Test
    public void onlyOneInstance() {
        var w = WorkflowManager.createWorkflow("test-w");
        assert Cache.get(w) == Cache.get(w);
    }

    @Test
    public void test() {
        var w = WorkflowManager.createWorkflow("test-w");
        var node = w.getNodeBuilder().buildPrimitiveNode(WCollection.of(WObject.of()));
        var cache = Cache.get(w);
        var arguments = new NodeArguments();
        arguments.putArgument(PrimitiveNode.OUTPUT_NAME, List.of(1, "a", (byte) 0));

        //Set the value in the cache
        cache.set(node, arguments);

        var resReturn = cache.get(node);
        assert resReturn.isPresent();
        var outputArg = resReturn.get().getArgument(PrimitiveNode.OUTPUT_NAME);
        assert outputArg.isPresent();

        //Verifying if we have a list with the correct values
        if (outputArg.get() instanceof List<?> lst) {
            assert lst.get(0) instanceof Integer i && i == 1;
            assert lst.get(1) instanceof String s && s.equals("a");
            assert lst.get(2) instanceof Byte b && b == (byte) 0;
        } else {
            assert false;
        }
    }

    @Test
    public void testCodeNode() {
        var w = WorkflowManager.createWorkflow("test-w");
        var node = w.getNodeBuilder().buildCodeNode();
        var cache = Cache.get(w);
        var arguments = new NodeArguments();
        var outInt = node.getConnectorBuilder().buildOutputConnector("outInt", WPrimitive.Integer);
        arguments.putArgument(outInt.getName(), 7);

        var outListDouble = node.getConnectorBuilder().buildOutputConnector("outListDouble", WCollection.of(WPrimitive.Double));
        arguments.putArgument(outListDouble.getName(), List.of(0.0, 1.2, 5.6));

        var outMapIntString = node.getConnectorBuilder().buildOutputConnector("outMapIntString", WMap.of(WPrimitive.Integer, WPrimitive.String));
        arguments.putArgument(outMapIntString.getName(), Map.of(1, "test", 3, "test2"));

        //Set the value in the cache
        cache.set(node, arguments);

        var resReturn = cache.get(node);
        assert resReturn.isPresent();

        var outputArg = resReturn.get().getArgument("outInt");
        assert outputArg.isPresent();
        assert outputArg.get() instanceof Integer i && i == 7;

        outputArg = resReturn.get().getArgument("outListDouble");
        assert outputArg.isPresent();
        assert outputArg.get() instanceof List<?> lst
                && lst.get(0) instanceof Double d && d == 0.0
                && lst.get(1) instanceof Double d2 && d2 == 1.2
                && lst.get(2) instanceof Double d3 && d3 == 5.6;

        outputArg = resReturn.get().getArgument("outMapIntString");
        assert outputArg.isPresent();
        assert outputArg.get() instanceof Map<?, ?> map
                && map.get(1) instanceof String s && s.equals("test")
                && map.get(3) instanceof String s2 && s2.equals("test2");

        //After clearing the cache for the workflow, we should not have any cache result anymore
        cache.clear();
        assert cache.get(node).isEmpty();
    }

    @Test
    public void testSpecific() {
        var w = WorkflowManager.createWorkflow("test-w");
        var node = w.getNodeBuilder().buildPrimitiveNode(WCollection.of(WPrimitive.String));
        var cache = Cache.get(w);
        var arguments = new NodeArguments();
        arguments.putArgument(PrimitiveNode.OUTPUT_NAME, Sets.newHashSet("test1", "test2"));

        //Set the value in the cache
        cache.set(node, arguments);

        var resReturn = cache.get(node);
        assert resReturn.isPresent();
        var outputArg = resReturn.get().getArgument(PrimitiveNode.OUTPUT_NAME);
        assert outputArg.isPresent();

        //With this test we verify that the underlying type is still a set
        if (outputArg.get() instanceof Collection<?> c) {
            var cs = (Collection<String>)c;
            assert cs.size() == 2;
            cs.add("test3");
            assert c.size() == 3;
            cs.add("test2");
            assert cs.size() == 3;
        } else {
            assert false;
        }
    }

    @AfterAll
    public static void delete() {
        //Delete everything if there was still a cache directory
        Cache.clearAll();
    }
}
