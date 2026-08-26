'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/hooks/useAuthStore';
import { AuthService } from '@/services/api/authService';
import { toast } from 'sonner';

/**
 * Page that handles the redirect from Backend after successful Google Login.
 * Since the backend set HttpOnly cookies, we just need to fetch the current user
 * to update the frontend state and then redirect to home.
 */
export default function OAuth2CallbackPage() {
    const router = useRouter();
    const { setAuth } = useAuthStore();

    useEffect(() => {
        const onboardOAuth2User = async () => {
            try {
                // Fetch user info using the cookies set by backend
                // This will also verify that the session is valid
                const response = await AuthService.me();
                
                if (response.success && response.data) {
                    setAuth(response.data);
                    toast.success('Đăng nhập bằng Google thành công!');
                    router.push('/');
                } else {
                    throw new Error('Không thể lấy thông tin người dùng');
                }
            } catch (error) {
                console.error('OAuth2 Callback Error:', error);
                toast.error('Có lỗi xảy ra trong quá trình đăng nhập bằng Google.');
                router.push('/login');
            }
        };

        onboardOAuth2User();
    }, [router, setAuth]);

    return (
        <div className="flex h-screen w-screen flex-col items-center justify-center bg-black text-white">
            <div className="h-12 w-12 animate-spin rounded-full border-4 border-spotify-green border-t-transparent"></div>
            <p className="mt-4 text-lg font-medium">Đang hoàn tất đăng nhập...</p>
        </div>
    );
}
