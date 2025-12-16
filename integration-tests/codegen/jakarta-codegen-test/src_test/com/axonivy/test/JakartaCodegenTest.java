package com.axonivy.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.util.List;

import jakarta.data.repository.BasicRepository;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Test;

import com.axonivy.test.jakarta.codegen.Car;
import com.axonivy.test.jakarta.codegen.CarRepo_;
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
    var repo = Car.repository();
    var session = sessionOf(repo);
    var cb = session.getCriteriaBuilder();

    var criteria = cb.createQuery(Car.class);
    var root = criteria.from(Car.class);

    criteria.select(root)
        .where(cb.equal(root.get(Car_.RELEASE), 2015));

    var query = session.createQuery(criteria);
    List<Car> cars2015 = query.getResultList();
    assertThat(cars2015)
        .isEmpty();
  }

  private static StatelessSession sessionOf(BasicRepository<?, ?> repo) {
    try {
      var handler = Proxy.getInvocationHandler(repo);
      var getSession = MethodUtils.getMatchingMethod(CarRepo_.class, "session");
      return (StatelessSession) handler.invoke(repo, getSession, new Object[0]);
    } catch (Throwable ex) {
      throw new RuntimeException("Failed to get session of " + repo, ex);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void useJakartaMetaModel() {
    var cars2025 = Car.repository().findByYear(2025, _Car.brand.asc(), _Car.model.desc());
    assertThat(cars2025).isEmpty();
  }

}
