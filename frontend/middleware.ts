import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const token = request.cookies.get('auth-token')?.value;
  const isAuthPage =
    request.nextUrl.pathname.startsWith('/login') ||
    request.nextUrl.pathname.startsWith('/register') ||
    request.nextUrl.pathname.startsWith('/forgot-password') ||
    request.nextUrl.pathname.startsWith('/reset-password') ||
    request.nextUrl.pathname.startsWith('/verify-email');
  // Playlist không còn là demo page — backend yêu cầu JWT nên cần login.
  const isPublicPage = request.nextUrl.pathname === '/' || isAuthPage;

  // If user is not logged in and tries to access private routes
  if (!token && !isPublicPage) {
    return NextResponse.redirect(new URL('/login', request.url));
  }

  // If user is logged in and tries to access login or register page
  if (token && isAuthPage) {
    return NextResponse.redirect(new URL('/', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - figma (local assets under public/figma)
     * - favicon.ico, sitemap.xml, robots.txt (static files)
     * - any path ending in a known static file extension
     */
    '/((?!api|_next/static|_next/image|figma|favicon.ico|sitemap.xml|robots.txt|.*\\.(?:png|jpe?g|svg|gif|webp|avif|ico|woff2?|css|js|map|txt)$).*)',
  ],
};
