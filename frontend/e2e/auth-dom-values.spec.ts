import { expect, test, type Locator } from '@playwright/test';

async function inputValue(locator: Locator) {
  return locator.evaluate((el) => (el as HTMLInputElement).value);
}

test('live Vite login and signup keep the input.value property', async ({ page }) => {
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

  const emailInput = page.locator('#login-email');
  const passwordInput = page.locator('#login-password');

  await emailInput.fill('user@example.com');
  await passwordInput.fill('Typed@123');
  expect(await inputValue(emailInput)).toBe('user@example.com');
  expect(await inputValue(passwordInput)).toBe('Typed@123');
  await expect(emailInput).toHaveValue('user@example.com');
  await expect(passwordInput).toHaveValue('Typed@123');

  await page.reload({ waitUntil: 'load' });
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible({ timeout: 15_000 });
  await page.getByRole('button', { name: 'Fill credentials' }).click();
  expect(await inputValue(page.locator('#login-email'))).toBe('demo@agentcrm.app');
  expect(await inputValue(page.locator('#login-password'))).toBe('Demo@123');
  await expect(page.locator('#login-email')).toHaveValue('demo@agentcrm.app');
  await expect(page.locator('#login-password')).toHaveValue('Demo@123');
  await expect(page).toHaveURL(/\/login/);
  expect(loginBodies).toEqual([]);

  await page.goto('/signup', { waitUntil: 'load' });
  await expect(page.getByRole('heading', { name: 'Create an account' })).toBeVisible({ timeout: 15_000 });

  const signupName = page.locator('#signup-full-name');
  const signupEmail = page.locator('#signup-email');
  const signupPassword = page.locator('#signup-password');
  const signupConfirm = page.locator('#signup-confirm-password');

  await signupEmail.fill('user@example.com');
  expect(await inputValue(signupEmail)).toBe('user@example.com');
  await expect(signupEmail).toHaveValue('user@example.com');

  await signupName.fill('Ada Lovelace');
  await signupPassword.fill('Typed@123');
  await signupConfirm.fill('Typed@123');
  expect(await inputValue(signupName)).toBe('Ada Lovelace');
  expect(await inputValue(signupEmail)).toBe('user@example.com');
  expect(await inputValue(signupPassword)).toBe('Typed@123');
  expect(await inputValue(signupConfirm)).toBe('Typed@123');
  await expect(signupName).toHaveValue('Ada Lovelace');
  await expect(signupEmail).toHaveValue('user@example.com');
  await expect(signupPassword).toHaveValue('Typed@123');
  await expect(signupConfirm).toHaveValue('Typed@123');
});
