"use client"

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { RefreshCw, Key, AlertCircle, CheckCircle2, Clock, Loader2 } from 'lucide-react'
import { useToast } from '@/components/ui/use-toast'
import { zaloOAuthApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { formatDateTime } from '@/lib/utils'

interface ZaloTokenInfo {
  id: number
  tokenType?: string
  scope?: string
  expiresAt?: string
  createdAt?: string
  updatedAt?: string
  isExpired: boolean
  isExpiringSoon: boolean
  expiresIn: string
}

export default function ZaloTokenManagementPage() {
  const { user, isAuthenticated } = useAuthStore()
  const router = useRouter()
  const { toast } = useToast()
  const [refreshToken, setRefreshToken] = useState('')
  const [tokenInfo, setTokenInfo] = useState<ZaloTokenInfo | null>(null)
  const [loading, setLoading] = useState(true)
  const [initLoading, setInitLoading] = useState(false)
  const [refreshLoading, setRefreshLoading] = useState(false)

  // Check authentication and admin role
  useEffect(() => {
    if (!isAuthenticated) {
      router.push('/login')
      return
    }
    
    if (user && user.role?.name !== 'Admin') {
      router.push('/admin')
      return
    }
  }, [isAuthenticated, user, router])

  // Fetch token info
  useEffect(() => {
    if (isAuthenticated && user?.role?.name === 'Admin') {
      fetchTokenInfo()
    }
  }, [isAuthenticated, user])

  const fetchTokenInfo = async () => {
    try {
      setLoading(true)
      const res = await zaloOAuthApi.getInfo()
      if (res.data.success && res.data.data) {
        setTokenInfo(res.data.data)
      } else {
        setTokenInfo(null)
      }
    } catch (error: any) {
      console.error('Error fetching token info:', error)
      setTokenInfo(null)
    } finally {
      setLoading(false)
    }
  }

  const handleInit = async () => {
    if (!refreshToken.trim()) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng nhập refresh token',
        variant: 'destructive',
      })
      return
    }

    try {
      setInitLoading(true)
      const res = await zaloOAuthApi.init(refreshToken.trim())
      if (res.data.success) {
        toast({
          title: 'Thành công',
          description: 'Khởi tạo token Zalo thành công',
        })
        setRefreshToken('')
        await fetchTokenInfo()
      } else {
        throw new Error(res.data.message || 'Khởi tạo token thất bại')
      }
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || error.message || 'Không thể khởi tạo token',
        variant: 'destructive',
      })
    } finally {
      setInitLoading(false)
    }
  }

  const handleRefresh = async () => {
    try {
      setRefreshLoading(true)
      const res = await zaloOAuthApi.refresh()
      if (res.data.success) {
        toast({
          title: 'Thành công',
          description: 'Làm mới token thành công',
        })
        await fetchTokenInfo()
      } else {
        throw new Error(res.data.message || 'Làm mới token thất bại')
      }
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || error.message || 'Không thể làm mới token',
        variant: 'destructive',
      })
    } finally {
      setRefreshLoading(false)
    }
  }

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'Không có'
    return formatDateTime(dateString)
  }

  if (!isAuthenticated || user?.role?.name !== 'Admin') {
    return null
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Quản lý Zalo OAuth Token</h1>
        <p className="text-gray-600 mt-2">Khởi tạo, xem trạng thái và làm mới token Zalo OAuth</p>
      </div>

      {/* Initialize Token Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Key className="w-5 h-5 text-[#DA251D]" />
            Khởi tạo Token
          </CardTitle>
          <CardDescription>Nhập refresh token để khởi tạo token lần đầu</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="refreshToken">Refresh Token</Label>
            <Input
              id="refreshToken"
              type="password"
              placeholder="Nhập refresh token..."
              value={refreshToken}
              onChange={(e) => setRefreshToken(e.target.value)}
              disabled={initLoading}
            />
          </div>
          <Button 
            onClick={handleInit} 
            disabled={!refreshToken.trim() || initLoading}
            className="w-full md:w-auto bg-[#DA251D] hover:bg-[#b81f18] text-white"
          >
            {initLoading ? (
              <>
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                Đang khởi tạo...
              </>
            ) : (
              'Khởi tạo Token'
            )}
          </Button>
        </CardContent>
      </Card>

      {/* Token Status Card */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-[#DA251D]" />
            Trạng thái Token
          </CardTitle>
          <CardDescription>Thông tin chi tiết về token hiện tại</CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="w-6 h-6 animate-spin text-[#DA251D]" />
            </div>
          ) : !tokenInfo ? (
            <div className="text-center py-8">
              <AlertCircle className="w-12 h-12 mx-auto text-[#DA251D] mb-4" />
              <p className="text-sm text-muted-foreground">Token chưa được khởi tạo</p>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <Label className="text-sm text-muted-foreground">Trạng thái</Label>
                  <div className="mt-1">
                    {tokenInfo.isExpired ? (
                      <Badge variant="destructive" className="gap-1">
                        <AlertCircle className="w-3 h-3" />
                        Đã hết hạn
                      </Badge>
                    ) : tokenInfo.isExpiringSoon ? (
                      <Badge className="gap-1 bg-[#FFCD00] text-black hover:bg-[#e6b800]">
                        <Clock className="w-3 h-3" />
                        Sắp hết hạn
                      </Badge>
                    ) : (
                      <Badge className="gap-1 bg-green-500 hover:bg-green-600 text-white">
                        <CheckCircle2 className="w-3 h-3" />
                        Hoạt động
                      </Badge>
                    )}
                  </div>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">Thời gian còn lại</Label>
                  <p className="mt-1 text-sm font-medium">{tokenInfo.expiresIn}</p>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">Loại Token</Label>
                  <p className="mt-1 text-sm">{tokenInfo.tokenType || 'Không có'}</p>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">Phạm vi</Label>
                  <p className="mt-1 text-sm">{tokenInfo.scope || 'Không có'}</p>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">Hết hạn lúc</Label>
                  <p className="mt-1 text-sm">{formatDate(tokenInfo.expiresAt)}</p>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">Tạo lúc</Label>
                  <p className="mt-1 text-sm">{formatDate(tokenInfo.createdAt)}</p>
                </div>
                <div>
                  <Label className="text-sm text-muted-foreground">Cập nhật lúc</Label>
                  <p className="mt-1 text-sm">{formatDate(tokenInfo.updatedAt)}</p>
                </div>
              </div>

              <div className="pt-4 border-t">
                <Button
                  onClick={handleRefresh}
                  disabled={refreshLoading}
                  className="w-full md:w-auto bg-[#DA251D] hover:bg-[#b81f18] text-white"
                >
                  <RefreshCw
                    className={`w-4 h-4 mr-2 ${refreshLoading ? 'animate-spin' : ''}`}
                  />
                  {refreshLoading ? 'Đang làm mới...' : 'Làm mới Token'}
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
