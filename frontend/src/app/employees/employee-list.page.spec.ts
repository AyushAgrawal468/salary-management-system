import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatSelectHarness } from '@angular/material/select/testing';
import { of } from 'rxjs';
import { EmployeeListPage } from './employee-list.page';
import { EmployeeApi } from './employee-api';
import { Employee, EmployeeQuery, Page } from './employee.model';
import { AuthService } from '../core/auth.service';

const ASHA: Employee = {
  id: 'a1', employeeCode: 'EMP-00001', fullName: 'Asha Rao', jobTitle: 'Senior Software Engineer',
  department: 'Engineering', country: 'IN', workEmail: 'asha.rao@acme.com', phone: null,
  hireDate: '2020-01-01', status: 'ACTIVE', terminationDate: null,
  currentSalary: { amount: 1850000, currency: 'INR', effectiveDate: '2025-01-01' }, version: 0,
};
const DEV: Employee = {
  ...ASHA, id: 'd4', employeeCode: 'EMP-00004', fullName: 'Dev Patel', country: 'US', status: 'TERMINATED',
  terminationDate: '2024-06-30', currentSalary: null,
};

function page(content: Employee[]): Page<Employee> {
  return { content, page: { size: 20, number: 0, totalElements: content.length, totalPages: 1 } };
}

describe('EmployeeListPage', () => {
  let list: ReturnType<typeof vi.fn>;
  let rows: Employee[];

  async function open(url: string) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'employees', component: EmployeeListPage }]),
        {
          provide: EmployeeApi,
          useValue: {
            list,
            filters: () => of({ countries: ['IN', 'US'], departments: ['Engineering', 'Sales'], jobTitles: ['Recruiter'] }),
          },
        },
        { provide: AuthService, useValue: { canEdit: () => true } },
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl(url, EmployeeListPage);
    await harness.fixture.whenStable();
    return harness;
  }

  const lastQuery = (): EmployeeQuery => list.mock.lastCall![0];
  const url = () => TestBed.inject(Router).url;

  beforeEach(() => {
    rows = [ASHA, DEV];
    list = vi.fn(() => of(page(rows)));
  });

  it('shows one row per employee with readable country, pay and status', async () => {
    const harness = await open('/employees');
    const text = (harness.routeNativeElement as HTMLElement).textContent!;

    expect(text).toContain('Asha Rao');
    expect(text).toContain('EMP-00001');
    expect(text).toContain('India');
    expect(text).toContain('₹1,850,000');
    expect(text).toContain('Dev Patel');
    expect(text).toContain('Terminated');
  });

  it('asks the API for what the URL describes', async () => {
    await open('/employees?q=rao&country=IN&status=ACTIVE&page=2&sort=hireDate,desc');

    expect(lastQuery()).toEqual(
      expect.objectContaining({ q: 'rao', country: 'IN', status: 'ACTIVE', page: 2, sort: 'hireDate,desc' }),
    );
  });

  it('puts the search in the URL and starts from the first page', async () => {
    const harness = await open('/employees?page=3');
    const input = (harness.routeNativeElement as HTMLElement).querySelector<HTMLInputElement>('input[type=search]')!;

    input.value = 'patel';
    input.dispatchEvent(new Event('input'));
    await new Promise(r => setTimeout(r, 400)); // debounce
    await harness.fixture.whenStable();

    expect(url()).toBe('/employees?q=patel');
    expect(lastQuery()).toEqual(expect.objectContaining({ q: 'patel', page: 0 }));
  });

  it('filters by country from the dropdown', async () => {
    const harness = await open('/employees?page=3');
    const loader = TestbedHarnessEnvironment.loader(harness.fixture);
    const country = await loader.getHarness(MatSelectHarness.with({ selector: '[aria-label="Country"]' }));

    await country.open();
    await country.clickOptions({ text: 'United States' });
    await harness.fixture.whenStable();

    expect(url()).toBe('/employees?country=US');
    expect(lastQuery()).toEqual(expect.objectContaining({ country: 'US', page: 0 }));
  });

  it('says so when nothing matches', async () => {
    rows = [];
    const harness = await open('/employees?q=zzz');

    expect((harness.routeNativeElement as HTMLElement).textContent).toContain('No employees match');
  });
});
