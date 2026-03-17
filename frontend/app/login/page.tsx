import Link from 'next/link';
import { Chrome, Facebook, Apple } from 'lucide-react';

export default function LoginPage() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-zinc-900 to-black flex flex-col items-center py-10 px-4">
      {/* Logo */}
      <div className="mb-10 text-white flex items-center gap-2">
        <div className="w-10 h-10 bg-[#1DB954] rounded-full flex items-center justify-center">
            <svg viewBox="0 0 24 24" className="w-6 h-6 fill-black" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0zm5.502 17.31c-.22.36-.688.472-1.048.252-2.85-1.74-6.438-2.13-10.665-1.166-.41.093-.822-.162-.916-.57-.094-.41.162-.82.57-.914 4.63-1.058 8.59-.61 11.808 1.352.36.22.472.688.252 1.047zm1.468-3.258c-.276.45-.86.594-1.31.318-3.264-2.007-8.24-2.587-12.098-1.417-.506.154-1.036-.134-1.19-.64-.154-.506.134-1.037.64-1.19 4.41-1.34 9.9-1.34 13.63 1.14.45.276.593.86.318 1.31zm.134-3.4c-3.913-2.325-10.366-2.543-14.123-1.402-.6.183-1.238-.158-1.42-.76-.183-.6.158-1.238.76-1.42 4.307-1.306 11.412-1.053 15.908 1.613.54.32.715 1.018.395 1.56-.32.54-1.018.714-1.56.394z"/>
            </svg>
        </div>
        <span className="text-2xl font-bold tracking-tight">Spotify</span>
      </div>

      {/* Login Card */}
      <div className="w-full max-w-[450px] bg-black border border-zinc-800 rounded-lg p-8 md:p-12 shadow-2xl">
        <h1 className="text-3xl md:text-4xl font-bold text-white text-center mb-8">
          Log in to Spotify
        </h1>

        {/* Social Buttons */}
        <div className="flex flex-col gap-3 mb-8">
          <button className="flex items-center justify-center gap-3 w-full py-3 px-4 rounded-full border border-zinc-500 font-bold hover:border-white transition group relative overflow-hidden">
            <Chrome className="w-5 h-5 absolute left-5 group-hover:scale-110 transition" />
            <span>Continue with Google</span>
          </button>
          <button className="flex items-center justify-center gap-3 w-full py-3 px-4 rounded-full border border-zinc-500 font-bold hover:border-white transition group relative">
            <Facebook className="w-5 h-5 absolute left-5 group-hover:scale-110 transition text-blue-600" />
            <span>Continue with Facebook</span>
          </button>
          <button className="flex items-center justify-center gap-3 w-full py-3 px-4 rounded-full border border-zinc-500 font-bold hover:border-white transition group relative">
            <Apple className="w-5 h-5 absolute left-5 group-hover:scale-110 transition" />
            <span>Continue with Apple</span>
          </button>
        </div>

        <div className="relative flex items-center py-4 mb-4">
          <div className="flex-grow border-t border-zinc-800"></div>
          <span className="flex-shrink mx-4 text-zinc-400 text-sm">or</span>
          <div className="flex-grow border-t border-zinc-800"></div>
        </div>

        {/* Login Form */}
        <form className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <label className="text-sm font-bold text-white" htmlFor="email">
              Email or username
            </label>
            <input
              type="text"
              id="email"
              placeholder="Email or username"
              className="bg-transparent border border-zinc-500 rounded-md py-3 px-4 text-white focus:border-[#1DB954] focus:ring-1 focus:ring-[#1DB954] outline-none transition placeholder:text-zinc-500"
            />
          </div>

          <div className="flex flex-col gap-2">
            <label className="text-sm font-bold text-white" htmlFor="password">
              Password
            </label>
            <input
              type="password"
              id="password"
              placeholder="Password"
              className="bg-transparent border border-zinc-500 rounded-md py-3 px-4 text-white focus:border-[#1DB954] focus:ring-1 focus:ring-[#1DB954] outline-none transition placeholder:text-zinc-500"
            />
          </div>

          <div className="flex items-center gap-2 mt-2">
            <input
              type="checkbox"
              id="remember"
              className="w-4 h-4 accent-[#1DB954] rounded cursor-pointer"
            />
            <label htmlFor="remember" className="text-sm text-zinc-300 cursor-pointer select-none">
              Remember me
            </label>
          </div>

          <button
            type="submit"
            className="bg-[#1DB954] text-black font-bold py-3 rounded-full mt-6 hover:scale-105 active:scale-95 transition shadow-lg"
          >
            Log In
          </button>
        </form>

        <div className="text-center mt-6">
          <Link href="#" className="text-white text-sm hover:text-[#1DB954] underline transition">
            Forgot your password?
          </Link>
        </div>

        <div className="mt-8 pt-8 border-t border-zinc-800 text-center">
            <p className="text-zinc-400">
                Don't have an account?{' '}
                <Link href="#" className="text-white hover:text-[#1DB954] underline transition">
                    Sign up for Spotify
                </Link>
            </p>
        </div>
      </div>

      {/* Footer */}
      <footer className="mt-auto py-8 text-zinc-500 text-xs text-center max-w-[600px]">
        This site is protected by reCAPTCHA and the Google{' '}
        <Link href="#" className="underline">Privacy Policy</Link> and{' '}
        <Link href="#" className="underline">Terms of Service</Link> apply.
      </footer>
    </div>
  );
}
