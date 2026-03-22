import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Search } from "lucide-react";

export default function NotFound() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[400px] h-full space-y-6 text-center">
      <div className="rounded-full bg-muted p-4">
        <Search className="w-12 h-12 text-muted-foreground" />
      </div>
      <div className="space-y-2">
        <h1 className="text-4xl font-bold tracking-tight">404</h1>
        <h2 className="text-2xl font-semibold">Trang không tồn tại</h2>
        <p className="text-muted-foreground max-w-[400px]">
          Chúng tôi không thể tìm thấy trang bạn đang tìm kiếm. Có thể nội dung đã bị xóa hoặc đường dẫn bị sai.
        </p>
      </div>
      <Button asChild variant="default" size="lg" className="rounded-full bg-foreground text-background hover:scale-105 transition-transform px-8">
        <Link href="/">Quay về trang chủ</Link>
      </Button>
    </div>
  );
}
