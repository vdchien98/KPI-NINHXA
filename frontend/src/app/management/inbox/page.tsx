"use client"

import { useEffect, useState } from 'react'
import { Inbox, Loader2, Search, Eye, Clock, AlertCircle, CheckCircle, Forward, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/components/ui/use-toast'
import { useRouter } from 'next/navigation'
import { reportRequestApi, commonApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import { formatDate as formatDateUtil, isPast, fromDateTimeLocal } from '@/lib/utils'

interface ReportRequest {
  id: number
  title: string
  description?: string
  deadline: string
  status: string
  myResponseStatus?: string // Status của response của user hiện tại
  createdAt: string
  createdBy?: {
    id: number
    fullName: string
    email: string
  }
}

interface User {
  id: number
  fullName: string
  email: string
  department?: { id: number; name: string }
}

export default function InboxPage() {
  const { toast } = useToast()
  const router = useRouter()
  const currentUser = useAuthStore((state) => state.user)
  const [requests, setRequests] = useState<ReportRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [forwardDialogOpen, setForwardDialogOpen] = useState(false)
  const [selectedRequest, setSelectedRequest] = useState<ReportRequest | null>(null)
  const [availableUsers, setAvailableUsers] = useState<User[]>([])
  const [forwardLoading, setForwardLoading] = useState(false)
  const [selectedFiles, setSelectedFiles] = useState<File[]>([])
  
  const [forwardFormData, setForwardFormData] = useState({
    title: '',
    forwardNote: '',
    userIds: [] as number[],
    deadline: '',
  })

  const fetchRequests = async () => {
    try {
      const response = await reportRequestApi.getReceivedRequests()
      setRequests(response.data.data || [])
    } catch (error) {
      toast({
        title: 'Lỗi',
        description: 'Không thể tải danh sách yêu cầu',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRequests()
  }, [])

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Badge className="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">Đang chờ</Badge>
      case 'IN_PROGRESS':
        return <Badge className="bg-blue-100 text-blue-700 hover:bg-blue-100">Đang thực hiện</Badge>
      case 'COMPLETED':
        return <Badge className="bg-green-100 text-green-700 hover:bg-green-100">Hoàn thành</Badge>
      case 'CANCELLED':
        return <Badge className="bg-gray-100 text-gray-700 hover:bg-gray-100">Đã hủy</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const formatDate = (dateString: string) => {
    return formatDateUtil(dateString)
  }

  const isOverdue = (deadline: string, status: string) => {
    return isPast(deadline) && status !== 'COMPLETED' && status !== 'CANCELLED'
  }

  const filteredRequests = requests.filter((request) =>
    request.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    request.createdBy?.fullName.toLowerCase().includes(searchTerm.toLowerCase())
  )

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
      
      if (isSeniorManagement()) {
        response = await commonApi.getUsers()
      } else if (currentUser?.department) {
        response = await commonApi.getUsersByDepartment(currentUser.department.id)
      } else if (currentUser?.organizations && currentUser.organizations.length > 0) {
        const firstOrg = currentUser.organizations[0]
        response = await commonApi.getUsersByOrganization(firstOrg.id)
      } else {
        response = await commonApi.getUsers()
      }
      
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
    }
  }

  const handleOpenForwardDialog = async (request: ReportRequest) => {
    setSelectedRequest(request)
    setForwardFormData({
      title: `[Chuyển tiếp] ${request.title}`,
      forwardNote: '',
      userIds: [],
      deadline: '',
    })
    setSelectedFiles([])
    await fetchAvailableUsers()
    setForwardDialogOpen(true)
  }

  const handleForwardSubmit = async () => {
    if (!selectedRequest) return

    if (!forwardFormData.title.trim()) {
      toast({
        title: 'Lỗi',
        description: 'Tiêu đề không được để trống',
        variant: 'destructive',
      })
      return
    }

    if (!forwardFormData.deadline) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn thời hạn báo cáo',
        variant: 'destructive',
      })
      return
    }

    if (forwardFormData.userIds.length === 0) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn ít nhất một người nhận',
        variant: 'destructive',
      })
      return
    }

    setForwardLoading(true)
    try {
      // Forward request
      await reportRequestApi.forward(selectedRequest.id, {
        title: forwardFormData.title.trim(),
        forwardNote: forwardFormData.forwardNote.trim() || undefined,
        userIds: forwardFormData.userIds,
        deadline: fromDateTimeLocal(forwardFormData.deadline),
      })

      toast({
        title: 'Thành công',
        description: 'Chuyển tiếp yêu cầu báo cáo thành công',
      })
      
      setForwardDialogOpen(false)
      setSelectedRequest(null)
      setSelectedFiles([])
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Có lỗi xảy ra khi chuyển tiếp',
        variant: 'destructive',
      })
    } finally {
      setForwardLoading(false)
    }
  }

  const toggleUser = (id: number) => {
    setForwardFormData(prev => ({
      ...prev,
      userIds: prev.userIds.includes(id)
        ? prev.userIds.filter(i => i !== id)
        : [...prev.userIds, id]
    }))
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const files = Array.from(e.target.files)
      setSelectedFiles(prev => [...prev, ...files])
    }
  }

  const handleRemoveFile = (index: number) => {
    setSelectedFiles(prev => prev.filter((_, i) => i !== index))
  }

  const stats = {
    total: requests.length,
    pending: requests.filter(r => r.status === 'PENDING').length,
    inProgress: requests.filter(r => r.status === 'IN_PROGRESS').length,
    overdue: requests.filter(r => isOverdue(r.deadline, r.status)).length,
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Yêu cầu nhận được</h1>
          <p className="text-gray-500 mt-1">Các yêu cầu báo cáo được gửi đến bạn</p>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="border-0 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-full bg-blue-100">
                <Inbox className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-xs text-gray-500">Tổng nhận</p>
                <p className="text-xl font-bold text-gray-900">{stats.total}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card className="border-0 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-full bg-yellow-100">
                <Clock className="h-5 w-5 text-yellow-600" />
              </div>
              <div>
                <p className="text-xs text-gray-500">Đang chờ</p>
                <p className="text-xl font-bold text-gray-900">{stats.pending}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card className="border-0 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-full bg-blue-100">
                <AlertCircle className="h-5 w-5 text-blue-600" />
              </div>
              <div>
                <p className="text-xs text-gray-500">Đang làm</p>
                <p className="text-xl font-bold text-gray-900">{stats.inProgress}</p>
              </div>
            </div>
          </CardContent>
        </Card>
        <Card className="border-0 shadow-sm">
          <CardContent className="p-4">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-full bg-red-100">
                <AlertCircle className="h-5 w-5 text-red-600" />
              </div>
              <div>
                <p className="text-xs text-gray-500">Quá hạn</p>
                <p className="text-xl font-bold text-red-600">{stats.overdue}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Table Card */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <CardTitle className="text-lg font-semibold flex items-center gap-2">
                <Inbox className="h-5 w-5 text-[#DA251D]" />
                Danh sách yêu cầu
              </CardTitle>
              <CardDescription>
                {filteredRequests.length} yêu cầu
              </CardDescription>
            </div>
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
              <Input
                placeholder="Tìm kiếm..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-[#DA251D]" />
            </div>
          ) : filteredRequests.length === 0 ? (
            <div className="text-center py-12">
              <Inbox className="h-12 w-12 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500">
                {searchTerm ? 'Không tìm thấy yêu cầu phù hợp' : 'Bạn chưa nhận được yêu cầu nào'}
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Tiêu đề</TableHead>
                    <TableHead>Người gửi</TableHead>
                    <TableHead>Hạn báo cáo</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredRequests.map((request) => (
                    <TableRow 
                      key={request.id} 
                      className={`hover:bg-gray-50 cursor-pointer ${
                        isOverdue(request.deadline, request.status) ? 'bg-red-50' : ''
                      }`}
                      onClick={() => router.push(`/management/inbox/${request.id}`)}
                    >
                      <TableCell>
                        <div className="max-w-xs">
                          <p className="font-medium text-gray-900 truncate">{request.title}</p>
                          <p className="text-xs text-gray-500">
                            Nhận: {formatDate(request.createdAt)}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div>
                          <p className="font-medium text-gray-900">{request.createdBy?.fullName}</p>
                          <p className="text-xs text-gray-500">{request.createdBy?.email}</p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className={isOverdue(request.deadline, request.status) ? 'text-red-600' : ''}>
                          <span className="text-sm">{formatDate(request.deadline)}</span>
                          {isOverdue(request.deadline, request.status) && (
                            <Badge className="ml-2 bg-red-100 text-red-700 text-xs">Quá hạn</Badge>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        {getStatusBadge(request.myResponseStatus || request.status)}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={(e) => {
                              e.stopPropagation()
                              router.push(`/management/inbox/${request.id}`)
                            }}
                            className="hover:bg-blue-50 hover:text-blue-600"
                            title="Xem chi tiết"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={(e) => {
                              e.stopPropagation()
                              handleOpenForwardDialog(request)
                            }}
                            className="hover:bg-green-50 hover:text-green-600"
                            title="Chuyển tiếp"
                          >
                            <Forward className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Forward Dialog */}
      <Dialog open={forwardDialogOpen} onOpenChange={setForwardDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Chuyển tiếp yêu cầu báo cáo</DialogTitle>
            <DialogDescription>
              Chuyển tiếp yêu cầu này đến người khác
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4 py-4">
            <div>
              <Label htmlFor="forward-title">Tiêu đề *</Label>
              <Input
                id="forward-title"
                value={forwardFormData.title}
                onChange={(e) => setForwardFormData(prev => ({ ...prev, title: e.target.value }))}
                placeholder="Tiêu đề yêu cầu"
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="forward-note">Ghi chú chuyển tiếp</Label>
              <Textarea
                id="forward-note"
                value={forwardFormData.forwardNote}
                onChange={(e) => setForwardFormData(prev => ({ ...prev, forwardNote: e.target.value }))}
                placeholder="Nhập ghi chú chuyển tiếp (tùy chọn)"
                className="mt-1"
                rows={3}
              />
            </div>

            <div>
              <Label htmlFor="forward-deadline">Thời hạn báo cáo *</Label>
              <Input
                id="forward-deadline"
                type="datetime-local"
                value={forwardFormData.deadline}
                onChange={(e) => setForwardFormData(prev => ({ ...prev, deadline: e.target.value }))}
                className="mt-1"
              />
            </div>

            <div>
              <Label>Người nhận *</Label>
              <div className="mt-2 border rounded-lg p-4 max-h-60 overflow-y-auto">
                {availableUsers.length === 0 ? (
                  <p className="text-sm text-gray-500 text-center py-4">Đang tải danh sách người nhận...</p>
                ) : (
                  <div className="space-y-2">
                    {availableUsers.map((user) => (
                      <div key={user.id} className="flex items-center space-x-2">
                        <Checkbox
                          id={`user-${user.id}`}
                          checked={forwardFormData.userIds.includes(user.id)}
                          onCheckedChange={() => toggleUser(user.id)}
                        />
                        <Label
                          htmlFor={`user-${user.id}`}
                          className="flex-1 cursor-pointer"
                        >
                          <div>
                            <p className="text-sm font-medium">{user.fullName}</p>
                            <p className="text-xs text-gray-500">{user.email}</p>
                            {user.department && (
                              <p className="text-xs text-gray-400">{user.department.name}</p>
                            )}
                          </div>
                        </Label>
                      </div>
                    ))}
                  </div>
                )}
              </div>
              {forwardFormData.userIds.length > 0 && (
                <p className="text-xs text-gray-500 mt-2">
                  Đã chọn {forwardFormData.userIds.length} người nhận
                </p>
              )}
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setForwardDialogOpen(false)}
              disabled={forwardLoading}
            >
              Hủy
            </Button>
            <Button
              onClick={handleForwardSubmit}
              disabled={forwardLoading}
              className="bg-[#DA251D] hover:bg-[#b81f18]"
            >
              {forwardLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Chuyển tiếp
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

