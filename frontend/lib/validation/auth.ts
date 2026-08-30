const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateEmail(email: string): string | null {
  if (!email.trim()) return "Email is required.";
  if (!EMAIL_RE.test(email.trim())) return "Enter a valid email address.";
  return null;
}

export function validatePassword(password: string): string | null {
  if (!password) return "Password is required.";
  if (password.length < 8) return "Password must be at least 8 characters.";
  return null;
}

export function validateConfirmPassword(password: string, confirm: string): string | null {
  if (password !== confirm) return "Passwords do not match.";
  return null;
}

export function validateDisplayName(name: string): string | null {
  if (!name.trim()) return "Display name is required.";
  return null;
}

export function validateTotpCode(code: string): string | null {
  if (!/^\d{6}$/.test(code.trim())) return "Enter the 6-digit code.";
  return null;
}
