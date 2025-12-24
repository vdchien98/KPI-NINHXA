"use client"

import { useEffect, useState } from 'react'
import { Loader2, TrendingUp, Users, CheckCircle, Clock, AlertCircle, Filter } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/components/ui/use-toast'
import { adminReportApi, commonApi, reportResponseApi } from '@/lib/api'
import { formatDateTime } from '@/lib/utils'

interface ReportStatisticItem {
  id: number
  stt: number
  reportName: string
  reportAuthor: {
    id: number
    fullName: string
    email: string
  }
  department?: {
    id: number
    name: string
  }
  organizations: Array<{
    id: number
    name: string
  }>
  score: number
  reviewer?: {
    id: number
    fullName: string
  }
  submissionDate: string
  status: string // "Đúng hạn" hoặc "Quá hạn"
  documentLink: string
  documentFiles?: Array<{
    fileName: string
    filePath: string
  }>
  reportRequestId: number
}

interface Summary {
  totalReports: number
  onTimeReports: number
  overdueReports: number
  averageScore: number
  rating: string // "A", "B", "C", "D"
}

interface ReportStatistics {
  reports: ReportStatisticItem[]
  summary: Summary
}

interface User {
  id: number
  fullName: string
  email: string
}

export default function AdminReportStatisticsPage() {
  const { toast } = useToast()
  const [statistics, setStatistics] = useState<ReportStatistics | null>(null)
  const [users, setUsers] = useState<User[]>([])
  const [selectedUserId, setSelectedUserId] = useState<string>('all')
  const [loading, setLoading] = useState(true)

  const fetchData = async (userId?: number) => {
    try {
      setLoading(true)
      const [statsRes, usersRes] = await Promise.all([
        adminReportApi.getScoreStatistics(userId),
        commonApi.getUsers(),
      ])
      
      setStatistics(statsRes.data.data || null)
      setUsers(usersRes.data.data || [])
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tải dữ liệu',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData()
  }, [])

  const handleUserFilterChange = (userId: string) => {
    setSelectedUserId(userId)
    if (userId === 'all') {
      fetchData()
    } else {
      fetchData(parseInt(userId))
    }
  }

  const getRatingBadge = (rating: string) => {
    const ratingMap: { [key: string]: { label: string; className: string } } = {
      A: { label: 'A', className: 'bg-green-100 text-green-800' },
      B: { label: 'B', className: 'bg-blue-100 text-blue-800' },
      C: { label: 'C', className: 'bg-yellow-100 text-yellow-800' },
      D: { label: 'D', className: 'bg-red-100 text-red-800' },
    }
    const ratingInfo = ratingMap[rating] || { label: rating, className: 'bg-gray-100 text-gray-800' }
    return <Badge className={ratingInfo.className}>{ratingInfo.label}</Badge>
  }

  const getStatusBadge = (status: string) => {
    if (status === 'Đúng hạn') {
      return <Badge className="bg-green-100 text-green-800">{status}</Badge>
    } else if (status === 'Quá hạn') {
      return <Badge className="bg-red-100 text-red-800">{status}</Badge>
    } else {
      return <Badge className="bg-gray-100 text-gray-800">{status}</Badge>
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-[#DA251D] mx-auto"></div>
          <p className="mt-4 text-gray-600">Đang tải dữ liệu...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="container mx-auto p-6 space-y-6">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Thống kê Điểm Công việc</h1>
          <p className="text-gray-500 mt-1">Xem thống kê điểm  Công việc của các thành viên</p>
        </div>
      </div>

      {/* Filter */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Filter className="h-5 w-5" />
            Bộ lọc
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="space-y-2">
              <label className="text-sm font-medium">Lọc theo thành viên</label>
              <Select value={selectedUserId} onValueChange={handleUserFilterChange}>
                <SelectTrigger>
                  <SelectValue placeholder="Tất cả thành viên" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả thành viên</SelectItem>
                  {users.map((user) => (
                    <SelectItem key={user.id} value={user.id.toString()}>
                      {user.fullName} ({user.email})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Summary Section - chỉ hiển thị khi chọn user cụ thể */}
      {statistics && statistics.summary && selectedUserId !== 'all' && (
        <Card className="bg-yellow-50 border-yellow-200">
          <CardHeader>
            <CardTitle className="text-xl">Kết luận</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
              <div>
                <p className="text-sm text-gray-600">Có {statistics.summary.totalReports} báo cáo</p>
              </div>
              <div>
                <p className="text-sm text-gray-600">
                  <span className="font-semibold text-green-700">Đúng hạn:</span>{' '}
                  {statistics.summary.onTimeReports} báo cáo
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-600">
                  <span className="font-semibold text-red-700">Quá hạn:</span>{' '}
                  {statistics.summary.overdueReports} báo cáo
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-600">
                  <span className="font-semibold">Điểm trung bình:</span>{' '}
                  {statistics.summary.averageScore > 0
                    ? `${statistics.summary.averageScore.toFixed(2)}`
                    : 'Chưa có điểm'}
                </p>
              </div>
              <div>
                <p className="text-sm text-gray-600">
                  <span className="font-semibold">Xếp loại:</span>{' '}
                  {getRatingBadge(statistics.summary.rating)}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Reports Table */}
      <Card>
        <CardHeader>
          <CardTitle>Danh sách báo cáo</CardTitle>
          <CardDescription>
            {statistics ? `${statistics.reports.length} báo cáo` : 'Đang tải...'}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {statistics && statistics.reports.length > 0 ? (
            <div className="overflow-x-auto">
              <Table className="min-w-[1400px]">
                <TableHeader>
                  <TableRow className="bg-yellow-100">
                    <TableHead className="font-semibold min-w-[60px]">STT</TableHead>
                    <TableHead className="font-semibold min-w-[200px]">Tên báo cáo</TableHead>
                    <TableHead className="font-semibold min-w-[180px]">Người thực hiện báo cáo</TableHead>
                    <TableHead className="font-semibold min-w-[150px]">Phòng ban</TableHead>
                    <TableHead className="font-semibold min-w-[150px]">Cơ quan</TableHead>
                    <TableHead className="font-semibold min-w-[80px]">Điểm</TableHead>
                    <TableHead className="font-semibold min-w-[150px]">Người chấm</TableHead>
                    <TableHead className="font-semibold min-w-[160px]">Ngày giao báo cáo</TableHead>
                    <TableHead className="font-semibold min-w-[120px]">Trạng thái</TableHead>
                    <TableHead className="font-semibold min-w-[200px]">Tài liệu kiểm chứng</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {statistics.reports.map((report) => (
                    <TableRow key={report.id || `request-${report.reportRequestId}-${report.stt}`}>
                      <TableCell className="min-w-[60px]">{report.stt}</TableCell>
                      <TableCell className="font-medium min-w-[200px]">{report.reportName}</TableCell>
                      <TableCell className="min-w-[180px]">
                        <div className="text-sm">{report.reportAuthor.fullName}</div>
                        <div className="text-xs text-gray-500">{report.reportAuthor.email}</div>
                      </TableCell>
                      <TableCell className="min-w-[150px]">
                        {report.department ? report.department.name : '-'}
                      </TableCell>
                      <TableCell className="min-w-[150px]">
                        {report.organizations && report.organizations.length > 0
                          ? report.organizations.map((org) => org.name).join(', ')
                          : '-'}
                      </TableCell>
                      <TableCell className="min-w-[80px]">
                        {report.score !== null && report.score !== undefined ? (
                          <Badge className="bg-blue-100 text-blue-800">
                            {report.score}
                          </Badge>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </TableCell>
                      <TableCell className="min-w-[150px]">
                        {report.reviewer ? report.reviewer.fullName : '-'}
                      </TableCell>
                      <TableCell className="min-w-[160px]">
                        {report.submissionDate
                          ? formatDateTime(report.submissionDate)
                          : '-'}
                      </TableCell>
                      <TableCell className="min-w-[120px]">{getStatusBadge(report.status)}</TableCell>
                      <TableCell className="min-w-[200px]">
                        {report.documentFiles && report.documentFiles.length > 0 ? (
                          <div className="flex flex-col gap-1">
                            {report.documentFiles.map((file, idx) => {
                              const handleDownload = async () => {
                                try {
                                  const response = await reportResponseApi.downloadFile(file.filePath)
                                  const blob = new Blob([response.data])
                                  const url = window.URL.createObjectURL(blob)
                                  const link = document.createElement('a')
                                  link.href = url
                                  link.download = file.fileName
                                  document.body.appendChild(link)
                                  link.click()
                                  document.body.removeChild(link)
                                  window.URL.revokeObjectURL(url)
                                } catch (error: any) {
                                  toast({
                                    title: 'Lỗi',
                                    description: error.response?.data?.message || 'Không thể tải file',
                                    variant: 'destructive',
                                  })
                                }
                              }
                              return (
                                <span
                                  key={idx}
                                  onClick={handleDownload}
                                  className="text-sm text-blue-600 hover:underline cursor-pointer break-words"
                                >
                                  {file.fileName}
                                </span>
                              )
                            })}
                          </div>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          ) : (
            <div className="text-center py-8 text-gray-500">
              Không có báo cáo nào
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

