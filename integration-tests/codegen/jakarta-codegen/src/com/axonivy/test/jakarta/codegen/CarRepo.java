package com.axonivy.test.jakarta.codegen;

import java.util.List;

import jakarta.data.Sort;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;

@Repository
public interface CarRepo extends CrudRepository<Car, Integer> {

  @Find
  List<Car> findByYear(Integer release, @SuppressWarnings("unchecked") Sort<Car>... order);

}
