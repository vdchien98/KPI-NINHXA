"use client"

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { 
  ArrowLeft, 
  Loader2, 
  Calendar, 
  Clock, 
  User,
  FileText,
  CheckCircle,
  XCircle,
  AlertCircle,
  Edit,
  Star,
  Forward
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { useToast } from '@/components/ui/use-toast'
import { reportRequestApi, reportResponseApi, commonApi } from '@/lib/api'
import { useAuthStore } from '@/lib/store'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

interface ReportResponse {
  id: number
  note?: string
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
}

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
}

export default function ReceivedRequestDetailPage() {
  const params = useParams()
  const router = useRouter()
  const { toast } = useToast()
  const [request, setRequest] = useState<ReportRequest | null>(null)
  const [myResponse, setMyResponse] = useState<ReportResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [isUpdatingStatus, setIsUpdatingStatus] = useState(false)
  
  // Tính status của response của user hiện tại
  const getMyResponseStatus = (): string => {
    if (!myResponse) {
      return 'PENDING' // Chưa nộp
    }
    if (myResponse.score !== null && myResponse.score !== undefined) {
      return 'COMPLETED' // Đã được đánh giá (hoàn thành)
    }
    return 'SUBMITTED' // Đã nộp nhưng chưa được đánh giá
  }
  
  const myResponseStatus = getMyResponseStatus()
  const [selfEvaluatingResponseId, setSelfEvaluatingResponseId] = useState<number | null>(null)
  const [selfEvaluationScore, setSelfEvaluationScore] = useState('')
  const [isSelfEvaluating, setIsSelfEvaluating] = useState(false)
  const [requestHistoryOpen, setRequestHistoryOpen] = useState(false)
  const [requestHistoryLoading, setRequestHistoryLoading] = useState(false)
  const [requestHistoryItems, setRequestHistoryItems] = useState<any[]>([])
  const [commentHistoryOpen, setCommentHistoryOpen] = useState(false)
  const [commentHistoryLoading, setCommentHistoryLoading] = useState(false)
  const [commentHistoryItems, setCommentHistoryItems] = useState<any[]>([])
  const [forwardDialogOpen, setForwardDialogOpen] = useState(false)
  const [forwardLoading, setForwardLoading] = useState(false)
  const [availableUsers, setAvailableUsers] = useState<any[]>([])
  const [forwardFormData, setForwardFormData] = useState({
    title: '',
    forwardNote: '',
    userIds: [] as number[],
    deadline: '',
  })
  const currentUser = useAuthStore((state) => state.user)
  
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
      const [requestRes, responseRes, historyRes] = await Promise.all([
        reportRequestApi.getById(Number(params.id)),
        reportResponseApi.getMyResponseForRequest(Number(params.id)),
        reportRequestApi.getHistory(Number(params.id)).catch(() => ({ data: { data: [] } }))
      ])
      setRequest(requestRes.data.data)
      setMyResponse(responseRes.data.data)
      setRequestHistoryItems(historyRes.data.data || [])
    } catch (error) {
      toast({
        title: 'Lỗi',
        description: 'Không thể tải thông tin yêu cầu',
        variant: 'destructive',
      })
      router.push('/management/inbox')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (params.id) {
      fetchRequest()
    }
  }, [params.id])
  

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

  const handleSelfEvaluate = async (responseId: number) => {
    const score = parseFloat(selfEvaluationScore)
    if (isNaN(score) || score < 0 || score > 10) {
      toast({
        title: 'Lỗi',
        description: 'Điểm số phải từ 0 đến 10',
        variant: 'destructive',
      })
      return
    }

    setIsSelfEvaluating(true)
    try {
      await reportResponseApi.selfEvaluate(responseId, score)
      toast({
        title: 'Thành công',
        description: 'Tự đánh giá báo cáo thành công',
      })
      setSelfEvaluatingResponseId(null)
      setSelfEvaluationScore('')
      fetchRequest()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tự đánh giá báo cáo',
        variant: 'destructive',
      })
    } finally {
      setIsSelfEvaluating(false)
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
    return new Date(deadline) < new Date() && myResponseStatus !== 'COMPLETED'
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

  const openCommentHistory = async () => {
    if (!myResponse) return
    setCommentHistoryOpen(true)
    setCommentHistoryLoading(true)
    try {
      const res = await reportResponseApi.getCommentHistory(myResponse.id)
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
        (u: any) => u.id !== currentUser?.id && u.id !== 1
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

  const handleOpenForwardDialog = async () => {
    if (!request) return
    setForwardFormData({
      title: `[Chuyển tiếp] ${request.title}`,
      forwardNote: '',
      userIds: [],
      deadline: '',
    })
    await fetchAvailableUsers()
    setForwardDialogOpen(true)
  }

  const handleForwardSubmit = async () => {
    if (!request) return

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
      await reportRequestApi.forward(request.id, {
        title: forwardFormData.title.trim(),
        forwardNote: forwardFormData.forwardNote.trim() || undefined,
        userIds: forwardFormData.userIds,
        deadline: new Date(forwardFormData.deadline).toISOString(),
      })

      toast({
        title: 'Thành công',
        description: 'Chuyển tiếp yêu cầu báo cáo thành công',
      })
      
      setForwardDialogOpen(false)
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
        <Button onClick={() => router.push('/management/inbox')} className="mt-4">
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
            <p className="text-gray-500 mt-1">Xem thông tin yêu cầu báo cáo nhận được</p>
          </div>
        </div>
      </div>

      {/* Overdue Warning */}
      {isOverdue(request.deadline) && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-3">
          <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0" />
          <div>
            <p className="text-sm font-medium text-red-800">Yêu cầu này đã quá hạn!</p>
            <p className="text-sm text-red-600">
              Vui lòng hoàn thành báo cáo ngay hoặc liên hệ người gửi.
            </p>
          </div>
        </div>
      )}

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
                      Yêu cầu từ: {request.createdBy?.fullName || 'N/A'}
                    </CardDescription>
                  </div>
                </div>
                {getStatusBadge(myResponseStatus)}
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              {request.description && (
                <div className="bg-gray-50 rounded-lg p-4">
                  <h3 className="text-sm font-medium text-gray-500 mb-2">Nội dung yêu cầu</h3>
                  <p className="text-gray-900 whitespace-pre-wrap">{request.description}</p>
                </div>
              )}
              
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-4 border-t">
                <div className="flex items-center gap-3">
                  <Calendar className={`h-5 w-5 ${isOverdue(request.deadline) ? 'text-red-500' : 'text-gray-400'}`} />
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
                    <p className="text-sm text-gray-500">Ngày nhận</p>
                    <p className="font-medium text-gray-900">{formatDate(request.createdAt)}</p>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Sender Info */}
          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle className="text-lg flex items-center gap-2">
                <User className="h-5 w-5 text-[#DA251D]" />
                Thông tin người gửi
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex items-center gap-4">
                <div className="h-12 w-12 rounded-full bg-[#DA251D] flex items-center justify-center text-white font-bold text-lg">
                  {request.createdBy?.fullName?.charAt(0) || '?'}
                </div>
                <div>
                  <p className="font-medium text-gray-900">{request.createdBy?.fullName}</p>
                  <p className="text-sm text-gray-500">{request.createdBy?.email}</p>
                </div>
              </div>
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
                Cập nhật trạng thái
              </CardTitle>
              <CardDescription>
                Cập nhật tiến độ thực hiện báo cáo của bạn
              </CardDescription>
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
                  <SelectItem value="COMPLETED">Hoàn thành</SelectItem>
                </SelectContent>
              </Select>
              {isUpdatingStatus && (
                <p className="text-sm text-gray-500 mt-2 flex items-center gap-2">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Đang cập nhật...
                </p>
              )}
              
              <div className="mt-4 p-3 bg-blue-50 rounded-lg">
                <p className="text-xs text-blue-700">
                  <strong>Gợi ý:</strong> Cập nhật trạng thái để người gửi biết tiến độ thực hiện của bạn.
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Quick Actions */}
          <Card className="border-0 shadow-md">
            <CardHeader>
              <CardTitle className="text-lg">Thao tác nhanh</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              {isOverdue(request.deadline) ? (
                <div className="w-full p-3 bg-red-50 rounded-lg border border-red-200 text-center">
                  <p className="text-sm font-medium text-red-800">
                    Yêu cầu đã quá hạn, không thể thực hiện báo cáo
                  </p>
                </div>
              ) : myResponseStatus === 'COMPLETED' ? (
                <div className="w-full p-3 bg-green-50 rounded-lg border border-green-200 text-center">
                  <p className="text-sm font-medium text-green-800">
                    Báo cáo đã hoàn thành, không thể chỉnh sửa
                  </p>
                </div>
              ) : (!myResponse || (myResponse.score === null || myResponse.score === undefined)) ? (
                <Button 
                  className="w-full bg-[#DA251D] hover:bg-[#b91c1c]"
                  onClick={() => router.push(`/management/inbox/${params.id}/submit`)}
                >
                  <Edit className="h-4 w-4 mr-2" />
                  {myResponse ? 'Sửa báo cáo' : 'Thực hiện báo cáo'}
                </Button>
              ) : (
                <div className="w-full p-3 bg-gray-50 rounded-lg border border-gray-200 text-center">
                  <p className="text-sm text-gray-600">
                    Báo cáo đã được đánh giá, không thể chỉnh sửa
                  </p>
                </div>
              )}
              <Button 
                className="w-full bg-green-600 hover:bg-green-700"
                onClick={() => handleStatusChange('COMPLETED')}
                disabled={myResponseStatus === 'COMPLETED' || isUpdatingStatus}
              >
                <CheckCircle className="h-4 w-4 mr-2" />
                Đánh dấu hoàn thành
              </Button>
              <Button 
                variant="outline"
                className="w-full"
                onClick={handleOpenForwardDialog}
              >
                <Forward className="h-4 w-4 mr-2" />
                Chuyển tiếp
              </Button>
              <Button 
                variant="outline"
                className="w-full"
                onClick={() => router.push('/management/inbox')}
              >
                Quay lại danh sách
              </Button>
            </CardContent>
          </Card>

          {/* Request History Card */}
          <Card className="border-0 shadow-md">
            <CardHeader>
              <div className="flex items-center justify-between">
                <CardTitle className="text-lg">Lịch sử chỉnh sửa</CardTitle>
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

      {/* My Report Response */}
      {myResponse && (
        <Card className="border-0 shadow-md">
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-lg flex items-center gap-2">
                  <FileText className="h-5 w-5 text-green-600" />
                  Báo cáo của bạn
                </CardTitle>
                <CardDescription>
                  Nộp lúc: {new Date(myResponse.submittedAt).toLocaleString('vi-VN')}
                  <div className="flex items-center gap-4 mt-2">
                    {myResponse.score !== null && myResponse.score !== undefined && (
                      <span className="flex items-center gap-1">
                        <Star className="h-4 w-4 text-yellow-500 fill-yellow-500" />
                        <span className="font-semibold text-yellow-600">
                          Điểm người gửi: {myResponse.score}/10
                        </span>
                        {myResponse.evaluatedAt && (
                          <span className="text-gray-500 text-xs">
                            ({new Date(myResponse.evaluatedAt).toLocaleDateString('vi-VN')})
                          </span>
                        )}
                      </span>
                    )}
                    {myResponse.selfScore !== null && myResponse.selfScore !== undefined && (
                      <span className="flex items-center gap-1">
                        <Star className="h-4 w-4 text-blue-500 fill-blue-500" />
                        <span className="font-semibold text-blue-600">
                          Điểm tự đánh giá: {myResponse.selfScore}/10
                        </span>
                        {myResponse.selfEvaluatedAt && (
                          <span className="text-gray-500 text-xs">
                            ({new Date(myResponse.selfEvaluatedAt).toLocaleDateString('vi-VN')})
                          </span>
                        )}
                      </span>
                    )}
                  </div>
                </CardDescription>
              </div>
              {myResponse.score === null || myResponse.score === undefined ? (
                <Button 
                  variant="outline"
                  size="sm"
                  onClick={() => router.push(`/management/inbox/${params.id}/submit`)}
                >
                  <Edit className="h-4 w-4 mr-2" />
                  Chỉnh sửa
                </Button>
              ) : (
                <Badge className="bg-gray-100 text-gray-600 hover:bg-gray-100">
                  Đã được đánh giá
                </Badge>
              )}
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {/* Tự đánh giá */}
            {myResponse.selfScore === null || myResponse.selfScore === undefined ? (
              <div className="pt-4 border-t">
                {selfEvaluatingResponseId === myResponse.id ? (
                  <div className="flex items-center gap-3">
                    <Input
                      type="number"
                      min="0"
                      max="10"
                      step="0.1"
                      placeholder="Nhập điểm tự đánh giá (0-10)"
                      value={selfEvaluationScore}
                      onChange={(e) => setSelfEvaluationScore(e.target.value)}
                      className="w-48"
                    />
                    <Button
                      onClick={() => handleSelfEvaluate(myResponse.id)}
                      disabled={isSelfEvaluating}
                      className="bg-blue-600 hover:bg-blue-700"
                    >
                      {isSelfEvaluating ? (
                        <>
                          <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                          Đang xử lý...
                        </>
                      ) : (
                        'Xác nhận'
                      )}
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => {
                        setSelfEvaluatingResponseId(null)
                        setSelfEvaluationScore('')
                      }}
                    >
                      Hủy
                    </Button>
                  </div>
                ) : (
                  <Button
                    onClick={() => {
                      setSelfEvaluatingResponseId(myResponse.id)
                      setSelfEvaluationScore('')
                    }}
                    variant="outline"
                    className="border-blue-600 text-blue-600 hover:bg-blue-50"
                  >
                    <Star className="h-4 w-4 mr-2" />
                    Tự đánh giá báo cáo
                  </Button>
                )}
              </div>
            ) : null}

            {myResponse.note && (
              <div className="bg-gray-50 rounded-lg p-4">
                <h4 className="text-sm font-medium text-gray-500 mb-1">Ghi chú:</h4>
                <p className="text-gray-900">{myResponse.note}</p>
              </div>
            )}
            {myResponse.comment && (
              <div className="bg-blue-50 rounded-lg p-4 border border-blue-200">
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-sm font-medium text-blue-700">Nhận xét từ người đánh giá:</h4>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={openCommentHistory}
                  >
                    Xem lịch sử nhận xét
                  </Button>
                </div>
                <p className="text-blue-900 whitespace-pre-wrap">{myResponse.comment}</p>
                {myResponse.evaluatedBy && (
                  <p className="text-xs text-blue-600 mt-2">
                    - {myResponse.evaluatedBy.fullName}
                    {myResponse.evaluatedAt && (
                      <span className="ml-2">
                        ({new Date(myResponse.evaluatedAt).toLocaleString('vi-VN')})
                      </span>
                    )}
                  </p>
                )}
              </div>
            )}
            
            <div className="space-y-3">
              <h4 className="text-sm font-medium text-gray-500">Nội dung báo cáo:</h4>
              {myResponse.items.map((item, index) => (
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
          </CardContent>
        </Card>
      )}

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
              {myResponse ? `Báo cáo của bạn` : ''}
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

