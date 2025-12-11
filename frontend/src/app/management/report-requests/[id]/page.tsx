"use client"

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { 
  ArrowLeft, 
  Loader2, 
  Calendar, 
  Clock, 
  Building2, 
  Building, 
  Users, 
  FileText,
  Pencil,
  Trash2,
  CheckCircle,
  XCircle,
  AlertCircle,
  Download
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useToast } from '@/components/ui/use-toast'
import { reportRequestApi, reportResponseApi } from '@/lib/api'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Star } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

interface ReportRequest {
  id: number
  title: string
  description?: string
  deadline: string
  status: string
  createdAt: string
  updatedAt: string
  createdBy?: {
    id: number
    fullName: string
    email: string
  }
  targetOrganizations?: { id: number; name: string }[]
  targetDepartments?: { id: number; name: string }[]
  targetUsers?: { id: number; fullName: string; email: string }[]
}

interface ReportResponse {
  id: number
  submittedBy: {
    id: number
    fullName: string
    email: string
  }
  note?: string
  items: {
    id: number
    title?: string
    content?: string
    progress?: number
    difficulties?: string
    fileName?: string
    filePath?: string
    fileType?: string
    fileSize?: number
  }[]
  submittedAt: string
  score?: number // Điểm đánh giá của người gửi yêu cầu
  evaluatedBy?: {
    id: number
    fullName: string
  }
  evaluatedAt?: string // Thời gian đánh giá của người gửi yêu cầu
  selfScore?: number // Điểm tự đánh giá của người nộp báo cáo
  selfEvaluatedAt?: string // Thời gian tự đánh giá của người nộp báo cáo
  comment?: string // Nhận xét của người đánh giá
}

interface HistoryItem {
  id: number
  version: number
  editedAt: string
  editedBy?: {
    id: number
    fullName: string
    email?: string
  }
  note?: string
  items: {
    title?: string
    content?: string
    progress?: number
    difficulties?: string
    fileName?: string
    filePath?: string
  }[]
}

export default function ReportRequestDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { toast } = useToast()
  const [request, setRequest] = useState<ReportRequest | null>(null)
  const [responses, setResponses] = useState<ReportResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false)
  const [evaluatingResponseId, setEvaluatingResponseId] = useState<number | null>(null)
  const [evaluationScore, setEvaluationScore] = useState<string>('')
  const [evaluationComment, setEvaluationComment] = useState<string>('')
  const [isEvaluating, setIsEvaluating] = useState(false)
  const [isSendingBack, setIsSendingBack] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyResponse, setHistoryResponse] = useState<ReportResponse | null>(null)
  const [historyItems, setHistoryItems] = useState<HistoryItem[]>([])
  const [requestHistoryOpen, setRequestHistoryOpen] = useState(false)
  const [requestHistoryLoading, setRequestHistoryLoading] = useState(false)
  const [requestHistoryItems, setRequestHistoryItems] = useState<any[]>([])
  const [commentHistoryOpen, setCommentHistoryOpen] = useState(false)
  const [commentHistoryLoading, setCommentHistoryLoading] = useState(false)
  const [commentHistoryItems, setCommentHistoryItems] = useState<any[]>([])
  const [commentHistoryResponse, setCommentHistoryResponse] = useState<ReportResponse | null>(null)
  const [isExportingWord, setIsExportingWord] = useState(false)
  
  const handleDownload = async (filePath?: string, fileName?: string) => {
    if (!filePath) return
    try {
      const res = await reportResponseApi.downloadFile(filePath)
      const blob = new Blob([res.data])
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName || 'file'
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (error: any) {
      toast({
        title: 'Không thể tải file',
        description: error.response?.data?.message || 'Vui lòng thử lại',
        variant: 'destructive',
      })
    }
  }

  const fetchRequest = async () => {
    try {
      const [requestRes, responsesRes, historyRes] = await Promise.all([
        reportRequestApi.getById(Number(params.id)),
        reportResponseApi.getByRequestId(Number(params.id)),
        reportRequestApi.getHistory(Number(params.id)).catch(() => ({ data: { data: [] } }))
      ])
      setRequest(requestRes.data.data)
      setResponses(responsesRes.data.data || [])
      setRequestHistoryItems(historyRes.data.data || [])
    } catch (error) {
      toast({
        title: 'Lỗi',
        description: 'Không thể tải thông tin yêu cầu',
        variant: 'destructive',
      })
      router.push('/management/report-requests')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (params.id) {
      fetchRequest()
    }
  }, [params.id])

  const handleDelete = async () => {
    if (!request) return

    setIsDeleting(true)
    try {
      await reportRequestApi.delete(request.id)
      toast({
        title: 'Thành công',
        description: 'Xóa yêu cầu báo cáo thành công',
      })
      router.push('/management/report-requests')
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể xóa yêu cầu',
        variant: 'destructive',
      })
    } finally {
      setIsDeleting(false)
      setIsDeleteDialogOpen(false)
    }
  }

  const handleStatusChange = async (newStatus: string) => {
    if (!request) return

    setIsUpdatingStatus(true)
    try {
      await reportRequestApi.updateStatus(request.id, newStatus)
      toast({
        title: 'Thành công',
        description: 'Cập nhật trạng thái thành công',
      })
      fetchRequest()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể cập nhật trạng thái',
        variant: 'destructive',
      })
    } finally {
      setIsUpdatingStatus(false)
    }
  }

  const handleEvaluate = async (responseId: number) => {
    const score = parseFloat(evaluationScore)
    if (isNaN(score) || score < 0 || score > 10) {
      toast({
        title: 'Lỗi',
        description: 'Điểm số phải từ 0 đến 10',
        variant: 'destructive',
      })
      return
    }

    setIsEvaluating(true)
    try {
      await reportResponseApi.evaluate(responseId, score, evaluationComment)
      toast({
        title: 'Thành công',
        description: 'Đánh giá báo cáo thành công',
      })
      setEvaluatingResponseId(null)
      setEvaluationScore('')
      setEvaluationComment('')
      fetchRequest()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể đánh giá báo cáo',
        variant: 'destructive',
      })
    } finally {
      setIsEvaluating(false)
    }
  }

  const handleSendBack = async (responseId: number) => {
    if (!evaluationComment.trim()) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng nhập nhận xét',
        variant: 'destructive',
      })
      return
    }

    setIsSendingBack(true)
    try {
      await reportResponseApi.sendBack(responseId, evaluationComment)
      toast({
        title: 'Thành công',
        description: 'Đã gửi lại người nộp báo cáo',
      })
      setEvaluatingResponseId(null)
      setEvaluationComment('')
      fetchRequest()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể gửi lại báo cáo',
        variant: 'destructive',
      })
    } finally {
      setIsSendingBack(false)
    }
  }

  const openHistory = async (response: ReportResponse) => {
    setHistoryResponse(response)
    setHistoryOpen(true)
    setHistoryLoading(true)
    try {
      const res = await reportResponseApi.getHistory(response.id)
      setHistoryItems(res.data.data || [])
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tải lịch sử',
        variant: 'destructive',
      })
      setHistoryItems([])
    } finally {
      setHistoryLoading(false)
    }
  }

  const openRequestHistory = async () => {
    if (!request) return
    setRequestHistoryOpen(true)
    setRequestHistoryLoading(true)
    try {
      const res = await reportRequestApi.getHistory(request.id)
      setRequestHistoryItems(res.data.data || [])
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tải lịch sử chỉnh sửa yêu cầu',
        variant: 'destructive',
      })
      setRequestHistoryItems([])
    } finally {
      setRequestHistoryLoading(false)
    }
  }

  const openCommentHistory = async (response: ReportResponse) => {
    setCommentHistoryResponse(response)
    setCommentHistoryOpen(true)
    setCommentHistoryLoading(true)
    try {
      const res = await reportResponseApi.getCommentHistory(response.id)
      setCommentHistoryItems(res.data.data || [])
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tải lịch sử nhận xét',
        variant: 'destructive',
      })
      setCommentHistoryItems([])
    } finally {
      setCommentHistoryLoading(false)
    }
  }

  const handleExportWord = async () => {
    if (!request) return
    
    setIsExportingWord(true)
    try {
      const res = await reportRequestApi.exportWord(request.id)
      const blob = new Blob([res.data], { 
        type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' 
      })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `BaoCao_${request.id}_${new Date().toISOString().split('T')[0]}.docx`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      toast({
        title: 'Thành công',
        description: 'Đã tải file Word tổng hợp báo cáo',
      })
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tải file Word',
        variant: 'destructive',
      })
    } finally {
      setIsExportingWord(false)
    }
  }


  const getStatusBadge = (status: string) => {
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

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Clock className="h-5 w-5 text-yellow-600" />
      case 'IN_PROGRESS':
        return <AlertCircle className="h-5 w-5 text-blue-600" />
      case 'SUBMITTED':
        return <FileText className="h-5 w-5 text-purple-600" />
      case 'COMPLETED':
        return <CheckCircle className="h-5 w-5 text-green-600" />
      case 'CANCELLED':
        return <XCircle className="h-5 w-5 text-gray-600" />
      default:
        return <Clock className="h-5 w-5" />
    }
  }

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('vi-VN', {
      weekday: 'long',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }

  const isOverdue = (deadline: string) => {
    return new Date(deadline) < new Date() && request?.status !== 'COMPLETED' && request?.status !== 'CANCELLED'
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-[#DA251D]" />
      </div>
    )
  }

  if (!request) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Không tìm thấy yêu cầu báo cáo</p>
        <Button onClick={() => router.push('/management/report-requests')} className="mt-4">
          Quay lại danh sách
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.back()}
          >
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-gray-900">Chi tiết yêu cầu</h1>
            <p className="text-gray-500 mt-1">Xem thông tin chi tiết yêu cầu báo cáo</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={() => router.push(`/management/report-requests/${request.id}/edit`)}
          >
            <Pencil className="h-4 w-4 mr-2" />
            Chỉnh sửa
          </Button>
          <Button
            variant="destructive"
            onClick={() => setIsDeleteDialogOpen(true)}
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Xóa
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
          {/* Request Info */}
          <Card className="border-0 shadow-md">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-3">
                  <div className="p-3 rounded-full bg-[#DA251D]/10">
                    <FileText className="h-6 w-6 text-[#DA251D]" />
                  </div>
                  <div>
                    <CardTitle className="text-xl">{request.title}</CardTitle>
                    <CardDescription className="mt-1">
                      Tạo bởi: {request.createdBy?.fullName || 'N/A'}
                    </CardDescription>
                  </div>
                </div>
                {getStatusBadge(request.status)}
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {request.description && (
                <div>
                  <h3 className="text-sm font-medium text-gray-500 mb-2">Mô tả</h3>
                  <p className="text-gray-900 whitespace-pre-wrap">{request.description}</p>
                </div>
              )}
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t">
                <div className="flex items-center gap-3">
                  <Calendar className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Thời hạn báo cáo</p>
                    <p className={`font-medium ${isOverdue(request.deadline) ? 'text-red-600' : 'text-gray-900'}`}>
                      {formatDate(request.deadline)}
                      {isOverdue(request.deadline) && (
                        <span className="ml-2 text-xs bg-red-100 text-red-600 px-2 py-0.5 rounded">
                          Quá hạn
                        </span>
                      )}
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Clock className="h-5 w-5 text-gray-400" />
                  <div>
                    <p className="text-sm text-gray-500">Ngày tạo</p>
                    <p className="font-medium text-gray-900">{formatDate(request.createdAt)}</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Target Recipients */}
          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle className="text-lg">Đối tượng nhận yêu cầu</CardTitle>
              <CardDescription>
                Danh sách các cơ quan, phòng ban và cá nhân được gửi yêu cầu
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Organizations */}
              {request.targetOrganizations && request.targetOrganizations.length > 0 && (
                <div>
                  <div className="flex items-center gap-2 mb-3">
                    <Building2 className="h-5 w-5 text-[#DA251D]" />
                    <h3 className="font-medium">Cơ quan ({request.targetOrganizations.length})</h3>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {request.targetOrganizations.map((org) => (
                      <Badge key={org.id} variant="outline" className="px-3 py-1">
                        {org.name}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Departments */}
              {request.targetDepartments && request.targetDepartments.length > 0 && (
                <div>
                  <div className="flex items-center gap-2 mb-3">
                    <Building className="h-5 w-5 text-blue-600" />
                    <h3 className="font-medium">Phòng ban ({request.targetDepartments.length})</h3>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {request.targetDepartments.map((dept) => (
                      <Badge key={dept.id} variant="secondary" className="px-3 py-1">
                        {dept.name}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Users */}
              {request.targetUsers && request.targetUsers.length > 0 && (
                <div>
                  <div className="flex items-center gap-2 mb-3">
                    <Users className="h-5 w-5 text-green-600" />
                    <h3 className="font-medium">Cá nhân ({request.targetUsers.length})</h3>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {request.targetUsers.map((user) => (
                      <Badge key={user.id} className="px-3 py-1 bg-green-100 text-green-700 hover:bg-green-100">
                        {user.fullName}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {(!request.targetOrganizations || request.targetOrganizations.length === 0) &&
               (!request.targetDepartments || request.targetDepartments.length === 0) &&
               (!request.targetUsers || request.targetUsers.length === 0) && (
                <p className="text-gray-500 text-center py-4">Không có đối tượng nhận</p>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Status Card */}
          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2">
                {getStatusIcon(request.status)}
                Trạng thái
              </CardTitle>
            </CardHeader>
            <CardContent>
              <Select
                value={request.status}
                onValueChange={handleStatusChange}
                disabled={isUpdatingStatus}
              >
                <SelectTrigger className="w-full">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PENDING">Đang chờ</SelectItem>
                  <SelectItem value="IN_PROGRESS">Đang thực hiện</SelectItem>
                  <SelectItem value="SUBMITTED">Đã nộp, chờ đánh giá</SelectItem>
                  <SelectItem value="COMPLETED">Hoàn thành</SelectItem>
                  <SelectItem value="CANCELLED">Đã hủy</SelectItem>
                </SelectContent>
              </Select>
              {isUpdatingStatus && (
                <p className="text-sm text-gray-500 mt-2 flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Đang cập nhật...
                </p>
              )}
            </CardContent>
          </Card>

          {/* Timeline Card */}
          <Card className="border-0 shadow-md">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">Lịch sử</CardTitle>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={openRequestHistory}
                >
                  Xem chi tiết
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                <div className="flex gap-3">
                  <div className="flex flex-col items-center">
                    <div className="w-3 h-3 rounded-full bg-[#DA251D]"></div>
                    {requestHistoryItems.length > 0 && (
                      <div className="w-0.5 h-full bg-gray-200"></div>
                    )}
                  </div>
                  <div className={requestHistoryItems.length > 0 ? 'pb-2' : ''}>
                    <p className="text-sm font-medium">Tạo yêu cầu</p>
                    <p className="text-xs text-gray-500">{formatDate(request.createdAt)}</p>
                  </div>
                </div>
                {requestHistoryItems.map((h, index) => (
                  <div key={h.id} className="flex gap-3">
                    <div className="flex flex-col items-center">
                      <div className="w-3 h-3 rounded-full bg-blue-500"></div>
                      {index < requestHistoryItems.length - 1 && (
                        <div className="w-0.5 h-full bg-gray-200"></div>
                      )}
                    </div>
                    <div className={index < requestHistoryItems.length - 1 ? 'pb-2' : ''}>
                      <p className="text-sm font-medium">Lần {h.version} chỉnh sửa</p>
                      <p className="text-xs text-gray-500">
                        {new Date(h.editedAt).toLocaleString('vi-VN')}
                      </p>
                      <p className="text-xs text-gray-400 mt-0.5">
                        {h.editedBy?.fullName || 'N/A'}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Submitted Reports Section */}
      {responses.length > 0 && (
        <Card className="border-0 shadow-md">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-lg flex items-center gap-2">
                  <FileText className="h-5 w-5 text-[#DA251D]" />
                  Báo cáo đã nộp ({responses.length})
                </CardTitle>
                <CardDescription>
                  Xem và đánh giá các báo cáo đã được nộp
                </CardDescription>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  onClick={handleExportWord}
                  disabled={isExportingWord || responses.length === 0}
                >
                  {isExportingWord ? (
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  ) : (
                    <Download className="h-4 w-4 mr-2" />
                  )}
                  Tải tổng hợp Word
                </Button>
                {isOverdue(request.deadline) && responses.some(r => new Date(r.submittedAt) > new Date(request.deadline)) && (
                  <Badge className="bg-red-100 text-red-700 border-red-300 text-sm">
                    Có báo cáo quá hạn
                  </Badge>
                )}
              </div>
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {responses.map((response) => (
              <Card key={response.id} className="border">
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle className="text-base">
                        Báo cáo từ: {response.submittedBy.fullName}
                      </CardTitle>
                      <CardDescription>
                        Nộp lúc: {new Date(response.submittedAt).toLocaleString('vi-VN')}
                        {isOverdue(request.deadline) && new Date(response.submittedAt) > new Date(request.deadline) && (
                          <span className="ml-2 text-red-600 font-medium">(Nộp sau hạn)</span>
                        )}
                      </CardDescription>
                    </div>
                    <div className="flex flex-col items-end gap-1">
                      {isOverdue(request.deadline) && new Date(response.submittedAt) > new Date(request.deadline) ? (
                        <Badge className="bg-red-100 text-red-700 border-red-300">Báo cáo quá hạn</Badge>
                      ) : response.score !== null && response.score !== undefined ? (
                        <div className="flex items-center gap-2">
                          <Star className="h-5 w-5 text-yellow-500 fill-yellow-500" />
                          <span className="text-lg font-bold text-yellow-600">
                            Điểm của tôi: {response.score}/10
                          </span>
                          {response.evaluatedAt && (
                            <span className="text-xs text-gray-500">
                              ({new Date(response.evaluatedAt).toLocaleDateString('vi-VN')})
                            </span>
                          )}
                        </div>
                      ) : (
                        <Badge className="bg-purple-100 text-purple-700">Chờ đánh giá</Badge>
                      )}
                      {response.selfScore !== null && response.selfScore !== undefined && (
                        <div className="flex items-center gap-2">
                          <Star className="h-4 w-4 text-blue-500 fill-blue-500" />
                          <span className="text-sm font-semibold text-blue-600">
                            Tự đánh giá: {response.selfScore}/10
                          </span>
                          {response.selfEvaluatedAt && (
                            <span className="text-xs text-gray-500">
                              ({new Date(response.selfEvaluatedAt).toLocaleDateString('vi-VN')})
                            </span>
                          )}
                        </div>
                      )}
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openHistory(response)}
                        >
                          Lịch sử chỉnh sửa
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => router.push(`/management/report-requests/${params.id}/evaluate/${response.id}`)}
                        >
                          Xem chi tiết
                        </Button>
                      </div>
                    </div>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  {response.note && (
                    <div className="bg-gray-50 rounded-lg p-4">
                      <h4 className="text-sm font-medium text-gray-500 mb-1">Ghi chú:</h4>
                      <p className="text-gray-900">{response.note}</p>
                    </div>
                  )}
                  
                  <div className="space-y-3">
                    <h4 className="text-sm font-medium text-gray-500">Nội dung báo cáo:</h4>
                    {response.items.map((item, index) => (
                      <div key={item.id} className="border rounded-lg p-4 bg-white space-y-3">
                        <div className="flex items-start gap-3">
                          <Badge variant="outline" className="shrink-0">Mục {index + 1}</Badge>
                          <div className="flex-1 min-w-0 space-y-2">
                            {item.title && (
                              <h4 className="font-semibold text-gray-900">{item.title}</h4>
                            )}
                            {item.content && (
                              <p className="text-gray-700 whitespace-pre-wrap">{item.content}</p>
                            )}
                            {item.progress !== undefined && item.progress !== null && (
                              <div className="space-y-1">
                                <div className="flex items-center justify-between text-sm">
                                  <span className="text-gray-600">Tiến độ hoàn thành:</span>
                                  <span className="font-medium">{item.progress}%</span>
                                </div>
                                <div className="w-full bg-gray-200 rounded-full h-2">
                                  <div 
                                    className="bg-[#DA251D] h-2 rounded-full transition-all"
                                    style={{ width: `${item.progress}%` }}
                                  />
                                </div>
                              </div>
                            )}
                            {item.difficulties && (
                              <div className="bg-yellow-50 border border-yellow-200 rounded p-3">
                                <p className="text-sm font-medium text-yellow-800 mb-1">Khó khăn gặp phải:</p>
                                <p className="text-sm text-yellow-900 whitespace-pre-wrap">{item.difficulties}</p>
                              </div>
                            )}
                            {item.filePath && (
                              <div className="flex items-center gap-2 pt-2 border-t">
                                <FileText className="h-4 w-4 text-gray-500" />
                                <Button
                                  variant="link"
                                  className="p-0 text-blue-600"
                                  onClick={() => handleDownload(item.filePath, item.fileName)}
                                >
                                  {item.fileName || 'Tải file đính kèm'}
                                </Button>
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {response.score === null || response.score === undefined ? (
                    <div className="pt-4 border-t space-y-4">
                      {evaluatingResponseId === response.id ? (
                        <div className="space-y-3">
                          <div>
                            <label className="text-sm font-medium text-gray-700 mb-2 block">
                              Nhận xét
                            </label>
                            <Textarea
                              placeholder="Nhập nhận xét về báo cáo..."
                              value={evaluationComment}
                              onChange={(e) => setEvaluationComment(e.target.value)}
                              rows={4}
                              className="w-full"
                            />
                          </div>
                          <div className="flex items-center gap-3">
                            <Input
                              type="number"
                              min="0"
                              max="10"
                              step="0.1"
                              placeholder="Nhập điểm (0-10)"
                              value={evaluationScore}
                              onChange={(e) => setEvaluationScore(e.target.value)}
                              className="w-32"
                            />
                            <Button
                              onClick={() => handleEvaluate(response.id)}
                              disabled={isEvaluating || !evaluationScore}
                              className="bg-[#DA251D] hover:bg-[#b91c1c]"
                            >
                              {isEvaluating ? (
                                <>
                                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                  Đang xử lý...
                                </>
                              ) : (
                                <>
                                  <Star className="h-4 w-4 mr-2" />
                                  Nhận xét và đánh giá
                                </>
                              )}
                            </Button>
                            <Button
                              onClick={() => handleSendBack(response.id)}
                              disabled={isSendingBack || !evaluationComment.trim()}
                              variant="outline"
                              className="border-blue-500 text-blue-600 hover:bg-blue-50"
                            >
                              {isSendingBack ? (
                                <>
                                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                                  Đang gửi...
                                </>
                              ) : (
                                'Gửi lại người nộp báo cáo'
                              )}
                            </Button>
                            <Button
                              variant="outline"
                              onClick={() => {
                                setEvaluatingResponseId(null)
                                setEvaluationScore('')
                                setEvaluationComment('')
                              }}
                            >
                              Hủy
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <Button
                          onClick={() => {
                            setEvaluatingResponseId(response.id)
                            setEvaluationScore('')
                            setEvaluationComment('')
                          }}
                          className="bg-[#DA251D] hover:bg-[#b91c1c]"
                        >
                          <Star className="h-4 w-4 mr-2" />
                          Đánh giá báo cáo
                        </Button>
                      )}
                    </div>
                  ) : (
                    <div className="pt-4 border-t space-y-2">
                      <div className="flex items-center justify-between">
                        <p className="text-sm text-gray-500">
                          Đã được đánh giá bởi: {response.evaluatedBy?.fullName || 'N/A'}
                          {response.evaluatedAt && (
                            <span className="ml-2">
                              ({new Date(response.evaluatedAt).toLocaleString('vi-VN')})
                            </span>
                          )}
                        </p>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openCommentHistory(response)}
                        >
                          Xem lịch sử nhận xét
                        </Button>
                      </div>
                      {response.comment && (
                        <div className="mt-2 p-3 bg-blue-50 rounded-lg border border-blue-200">
                          <p className="text-sm font-medium text-blue-700 mb-1">Nhận xét mới nhất:</p>
                          <p className="text-sm text-blue-900 whitespace-pre-wrap">{response.comment}</p>
                        </div>
                      )}
                    </div>
                  )}
                </CardContent>
              </Card>
            ))}
          </CardContent>
        </Card>
      )}

      {/* History dialog */}
      <Dialog open={historyOpen} onOpenChange={setHistoryOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Lịch sử chỉnh sửa</DialogTitle>
            <DialogDescription>
              {historyResponse ? `Báo cáo của ${historyResponse.submittedBy.fullName}` : ''}
            </DialogDescription>
          </DialogHeader>
          {historyLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-[#DA251D]" />
            </div>
          ) : historyItems.length === 0 ? (
            <p className="text-sm text-gray-500 py-4">Chưa có lịch sử chỉnh sửa</p>
          ) : (
            <div className="max-h-[500px] pr-2 overflow-y-auto space-y-4">
              {historyItems.map((h) => (
                <div key={h.id} className="border rounded-lg p-3 bg-gray-50">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold">
                        Lần {h.version} • {new Date(h.editedAt).toLocaleString('vi-VN')}
                      </p>
                      <p className="text-xs text-gray-600">
                        Người chỉnh sửa: {h.editedBy?.fullName || 'N/A'}
                      </p>
                    </div>
                    {h.note && (
                      <span className="text-xs text-gray-700 bg-white px-2 py-1 rounded border">
                        Ghi chú: {h.note}
                      </span>
                    )}
                  </div>
                  <div className="mt-3 space-y-2">
                    {h.items.map((item, idx) => (
                      <div key={idx} className="border rounded p-2 bg-white space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="text-sm font-medium">Mục {idx + 1}</span>
                          {item.progress !== undefined && (
                            <span className="text-xs text-gray-500">{item.progress}%</span>
                          )}
                        </div>
                        {item.title && <p className="text-sm font-semibold text-gray-900">{item.title}</p>}
                        {item.content && <p className="text-sm text-gray-700 whitespace-pre-wrap">{item.content}</p>}
                        {item.difficulties && (
                          <p className="text-xs text-yellow-700 bg-yellow-50 border border-yellow-200 rounded p-2">
                            Khó khăn: {item.difficulties}
                          </p>
                        )}
                        {item.filePath && (
                          <Button
                            variant="link"
                            className="p-0 text-blue-600"
                            onClick={() => handleDownload(item.filePath, item.fileName)}
                          >
                            {item.fileName || 'Tải file đính kèm'}
                          </Button>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Request History Dialog */}
      <Dialog open={requestHistoryOpen} onOpenChange={setRequestHistoryOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Lịch sử chỉnh sửa yêu cầu</DialogTitle>
            <DialogDescription>
              {request ? `Yêu cầu: ${request.title}` : ''}
            </DialogDescription>
          </DialogHeader>
          {requestHistoryLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-[#DA251D]" />
            </div>
          ) : requestHistoryItems.length === 0 ? (
            <p className="text-sm text-gray-500 py-4">Chưa có lịch sử chỉnh sửa</p>
          ) : (
            <div className="max-h-[500px] pr-2 overflow-y-auto space-y-4">
              {requestHistoryItems.map((h) => (
                <div key={h.id} className="border rounded-lg p-4 bg-gray-50">
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <div>
                      <p className="text-sm font-semibold">
                        Lần {h.version} • {new Date(h.editedAt).toLocaleString('vi-VN')}
                      </p>
                      <p className="text-xs text-gray-600">
                        Người chỉnh sửa: {h.editedBy?.fullName || 'N/A'}
                      </p>
                    </div>
                  </div>
                  <div className="space-y-3">
                    <div>
                      <p className="text-xs font-medium text-gray-700 mb-1">Tiêu đề:</p>
                      <p className="text-sm text-gray-900 bg-white p-2 rounded border">{h.title || 'N/A'}</p>
                    </div>
                    {h.description && (
                      <div>
                        <p className="text-xs font-medium text-gray-700 mb-1">Mô tả:</p>
                        <p className="text-sm text-gray-700 bg-white p-2 rounded border whitespace-pre-wrap">{h.description}</p>
                      </div>
                    )}
                    <div>
                      <p className="text-xs font-medium text-gray-700 mb-1">Hạn báo cáo:</p>
                      <p className="text-sm text-gray-900 bg-white p-2 rounded border">
                        {new Date(h.deadline).toLocaleString('vi-VN')}
                      </p>
                    </div>
                    {(h.targetOrganizations?.length > 0 || h.targetDepartments?.length > 0 || h.targetUsers?.length > 0) && (
                      <div>
                        <p className="text-xs font-medium text-gray-700 mb-2">Người nhận:</p>
                        <div className="flex flex-wrap gap-2">
                          {h.targetOrganizations?.map((org: any) => (
                            <Badge key={`org-${org.id}`} variant="outline" className="text-xs">
                              {org.name}
                            </Badge>
                          ))}
                          {h.targetDepartments?.map((dept: any) => (
                            <Badge key={`dept-${dept.id}`} variant="secondary" className="text-xs">
                              {dept.name}
                            </Badge>
                          ))}
                          {h.targetUsers?.map((user: any) => (
                            <Badge key={`user-${user.id}`} className="text-xs bg-blue-100 text-blue-700">
                              {user.fullName}
                            </Badge>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Comment History Dialog */}
      <Dialog open={commentHistoryOpen} onOpenChange={setCommentHistoryOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Lịch sử nhận xét</DialogTitle>
            <DialogDescription>
              {commentHistoryResponse ? `Báo cáo của ${commentHistoryResponse.submittedBy.fullName}` : ''}
            </DialogDescription>
          </DialogHeader>
          {commentHistoryLoading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="h-6 w-6 animate-spin text-[#DA251D]" />
            </div>
          ) : commentHistoryItems.length === 0 ? (
            <p className="text-sm text-gray-500 py-4">Chưa có lịch sử nhận xét</p>
          ) : (
            <div className="max-h-[500px] pr-2 overflow-y-auto space-y-4">
              {commentHistoryItems.map((comment) => (
                <div key={comment.id} className="border rounded-lg p-4 bg-gray-50">
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-2">
                        <p className="text-sm font-semibold">
                          {new Date(comment.commentedAt).toLocaleString('vi-VN')}
                        </p>
                        {comment.isFinalEvaluation && (
                          <Badge className="bg-green-100 text-green-700 text-xs">Đánh giá cuối</Badge>
                        )}
                        {!comment.isFinalEvaluation && (
                          <Badge className="bg-blue-100 text-blue-700 text-xs">Gửi lại</Badge>
                        )}
                      </div>
                      <p className="text-xs text-gray-600">
                        Người nhận xét: {comment.commentedBy?.fullName || 'N/A'}
                      </p>
                      {comment.score !== null && comment.score !== undefined && (
                        <div className="flex items-center gap-1 mt-1">
                          <Star className="h-4 w-4 text-yellow-500 fill-yellow-500" />
                          <span className="text-sm font-semibold text-yellow-600">
                            Điểm: {comment.score}/10
                          </span>
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="mt-3 p-3 bg-white rounded-lg border">
                    <p className="text-sm text-gray-900 whitespace-pre-wrap">{comment.comment}</p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xác nhận xóa</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc chắn muốn xóa yêu cầu &quot;{request.title}&quot;?
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

