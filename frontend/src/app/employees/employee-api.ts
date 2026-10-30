import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Employee, EmployeeFilters, EmployeeQuery, Page } from './employee.model';

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
}
