"use client"

import { useEffect, useState } from 'react'
import { ArrowLeft, Loader2, Users, Calendar, Info, Upload, X, FileText } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/components/ui/use-toast'
import { useRouter } from 'next/navigation'
import { reportRequestApi, commonApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { fromDateTimeLocal } from '@/lib/utils'

interface User {
  id: number
  fullName: string
  email: string
  department?: { id: number; name: string }
  position?: { id: number; name: string }
}

export default function CreateReportRequestPage() {
  const { toast } = useToast()
  const router = useRouter()
  const currentUser = useAuthStore((state) => state.user)
  const [availableUsers, setAvailableUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [scopeInfo, setScopeInfo] = useState<string>('')
  
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    userIds: [] as number[],
    deadline: '',
  })
  const [selectedFiles, setSelectedFiles] = useState<File[]>([])

  const isSeniorManagement = () => {
    const roleName = currentUser?.role?.name?.toLowerCase() || ''
    return roleName.includes('senior') || 
           roleName.includes('management') || 
           roleName.includes('quản lý') ||
           roleName.includes('lãnh đạo')
  }

  const fetchAvailableUsers = async () => {
    try {
      let response
      
      // Senior Management can send to everyone
      if (isSeniorManagement()) {
        response = await commonApi.getUsers()
        setScopeInfo('Tất cả người dùng (Quyền Lãnh đạo)')
      }
      // Determine scope based on current user's organization and department
      else if (currentUser?.department) {
        // User belongs to a department -> only show users in that department
        response = await commonApi.getUsersByDepartment(currentUser.department.id)
        setScopeInfo(`Phòng ban: ${currentUser.department.name}`)
      } else if (currentUser?.organizations && currentUser.organizations.length > 0) {
        // User belongs to organization(s) but no department -> show all users in first organization
        const firstOrg = currentUser.organizations[0]
        response = await commonApi.getUsersByOrganization(firstOrg.id)
        setScopeInfo(`Cơ quan: ${firstOrg.name}`)
      } else {
        // Fallback - no restriction
        response = await commonApi.getUsers()
        setScopeInfo('Tất cả người dùng')
      }
      
      // Filter out current user and admin
      const users = (response.data.data || []).filter(
        (u: User) => u.id !== currentUser?.id && u.id !== 1
      )
      setAvailableUsers(users)
    } catch (error) {
      toast({
        title: 'Lỗi',
        description: 'Không thể tải danh sách người nhận',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (currentUser) {
      fetchAvailableUsers()
    }
  }, [currentUser])

  const handleSubmit = async () => {
    if (!formData.title.trim()) {
      toast({
        title: 'Lỗi',
        description: 'Tiêu đề không được để trống',
        variant: 'destructive',
      })
      return
    }

    if (!formData.deadline) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn thời hạn báo cáo',
        variant: 'destructive',
      })
      return
    }

    if (formData.userIds.length === 0) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn ít nhất một người nhận',
        variant: 'destructive',
      })
      return
    }

    setIsSubmitting(true)
    try {
      await reportRequestApi.create({
        title: formData.title.trim(),
        description: formData.description.trim() || undefined,
        userIds: formData.userIds,
        deadline: fromDateTimeLocal(formData.deadline),
      }, selectedFiles.length > 0 ? selectedFiles : undefined)
      
      toast({
        title: 'Thành công',
        description: 'Tạo yêu cầu báo cáo thành công',
      })
      router.push('/management/report-requests')
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Có lỗi xảy ra',
        variant: 'destructive',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const toggleUser = (id: number) => {
    setFormData(prev => ({
      ...prev,
      userIds: prev.userIds.includes(id)
        ? prev.userIds.filter(i => i !== id)
        : [...prev.userIds, id]
    }))
  }

  const selectAll = () => {
    setFormData(prev => ({
      ...prev,
      userIds: availableUsers.map(u => u.id)
    }))
  }

  const deselectAll = () => {
    setFormData(prev => ({
      ...prev,
      userIds: []
    }))
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-[#DA251D]" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.back()}
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Tạo yêu cầu báo cáo</h1>
          <p className="text-gray-500 mt-1">Gửi yêu cầu báo cáo đến các thành viên</p>
        </div>
      </div>

      {/* Scope Info Banner */}
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center gap-3">
        <Info className="h-5 w-5 text-blue-600 flex-shrink-0" />
        <div>
          <p className="text-sm font-medium text-blue-800">Phạm vi gửi yêu cầu</p>
          <p className="text-sm text-blue-600">
            Bạn có thể gửi yêu cầu đến: <strong>{scopeInfo}</strong>
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Form */}
        <div className="lg:col-span-2 space-y-6">
          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle>Thông tin yêu cầu</CardTitle>
              <CardDescription>Nhập tiêu đề và mô tả cho yêu cầu báo cáo</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="title">Tiêu đề yêu cầu *</Label>
                <Input
                  id="title"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  placeholder="VD: Báo cáo tình hình công tác tháng 12..."
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">Mô tả chi tiết</Label>
                <Textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Nhập nội dung chi tiết yêu cầu..."
                  rows={4}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="deadline" className="flex items-center gap-2">
                  <Calendar className="h-4 w-4" />
                  Thời hạn báo cáo *
                </Label>
                <Input
                  id="deadline"
                  type="datetime-local"
                  value={formData.deadline}
                  onChange={(e) => setFormData({ ...formData, deadline: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="files" className="flex items-center gap-2">
                  <Upload className="h-4 w-4" />
                  File đính kèm
                </Label>
                <div className="space-y-2">
                  <Input
                    id="files"
                    type="file"
                    multiple
                    onChange={(e) => {
                      const files = Array.from(e.target.files || [])
                      setSelectedFiles(prev => [...prev, ...files])
                    }}
                    className="cursor-pointer"
                  />
                  {selectedFiles.length > 0 && (
                    <div className="space-y-2 mt-2">
                      {selectedFiles.map((file, index) => (
                        <div key={index} className="flex items-center justify-between p-2 bg-gray-50 rounded border">
                          <div className="flex items-center gap-2 flex-1 min-w-0">
                            <FileText className="h-4 w-4 text-gray-500 flex-shrink-0" />
                            <span className="text-sm text-gray-700 truncate">{file.name}</span>
                            <span className="text-xs text-gray-500 flex-shrink-0">
                              ({(file.size / 1024).toFixed(2)} KB)
                            </span>
                          </div>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              setSelectedFiles(prev => prev.filter((_, i) => i !== index))
                            }}
                            className="h-6 w-6 p-0 text-red-600 hover:text-red-700 hover:bg-red-50"
                          >
                            <X className="h-4 w-4" />
                          </Button>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Recipients Selection */}
          <Card className="border-0 shadow-md">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <Users className="h-5 w-5 text-[#DA251D]" />
                    Người nhận yêu cầu
                  </CardTitle>
                  <CardDescription>
                    Chọn người sẽ nhận yêu cầu báo cáo này ({formData.userIds.length}/{availableUsers.length} đã chọn)
                  </CardDescription>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" size="sm" onClick={selectAll}>
                    Chọn tất cả
                  </Button>
                  <Button variant="outline" size="sm" onClick={deselectAll}>
                    Bỏ chọn
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              {availableUsers.length === 0 ? (
                <div className="text-center py-8 text-gray-500">
                  <Users className="h-12 w-12 mx-auto mb-3 text-gray-300" />
                  <p>Không có người dùng nào trong phạm vi của bạn</p>
                </div>
              ) : (
                <div className="border rounded-lg divide-y max-h-96 overflow-y-auto">
                  {availableUsers.map((user) => (
                    <div 
                      key={user.id} 
                      className={`flex items-center justify-between p-3 hover:bg-gray-50 cursor-pointer transition-colors ${
                        formData.userIds.includes(user.id) ? 'bg-green-50' : ''
                      }`}
                      onClick={() => toggleUser(user.id)}
                    >
                      <div className="flex items-center gap-3">
                        <Checkbox
                          id={`user-${user.id}`}
                          checked={formData.userIds.includes(user.id)}
                          onCheckedChange={() => toggleUser(user.id)}
                        />
                        <div>
                          <p className="font-medium text-gray-900">{user.fullName}</p>
                          <p className="text-sm text-gray-500">{user.email}</p>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        {user.position && (
                          <Badge variant="outline" className="text-xs">
                            {user.position.name}
                          </Badge>
                        )}
                        {user.department && (
                          <Badge variant="secondary" className="text-xs">
                            {user.department.name}
                          </Badge>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Summary Sidebar */}
        <div className="space-y-6">
          <Card className="border-0 shadow-md sticky top-24">
            <CardHeader>
              <CardTitle>Tóm tắt</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <p className="text-sm text-gray-500">Tiêu đề</p>
                <p className="font-medium">{formData.title || '(Chưa nhập)'}</p>
              </div>
              <div>
                <p className="text-sm text-gray-500">Thời hạn</p>
                <p className="font-medium">
                  {formData.deadline 
                    ? (() => {
                        // formData.deadline is from datetime-local input (YYYY-MM-DDTHH:mm)
                        // Parse as local time and format
                        const date = new Date(formData.deadline)
                        return date.toLocaleString('vi-VN', {
                          day: '2-digit',
                          month: '2-digit',
                          year: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })
                      })()
                    : '(Chưa chọn)'}
                </p>
              </div>
              <div className="border-t pt-4">
                <p className="text-sm text-gray-500 mb-2">Người nhận</p>
                <p className="text-2xl font-bold text-[#DA251D]">{formData.userIds.length}</p>
                <p className="text-sm text-gray-500">người được chọn</p>
              </div>
              
              {formData.userIds.length > 0 && (
                <div className="border-t pt-4">
                  <p className="text-sm text-gray-500 mb-2">Danh sách</p>
                  <div className="space-y-1 max-h-32 overflow-y-auto">
                    {formData.userIds.map(id => {
                      const user = availableUsers.find(u => u.id === id)
                      return user ? (
                        <p key={id} className="text-sm truncate">{user.fullName}</p>
                      ) : null
                    })}
                  </div>
                </div>
              )}

              <div className="border-t pt-4 space-y-2">
                <Button
                  onClick={handleSubmit}
                  disabled={isSubmitting || formData.userIds.length === 0}
                  className="w-full bg-[#DA251D] hover:bg-[#b81f18]"
                >
                  {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  Gửi yêu cầu
                </Button>
                <Button
                  variant="outline"
                  onClick={() => router.back()}
                  className="w-full"
                >
                  Hủy
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
