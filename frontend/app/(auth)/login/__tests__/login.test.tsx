import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import LoginPage from "@/app/(auth)/login/page";

// === Mock các dependency ngoại vi — chỉ tập trung test logic + render của LoginPage ===
vi.mock("@/hooks/useAuth", () => ({
  useLogin: () => loginMock,
  useVerify2faLogin: () => verifyMock,
}));

vi.mock("sonner", () => ({
  toast: { error: vi.fn() },
}));

// next/link — Next Link cần router context, mock thành <a> đơn giản.
vi.mock("next/link", () => ({
  default: ({ href, children, ...props }: { href: string; children: React.ReactNode }) =>
    <a href={href} {...props}>{children}</a>,
}));

// Shape tối thiểu của mutation hook mà LoginPage dùng — tránh `any` (strict rule).
interface MutationMock<TData = unknown> {
  mutate: ReturnType<typeof vi.fn>;
  isPending: boolean;
  data: TData | null;
}
type LoginData = { mfaRequired?: boolean; mfaToken?: string };

const loginMock: MutationMock<LoginData> = { mutate: vi.fn(), isPending: false, data: null };
const verifyMock: MutationMock = { mutate: vi.fn(), isPending: false, data: null };

describe("LoginPage", () => {
  beforeEach(() => {
    loginMock.mutate.mockReset();
    verifyMock.mutate.mockReset();
    loginMock.isPending = false;
    loginMock.data = null;
  });

  it("render form đăng nhập (email, password, nút Log in)", () => {
    render(<LoginPage />);
    expect(screen.getByLabelText(/Email or username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Password/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Log in/i })).toBeInTheDocument();
  });

  it("submit gọi useLogin().mutate(email, password)", () => {
    render(<LoginPage />);
    fireEvent.change(screen.getByLabelText(/Email or username/i), {
      target: { value: "alice@example.com" },
    });
    fireEvent.change(screen.getByLabelText(/Password/i), {
      target: { value: "SuperPass2026!" },
    });
    fireEvent.click(screen.getByRole("button", { name: /Log in/i }));

    expect(loginMock.mutate).toHaveBeenCalledTimes(1);
    expect(loginMock.mutate).toHaveBeenCalledWith(
      expect.objectContaining({ email: "alice@example.com", password: "SuperPass2026!" }),
      expect.any(Object)
    );
  });

  it("khi mfaRequired → hiện bước nhập mã 6 số (Two-factor authentication)", () => {
    loginMock.data = { mfaRequired: true, mfaToken: "mfatok" };
    render(<LoginPage />);

    expect(screen.getByText("Two-factor authentication")).toBeInTheDocument();
    const codeInput = screen.getByPlaceholderText("123456");
    expect(codeInput).toBeInTheDocument();

    fireEvent.change(codeInput, { target: { value: "123456" } });
    fireEvent.click(screen.getByRole("button", { name: /Verify/i }));

    // verify2faLogin nhận mfaToken + code
    expect(verifyMock.mutate).toHaveBeenCalledWith(
      expect.objectContaining({ mfaToken: "mfatok", code: "123456" }),
      expect.any(Object)
    );
  });

  it("render nút Continue with Google (OAuth2 redirect qua gateway)", () => {
    render(<LoginPage />);
    expect(screen.getByText("Continue with Google")).toBeInTheDocument();
  });
});
