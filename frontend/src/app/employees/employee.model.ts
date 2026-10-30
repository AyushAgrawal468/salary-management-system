export type EmployeeStatus = 'ACTIVE' | 'TERMINATED';

/** Annual base salary in effect today. */
export interface CurrentSalary {
  amount: number;
  currency: string;
  effectiveDate: string;
}

export interface Employee {
  id: string;
  employeeCode: string;
  fullName: string;
  jobTitle: string;
  department: string;
  country: string;
  workEmail: string;
  phone: string | null;
  hireDate: string;
  status: EmployeeStatus;
  terminationDate: string | null;
  currentSalary: CurrentSalary | null;
  version: number;
}

export interface Page<T> {
  content: T[];
  page: { size: number; number: number; totalElements: number; totalPages: number };
}

export interface EmployeeFilters {
  countries: string[];
  departments: string[];
  jobTitles: string[];
}

export interface EmployeeQuery {
  q?: string;
  country?: string;
  department?: string;
  jobTitle?: string;
  status?: EmployeeStatus | '';
  page?: number;
  size?: number;
  /** "property,asc|desc", e.g. "hireDate,desc". */
  sort?: string;
}
