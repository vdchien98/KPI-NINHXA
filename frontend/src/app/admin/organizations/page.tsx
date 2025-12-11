"use client"

import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, Building2, Loader2, Search, MapPin, Phone, Mail } from 'lucide-react'
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
import { Badge } from '@/components/ui/badge'
import { Switch } from '@/components/ui/switch'
import { useToast } from '@/components/ui/use-toast'
import { organizationApi } from '@/lib/api'

interface Organization {
  id: number
  name: string
  code?: string
  address?: string
  phone?: string
  email?: string
  isActive: boolean
  departments?: any[]
  createdAt: string
}

export default function OrganizationsPage() {
  const { toast } = useToast()
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [loading, setLoading] = useState(true)
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    address: '',
    phone: '',
    email: '',
    isActive: true,
  })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const fetchOrganizations = async () => {
    try {
      const response = await organizationApi.getAll()
      setOrganizations(response.data.data || [])
    } catch (error) {
      toast({
        title: 'Lỗi',
        description: 'Không thể tải danh sách cơ quan',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchOrganizations()
  }, [])

  const handleOpenDialog = (org?: Organization) => {
    if (org) {
      setSelectedOrg(org)
      setFormData({
        name: org.name,
        code: org.code || '',
        address: org.address || '',
        phone: org.phone || '',
        email: org.email || '',
        isActive: org.isActive,
      })
    } else {
      setSelectedOrg(null)
      setFormData({
        name: '',
        code: '',
        address: '',
        phone: '',
        email: '',
        isActive: true,
      })
    }
    setIsDialogOpen(true)
  }

  const handleCloseDialog = () => {
    setIsDialogOpen(false)
    setSelectedOrg(null)
  }

  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      toast({
        title: 'Lỗi',
        description: 'Tên cơ quan không được để trống',
        variant: 'destructive',
      })
      return
    }

    setIsSubmitting(true)
    try {
      const data = {
        name: formData.name,
        code: formData.code || undefined,
        address: formData.address || undefined,
        phone: formData.phone || undefined,
        email: formData.email || undefined,
        isActive: formData.isActive,
      }

      if (selectedOrg) {
        await organizationApi.update(selectedOrg.id, data)
        toast({
          title: 'Thành công',
          description: 'Cập nhật cơ quan thành công',
        })
      } else {
        await organizationApi.create(data)
        toast({
          title: 'Thành công',
          description: 'Tạo cơ quan mới thành công',
        })
      }

      handleCloseDialog()
      fetchOrganizations()
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
    if (!selectedOrg) return

    setIsSubmitting(true)
    try {
      await organizationApi.delete(selectedOrg.id)
      toast({
        title: 'Thành công',
        description: 'Xóa cơ quan thành công',
      })
      setIsDeleteDialogOpen(false)
      setSelectedOrg(null)
      fetchOrganizations()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể xóa cơ quan này',
        variant: 'destructive',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const filteredOrgs = organizations.filter(
    (org) =>
      org.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (org.code && org.code.toLowerCase().includes(searchTerm.toLowerCase()))
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quản lý Cơ quan</h1>
          <p className="text-gray-500 mt-1">Quản lý các cơ quan trong hệ thống</p>
        </div>
        <Button
          onClick={() => handleOpenDialog()}
          className="bg-[#DA251D] hover:bg-[#b81f18]"
        >
          <Plus className="h-4 w-4 mr-2" />
          Thêm cơ quan
        </Button>
      </div>

      {/* Search */}
      <div className="relative w-full sm:w-64">
        <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
        <Input
          placeholder="Tìm kiếm cơ quan..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="pl-9"
        />
      </div>

      {/* Organizations Grid */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-[#DA251D]" />
        </div>
      ) : filteredOrgs.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          Không tìm thấy cơ quan nào
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredOrgs.map((org) => (
            <Card key={org.id} className="border-0 shadow-md hover:shadow-lg transition-shadow">
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-lg bg-[#DA251D]/10">
                      <Building2 className="h-6 w-6 text-[#DA251D]" />
                    </div>
                    <div>
                      <CardTitle className="text-lg">{org.name}</CardTitle>
                      {org.code && (
                        <Badge variant="outline" className="mt-1">
                          {org.code}
                        </Badge>
                      )}
                    </div>
                  </div>
                  <Badge variant={org.isActive ? 'success' : 'destructive'}>
                    {org.isActive ? 'Hoạt động' : 'Vô hiệu'}
                  </Badge>
                </div>
              </CardHeader>
              <CardContent className="space-y-2">
                {org.address && (
                  <div className="flex items-start gap-2 text-sm text-gray-600">
                    <MapPin className="h-4 w-4 mt-0.5 text-gray-400" />
                    <span>{org.address}</span>
                  </div>
                )}
                {org.phone && (
                  <div className="flex items-center gap-2 text-sm text-gray-600">
                    <Phone className="h-4 w-4 text-gray-400" />
                    <span>{org.phone}</span>
                  </div>
                )}
                {org.email && (
                  <div className="flex items-center gap-2 text-sm text-gray-600">
                    <Mail className="h-4 w-4 text-gray-400" />
                    <span>{org.email}</span>
                  </div>
                )}
                <div className="text-sm text-gray-500">
                  {org.departments?.length || 0} phòng ban
                </div>
                <div className="flex justify-end gap-2 pt-2 border-t">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleOpenDialog(org)}
                    className="hover:bg-blue-50 hover:text-blue-600"
                  >
                    <Pencil className="h-4 w-4 mr-1" />
                    Sửa
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      setSelectedOrg(org)
                      setIsDeleteDialogOpen(true)
                    }}
                    className="hover:bg-red-50 hover:text-red-600"
                  >
                    <Trash2 className="h-4 w-4 mr-1" />
                    Xóa
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Create/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>
              {selectedOrg ? 'Chỉnh sửa cơ quan' : 'Thêm cơ quan mới'}
            </DialogTitle>
            <DialogDescription>
              {selectedOrg
                ? 'Cập nhật thông tin cơ quan'
                : 'Điền thông tin để tạo cơ quan mới'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="name">Tên cơ quan *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="Nhập tên cơ quan"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="code">Mã cơ quan</Label>
                <Input
                  id="code"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  placeholder="VD: UBND-NX"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="address">Địa chỉ</Label>
              <Input
                id="address"
                value={formData.address}
                onChange={(e) => setFormData({ ...formData, address: e.target.value })}
                placeholder="Nhập địa chỉ"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="phone">Số điện thoại</Label>
                <Input
                  id="phone"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  placeholder="Nhập SĐT"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email</Label>
                <Input
                  id="email"
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  placeholder="Nhập email"
                />
              </div>
            </div>
            <div className="flex items-center justify-between">
              <Label htmlFor="isActive">Trạng thái hoạt động</Label>
              <Switch
                id="isActive"
                checked={formData.isActive}
                onCheckedChange={(checked) => setFormData({ ...formData, isActive: checked })}
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
              {selectedOrg ? 'Cập nhật' : 'Tạo mới'}
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
              Bạn có chắc chắn muốn xóa cơ quan &quot;{selectedOrg?.name}&quot;? Tất cả
              phòng ban thuộc cơ quan này cũng sẽ bị xóa. Hành động này không thể
              hoàn tác.
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

