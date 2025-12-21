"use client"

import { useEffect, useState } from 'react'
import { FileText, Clock, CheckCircle, AlertCircle, Plus } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { useRouter } from 'next/navigation'
import { reportRequestApi } from '@/lib/api'
import { useToast } from '@/components/ui/use-toast'
import { formatDate as formatDateUtil } from '@/lib/utils'

interface ReportRequest {
  id: number
  title: string
  deadline: string
  status: string
  createdAt: string
  targetOrganizations?: { id: number; name: string }[]
  targetDepartments?: { id: number; name: string }[]
  targetUsers?: { id: number; fullName: string }[]
}

export default function ManagementDashboard() {
  const router = useRouter()
  const { toast } = useToast()
  const [requests, setRequests] = useState<ReportRequest[]>([])
  const [loading, setLoading] = useState(true)

  const fetchRequests = async () => {
    try {
      const response = await reportRequestApi.getMyRequests()
      setRequests(response.data.data || [])
    } catch (error) {
      toast({
        title: 'Lỗi',
        description: 'Không thể tải dữ liệu',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRequests()
  }, [])

  const stats = {
    total: requests.length,
    pending: requests.filter(r => r.status === 'PENDING').length,
    inProgress: requests.filter(r => r.status === 'IN_PROGRESS').length,
    completed: requests.filter(r => r.status === 'COMPLETED').length,
  }

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDING':
        return <Badge className="bg-yellow-100 text-yellow-700">Đang chờ</Badge>
      case 'IN_PROGRESS':
        return <Badge className="bg-blue-100 text-blue-700">Đang thực hiện</Badge>
      case 'COMPLETED':
        return <Badge className="bg-green-100 text-green-700">Hoàn thành</Badge>
      case 'CANCELLED':
        return <Badge className="bg-gray-100 text-gray-700">Đã hủy</Badge>
      default:
        return <Badge variant="outline">{status}</Badge>
    }
  }

  const formatDate = (dateString: string) => {
    return formatDateUtil(dateString)
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Tổng quan</h1>
          <p className="text-gray-500 mt-1">Quản lý yêu cầu báo cáo của bạn</p>
        </div>
        <Button
          onClick={() => router.push('/management/report-requests/create')}
          className="bg-[#DA251D] hover:bg-[#b81f18]"
        >
          <Plus className="h-4 w-4 mr-2" />
          Tạo yêu cầu mới
        </Button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="border-0 shadow-sm">
          <CardContent className="p-6">
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-full bg-blue-100">
                <FileText className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Tổng yêu cầu</p>
                <p className="text-2xl font-bold text-gray-900">{stats.total}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-0 shadow-sm">
          <CardContent className="p-6">
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-full bg-yellow-100">
                <Clock className="h-6 w-6 text-yellow-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Đang chờ</p>
                <p className="text-2xl font-bold text-gray-900">{stats.pending}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-0 shadow-sm">
          <CardContent className="p-6">
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-full bg-blue-100">
                <AlertCircle className="h-6 w-6 text-blue-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Đang thực hiện</p>
                <p className="text-2xl font-bold text-gray-900">{stats.inProgress}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        <Card className="border-0 shadow-sm">
          <CardContent className="p-6">
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-full bg-green-100">
                <CheckCircle className="h-6 w-6 text-green-600" />
              </div>
              <div>
                <p className="text-sm text-gray-500">Hoàn thành</p>
                <p className="text-2xl font-bold text-gray-900">{stats.completed}</p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Requests */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <CardTitle className="text-lg font-semibold flex items-center gap-2">
            <FileText className="h-5 w-5 text-[#DA251D]" />
            Yêu cầu báo cáo gần đây
          </CardTitle>
          <CardDescription>
            Danh sách các yêu cầu báo cáo bạn đã tạo
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-[#DA251D]"></div>
            </div>
          ) : requests.length === 0 ? (
            <div className="text-center py-12">
              <FileText className="h-12 w-12 text-gray-300 mx-auto mb-4" />
              <p className="text-gray-500">Chưa có yêu cầu báo cáo nào</p>
              <Button
                onClick={() => router.push('/management/report-requests/create')}
                className="mt-4 bg-[#DA251D] hover:bg-[#b81f18]"
              >
                Tạo yêu cầu đầu tiên
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              {requests.slice(0, 5).map((request) => (
                <div
                  key={request.id}
                  className="flex items-center justify-between p-4 rounded-lg border hover:bg-gray-50 cursor-pointer transition-colors"
                  onClick={() => router.push(`/management/report-requests/${request.id}`)}
                >
                  <div className="flex-1">
                    <h3 className="font-medium text-gray-900">{request.title}</h3>
                    <div className="flex items-center gap-4 mt-1 text-sm text-gray-500">
                      <span>Hạn: {formatDate(request.deadline)}</span>
                      <span>•</span>
                      <span>Tạo: {formatDate(request.createdAt)}</span>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    {getStatusBadge(request.status)}
                  </div>
                </div>
              ))}
              {requests.length > 5 && (
                <div className="text-center pt-2">
                  <Button
                    variant="outline"
                    onClick={() => router.push('/management/report-requests')}
                  >
                    Xem tất cả ({requests.length})
                  </Button>
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

