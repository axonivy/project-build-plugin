package hibernate.validator.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.acme.PersonDao;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.environment.IvyTest;
import hibernate.validator.Person;


@IvyTest
public class IvyValidatorFactoryTest{
  
  @BeforeEach
  void setup() {
    Ivy.persistence().get("memory").createEntityManager().clear();
  }

  /** 
   * ISSUE XIVY-12678 IvyTest runtime error because of non-breakable circular DI of IvyValidatorFactory
   */
  @Test
  void resolveEntities(){
    List<Person> persons = PersonDao.readPersons();
    assertThat(persons)
      .as("entity manager can be created; without throwing DI errors due to circular deps to IvyValidatorFactory")
      .isNotNull();
  }
  
}
