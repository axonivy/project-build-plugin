package com.acme;

import java.util.List;

import ch.ivyteam.ivy.environment.Ivy;
import hibernate.validator.Person;

public class PersonDao {

	public static List<Person> readPersons() {
		return Ivy.persistence().get("memory").findAll(Person.class);
	}
	
}
