"use client"

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import * as z from 'zod'
import { Mail, Lock, Loader2, Star } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useToast } from '@/components/ui/use-toast'
import { authApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'

const loginSchema = z.object({
  email: z.string().email('Email không hợp lệ'),
  password: z.string().min(1, 'Mật khẩu không được để trống'),
})

type LoginFormData = z.infer<typeof loginSchema>

export default function LoginPage() {
  const router = useRouter()
  const { toast } = useToast()
  const setAuth = useAuthStore((state) => state.setAuth)
  const [isLoading, setIsLoading] = useState(false)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const onSubmit = async (data: LoginFormData) => {
    setIsLoading(true)
    try {
      const response = await authApi.login(data.email, data.password)
      if (response.data.success) {
        const user = response.data.data.user
        setAuth(user, response.data.data.token)
        toast({
          title: 'Đăng nhập thành công',
          description: `Chào mừng ${user.fullName}`,
          variant: 'default',
        })
        
        // Redirect based on role
        if (user.role?.name === 'Admin') {
          router.push('/admin')
        } else {
          router.push('/management')
        }
      } else {
        toast({
          title: 'Đăng nhập thất bại',
          description: response.data.message,
          variant: 'destructive',
        })
      }
    } catch (error: any) {
      toast({
        title: 'Đăng nhập thất bại',
        description: error.response?.data?.message || 'Email hoặc mật khẩu không đúng',
        variant: 'destructive',
      })
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden bg-gradient-to-br from-gray-50 via-white to-gray-100">
      {/* Background Pattern */}
      <div className="absolute inset-0 z-0">
        <div className="absolute top-0 left-0 w-full h-2 bg-gradient-to-r from-[#DA251D] via-[#FFCD00] to-[#DA251D]"></div>
        <div className="absolute bottom-0 left-0 w-full h-2 bg-gradient-to-r from-[#DA251D] via-[#FFCD00] to-[#DA251D]"></div>
        
        {/* Decorative stars */}
        <div className="absolute top-20 left-10 text-[#FFCD00] opacity-20">
          <Star className="w-24 h-24 fill-current" />
        </div>
        <div className="absolute bottom-20 right-10 text-[#FFCD00] opacity-20">
          <Star className="w-32 h-32 fill-current" />
        </div>
        <div className="absolute top-1/3 right-20 text-[#FFCD00] opacity-10">
          <Star className="w-16 h-16 fill-current" />
        </div>
      </div>

      <div className="relative z-10 w-full max-w-md px-4">
        {/* Logo and Title */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-[#DA251D] mb-4 shadow-lg animate-pulse-glow">
            <Star className="w-12 h-12 text-[#FFCD00] fill-current" />
          </div>
          <h1 className="text-2xl md:text-3xl font-bold text-gray-900">
            UBND Phường Ninh Xá
          </h1>
          <p className="text-gray-600 mt-2">Hệ thống tổng hợp báo cáo công việc</p>
        </div>

        {/* Login Card */}
        <Card className="border-0 shadow-2xl backdrop-blur-sm bg-white/95">
          <CardHeader className="space-y-1 pb-4">
            <CardTitle className="text-2xl font-bold text-center text-gray-900">
              Đăng nhập
            </CardTitle>
            <CardDescription className="text-center text-gray-500">
              Nhập thông tin để truy cập hệ thống
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="email" className="text-gray-700 font-medium">
                  Email
                </Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <Input
                    id="email"
                    type="email"
                    placeholder="admin@bacninh.gov.vn"
                    className="pl-10 h-12 border-gray-200 focus:border-[#DA251D] focus:ring-[#DA251D]"
                    {...register('email')}
                  />
                </div>
                {errors.email && (
                  <p className="text-sm text-[#DA251D]">{errors.email.message}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="password" className="text-gray-700 font-medium">
                  Mật khẩu
                </Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
                  <Input
                    id="password"
                    type="password"
                    placeholder="••••••••"
                    className="pl-10 h-12 border-gray-200 focus:border-[#DA251D] focus:ring-[#DA251D]"
                    {...register('password')}
                  />
                </div>
                {errors.password && (
                  <p className="text-sm text-[#DA251D]">{errors.password.message}</p>
                )}
              </div>

              <Button
                type="submit"
                className="w-full h-12 text-lg font-semibold bg-[#DA251D] hover:bg-[#b81f18] text-white transition-all duration-300 shadow-lg hover:shadow-xl"
                disabled={isLoading}
              >
                {isLoading ? (
                  <>
                    <Loader2 className="mr-2 h-5 w-5 animate-spin" />
                    Đang đăng nhập...
                  </>
                ) : (
                  'Đăng nhập'
                )}
              </Button>
            </form>
          </CardContent>
        </Card>

        {/* Footer */}
        <div className="text-center mt-6 text-sm text-gray-500">
          <p>© 2024 UBND Phường Ninh Xá, TP Bắc Ninh</p>
          <p className="mt-1 flex items-center justify-center gap-1">
            <span className="text-[#DA251D]">★</span>
            <span>Vì nhân dân phục vụ</span>
            <span className="text-[#FFCD00]">★</span>
          </p>
        </div>
      </div>
    </div>
  )
}

