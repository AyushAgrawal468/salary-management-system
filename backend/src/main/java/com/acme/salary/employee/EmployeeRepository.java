package com.acme.salary.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByEmployeeCode(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    @Query("select distinct e.country from Employee e order by e.country")
    List<String> distinctCountries();

    @Query("select distinct e.department from Employee e order by e.department")
    List<String> distinctDepartments();

    @Query("select distinct e.jobTitle from Employee e order by e.jobTitle")
    List<String> distinctJobTitles();
}
