import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { EmployeeDetailPage } from './employee-detail.page';
import { EmployeeApi } from './employee-api';
import { Employee, SalaryEntry } from './employee.model';
import { AuthService } from '../core/auth.service';

const ASHA: Employee = {
  id: 'a1', employeeCode: 'EMP-00001', fullName: 'Asha Rao', jobTitle: 'Software Engineer',
  department: 'Engineering', country: 'IN', workEmail: 'asha.rao@acme.com', phone: '+91 98765 43210',
  hireDate: '2022-04-01', status: 'ACTIVE', terminationDate: null,
  currentSalary: { amount: 2000000, currency: 'INR', effectiveDate: '2024-04-01' }, version: 4,
};
const HISTORY: SalaryEntry[] = [
  { id: 's2', amount: 2000000, currency: 'INR', effectiveDate: '2024-04-01' },
  { id: 's1', amount: 1600000, currency: 'INR', effectiveDate: '2022-04-01' },
];

describe('EmployeeDetailPage', () => {
  let api: Record<string, ReturnType<typeof vi.fn>>;
  let canEdit: boolean;

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'employees/:id', component: EmployeeDetailPage }]),
        { provide: EmployeeApi, useValue: api },
        { provide: AuthService, useValue: { canEdit: () => canEdit } },
      ],
    });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/employees/a1', EmployeeDetailPage);
    await harness.fixture.whenStable();
    return harness;
  }

  const el = (h: RouterTestingHarness) => h.routeNativeElement as HTMLElement;
  const button = (h: RouterTestingHarness, label: string) =>
    Array.from(el(h).querySelectorAll('button')).find(b => b.textContent!.trim().includes(label));

  async function fill(h: RouterTestingHarness, values: Record<string, string>) {
    for (const [name, value] of Object.entries(values)) {
      const input = el(h).querySelector<HTMLInputElement>(`input[name=${name}]`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    await h.fixture.whenStable();
  }

  async function click(h: RouterTestingHarness, label: string) {
    button(h, label)!.click();
    await h.fixture.whenStable();
  }

  beforeEach(() => {
    canEdit = true;
    api = {
      get: vi.fn(() => of(ASHA)),
      salaries: vi.fn(() => of(HISTORY)),
      update: vi.fn(),
      terminate: vi.fn(),
      addSalary: vi.fn(),
    };
  });

  it('shows the profile, current pay and pay history with each raise', async () => {
    const h = await open();
    const text = el(h).textContent!;

    expect(text).toContain('Asha Rao');
    expect(text).toContain('EMP-00001');
    expect(text).toContain('asha.rao@acme.com');
    expect(text).toContain('India');
    expect(text).toContain('₹2,000,000');
    expect(text).toContain('₹1,600,000');
    expect(text).toContain('+25.0%');
  });

  it('offers no changes to read-only users', async () => {
    canEdit = false;
    const h = await open();

    expect(button(h, 'Edit')).toBeUndefined();
    expect(button(h, 'Add raise')).toBeUndefined();
    expect(button(h, 'Terminate')).toBeUndefined();
  });

  it('saves edits with the version that was read', async () => {
    api['update'].mockReturnValue(of({ ...ASHA, fullName: 'Asha Menon', version: 5 }));
    const h = await open();

    await click(h, 'Edit');
    await fill(h, { fullName: 'Asha Menon' });
    await click(h, 'Save');

    expect(api['update']).toHaveBeenCalledWith('a1', expect.objectContaining({ fullName: 'Asha Menon', version: 4 }));
    expect(el(h).querySelector('h1')!.textContent).toContain('Asha Menon');
    expect(button(h, 'Save')).toBeUndefined();
  });

  it('explains a conflicting edit instead of overwriting it', async () => {
    api['update'].mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 409, error: { detail: 'Employee was modified by someone else; reload and try again' },
    })));
    const h = await open();

    await click(h, 'Edit');
    await click(h, 'Save');

    expect(el(h).querySelector('[role=alert]')!.textContent).toContain('modified by someone else');
    expect(button(h, 'Reload')).toBeDefined();
  });

  it('shows field errors from the server next to the field', async () => {
    api['update'].mockReturnValue(throwError(() => new HttpErrorResponse({
      status: 400, error: { detail: 'Request has invalid fields', errors: { workEmail: 'must be a well-formed email address' } },
    })));
    const h = await open();

    await click(h, 'Edit');
    await click(h, 'Save');

    expect(el(h).textContent).toContain('must be a well-formed email address');
  });

  it('records a raise and refreshes the history', async () => {
    api['addSalary'].mockReturnValue(of({ id: 's3', amount: 2200000, currency: 'INR', effectiveDate: '2025-04-01' }));
    const h = await open();
    api['salaries'].mockReturnValue(of([{ id: 's3', amount: 2200000, currency: 'INR', effectiveDate: '2025-04-01' }, ...HISTORY]));
    api['get'].mockReturnValue(of({ ...ASHA, version: 5, currentSalary: { amount: 2200000, currency: 'INR', effectiveDate: '2025-04-01' } }));

    await click(h, 'Add raise');
    await fill(h, { amount: '2200000', effectiveDate: '2025-04-01' });
    await click(h, 'Save raise');

    expect(api['addSalary']).toHaveBeenCalledWith('a1', { amount: 2200000, currency: 'INR', effectiveDate: '2025-04-01' });
    expect(el(h).textContent).toContain('₹2,200,000');
    expect(el(h).textContent).toContain('+10.0%');
  });

  it('terminates with a date and the version that was read', async () => {
    api['terminate'].mockReturnValue(of({ ...ASHA, status: 'TERMINATED', terminationDate: '2025-01-31', version: 5 }));
    const h = await open();

    await click(h, 'Terminate');
    await fill(h, { terminationDate: '2025-01-31' });
    await click(h, 'Confirm termination');

    expect(api['terminate']).toHaveBeenCalledWith('a1', { terminationDate: '2025-01-31', version: 4 });
    expect(el(h).textContent).toContain('Terminated');
    expect(button(h, 'Terminate')).toBeUndefined();
  });
});
