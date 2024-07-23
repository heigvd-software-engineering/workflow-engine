package com.heig.entities.documentation;

import java.util.List;

public record DocumentClass(String name, List<DocumentMethod> methods, String comment) { }
