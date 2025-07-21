package com.axonivy.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hibernate.query.Query;
import org.junit.jupiter.api.Test;

import com.axonivy.test.jakarta.codegen.Car;
import com.axonivy.test.jakarta.codegen.Car_;
import com.axonivy.test.jakarta.codegen._Car;

import ch.ivyteam.ivy.environment.IvyTest;

@IvyTest
class JakartaCodegenTest {

  @Test
  void useRepo() {
    List<Car> cars = Car.repository().findAll().toList();
    assertThat(cars).isEmpty();
  }

  @Test
  void useHibernateMetaModel() {
    var session = ((com.axonivy.test.jakarta.codegen.CarRepo_) Car.repository()).session();
    var cb = session.getCriteriaBuilder();

    var criteria = cb.createQuery(Car.class);
    var root = criteria.from(Car.class);

    criteria.select(root)
        .where(cb.equal(root.get(Car_.RELEASE), 2015));

    Query<Car> query = session.createQuery(criteria);
    List<Car> cars2015 = query.getResultList();
    assertThat(cars2015)
        .isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void useJakartaMetaModel() {
    var cars2025 = Car.repository().findByYear(2025, _Car.brand.asc(), _Car.model.desc());
    assertThat(cars2025).isEmpty();
  }

}
