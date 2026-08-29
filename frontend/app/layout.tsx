import type { Metadata } from "next";
import { Geist_Mono, Inter } from "next/font/google";
import "./globals.css";
import QueryProvider from "@/components/providers/QueryProvider";
import { ThemeProvider } from "@/components/providers/ThemeProvider";
import { Toaster } from "sonner";

// Inter = typeface trong Figma design (400-800). Load trước để `--font-inter`
// được tokens.css dùng làm font-sans mặc định.
const inter = Inter({
  variable: "--font-inter",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  display: "swap",
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: {
    template: "%s | Spotify Clone",
    default: "Spotify Clone - Premium Music Experience",
  },
  description: "A premium Spotify clone built with Next.js and Spring Boot. Clean Architecture & High Performance.",
  icons: {
    icon: "/icon.png",
  },
  openGraph: {
    title: "Spotify Clone - Premium Music",
    description: "Experience premium music streaming with our high-performance Spotify clone.",
    type: "website",
    locale: "vi_VN",
    url: "https://spotify-clone-demo.vercel.app",
    siteName: "Spotify Clone",
    images: [
      {
        url: "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?auto=format&fit=crop&w=1200&q=80",
        width: 1200,
        height: 630,
        alt: "Spotify Clone Preview",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "Spotify Clone",
    description: "High-performance Spotify Clone",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html
      lang="en"
      className={`${inter.variable} ${geistMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <head>
        <link rel="preconnect" href="https://images.unsplash.com" />
      </head>
      <body className="min-h-full flex flex-col">
        <ThemeProvider
          attribute="class"
          defaultTheme="dark"
          enableSystem={false}
          disableTransitionOnChange
        >
          <QueryProvider>
            {children}
            <Toaster position="top-center" richColors />
          </QueryProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
