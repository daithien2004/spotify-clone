"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  useCurrentUser,
  useUpdateProfile,
  useEnroll2fa,
  useVerify2faSetup,
  useDisable2fa,
  useResendVerification,
} from "@/hooks/useAuth";
import { validateDisplayName, validateTotpCode } from "@/lib/validation/auth";
import { toast } from "sonner";
import { Loader2 } from "lucide-react";

export default function AccountPage() {
  const user = useCurrentUser();
  // BootstrapAuth revalidate /me khi mount — emailVerified/twoFactorEnabled lấy từ store
  const emailVerified = user?.emailVerified ?? true;
  const twoFactorEnabled = user?.twoFactorEnabled ?? false;

  const [displayName, setDisplayName] = useState(user?.displayName ?? "");
  const [avatarUrl, setAvatarUrl] = useState(user?.avatarUrl ?? "");
  const [qr, setQr] = useState<{ otpauthUrl: string; qrDataUri: string } | null>(null);
  const [code, setCode] = useState("");
  const [disableCode, setDisableCode] = useState("");
  const [resendSent, setResendSent] = useState(false);

  const profileMutation = useUpdateProfile();
  const enrollMutation = useEnroll2fa();
  const verifySetupMutation = useVerify2faSetup(() => {
    setQr(null);
    setCode("");
    toast.success("Two-factor authentication enabled");
  });
  const disableMutation = useDisable2fa(() => {
    setDisableCode("");
    toast.success("Two-factor authentication disabled");
  });
  const resendMutation = useResendVerification(user?.email ?? "");

  const handleProfile = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateDisplayName(displayName);
    if (err) return toast.error(err);
    profileMutation.mutate(
      { displayName: displayName.trim(), avatarUrl: avatarUrl.trim() || null },
      {
        onSuccess: () => toast.success("Profile updated"),
        onError: (error) => toast.error("Update failed", { description: error.message || "Please try again." }),
      }
    );
  };

  const handleEnroll = () => {
    enrollMutation.mutate(undefined, {
      onSuccess: (data) => {
        setQr(data);
        toast.info("Scan the QR code with your authenticator app");
      },
      onError: (error) => toast.error("Could not start 2FA setup", { description: error.message }),
    });
  };

  const handleVerifySetup = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateTotpCode(code);
    if (err) return toast.error(err);
    verifySetupMutation.mutate(code);
  };

  const handleDisable = (e: React.FormEvent) => {
    e.preventDefault();
    const err = validateTotpCode(disableCode);
    if (err) return toast.error(err);
    disableMutation.mutate(disableCode, {
      onError: (error) => toast.error("Could not disable 2FA", { description: error.message }),
    });
  };

  return (
    <div className="mx-auto w-full max-w-2xl space-y-8 px-4 py-8">
      <h1 className="text-3xl font-bold tracking-tight text-foreground">Account</h1>

      {/* Email verification banner (D6) */}
      {!emailVerified && (
        <div className="flex items-center justify-between gap-4 rounded-lg border border-border bg-background px-4 py-3">
          <div>
            <p className="text-sm font-bold text-foreground">Email not verified</p>
            <p className="text-xs text-muted-foreground">Verify {user?.email} to secure your account.</p>
          </div>
          <Button
            variant="outline" size="sm"
            onClick={() => {
              resendMutation.mutate(undefined, {
                onSuccess: () => { setResendSent(true); toast.success("Verification email sent"); },
              });
            }}
          >
            {resendSent ? "Resent" : "Resend email"}
          </Button>
        </div>
      )}

      {/* Profile form */}
      <section className="space-y-4 rounded-lg border border-border bg-background p-6">
        <h2 className="text-lg font-bold text-foreground">Profile</h2>
        <form onSubmit={handleProfile} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="displayName" className="text-sm font-bold text-foreground">Display name</Label>
            <Input id="displayName" value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="h-11 bg-background border-border text-foreground rounded-[4px]" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="avatarUrl" className="text-sm font-bold text-foreground">Avatar URL</Label>
            <Input id="avatarUrl" type="url" placeholder="https://example.com/avatar.png" value={avatarUrl}
              onChange={(e) => setAvatarUrl(e.target.value)}
              className="h-11 bg-background border-border text-foreground rounded-[4px]" />
            <p className="text-xs text-muted-foreground">Image URL text — no file upload (đã chốt scope).</p>
          </div>
          <Button type="submit" disabled={profileMutation.isPending}
            className="bg-spotify-green hover:opacity-90 text-black font-bold h-10 rounded-full px-6">
            {profileMutation.isPending ? <Loader2 className="w-4 h-4 animate-spin" /> : "Save changes"}
          </Button>
        </form>
      </section>

      {/* 2FA section */}
      <section className="space-y-4 rounded-lg border border-border bg-background p-6">
        <h2 className="text-lg font-bold text-foreground">Two-factor authentication</h2>

        {!twoFactorEnabled ? (
          !qr ? (
            <div className="flex items-center justify-between gap-4">
              <p className="text-sm text-muted-foreground">
                2FA is disabled. Enable it to add an extra layer of security with an authenticator app.
              </p>
              <Button onClick={handleEnroll} disabled={enrollMutation.isPending}
                className="bg-spotify-green hover:opacity-90 text-black font-bold h-10 rounded-full px-6">
                Set up 2FA
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={qr.qrDataUri} alt="TOTP QR code" className="mx-auto h-48 w-48" />
              <p className="text-xs text-muted-foreground text-center">
                Scan with Google Authenticator, then enter the 6-digit code.
              </p>
              <form onSubmit={handleVerifySetup} className="mx-auto flex max-w-xs gap-2">
                <Input inputMode="numeric" maxLength={6} placeholder="123456" value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                  className="h-11 bg-background border-border text-foreground text-center tracking-[0.3em] rounded-[4px]" />
                <Button type="submit" disabled={verifySetupMutation.isPending}
                  className="bg-spotify-green hover:opacity-90 text-black font-bold h-11 rounded-full px-5">
                  Verify
                </Button>
              </form>
            </div>
          )
        ) : (
          <form onSubmit={handleDisable} className="space-y-3">
            <p className="text-sm text-muted-foreground">2FA is enabled. Enter your current code to turn it off.</p>
            <div className="flex max-w-xs gap-2">
              <Input inputMode="numeric" maxLength={6} placeholder="123456" value={disableCode}
                onChange={(e) => setDisableCode(e.target.value.replace(/\D/g, ""))}
                className="h-11 bg-background border-border text-foreground text-center tracking-[0.3em] rounded-[4px]" />
              <Button type="submit" variant="outline" disabled={disableMutation.isPending}>
                Disable
              </Button>
            </div>
          </form>
        )}
      </section>
    </div>
  );
}