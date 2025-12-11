"use client"

import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, Shield, ChevronRight, ChevronDown, Loader2 } from 'lucide-react'
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
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/components/ui/use-toast'
import { roleApi } from '@/lib/api'

interface Role {
  id: number
  name: string
  description?: string
  parentId?: number
  parentName?: string
  level: number
  children?: Role[]
  createdAt: string
}

export default function RolesPage() {
  const { toast } = useToast()
  const [roles, setRoles] = useState<Role[]>([])
  const [roleTree, setRoleTree] = useState<Role[]>([])
  const [loading, setLoading] = useState(true)
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [selectedRole, setSelectedRole] = useState<Role | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    parentId: '',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [expandedRoles, setExpandedRoles] = useState<Set<number>>(new Set())

  const fetchRoles = async () => {
    try {
      const [allRolesRes, treeRes] = await Promise.all([
        roleApi.getAll(),
        roleApi.getTree(),
      ])
      setRoles(allRolesRes.data.data || [])
      setRoleTree(treeRes.data.data || [])
    } catch (error) {
      toast({
        title: 'Lỗi',
        description: 'Không thể tải danh sách role',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRoles()
  }, [])

  const handleOpenDialog = (role?: Role) => {
    if (role) {
      setSelectedRole(role)
      setFormData({
        name: role.name,
        description: role.description || '',
        parentId: role.parentId?.toString() || '',
      })
    } else {
      setSelectedRole(null)
      setFormData({ name: '', description: '', parentId: '' })
    }
    setIsDialogOpen(true)
  }

  const handleCloseDialog = () => {
    setIsDialogOpen(false)
    setSelectedRole(null)
    setFormData({ name: '', description: '', parentId: '' })
  }

  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      toast({
        title: 'Lỗi',
        description: 'Tên role không được để trống',
        variant: 'destructive',
      })
      return
    }

    setIsSubmitting(true)
    try {
      const data = {
        name: formData.name,
        description: formData.description || undefined,
        parentId: formData.parentId && formData.parentId !== '__none__' ? parseInt(formData.parentId) : undefined,
      }

      if (selectedRole) {
        await roleApi.update(selectedRole.id, data)
        toast({
          title: 'Thành công',
          description: 'Cập nhật role thành công',
        })
      } else {
        await roleApi.create(data)
        toast({
          title: 'Thành công',
          description: 'Tạo role mới thành công',
        })
      }

      handleCloseDialog()
      fetchRoles()
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
    if (!selectedRole) return

    setIsSubmitting(true)
    try {
      await roleApi.delete(selectedRole.id)
      toast({
        title: 'Thành công',
        description: 'Xóa role thành công',
      })
      setIsDeleteDialogOpen(false)
      setSelectedRole(null)
      fetchRoles()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể xóa role này',
        variant: 'destructive',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const toggleExpand = (roleId: number) => {
    const newExpanded = new Set(expandedRoles)
    if (newExpanded.has(roleId)) {
      newExpanded.delete(roleId)
    } else {
      newExpanded.add(roleId)
    }
    setExpandedRoles(newExpanded)
  }

  const renderRoleTree = (roles: Role[], level = 0) => {
    return roles.map((role) => {
      const hasChildren = role.children && role.children.length > 0
      const isExpanded = expandedRoles.has(role.id)

      return (
        <div key={role.id}>
          <div
            className={`flex items-center justify-between p-3 rounded-lg hover:bg-gray-50 transition-colors ${
              level > 0 ? 'ml-6 border-l-2 border-gray-200' : ''
            }`}
            style={{ marginLeft: level > 0 ? `${level * 24}px` : 0 }}
          >
            <div className="flex items-center gap-3">
              {hasChildren ? (
                <button
                  onClick={() => toggleExpand(role.id)}
                  className="p-1 hover:bg-gray-200 rounded"
                >
                  {isExpanded ? (
                    <ChevronDown className="h-4 w-4 text-gray-500" />
                  ) : (
                    <ChevronRight className="h-4 w-4 text-gray-500" />
                  )}
                </button>
              ) : (
                <div className="w-6" />
              )}
              <div className="p-2 rounded-lg bg-[#DA251D]/10">
                <Shield className="h-5 w-5 text-[#DA251D]" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-medium text-gray-900">{role.name}</span>
                  {role.id === 1 && (
                    <Badge className="bg-[#FFCD00] text-black text-xs">Hệ thống</Badge>
                  )}
                </div>
                {role.description && (
                  <p className="text-sm text-gray-500">{role.description}</p>
                )}
              </div>
            </div>
            <div className="flex items-center gap-2">
              {role.id !== 1 && (
                <>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleOpenDialog(role)}
                    className="hover:bg-blue-50 hover:text-blue-600"
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => {
                      setSelectedRole(role)
                      setIsDeleteDialogOpen(true)
                    }}
                    className="hover:bg-red-50 hover:text-red-600"
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </>
              )}
            </div>
          </div>
          {hasChildren && isExpanded && renderRoleTree(role.children!, level + 1)}
        </div>
      )
    })
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quản lý Role</h1>
          <p className="text-gray-500 mt-1">Quản lý các vai trò và phân quyền trong hệ thống</p>
        </div>
        <Button
          onClick={() => handleOpenDialog()}
          className="bg-[#DA251D] hover:bg-[#b81f18]"
        >
          <Plus className="h-4 w-4 mr-2" />
          Thêm Role mới
        </Button>
      </div>

      {/* Role Tree Card */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <CardTitle className="text-lg font-semibold flex items-center gap-2">
            <Shield className="h-5 w-5 text-[#DA251D]" />
            Cây Role
          </CardTitle>
          <CardDescription>
            Cấu trúc phân cấp các vai trò trong hệ thống
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-[#DA251D]" />
            </div>
          ) : roleTree.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              Chưa có role nào được tạo
            </div>
          ) : (
            <div className="space-y-1">{renderRoleTree(roleTree)}</div>
          )}
        </CardContent>
      </Card>

      {/* Create/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-[425px]">
          <DialogHeader>
            <DialogTitle>
              {selectedRole ? 'Chỉnh sửa Role' : 'Thêm Role mới'}
            </DialogTitle>
            <DialogDescription>
              {selectedRole
                ? 'Cập nhật thông tin role'
                : 'Điền thông tin để tạo role mới'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">Tên Role *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder="Nhập tên role"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Mô tả</Label>
              <Input
                id="description"
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                placeholder="Nhập mô tả role"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="parentId">Role cha</Label>
              <Select
                value={formData.parentId}
                onValueChange={(value) =>
                  setFormData({ ...formData, parentId: value })
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn role cha (không bắt buộc)" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="__none__">Không có role cha</SelectItem>
                  {roles
                    .filter((r) => r.id !== selectedRole?.id)
                    .map((role) => (
                      <SelectItem key={role.id} value={role.id.toString()}>
                        {role.name}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
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
              {selectedRole ? 'Cập nhật' : 'Tạo mới'}
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
              Bạn có chắc chắn muốn xóa role &quot;{selectedRole?.name}&quot;? Hành động
              này không thể hoàn tác.
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

