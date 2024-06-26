package com.heig.entities.workflow.types;

/**
 * Interface used to tell which types are allowed as Map and Collection generic parameter
 * Example: Collection of WFlow is not allowed to exist. Same for a Map with WFlow as key or value type
 */
public interface WIterableType extends WType { }
