import type { Metadata } from 'next'
import { Be_Vietnam_Pro } from 'next/font/google'
import './globals.css'
import { Toaster } from '@/components/ui/toaster'

const beVietnamPro = Be_Vietnam_Pro({ 
  subsets: ['vietnamese', 'latin'],
  weight: ['300', '400', '500', '600', '700'],
})

export const metadata: Metadata = {
  title: 'Tổng hợp báo cáo công việc - Phường Ninh Xá',
  description: 'Hệ thống tổng hợp báo cáo công việc Phường Ninh Xá, TP Bắc Ninh',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="vi">
      <body className={beVietnamPro.className}>
        {children}
        <Toaster />
      </body>
    </html>
  )
}

