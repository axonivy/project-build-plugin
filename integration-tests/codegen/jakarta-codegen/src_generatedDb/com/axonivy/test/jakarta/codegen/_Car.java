package com.axonivy.test.jakarta.codegen;

import jakarta.data.metamodel.SortableAttribute;
import jakarta.data.metamodel.StaticMetamodel;
import jakarta.data.metamodel.TextAttribute;
import jakarta.data.metamodel.impl.SortableAttributeRecord;
import jakarta.data.metamodel.impl.TextAttributeRecord;

/**
 * Jakarta Data static metamodel for {@link com.axonivy.test.jakarta.codegen.Car}
 **/
@StaticMetamodel(Car.class)
public interface _Car {

	
	/**
	 * @see #id
	 **/
	String ID = "id";
	
	/**
	 * @see #brand
	 **/
	String BRAND = "brand";
	
	/**
	 * @see #model
	 **/
	String MODEL = "model";
	
	/**
	 * @see #release
	 **/
	String RELEASE = "release";

	
	/**
	 * Static metamodel for attribute {@link Car#id}
	 **/
	SortableAttribute<Car> id = new SortableAttributeRecord<>(ID);
	
	/**
	 * Static metamodel for attribute {@link Car#brand}
	 **/
	TextAttribute<Car> brand = new TextAttributeRecord<>(BRAND);
	
	/**
	 * Static metamodel for attribute {@link Car#model}
	 **/
	TextAttribute<Car> model = new TextAttributeRecord<>(MODEL);
	
	/**
	 * Static metamodel for attribute {@link Car#release}
	 **/
	SortableAttribute<Car> release = new SortableAttributeRecord<>(RELEASE);

}

