import { expect, test, type Page } from '@playwright/test';

const SAMPLE_EMAIL = 'demo@agentcrm.app';
const SAMPLE_PASSWORD = 'Demo@123';

async function clearSession(page: Page) {
  await page.addInitScript(() => {
    localStorage.removeItem('auth_token');
  });
  await page.context().clearCookies();
}

async function logoutFromApp(page: Page) {
  await page.getByRole('button', { name: /Account menu/ }).click();
  await page.getByRole('menuitem', { name: 'Log out' }).click();
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible({ timeout: 15_000 });
}

test.describe('Auth form controlled inputs', () => {
  test('typed and filled values stay visible, signup works, and redirect is restored', async ({ page }) => {
    const loginBodies: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('/auth/login')) {
        loginBodies.push(request.postData() ?? '');
      }
    });

    await clearSession(page);
    await page.goto('/login?redirect=/leads', { waitUntil: 'load' });
    await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible({ timeout: 15_000 });

    const loginEmail = page.getByLabel('Email', { exact: true });
    const loginPassword = page.getByLabel('Password', { exact: true });

    await loginEmail.pressSequentially('user@example.com', { delay: 20 });
    await expect(loginEmail).toHaveValue('user@example.com');

    await loginPassword.pressSequentially('VisiblePass1', { delay: 20 });
    await expect(loginPassword).toHaveValue('VisiblePass1');
    expect(loginBodies).toEqual([]);

    await page.reload({ waitUntil: 'load' });
    await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible({ timeout: 15_000 });

    await page.getByRole('button', { name: 'Fill credentials' }).click();
    await expect(page.getByLabel('Email', { exact: true })).toHaveValue(SAMPLE_EMAIL);
    await expect(page.getByLabel('Password', { exact: true })).toHaveValue(SAMPLE_PASSWORD);
    await expect(page).toHaveURL(/\/login/);
    expect(loginBodies).toEqual([]);

    const stamp = Date.now();
    const newName = `E2E User ${stamp}`;
    const newEmail = `e2e.user.${stamp}@example.com`;
    const newPassword = 'VisiblePass1';

    await page.goto('/signup', { waitUntil: 'load' });
    await expect(page.getByRole('heading', { name: 'Create an account' })).toBeVisible({ timeout: 15_000 });

    const fullName = page.getByLabel('Full name');
    const signupEmail = page.getByLabel('Email', { exact: true });
    const signupPassword = page.getByLabel('Password', { exact: true });
    const confirmPassword = page.getByLabel('Confirm password', { exact: true });

    await fullName.pressSequentially(newName, { delay: 15 });
    await signupEmail.pressSequentially(newEmail, { delay: 15 });
    await signupPassword.pressSequentially(newPassword, { delay: 15 });
    await confirmPassword.pressSequentially(newPassword, { delay: 15 });

    await expect(fullName).toHaveValue(newName);
    await expect(signupEmail).toHaveValue(newEmail);
    await expect(signupPassword).toHaveValue(newPassword);
    await expect(confirmPassword).toHaveValue(newPassword);

    await page.getByLabel(/terms of use/).check();
    await page.getByRole('button', { name: 'Create account' }).click();
    await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });
    await expect(page.getByRole('heading', { name: 'Dashboard' })).toBeVisible();

    await logoutFromApp(page);

    await page.goto('/leads', { waitUntil: 'load' });
    await expect(page).toHaveURL(/\/login\?redirect=/);
    await page.getByLabel('Email', { exact: true }).fill(newEmail);
    await page.getByLabel('Password', { exact: true }).fill(newPassword);
    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page).toHaveURL(/\/leads/, { timeout: 15_000 });
    await expect(page.getByRole('heading', { name: 'Leads' })).toBeVisible({ timeout: 15_000 });
  });

  test('an untouched empty login remains rejected', async ({ page }) => {
    const loginBodies: string[] = [];
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().includes('/auth/login')) {
        loginBodies.push(request.postData() ?? '');
      }
    });

    await clearSession(page);
    await page.goto('/login', { waitUntil: 'load' });
    await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible({ timeout: 15_000 });
    await expect(page.getByLabel('Email', { exact: true })).toHaveValue('');
    await expect(page.getByLabel('Password', { exact: true })).toHaveValue('');

    await page.getByRole('button', { name: 'Sign in' }).click();
    await expect(page.getByText('Enter your email.')).toBeVisible();
    await expect(page.getByText('Enter your password.')).toBeVisible();
    await expect(page).toHaveURL(/\/login/);
    expect(loginBodies).toEqual([]);
  });
});
