"use client"

import { useEffect, useState } from 'react'
import { Search, FileText, TrendingUp, Users, CheckCircle, Clock, AlertCircle, Pencil, Eye, List } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
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
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { useToast } from '@/components/ui/use-toast'
import { adminReportApi, commonApi, reportRequestApi } from '@/lib/api'
import { useRouter } from 'next/navigation'

interface ReportRequest {
  id: number
  title: string
  description?: string
  status: string
  deadline?: string
  createdBy: { id: number; fullName: string; email: string }
  createdAt: string
  totalResponses: number
  evaluatedResponses: number
  averageScore: number
  targetOrganizations?: { id: number; name: string }[]
  targetDepartments?: { id: number; name: string }[]
}

interface Statistics {
  totalReports: number
  byStatus: { [key: string]: number }
  totalResponses: number
  evaluatedResponses: number
  averageScore: number
  topCreators: { [key: string]: number }
  topSubmitters: { [key: string]: number }
  overdueReports: number
}

interface User {
  id: number
  fullName: string
  email: string
}

interface Organization {
  id: number
  name: string
}

interface Department {
  id: number
  name: string
}

export default function AdminReportsPage() {
  const { toast } = useToast()
  const router = useRouter()
  const [reports, setReports] = useState<ReportRequest[]>([])
  const [statistics, setStatistics] = useState<Statistics | null>(null)
  const [users, setUsers] = useState<User[]>([])
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [loading, setLoading] = useState(true)
  
  // Filter states
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [createdByFilter, setCreatedByFilter] = useState('all')
  const [submittedByFilter, setSubmittedByFilter] = useState('all')
  const [organizationFilter, setOrganizationFilter] = useState('all')
  const [departmentFilter, setDepartmentFilter] = useState('all')
  
  // Edit dialog states
  const [isEditRequestDialogOpen, setIsEditRequestDialogOpen] = useState(false)
  const [isViewResponsesDialogOpen, setIsViewResponsesDialogOpen] = useState(false)
  const [selectedReport, setSelectedReport] = useState<ReportRequest | null>(null)
  const [responses, setResponses] = useState<any[]>([])
  const [loadingResponses, setLoadingResponses] = useState(false)
  const [editFormData, setEditFormData] = useState({
    title: '',
    description: '',
    deadline: '',
    status: '',
    organizationIds: [] as number[],
    departmentIds: [] as number[],
  })

  const fetchData = async () => {
    try {
      setLoading(true)
      const [reportsRes, statsRes, usersRes, orgsRes, deptsRes] = await Promise.all([
        adminReportApi.getAll({
          search: searchTerm || undefined,
          status: statusFilter !== 'all' ? statusFilter : undefined,
          createdById: createdByFilter !== 'all' ? parseInt(createdByFilter) : undefined,
          submittedById: submittedByFilter !== 'all' ? parseInt(submittedByFilter) : undefined,
          organizationId: organizationFilter !== 'all' ? parseInt(organizationFilter) : undefined,
          departmentId: departmentFilter !== 'all' ? parseInt(departmentFilter) : undefined,
        }),
        adminReportApi.getStatistics(),
        commonApi.getUsers(),
        commonApi.getOrganizations(),
        commonApi.getDepartments(),
      ])
      
      setReports(reportsRes.data.data || [])
      setStatistics(statsRes.data.data || null)
      setUsers(usersRes.data.data || [])
      setOrganizations(orgsRes.data.data || [])
      setDepartments(deptsRes.data.data || [])
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
  }, [searchTerm, statusFilter, createdByFilter, submittedByFilter, organizationFilter, departmentFilter])

  const getStatusBadge = (status: string) => {
    const statusMap: { [key: string]: { label: string; className: string } } = {
      PENDING: { label: 'Chờ xử lý', className: 'bg-yellow-100 text-yellow-800' },
      COMPLETED: { label: 'Hoàn thành', className: 'bg-green-100 text-green-800' },
      CANCELLED: { label: 'Đã hủy', className: 'bg-gray-100 text-gray-800' },
      SUBMITTED: { label: 'Đã nộp', className: 'bg-blue-100 text-blue-800' },
    }
    const statusInfo = statusMap[status] || { label: status, className: 'bg-gray-100 text-gray-800' }
    return <Badge className={statusInfo.className}>{statusInfo.label}</Badge>
  }

  const handleOpenEditDialog = (report: ReportRequest) => {
    setSelectedReport(report)
    setEditFormData({
      title: report.title,
      description: report.description || '',
      deadline: report.deadline ? new Date(report.deadline).toISOString().slice(0, 16) : '',
      status: report.status,
      organizationIds: report.targetOrganizations?.map(o => o.id) || [],
      departmentIds: report.targetDepartments?.map(d => d.id) || [],
    })
    setIsEditRequestDialogOpen(true)
  }

  const handleUpdateRequest = async () => {
    if (!selectedReport) return
    
    try {
      await adminReportApi.updateRequest(selectedReport.id, {
        ...editFormData,
        deadline: editFormData.deadline || undefined,
      })
      
      toast({
        title: 'Thành công',
        description: 'Cập nhật yêu cầu báo cáo thành công',
      })
      
      setIsEditRequestDialogOpen(false)
      fetchData()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể cập nhật yêu cầu báo cáo',
        variant: 'destructive',
      })
    }
  }

  const handleResetFilters = () => {
    setSearchTerm('')
    setStatusFilter('all')
    setCreatedByFilter('all')
    setSubmittedByFilter('all')
    setOrganizationFilter('all')
    setDepartmentFilter('all')
  }

  const handleViewResponses = async (report: ReportRequest) => {
    setSelectedReport(report)
    setIsViewResponsesDialogOpen(true)
    setLoadingResponses(true)
    
    try {
      const res = await reportRequestApi.getById(report.id)
      setResponses(res.data.data.responses || [])
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: 'Không thể tải danh sách phản hồi',
        variant: 'destructive',
      })
    } finally {
      setLoadingResponses(false)
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
          <h1 className="text-3xl font-bold text-gray-900">Quản lý Báo cáo</h1>
          <p className="text-gray-500 mt-1">Thống kê và quản lý tất cả báo cáo trong hệ thống</p>
        </div>
      </div>

      {/* Statistics Cards */}
      {statistics && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Tổng số báo cáo</CardTitle>
              <FileText className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{statistics.totalReports}</div>
              <p className="text-xs text-muted-foreground mt-1">
                Quá hạn: {statistics.overdueReports}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Tổng phản hồi</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{statistics.totalResponses}</div>
              <p className="text-xs text-muted-foreground mt-1">
                Đã đánh giá: {statistics.evaluatedResponses}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Điểm trung bình</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{statistics.averageScore.toFixed(2)}</div>
              <p className="text-xs text-muted-foreground mt-1">Trên thang điểm 10</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Hoàn thành</CardTitle>
              <CheckCircle className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{statistics.byStatus.COMPLETED || 0}</div>
              <p className="text-xs text-muted-foreground mt-1">
                Chờ xử lý: {statistics.byStatus.PENDING || 0}
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Filters */}
      <Card>
        <CardHeader>
          <CardTitle>Bộ lọc</CardTitle>
          <CardDescription>Tìm kiếm và lọc báo cáo theo nhiều tiêu chí</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label htmlFor="search">Tìm kiếm</Label>
              <div className="relative">
                <Search className="absolute left-2 top-2.5 h-4 w-4 text-muted-foreground" />
                <Input
                  id="search"
                  placeholder="Tiêu đề hoặc mô tả..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="pl-8"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="status">Trạng thái</Label>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Tất cả trạng thái" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả</SelectItem>
                  <SelectItem value="PENDING">Chờ xử lý</SelectItem>
                  <SelectItem value="COMPLETED">Hoàn thành</SelectItem>
                  <SelectItem value="CANCELLED">Đã hủy</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="createdBy">Người yêu cầu</Label>
              <Select value={createdByFilter} onValueChange={setCreatedByFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Tất cả" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả</SelectItem>
                  {users.map((user) => (
                    <SelectItem key={user.id} value={user.id.toString()}>
                      {user.fullName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="submittedBy">Người báo cáo</Label>
              <Select value={submittedByFilter} onValueChange={setSubmittedByFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Tất cả" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả</SelectItem>
                  {users.map((user) => (
                    <SelectItem key={user.id} value={user.id.toString()}>
                      {user.fullName}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="organization">Cơ quan</Label>
              <Select value={organizationFilter} onValueChange={setOrganizationFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Tất cả" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả</SelectItem>
                  {organizations.map((org) => (
                    <SelectItem key={org.id} value={org.id.toString()}>
                      {org.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="department">Phòng ban</Label>
              <Select value={departmentFilter} onValueChange={setDepartmentFilter}>
                <SelectTrigger>
                  <SelectValue placeholder="Tất cả" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">Tất cả</SelectItem>
                  {departments.map((dept) => (
                    <SelectItem key={dept.id} value={dept.id.toString()}>
                      {dept.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          
          <div className="mt-4 flex justify-end">
            <Button variant="outline" onClick={handleResetFilters}>
              Xóa bộ lọc
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Reports Table */}
      <Card>
        <CardHeader>
          <CardTitle>Danh sách báo cáo ({reports.length})</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tiêu đề</TableHead>
                <TableHead>Người yêu cầu</TableHead>
                <TableHead>Trạng thái</TableHead>
                <TableHead>Hạn chót</TableHead>
                <TableHead>Phản hồi</TableHead>
                <TableHead>Điểm TB</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {reports.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center text-gray-500 py-8">
                    Không tìm thấy báo cáo nào
                  </TableCell>
                </TableRow>
              ) : (
                reports.map((report) => (
                  <TableRow key={report.id}>
                    <TableCell>
                      <div className="font-medium">{report.title}</div>
                      {report.description && (
                        <div className="text-sm text-gray-500 truncate max-w-xs">
                          {report.description}
                        </div>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="text-sm">{report.createdBy.fullName}</div>
                      <div className="text-xs text-gray-500">{report.createdBy.email}</div>
                    </TableCell>
                    <TableCell>{getStatusBadge(report.status)}</TableCell>
                    <TableCell>
                      {report.deadline ? (
                        <div className="text-sm">
                          {new Date(report.deadline).toLocaleDateString('vi-VN')}
                          {new Date(report.deadline) < new Date() && report.status !== 'COMPLETED' && (
                            <Badge className="ml-2 bg-red-100 text-red-800">Quá hạn</Badge>
                          )}
                        </div>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </TableCell>
                    <TableCell>
                      <div className="text-sm">
                        {report.totalResponses} phản hồi
                      </div>
                      <div className="text-xs text-gray-500">
                        Đã đánh giá: {report.evaluatedResponses}
                      </div>
                    </TableCell>
                    <TableCell>
                      {report.averageScore > 0 ? (
                        <Badge className="bg-blue-100 text-blue-800">
                          {report.averageScore.toFixed(1)}
                        </Badge>
                      ) : (
                        <span className="text-gray-400">-</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => router.push(`/management/inbox/${report.id}`)}
                          className="hover:bg-blue-50 hover:text-blue-600"
                          title="Xem chi tiết"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleOpenEditDialog(report)}
                          className="hover:bg-green-50 hover:text-green-600"
                          title="Sửa yêu cầu"
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => handleViewResponses(report)}
                          className="hover:bg-purple-50 hover:text-purple-600"
                          title="Xem/Sửa phản hồi"
                        >
                          <List className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* View Responses Dialog */}
      <Dialog open={isViewResponsesDialogOpen} onOpenChange={setIsViewResponsesDialogOpen}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Danh sách phản hồi</DialogTitle>
            <DialogDescription>
              Báo cáo: {selectedReport?.title}
            </DialogDescription>
          </DialogHeader>
          
          {loadingResponses ? (
            <div className="flex justify-center py-8">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-[#DA251D]"></div>
            </div>
          ) : responses.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              Chưa có phản hồi nào
            </div>
          ) : (
            <div className="space-y-4">
              {responses.map((response: any) => (
                <Card key={response.id}>
                  <CardHeader>
                    <div className="flex justify-between items-start">
                      <div>
                        <CardTitle className="text-lg">
                          {response.submittedBy?.fullName || 'N/A'}
                        </CardTitle>
                        <CardDescription>
                          {response.submittedBy?.email || 'N/A'}
                        </CardDescription>
                      </div>
                      <Button
                        size="sm"
                        onClick={() => router.push(`/admin/reports/responses/${response.id}`)}
                        className="bg-[#DA251D] hover:bg-[#b91d17]"
                      >
                        <Pencil className="h-4 w-4 mr-2" />
                        Chỉnh sửa
                      </Button>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="grid grid-cols-2 gap-4 text-sm">
                      <div>
                        <span className="text-gray-500">Ngày nộp:</span>{' '}
                        {response.submittedAt
                          ? new Date(response.submittedAt).toLocaleString('vi-VN')
                          : 'Chưa nộp'}
                      </div>
                      <div>
                        <span className="text-gray-500">Số mục báo cáo:</span>{' '}
                        {response.items?.length || 0}
                      </div>
                      <div>
                        <span className="text-gray-500">Điểm đánh giá:</span>{' '}
                        {response.score !== null && response.score !== undefined ? (
                          <Badge className="bg-blue-100 text-blue-800">
                            {response.score}
                          </Badge>
                        ) : (
                          <span className="text-gray-400">Chưa đánh giá</span>
                        )}
                      </div>
                      <div>
                        <span className="text-gray-500">Tự đánh giá:</span>{' '}
                        {response.selfScore !== null && response.selfScore !== undefined ? (
                          <Badge className="bg-green-100 text-green-800">
                            {response.selfScore}
                          </Badge>
                        ) : (
                          <span className="text-gray-400">Chưa tự đánh giá</span>
                        )}
                      </div>
                    </div>
                    {response.note && (
                      <div className="mt-4 p-3 bg-gray-50 rounded-md">
                        <p className="text-sm text-gray-600">
                          <strong>Ghi chú:</strong> {response.note}
                        </p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
          
          <DialogFooter>
            <Button variant="outline" onClick={() => setIsViewResponsesDialogOpen(false)}>
              Đóng
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit Request Dialog */}
      <Dialog open={isEditRequestDialogOpen} onOpenChange={setIsEditRequestDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Chỉnh sửa yêu cầu báo cáo</DialogTitle>
            <DialogDescription>
              Cập nhật thông tin yêu cầu báo cáo (Admin)
            </DialogDescription>
          </DialogHeader>
          
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="edit-title">Tiêu đề *</Label>
              <Input
                id="edit-title"
                value={editFormData.title}
                onChange={(e) => setEditFormData({ ...editFormData, title: e.target.value })}
                placeholder="Nhập tiêu đề báo cáo"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit-description">Mô tả</Label>
              <textarea
                id="edit-description"
                value={editFormData.description}
                onChange={(e) => setEditFormData({ ...editFormData, description: e.target.value })}
                placeholder="Nhập mô tả chi tiết"
                className="w-full min-h-[100px] px-3 py-2 border rounded-md"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit-deadline">Hạn chót</Label>
              <Input
                id="edit-deadline"
                type="datetime-local"
                value={editFormData.deadline}
                onChange={(e) => setEditFormData({ ...editFormData, deadline: e.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="edit-status">Trạng thái</Label>
              <Select
                value={editFormData.status}
                onValueChange={(value) => setEditFormData({ ...editFormData, status: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn trạng thái" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="PENDING">Chờ xử lý</SelectItem>
                  <SelectItem value="COMPLETED">Hoàn thành</SelectItem>
                  <SelectItem value="CANCELLED">Đã hủy</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Cơ quan</Label>
              <div className="border rounded-md p-3 max-h-40 overflow-y-auto space-y-2">
                {organizations.map((org) => (
                  <div key={org.id} className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      id={`edit-org-${org.id}`}
                      checked={editFormData.organizationIds.includes(org.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setEditFormData({
                            ...editFormData,
                            organizationIds: [...editFormData.organizationIds, org.id],
                          })
                        } else {
                          setEditFormData({
                            ...editFormData,
                            organizationIds: editFormData.organizationIds.filter(id => id !== org.id),
                          })
                        }
                      }}
                      className="h-4 w-4"
                    />
                    <label htmlFor={`edit-org-${org.id}`} className="text-sm cursor-pointer">
                      {org.name}
                    </label>
                  </div>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <Label>Phòng ban</Label>
              <div className="border rounded-md p-3 max-h-40 overflow-y-auto space-y-2">
                {departments.map((dept) => (
                  <div key={dept.id} className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      id={`edit-dept-${dept.id}`}
                      checked={editFormData.departmentIds.includes(dept.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setEditFormData({
                            ...editFormData,
                            departmentIds: [...editFormData.departmentIds, dept.id],
                          })
                        } else {
                          setEditFormData({
                            ...editFormData,
                            departmentIds: editFormData.departmentIds.filter(id => id !== dept.id),
                          })
                        }
                      }}
                      className="h-4 w-4"
                    />
                    <label htmlFor={`edit-dept-${dept.id}`} className="text-sm cursor-pointer">
                      {dept.name}
                    </label>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setIsEditRequestDialogOpen(false)}>
              Hủy
            </Button>
            <Button onClick={handleUpdateRequest} className="bg-[#DA251D] hover:bg-[#b91d17]">
              Cập nhật
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}

