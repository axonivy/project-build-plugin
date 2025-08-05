package com.axonivy.test.jakarta.codegen;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.exceptions.DataException;
import jakarta.data.exceptions.EmptyResultException;
import jakarta.data.exceptions.EntityExistsException;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.page.impl.PageRecord;
import jakarta.inject.Inject;
import jakarta.persistence.NoResultException;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.PersistenceException;
import java.util.List;
import static java.util.Objects.requireNonNull;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.stream.Stream;
import org.hibernate.StaleStateException;
import org.hibernate.StatelessSession;
import org.hibernate.exception.ConstraintViolationException;
import static org.hibernate.query.Order.asc;
import static org.hibernate.query.SortDirection.*;
import org.hibernate.query.specification.SelectionSpecification;

/**
 * Implements Jakarta Data repository {@link com.axonivy.test.jakarta.codegen.CarRepo}
 **/
public class CarRepo_ implements CarRepo {


	
	protected StatelessSession session;
	
	@Inject
	public CarRepo_(StatelessSession session) {
		this.session = session;
	}
	
	public StatelessSession session() {
		return session;
	}
	
	@Override
	public Car insert(Car arg0) {
		requireNonNull(arg0, "Null arg0");
		try {
			session.insert(arg0);
		}
		catch (ConstraintViolationException _ex) {
			throw new EntityExistsException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return arg0;
	}
	
	@Override
	public List insertAll(List arg0) {
		requireNonNull(arg0, "Null arg0");
		try {
			session.insertMultiple(arg0);
		}
		catch (ConstraintViolationException _ex) {
			throw new EntityExistsException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return arg0;
	}
	
	@Override
	public Car update(Car arg0) {
		requireNonNull(arg0, "Null arg0");
		try {
			session.update(arg0);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return arg0;
	}
	
	@Override
	public List updateAll(List arg0) {
		requireNonNull(arg0, "Null arg0");
		try {
			session.updateMultiple(arg0);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return arg0;
	}
	
	@Override
	public void delete(Car arg0) {
		requireNonNull(arg0, "Null arg0");
		try {
			session.delete(arg0);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	@Override
	public void deleteAll(List<? extends Car> arg0) {
		requireNonNull(arg0, "Null arg0");
		try {
			session.deleteMultiple(arg0);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	@Override
	public Car save(Car arg0) {
		requireNonNull(arg0, "Null arg0");
		try {
			if (session.getIdentifier(arg0) == null)
				session.insert(arg0);
			else
				session.upsert(arg0);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return arg0;
	}
	
	@Override
	public List saveAll(List arg0) {
		requireNonNull(arg0, "Null arg0");
		try {
			session.upsertMultiple(arg0);
		}
		catch (StaleStateException _ex) {
			throw new OptimisticLockingFailureException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
		return arg0;
	}
	
	/**
	 * Find {@link Car} by {@link Car#release release}.
	 *
	 * @see com.axonivy.test.jakarta.codegen.CarRepo#findByYear(Integer,Sort)
	 **/
	@Override
	public List<Car> findByYear(Integer release, Sort<Car>... order) {
		var _builder = session.getCriteriaBuilder();
		var _query = _builder.createQuery(Car.class);
		var _entity = _query.from(Car.class);
		_query.where(
				release==null
					? _entity.get(Car_.release).isNull()
					: _builder.equal(_entity.get(Car_.release), release)
		);
		var _spec = SelectionSpecification.create(_query);
		for (var _sort : order) {
			_spec.sort(asc(Car.class, _sort.property())
						.reversedIf(_sort.isDescending())
						.ignoringCaseIf(_sort.ignoreCase()));
		}
		try {
			return _spec.createQuery(session)
				.getResultList();
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Delete {@link Car} by {@link Car#id id}.
	 *
	 * @see com.axonivy.test.jakarta.codegen.CarRepo#deleteById(Integer)
	 **/
	@Override
	public void deleteById(Integer id) {
		requireNonNull(id, "Null id");
		var _builder = session.getCriteriaBuilder();
		var _query = _builder.createCriteriaDelete(Car.class);
		var _entity = _query.from(Car.class);
		_query.where(
				_builder.equal(_entity.get(Car_.id), id)
		);
		try {
			session.createMutationQuery(_query)
				.executeUpdate();
		}
		catch (NoResultException _ex) {
			throw new EmptyResultException(_ex.getMessage(), _ex);
		}
		catch (NonUniqueResultException _ex) {
			throw new jakarta.data.exceptions.NonUniqueResultException(_ex.getMessage(), _ex);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Find {@link Car}.
	 *
	 * @see com.axonivy.test.jakarta.codegen.CarRepo#findAll()
	 **/
	@Override
	public Stream<Car> findAll() {
		var _builder = session.getCriteriaBuilder();
		var _query = _builder.createQuery(Car.class);
		var _entity = _query.from(Car.class);
		_query.where(
		);
		try {
			return session.createSelectionQuery(_query)
				.getResultStream();
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Find {@link Car}.
	 *
	 * @see com.axonivy.test.jakarta.codegen.CarRepo#findAll(PageRequest,Order)
	 **/
	@Override
	public Page<Car> findAll(PageRequest arg0, Order<Car> arg1) {
		var _builder = session.getCriteriaBuilder();
		var _query = _builder.createQuery(Car.class);
		var _entity = _query.from(Car.class);
		_query.where(
		);
		var _spec = SelectionSpecification.create(_query);
		for (var _sort : arg1.sorts()) {
			_spec.sort(asc(Car.class, _sort.property())
						.reversedIf(_sort.isDescending())
						.ignoringCaseIf(_sort.ignoreCase()));
		}
		try {
			long _totalResults = 
					arg0.requestTotal()
							? _spec.createQuery(session)
									.getResultCount()
							: -1;
			var _results = _spec.createQuery(session)
				.setFirstResult((int) (arg0.page()-1) * arg0.size())
				.setMaxResults(arg0.size())
				.getResultList();
			return new PageRecord<>(arg0, _results, _totalResults);
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}
	
	/**
	 * Find {@link Car} by {@link Car#id id}.
	 *
	 * @see com.axonivy.test.jakarta.codegen.CarRepo#findById(Integer)
	 **/
	@Override
	public Optional<Car> findById(Integer id) {
		requireNonNull(id, "Null id");
		try {
			return ofNullable(session.get(Car.class, id));
		}
		catch (PersistenceException _ex) {
			throw new DataException(_ex.getMessage(), _ex);
		}
	}

}

