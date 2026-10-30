import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { EmployeeApi } from './employee-api';

describe('EmployeeApi', () => {
  let api: EmployeeApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(EmployeeApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends only the criteria that are set', async () => {
    const result = firstValueFrom(
      api.list({ q: 'rao', country: 'IN', department: '', status: 'ACTIVE', page: 2, size: 50, sort: 'hireDate,desc' }),
    );

    const req = http.expectOne(r => r.url === '/api/employees');
    expect(req.request.params.toString()).toBe('q=rao&country=IN&status=ACTIVE&page=2&size=50&sort=hireDate,desc');
    req.flush({ content: [], page: { size: 50, number: 2, totalElements: 0, totalPages: 0 } });
    expect((await result).page.number).toBe(2);
  });

  it('loads filter options', async () => {
    const result = firstValueFrom(api.filters());

    http.expectOne('/api/employees/filters').flush({ countries: ['IN'], departments: ['Sales'], jobTitles: ['Recruiter'] });

    expect(await result).toEqual({ countries: ['IN'], departments: ['Sales'], jobTitles: ['Recruiter'] });
  });

  it('reaches each employee endpoint with the right method and body', () => {
    const details = { fullName: 'A', jobTitle: 'B', department: 'C', country: 'IN', workEmail: 'a@b.c', phone: null, hireDate: '2020-01-01' };

    api.get('e1').subscribe();
    api.create({ ...details, employeeCode: 'EMP-1' }).subscribe();
    api.update('e1', { ...details, version: 3 }).subscribe();
    api.terminate('e1', { terminationDate: '2025-01-31', version: 3 }).subscribe();
    api.salaries('e1').subscribe();
    api.addSalary('e1', { amount: 100, currency: 'INR', effectiveDate: '2025-04-01' }).subscribe();

    expect(http.expectOne({ method: 'GET', url: '/api/employees/e1' })).toBeTruthy();
    expect(http.expectOne({ method: 'POST', url: '/api/employees' }).request.body.employeeCode).toBe('EMP-1');
    expect(http.expectOne({ method: 'PUT', url: '/api/employees/e1' }).request.body.version).toBe(3);
    expect(http.expectOne({ method: 'POST', url: '/api/employees/e1/termination' }).request.body)
      .toEqual({ terminationDate: '2025-01-31', version: 3 });
    expect(http.expectOne({ method: 'GET', url: '/api/employees/e1/salaries' })).toBeTruthy();
    expect(http.expectOne({ method: 'POST', url: '/api/employees/e1/salaries' }).request.body.amount).toBe(100);
  });
});
