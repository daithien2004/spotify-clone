---
name: new-page
description: Create a Next.js feature page component
---

# Skill: new-page

Chu trình thiết lập trang Web Frontend trên Next.js 14 App Router:

1. Xác định Feature Tên & Route (vd: `/track/[id]`).
2. Server Component mặc định (`page.tsx`): Load init context, metadata.
3. React Query custom Hook: `hooks/use{Feature}.ts` để quản lý các request data state.
4. Loading / Error Template: `loading.tsx` sử dụng Skeleton UI chuẩn (như shadcn/ui), `error.tsx` nhận các exception.
5. Thư mục `components/features/{Feature}`: Chia module Component thuần tuý, không business config fetch tại đây mà truyền context thông qua hook bên trên.
6. Types (`types/{feature}.types.ts`): Bám sát `ApiResponse<T>` map thẳng từ Interface backend trả về.
