import { describe, it, expect } from "vitest";
import {
  validateEmail,
  validatePassword,
  validateConfirmPassword,
  validateDisplayName,
  validateTotpCode,
} from "@/lib/validation/auth";

describe("validateEmail", () => {
  it("returns null for a valid email", () => {
    expect(validateEmail("user@example.com")).toBeNull();
  });
  it("returns a message for invalid email", () => {
    expect(validateEmail("not-an-email")).not.toBeNull();
    expect(validateEmail("")).not.toBeNull();
  });
});

describe("validatePassword", () => {
  it("accepts 8+ chars", () => {
    expect(validatePassword("password1")).toBeNull();
  });
  it("rejects short/blank", () => {
    expect(validatePassword("short")).not.toBeNull();
    expect(validatePassword("")).not.toBeNull();
  });
});

describe("validateConfirmPassword", () => {
  it("returns null when passwords match", () => {
    expect(validateConfirmPassword("password1", "password1")).toBeNull();
  });
  it("returns message when they differ", () => {
    expect(validateConfirmPassword("password1", "password2")).not.toBeNull();
  });
});

describe("validateDisplayName", () => {
  it("accepts non-blank", () => {
    expect(validateDisplayName("Quang")).toBeNull();
  });
  it("rejects blank", () => {
    expect(validateDisplayName("   ")).not.toBeNull();
  });
});

describe("validateTotpCode", () => {
  it("accepts 6 digits", () => {
    expect(validateTotpCode("123456")).toBeNull();
  });
  it("rejects non-6-digit", () => {
    expect(validateTotpCode("12345")).not.toBeNull();
    expect(validateTotpCode("abcdef")).not.toBeNull();
  });
});
