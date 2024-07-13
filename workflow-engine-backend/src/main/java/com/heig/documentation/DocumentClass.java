package com.heig.documentation;

import java.util.List;

public record DocumentClass(String name, List<DocumentMethod> methods, String comment) {
}
