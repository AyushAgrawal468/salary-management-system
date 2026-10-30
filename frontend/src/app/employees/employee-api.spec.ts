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
});
