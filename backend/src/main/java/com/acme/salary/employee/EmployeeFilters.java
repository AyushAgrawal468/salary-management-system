package com.acme.salary.employee;

import java.util.List;

/** Values that actually occur in the data, for filter dropdowns. */
public record EmployeeFilters(List<String> countries, List<String> departments, List<String> jobTitles) {
}
