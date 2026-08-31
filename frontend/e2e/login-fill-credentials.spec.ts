import { expect, test } from '@playwright/test';

test('Fill credentials populates visible inputs without signing in', async ({ page }) => {
  const loginBodies: string[] = [];
  page.on('request', (request) => {
    if (request.method() === 'POST' && request.url().includes('/auth/login')) {
      loginBodies.push(request.postData() ?? '');
    }
  });

  await page.addInitScript(() => {
    localStorage.removeItem('auth_token');
  });
  await page.context().clearCookies();
  await page.goto('/login', { waitUntil: 'load' });
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible({ timeout: 15_000 });

  const email = page.getByLabel('Email', { exact: true });
  const password = page.getByLabel('Password', { exact: true });
  await expect(email).toHaveValue('');
  await expect(password).toHaveValue('');

  await page.getByRole('button', { name: 'Fill credentials' }).click();

  await expect(email).toHaveValue('demo@agentcrm.app');
  await expect(password).toHaveValue('Demo@123');
  await expect(page).toHaveURL(/\/login/);
  expect(loginBodies).toEqual([]);

  await page.getByRole('button', { name: 'Sign in' }).click();
  await expect(page).toHaveURL(/\/dashboard/, { timeout: 15_000 });
  expect(loginBodies).toHaveLength(1);
  expect(loginBodies[0]).toContain('demo@agentcrm.app');
});
