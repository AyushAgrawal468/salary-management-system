import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Employee, EmployeeFilters, EmployeeQuery, EmployeeUpdate, NewEmployee, NewSalary, Page, SalaryEntry, Termination,
} from './employee.model';

@Injectable({ providedIn: 'root' })
export class EmployeeApi {
  private readonly http = inject(HttpClient);

  list(query: EmployeeQuery): Observable<Page<Employee>> {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<Page<Employee>>('/api/employees', { params });
  }

  filters(): Observable<EmployeeFilters> {
    return this.http.get<EmployeeFilters>('/api/employees/filters');
  }

  get(id: string): Observable<Employee> {
    return this.http.get<Employee>(`/api/employees/${id}`);
  }

  create(employee: NewEmployee): Observable<Employee> {
    return this.http.post<Employee>('/api/employees', employee);
  }

  update(id: string, update: EmployeeUpdate): Observable<Employee> {
    return this.http.put<Employee>(`/api/employees/${id}`, update);
  }

  terminate(id: string, termination: Termination): Observable<Employee> {
    return this.http.post<Employee>(`/api/employees/${id}/termination`, termination);
  }

  salaries(id: string): Observable<SalaryEntry[]> {
    return this.http.get<SalaryEntry[]>(`/api/employees/${id}/salaries`);
  }

  addSalary(id: string, salary: NewSalary): Observable<SalaryEntry> {
    return this.http.post<SalaryEntry>(`/api/employees/${id}/salaries`, salary);
  }
}
