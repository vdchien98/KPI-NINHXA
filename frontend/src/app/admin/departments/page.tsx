"use client"

import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, Building, Loader2, Search } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
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
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Switch } from '@/components/ui/switch'
import { useToast } from '@/components/ui/use-toast'
import { departmentApi, organizationApi } from '@/lib/api'

interface Department {
  id: number
  name: string
  code?: string
  organizationId: number
  organizationName?: string
  description?: string
  isActive: boolean
  createdAt: string
}

interface Organization {
  id: number
  name: string
}

export default function DepartmentsPage() {
  const { toast } = useToast()
  const [departments, setDepartments] = useState<Department[]>([])
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [loading, setLoading] = useState(true)
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [selectedDept, setSelectedDept] = useState<Department | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [filterOrgId, setFilterOrgId] = useState<string>('__all__')
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    organizationId: '',
    description: '',
    isActive: true,
  })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const fetchData = async () => {
    try {
      const [deptsRes, orgsRes] = await Promise.all([
        departmentApi.getAll(),
        organizationApi.getAll(),
      ])
      setDepartments(deptsRes.data.data || [])
      setOrganizations(orgsRes.data.data || [])
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
    fetchData()
  }, [])

  const handleOpenDialog = (dept?: Department) => {
    if (dept) {
      setSelectedDept(dept)
      setFormData({
        name: dept.name,
        code: dept.code || '',
        organizationId: dept.organizationId.toString(),
        description: dept.description || '',
        isActive: dept.isActive,
      })
    } else {
      setSelectedDept(null)
      setFormData({
        name: '',
        code: '',
        organizationId: filterOrgId !== '__all__' ? filterOrgId : '',
        description: '',
        isActive: true,
      })
    }
    setIsDialogOpen(true)
  }

  const handleCloseDialog = () => {
    setIsDialogOpen(false)
    setSelectedDept(null)
  }

  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      toast({
        title: 'Lỗi',
        description: 'Tên phòng ban không được để trống',
        variant: 'destructive',
      })
      return
    }

    if (!formData.organizationId) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn cơ quan',
        variant: 'destructive',
      })
      return
    }

    setIsSubmitting(true)
    try {
      const data = {
        name: formData.name,
        code: formData.code || undefined,
        organizationId: parseInt(formData.organizationId),
        description: formData.description || undefined,
        isActive: formData.isActive,
      }

      if (selectedDept) {
        await departmentApi.update(selectedDept.id, data)
        toast({
          title: 'Thành công',
          description: 'Cập nhật phòng ban thành công',
        })
      } else {
        await departmentApi.create(data)
        toast({
          title: 'Thành công',
          description: 'Tạo phòng ban mới thành công',
        })
      }

      handleCloseDialog()
      fetchData()
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

  const handleDelete = async () => {
    if (!selectedDept) return

    setIsSubmitting(true)
    try {
      await departmentApi.delete(selectedDept.id)
      toast({
        title: 'Thành công',
        description: 'Xóa phòng ban thành công',
      })
      setIsDeleteDialogOpen(false)
      setSelectedDept(null)
      fetchData()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể xóa phòng ban này',
        variant: 'destructive',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const filteredDepts = departments.filter((dept) => {
    const matchesSearch =
      dept.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (dept.code && dept.code.toLowerCase().includes(searchTerm.toLowerCase()))
    const matchesOrg = filterOrgId === '__all__' || dept.organizationId === parseInt(filterOrgId)
    return matchesSearch && matchesOrg
  })

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quản lý Phòng ban</h1>
          <p className="text-gray-500 mt-1">Quản lý các phòng ban trong từng cơ quan</p>
        </div>
        <Button
          onClick={() => handleOpenDialog()}
          className="bg-[#DA251D] hover:bg-[#b81f18]"
        >
          <Plus className="h-4 w-4 mr-2" />
          Thêm phòng ban
        </Button>
      </div>

      {/* Departments Table Card */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
            <div>
              <CardTitle className="text-lg font-semibold flex items-center gap-2">
                <Building className="h-5 w-5 text-[#DA251D]" />
                Danh sách phòng ban
              </CardTitle>
              <CardDescription>
                Tổng số {filteredDepts.length} phòng ban
              </CardDescription>
            </div>
            <div className="flex flex-col sm:flex-row gap-4">
              <Select value={filterOrgId} onValueChange={setFilterOrgId}>
                <SelectTrigger className="w-full sm:w-[200px]">
                  <SelectValue placeholder="Lọc theo cơ quan" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__all__">Tất cả cơ quan</SelectItem>
                  {organizations.map((org) => (
                    <SelectItem key={org.id} value={org.id.toString()}>
                      {org.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
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
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-[#DA251D]" />
            </div>
          ) : filteredDepts.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              Không tìm thấy phòng ban nào
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Phòng ban</TableHead>
                    <TableHead>Mã</TableHead>
                    <TableHead>Cơ quan</TableHead>
                    <TableHead className="hidden md:table-cell">Mô tả</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredDepts.map((dept) => (
                    <TableRow key={dept.id}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <div className="p-2 rounded-lg bg-[#DA251D]/10">
                            <Building className="h-5 w-5 text-[#DA251D]" />
                          </div>
                          <span className="font-medium text-gray-900">{dept.name}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        {dept.code ? (
                          <Badge variant="outline">{dept.code}</Badge>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </TableCell>
                      <TableCell>
                        <span className="text-gray-600">{dept.organizationName}</span>
                      </TableCell>
                      <TableCell className="hidden md:table-cell">
                        <span className="text-gray-500 text-sm">
                          {dept.description || '-'}
                        </span>
                      </TableCell>
                      <TableCell>
                        <Badge variant={dept.isActive ? 'success' : 'destructive'}>
                          {dept.isActive ? 'Hoạt động' : 'Vô hiệu'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleOpenDialog(dept)}
                            className="hover:bg-blue-50 hover:text-blue-600"
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                              setSelectedDept(dept)
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

      {/* Create/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>
              {selectedDept ? 'Chỉnh sửa phòng ban' : 'Thêm phòng ban mới'}
            </DialogTitle>
            <DialogDescription>
              {selectedDept
                ? 'Cập nhật thông tin phòng ban'
                : 'Điền thông tin để tạo phòng ban mới'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="organizationId">Cơ quan *</Label>
              <Select
                value={formData.organizationId}
                onValueChange={(value) =>
                  setFormData({ ...formData, organizationId: value })
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn cơ quan" />
                </SelectTrigger>
                <SelectContent>
                  {organizations.map((org) => (
                    <SelectItem key={org.id} value={org.id.toString()}>
                      {org.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="name">Tên phòng ban *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Nhập tên phòng ban"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="code">Mã phòng ban</Label>
                <Input
                  id="code"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  placeholder="VD: VP, TPHT"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Mô tả</Label>
              <Input
                id="description"
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                placeholder="Nhập mô tả phòng ban"
              />
            </div>
            <div className="flex items-center justify-between">
              <Label htmlFor="isActive">Trạng thái hoạt động</Label>
              <Switch
                id="isActive"
                checked={formData.isActive}
                onCheckedChange={(checked) =>
                  setFormData({ ...formData, isActive: checked })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={handleCloseDialog}>
              Hủy
            </Button>
            <Button
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="bg-[#DA251D] hover:bg-[#b81f18]"
            >
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {selectedDept ? 'Cập nhật' : 'Tạo mới'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Xác nhận xóa</AlertDialogTitle>
            <AlertDialogDescription>
              Bạn có chắc chắn muốn xóa phòng ban &quot;{selectedDept?.name}&quot;? Hành
              động này không thể hoàn tác.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Hủy</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDelete}
              className="bg-[#DA251D] hover:bg-[#b81f18]"
              disabled={isSubmitting}
            >
              {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Xóa
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}

