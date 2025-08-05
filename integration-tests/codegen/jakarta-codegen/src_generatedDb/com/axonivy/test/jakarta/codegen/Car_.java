package com.axonivy.test.jakarta.codegen;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.axonivy.test.jakarta.codegen.Car}
 **/
@StaticMetamodel(Car.class)
public abstract class Car_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #brand
	 **/
	public static final String BRAND = "brand";
	
	/**
	 * @see #model
	 **/
	public static final String MODEL = "model";
	
	/**
	 * @see #release
	 **/
	public static final String RELEASE = "release";

	
	/**
	 * Static metamodel type for {@link com.axonivy.test.jakarta.codegen.Car}
	 **/
	public static volatile EntityType<Car> class_;
	
	/**
	 * Static metamodel for attribute {@link com.axonivy.test.jakarta.codegen.Car#id}
	 **/
	public static volatile SingularAttribute<Car, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.axonivy.test.jakarta.codegen.Car#brand}
	 **/
	public static volatile SingularAttribute<Car, String> brand;
	
	/**
	 * Static metamodel for attribute {@link com.axonivy.test.jakarta.codegen.Car#model}
	 **/
	public static volatile SingularAttribute<Car, String> model;
	
	/**
	 * Static metamodel for attribute {@link com.axonivy.test.jakarta.codegen.Car#release}
	 **/
	public static volatile SingularAttribute<Car, Integer> release;

}

