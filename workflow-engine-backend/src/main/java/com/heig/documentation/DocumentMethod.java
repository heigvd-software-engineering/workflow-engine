package com.heig.documentation;

import java.util.List;

public record DocumentMethod(String name, String type, List<DocumentParameter> parameters, String comment) { }
