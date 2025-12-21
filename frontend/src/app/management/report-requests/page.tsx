"use client"

import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, FileText, Loader2, Search, Eye } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/components/ui/use-toast'
import { useRouter } from 'next/navigation'
import { reportRequestApi } from '@/lib/api'
import { isPast, formatDate as formatDateUtil } from '@/lib/utils'

interface ReportRequest {
  id: number
  title: string
  description?: string
  deadline: string
  status: string
  createdAt: string
  targetOrganizations?: { id: number; name: string }[]
  targetDepartments?: { id: number; name: string }[]
  targetUsers?: { id: number; fullName: string }[]
  completedCount?: number
  submittedCount?: number
  pendingCount?: number
  completedUserIds?: number[]
}

export default function ReportRequestsPage() {
  const { toast } = useToast()
  const router = useRouter()
  const [requests, setRequests] = useState<ReportRequest[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [selectedRequest, setSelectedRequest] = useState<ReportRequest | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const fetchRequests = async () => {
    try {
      const response = await reportRequestApi.getMyRequests()
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

  const handleDelete = async () => {
    if (!selectedRequest) return

    setIsDeleting(true)
    try {
      await reportRequestApi.delete(selectedRequest.id)
      toast({
        title: 'Thành công',
        description: 'Xóa yêu cầu báo cáo thành công',
      })
      setIsDeleteDialogOpen(false)
      setSelectedRequest(null)
      fetchRequests()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể xóa yêu cầu',
        variant: 'destructive',
      })
    } finally {
      setIsDeleting(false)
    }
  }

  const isOverdue = (deadline: string, status: string) => {
    return isPast(deadline) && status !== 'COMPLETED' && status !== 'CANCELLED'
  }

  const getStatusBadge = (status: string, deadline: string) => {
    // Check if overdue and status is PENDING or IN_PROGRESS
    if (isOverdue(deadline, status) && (status === 'PENDING' || status === 'IN_PROGRESS')) {
      return <Badge className="bg-red-100 text-red-700 hover:bg-red-100 border-red-300">Quá hạn</Badge>
    }
    
    switch (status) {
      case 'PENDING':
        return <Badge className="bg-yellow-100 text-yellow-700 hover:bg-yellow-100">Đang chờ</Badge>
      case 'IN_PROGRESS':
        return <Badge className="bg-blue-100 text-blue-700 hover:bg-blue-100">Đang thực hiện</Badge>
      case 'SUBMITTED':
        return <Badge className="bg-purple-100 text-purple-700 hover:bg-purple-100">Đã nộp, chờ đánh giá</Badge>
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

  const filteredRequests = requests.filter((request) =>
    request.title.toLowerCase().includes(searchTerm.toLowerCase())
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Yêu cầu báo cáo</h1>
          <p className="text-gray-500 mt-1">Quản lý các yêu cầu báo cáo của bạn</p>
        </div>
        <Button
          onClick={() => router.push('/management/report-requests/create')}
          className="bg-[#DA251D] hover:bg-[#b81f18]"
        >
          <Plus className="h-4 w-4 mr-2" />
          Tạo yêu cầu mới
        </Button>
      </div>

      {/* Table Card */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <CardTitle className="text-lg font-semibold flex items-center gap-2">
                <FileText className="h-5 w-5 text-[#DA251D]" />
                Danh sách yêu cầu
              </CardTitle>
              <CardDescription>
                Tổng cộng {filteredRequests.length} yêu cầu
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
            <div className="text-center py-12 text-gray-500">
              {searchTerm ? 'Không tìm thấy yêu cầu phù hợp' : 'Chưa có yêu cầu nào'}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Tiêu đề</TableHead>
                    <TableHead>Gửi đến</TableHead>
                    <TableHead>Hạn báo cáo</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredRequests.map((request) => (
                    <TableRow key={request.id} className="hover:bg-gray-50">
                      <TableCell>
                        <div className="max-w-xs">
                          <p className="font-medium text-gray-900 truncate">{request.title}</p>
                          <p className="text-xs text-gray-500">
                            Tạo: {formatDate(request.createdAt)}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="space-y-2">
                          <div className="flex flex-wrap gap-1 max-w-xs">
                            {request.targetOrganizations?.map((org) => (
                              <Badge key={`org-${org.id}`} variant="outline" className="text-xs">
                                {org.name}
                              </Badge>
                            ))}
                            {request.targetDepartments?.map((dept) => (
                              <Badge key={`dept-${dept.id}`} variant="secondary" className="text-xs">
                                {dept.name}
                              </Badge>
                            ))}
                            {request.targetUsers?.map((user) => {
                              const isCompleted = request.completedUserIds?.includes(user.id) || false
                              return (
                                <Badge 
                                  key={`user-${user.id}`} 
                                  className={`text-xs ${
                                    isCompleted 
                                      ? 'bg-green-100 text-green-700 border-green-300' 
                                      : 'bg-blue-100 text-blue-700'
                                  }`}
                                >
                                  {user.fullName}
                                </Badge>
                              )
                            })}
                          </div>
                          {(request.completedCount !== undefined || request.submittedCount !== undefined || request.pendingCount !== undefined) && (
                            <div className="text-xs text-gray-500 mt-1">
                              {request.completedCount ? `${request.completedCount} hoàn thành` : ''}
                              {request.completedCount && request.submittedCount ? ', ' : ''}
                              {request.submittedCount ? `${request.submittedCount} đã nộp, chờ đánh giá` : ''}
                              {((request.completedCount || request.submittedCount) && request.pendingCount) ? ', ' : ''}
                              {request.pendingCount ? `${request.pendingCount} chưa nộp` : ''}
                            </div>
                          )}
                        </div>
                      </TableCell>
                      <TableCell>
                        <span className="text-sm">{formatDate(request.deadline)}</span>
                      </TableCell>
                      <TableCell>
                        {getStatusBadge(request.status, request.deadline)}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => router.push(`/management/report-requests/${request.id}`)}
                            className="hover:bg-blue-50 hover:text-blue-600"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => router.push(`/management/report-requests/${request.id}/edit`)}
                            className="hover:bg-green-50 hover:text-green-600"
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                              setSelectedRequest(request)
                              setIsDeleteDialogOpen(true)
                            }}
                            className="hover:bg-red-50 hover:text-red-600"
                          >
                            <Trash2 className="h-4 w-4" />
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

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xác nhận xóa</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc chắn muốn xóa yêu cầu &quot;{selectedRequest?.title}&quot;?
              Hành động này không thể hoàn tác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-[#DA251D] hover:bg-[#b81f18]"
              disabled={isDeleting}
            >
              {isDeleting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Xóa
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

