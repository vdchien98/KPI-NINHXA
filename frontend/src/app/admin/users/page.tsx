"use client"

import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, Users, Loader2, Search, Mail, Phone } from 'lucide-react'
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
import { Checkbox } from '@/components/ui/checkbox'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { useToast } from '@/components/ui/use-toast'
import { userApi, roleApi, organizationApi, departmentApi, positionApi } from '@/lib/api'

interface User {
  id: number
  email: string
  fullName: string
  phone?: string
  avatar?: string
  role?: { id: number; name: string }
  organizations?: { id: number; name: string }[]
  department?: { id: number; name: string }
  position?: { id: number; name: string }
  representativeType?: string // 'organization', 'department', or null
  loginMethod?: string // 'SSO' or 'PASSWORD'
  isActive: boolean
  createdAt: string
}

interface Role {
  id: number
  name: string
}

interface Organization {
  id: number
  name: string
}

interface Department {
  id: number
  name: string
  organizationId: number
}

interface Position {
  id: number
  name: string
}

export default function UsersPage() {
  const { toast } = useToast()
  const [users, setUsers] = useState<User[]>([])
  const [roles, setRoles] = useState<Role[]>([])
  const [organizations, setOrganizations] = useState<Organization[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [positions, setPositions] = useState<Position[]>([])
  const [filteredDepartments, setFilteredDepartments] = useState<Department[]>([])
  const [loading, setLoading] = useState(true)
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)
  const [searchTerm, setSearchTerm] = useState('')
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    fullName: '',
    phone: '',
    roleId: '',
    representativeType: '' as 'organization' | 'department' | '', // Loại đại diện: 'organization' hoặc 'department'
    organizationIds: [] as number[],
    departmentId: '',
    positionId: '',
    loginMethod: 'SSO' as 'SSO' | 'PASSWORD',
    isActive: true,
  })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const fetchData = async () => {
    try {
      const [usersRes, rolesRes, orgsRes, deptsRes, posRes] = await Promise.all([
        userApi.getAll(),
        roleApi.getAll(),
        organizationApi.getAll(),
        departmentApi.getAll(),
        positionApi.getActive(),
      ])
      setUsers(usersRes.data.data || [])
      setRoles(rolesRes.data.data || [])
      setOrganizations(orgsRes.data.data || [])
      setDepartments(deptsRes.data.data || [])
      setPositions(posRes.data.data || [])
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

  useEffect(() => {
    if (formData.organizationIds.length > 0) {
      const filtered = departments.filter(
        (d) => formData.organizationIds.includes(d.organizationId)
      )
      setFilteredDepartments(filtered)
      
      // Reset departmentId nếu department hiện tại không thuộc các cơ quan được chọn
      if (formData.departmentId) {
        const isDeptInOrgs = filtered.some(d => d.id.toString() === formData.departmentId)
        if (!isDeptInOrgs) {
          setFormData(prev => ({ ...prev, departmentId: '' }))
        }
      }
    } else {
      setFilteredDepartments([])
      // Reset departmentId khi không chọn cơ quan nào
      if (formData.departmentId) {
        setFormData(prev => ({ ...prev, departmentId: '' }))
      }
    }
  }, [formData.organizationIds, departments])


  const handleOpenDialog = (user?: User) => {
    if (user) {
      setSelectedUser(user)
      // Sử dụng representativeType từ user data
      setFormData({
        email: user.email,
        password: '',
        fullName: user.fullName,
        phone: user.phone || '',
        roleId: user.role?.id.toString() || '',
        representativeType: (user.representativeType || '') as 'organization' | 'department' | '',
        organizationIds: user.organizations?.map(o => o.id) || [],
        departmentId: user.department?.id.toString() || '',
        positionId: user.position?.id.toString() || '',
        loginMethod: (user.loginMethod || 'SSO') as 'SSO' | 'PASSWORD',
        isActive: user.isActive,
      })
    } else {
      setSelectedUser(null)
      setFormData({
        email: '',
        password: '',
        fullName: '',
        phone: '',
        roleId: '',
        representativeType: '',
        organizationIds: [],
        departmentId: '',
        positionId: '',
        loginMethod: 'SSO' as 'SSO' | 'PASSWORD',
        isActive: true,
      })
    }
    setIsDialogOpen(true)
  }

  const handleCloseDialog = () => {
    setIsDialogOpen(false)
    setSelectedUser(null)
  }

  const handleSubmit = async () => {
    if (!formData.email.trim() || !formData.fullName.trim()) {
      toast({
        title: 'Lỗi',
        description: 'Email và họ tên không được để trống',
        variant: 'destructive',
      })
      return
    }

    if (!selectedUser && !formData.password) {
      toast({
        title: 'Lỗi',
        description: 'Mật khẩu không được để trống',
        variant: 'destructive',
      })
      return
    }

    // Validation: Nếu chọn đại diện tổ chức thì phải chọn ít nhất một tổ chức
    if (formData.representativeType === 'organization' && formData.organizationIds.length === 0) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn ít nhất một tổ chức khi chọn đại diện cho tổ chức',
        variant: 'destructive',
      })
      return
    }

    // Validation: Nếu chọn đại diện phòng ban thì phải chọn phòng ban
    if (formData.representativeType === 'department' && !formData.departmentId) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng chọn phòng ban khi chọn đại diện cho phòng ban',
        variant: 'destructive',
      })
      return
    }


    setIsSubmitting(true)
    try {
      // Chuẩn bị dữ liệu gửi lên
      const submitData: any = {
        email: formData.email,
        fullName: formData.fullName,
        phone: formData.phone || undefined,
        roleId: formData.roleId ? parseInt(formData.roleId) : undefined,
        positionId: formData.positionId ? parseInt(formData.positionId) : undefined,
        isActive: formData.isActive,
      }

      // Xử lý organizations và department - luôn gửi dữ liệu đã chọn
      submitData.organizationIds = formData.organizationIds.length > 0 ? formData.organizationIds : []
      submitData.departmentId = formData.departmentId ? parseInt(formData.departmentId) : null
      
      // Gửi representativeType để backend biết user có phải đại diện hay không
      submitData.representativeType = formData.representativeType || null
      
      // Gửi loginMethod
      submitData.loginMethod = formData.loginMethod

      if (selectedUser) {
        if (formData.password) {
          submitData.password = formData.password
        }
        await userApi.update(selectedUser.id, submitData)
        toast({
          title: 'Thành công',
          description: 'Cập nhật người dùng thành công',
        })
      } else {
        submitData.password = formData.password
        await userApi.create(submitData)
        toast({
          title: 'Thành công',
          description: 'Tạo người dùng mới thành công',
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
    if (!selectedUser) return

    setIsSubmitting(true)
    try {
      await userApi.delete(selectedUser.id)
      toast({
        title: 'Thành công',
        description: 'Xóa người dùng thành công',
      })
      setIsDeleteDialogOpen(false)
      setSelectedUser(null)
      fetchData()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể xóa người dùng này',
        variant: 'destructive',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const filteredUsers = users.filter(
    (user) =>
      user.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase())
  )

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quản lý Người dùng</h1>
          <p className="text-gray-500 mt-1">Quản lý tài khoản người dùng trong hệ thống</p>
        </div>
        <Button
          onClick={() => handleOpenDialog()}
          className="bg-[#DA251D] hover:bg-[#b81f18]"
        >
          <Plus className="h-4 w-4 mr-2" />
          Thêm người dùng
        </Button>
      </div>

      {/* Users Table Card */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <CardTitle className="text-lg font-semibold flex items-center gap-2">
                <Users className="h-5 w-5 text-[#DA251D]" />
                Danh sách người dùng
              </CardTitle>
              <CardDescription>
                Tổng số {filteredUsers.length} người dùng
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
          ) : filteredUsers.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              Không tìm thấy người dùng nào
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Người dùng</TableHead>
                    <TableHead>Vai trò</TableHead>
                    <TableHead className="hidden md:table-cell">Phương thức đăng nhập</TableHead>
                    <TableHead className="hidden md:table-cell">Loại đại diện</TableHead>
                    <TableHead className="hidden md:table-cell">Cơ quan</TableHead>
                    <TableHead className="hidden lg:table-cell">Phòng ban</TableHead>
                    <TableHead className="hidden md:table-cell">Chức vụ</TableHead>
                    <TableHead>Trạng thái</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredUsers.map((user) => (
                    <TableRow key={user.id}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <Avatar className="h-10 w-10">
                            <AvatarFallback className="bg-[#DA251D] text-white">
                              {user.fullName.charAt(0)}
                            </AvatarFallback>
                          </Avatar>
                          <div>
                            <p className="font-medium text-gray-900">{user.fullName}</p>
                            <div className="flex items-center gap-1 text-sm text-gray-500">
                              <Mail className="h-3 w-3" />
                              {user.email}
                            </div>
                            {user.phone && (
                              <div className="flex items-center gap-1 text-sm text-gray-500">
                                <Phone className="h-3 w-3" />
                                {user.phone}
                              </div>
                            )}
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        {user.role ? (
                          <Badge
                            className={
                              user.role.name === 'Admin'
                                ? 'bg-[#FFCD00] text-black'
                                : 'bg-gray-100 text-gray-700'
                            }
                          >
                            {user.role.name}
                          </Badge>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </TableCell>
                      <TableCell className="hidden md:table-cell">
                        {user.loginMethod === 'PASSWORD' ? (
                          <Badge className="bg-blue-100 text-blue-700 hover:bg-blue-200">
                            Mật khẩu cục bộ
                          </Badge>
                        ) : (
                          <Badge className="bg-green-100 text-green-700 hover:bg-green-200">
                            SSO
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell className="hidden md:table-cell">
                        {user.representativeType === 'department' ? (
                          <Badge className="bg-blue-100 text-blue-700 hover:bg-blue-200">
                            Đại diện Phòng ban
                          </Badge>
                        ) : user.representativeType === 'organization' ? (
                          <Badge className="bg-purple-100 text-purple-700 hover:bg-purple-200">
                            Đại diện Tổ chức
                          </Badge>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </TableCell>
                      <TableCell className="hidden md:table-cell">
                        {user.organizations && user.organizations.length > 0 ? (
                          <div className="flex flex-wrap gap-1">
                            {user.organizations.map((org) => (
                              <Badge key={org.id} variant="outline" className="text-xs">
                                {org.name}
                              </Badge>
                            ))}
                          </div>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </TableCell>
                      <TableCell className="hidden lg:table-cell">
                        {user.department?.name || <span className="text-gray-400">-</span>}
                      </TableCell>
                      <TableCell className="hidden md:table-cell">
                        {user.position?.name || <span className="text-gray-400">-</span>}
                      </TableCell>
                      <TableCell>
                        <Badge variant={user.isActive ? 'success' : 'destructive'}>
                          {user.isActive ? 'Hoạt động' : 'Vô hiệu'}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleOpenDialog(user)}
                            className="hover:bg-blue-50 hover:text-blue-600"
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          {user.id !== 1 && (
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => {
                                setSelectedUser(user)
                                setIsDeleteDialogOpen(true)
                              }}
                              className="hover:bg-red-50 hover:text-red-600"
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          )}
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
        <DialogContent className="sm:max-w-[500px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {selectedUser ? 'Chỉnh sửa người dùng' : 'Thêm người dùng mới'}
            </DialogTitle>
            <DialogDescription>
              {selectedUser
                ? 'Cập nhật thông tin người dùng'
                : 'Điền thông tin để tạo tài khoản mới'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="fullName">Họ tên *</Label>
                <Input
                  id="fullName"
                  value={formData.fullName}
                  onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                  placeholder="Nhập họ tên"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="phone">Số điện thoại</Label>
                <Input
                  id="phone"
                  value={formData.phone}
                  onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                  placeholder="Nhập SĐT"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email *</Label>
              <Input
                id="email"
                type="email"
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                placeholder="Nhập email"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">
                Mật khẩu {selectedUser ? '(để trống nếu không đổi)' : '*'}
              </Label>
              <Input
                id="password"
                type="password"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                placeholder="Nhập mật khẩu"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="loginMethod">Phương thức đăng nhập</Label>
              <Select
                value={formData.loginMethod}
                onValueChange={(value) => setFormData({ ...formData, loginMethod: value as 'SSO' | 'PASSWORD' })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn phương thức đăng nhập" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SSO">SSO (Bắc Ninh SSO)</SelectItem>
                  <SelectItem value="PASSWORD">Mật khẩu cục bộ</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-gray-500">
                SSO: Đăng nhập qua hệ thống Bắc Ninh SSO. Mật khẩu cục bộ: Đăng nhập bằng mật khẩu được quản lý trong hệ thống.
              </p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="roleId">Vai trò</Label>
              <Select
                value={formData.roleId}
                onValueChange={(value) => setFormData({ ...formData, roleId: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn vai trò" />
                </SelectTrigger>
                <SelectContent>
                  {roles.map((role) => (
                    <SelectItem key={role.id} value={role.id.toString()}>
                      {role.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Chọn loại đại diện */}
            <div className="space-y-3 p-4 border rounded-lg bg-gray-50">
              <Label className="text-base font-semibold">Đại diện</Label>
              <div className="space-y-3">
                <div className="flex items-center space-x-2">
                  <input
                    type="radio"
                    id="rep-organization"
                    name="representativeType"
                    value="organization"
                    checked={formData.representativeType === 'organization'}
                    onChange={(e) => {
                      setFormData({
                        ...formData,
                        representativeType: 'organization',
                        departmentId: '', // Xóa department khi chọn organization
                      })
                    }}
                    className="h-4 w-4 text-[#DA251D] focus:ring-[#DA251D]"
                  />
                  <label htmlFor="rep-organization" className="text-sm font-medium cursor-pointer">
                    Đại diện cho Tổ chức
                  </label>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="radio"
                    id="rep-department"
                    name="representativeType"
                    value="department"
                    checked={formData.representativeType === 'department'}
                    onChange={(e) => {
                      setFormData({
                        ...formData,
                        representativeType: 'department',
                        organizationIds: [], // Xóa organization khi chọn department
                      })
                    }}
                    className="h-4 w-4 text-[#DA251D] focus:ring-[#DA251D]"
                  />
                  <label htmlFor="rep-department" className="text-sm font-medium cursor-pointer">
                    Đại diện cho Phòng ban
                  </label>
                </div>
                <div className="flex items-center space-x-2">
                  <input
                    type="radio"
                    id="rep-none"
                    name="representativeType"
                    value=""
                    checked={formData.representativeType === ''}
                    onChange={(e) => {
                      setFormData({
                        ...formData,
                        representativeType: '',
                        organizationIds: [], // Xóa organization khi không chọn đại diện
                        departmentId: '', // Xóa department khi không chọn đại diện
                      })
                    }}
                    className="h-4 w-4 text-[#DA251D] focus:ring-[#DA251D]"
                  />
                  <label htmlFor="rep-none" className="text-sm font-medium cursor-pointer">
                    Không phải đại diện
                  </label>
                </div>
              </div>
            </div>

            {/* Trường Cơ quan - luôn hiển thị */}
            <div className="space-y-2">
              <Label>Cơ quan</Label>
              <div className="border rounded-md p-3 max-h-40 overflow-y-auto space-y-2">
                {organizations.map((org) => (
                  <div key={org.id} className="flex items-center space-x-2">
                    <Checkbox
                      id={`org-${org.id}`}
                      checked={formData.organizationIds.includes(org.id)}
                      onCheckedChange={(checked) => {
                        if (checked) {
                          setFormData({
                            ...formData,
                            organizationIds: [...formData.organizationIds, org.id],
                          })
                        } else {
                          setFormData({
                            ...formData,
                            organizationIds: formData.organizationIds.filter(id => id !== org.id),
                          })
                        }
                      }}
                    />
                    <label
                      htmlFor={`org-${org.id}`}
                      className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70 cursor-pointer"
                    >
                      {org.name}
                    </label>
                  </div>
                ))}
              </div>
            </div>

            {/* Trường Phòng ban - luôn hiển thị */}
            <div className="space-y-2">
              <Label htmlFor="departmentId">Phòng ban</Label>
              <Select
                value={formData.departmentId || 'none'}
                onValueChange={(value) => {
                  setFormData({ 
                    ...formData, 
                    departmentId: value === 'none' ? '' : value 
                  })
                }}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn phòng ban" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">
                    Không thuộc phòng ban nào
                  </SelectItem>
                  {formData.organizationIds.length > 0 ? (
                    // Chỉ hiển thị phòng ban thuộc các cơ quan đã chọn
                    filteredDepartments.map((dept) => (
                      <SelectItem key={dept.id} value={dept.id.toString()}>
                        {dept.name}
                      </SelectItem>
                    ))
                  ) : (
                    // Hiển thị tất cả phòng ban nếu chưa chọn cơ quan nào
                    departments.map((dept) => (
                      <SelectItem key={dept.id} value={dept.id.toString()}>
                        {dept.name}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
              {formData.organizationIds.length === 0 && (
                <p className="text-xs text-amber-600 mt-1">
                  💡 Chọn cơ quan trước để lọc phòng ban
                </p>
              )}
              {formData.departmentId && (() => {
                const selectedDept = departments.find(d => d.id.toString() === formData.departmentId)
                const orgName = selectedDept ? organizations.find(o => o.id === selectedDept.organizationId)?.name : null
                return orgName ? (
                  <p className="text-xs text-gray-500 mt-1">
                    Tổ chức: {orgName}
                  </p>
                ) : null
              })()}
            </div>

            <div className="space-y-2">
              <Label htmlFor="positionId">Chức vụ</Label>
              <Select
                value={formData.positionId}
                onValueChange={(value) => setFormData({ ...formData, positionId: value })}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Chọn chức vụ" />
                </SelectTrigger>
                <SelectContent>
                  {positions.map((pos) => (
                    <SelectItem key={pos.id} value={pos.id.toString()}>
                      {pos.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
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
              {selectedUser ? 'Cập nhật' : 'Tạo mới'}
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
              Bạn có chắc chắn muốn xóa người dùng &quot;{selectedUser?.fullName}&quot;?
              Hành động này không thể hoàn tác.
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

