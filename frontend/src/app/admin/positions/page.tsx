"use client"

import { useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, Award, Search, Loader2, ArrowUpDown } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Switch } from '@/components/ui/switch'
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/components/ui/use-toast'
import { positionApi } from '@/lib/api'

interface Position {
  id: number
  name: string
  code?: string
  description?: string
  displayOrder: number
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export default function PositionsPage() {
  const { toast } = useToast()
  const [positions, setPositions] = useState<Position[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false)
  const [selectedPosition, setSelectedPosition] = useState<Position | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    code: '',
    description: '',
    displayOrder: 0,
    isActive: true,
  })
  const [isSubmitting, setIsSubmitting] = useState(false)

  const fetchPositions = async () => {
    try {
      const response = await positionApi.getAll()
      setPositions(response.data.data || [])
    } catch (error) {
      toast({
        title: 'Lỗi',
        description: 'Không thể tải danh sách chức vụ',
        variant: 'destructive',
      })
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchPositions()
  }, [])

  const handleOpenDialog = (position?: Position) => {
    if (position) {
      setSelectedPosition(position)
      setFormData({
        name: position.name,
        code: position.code || '',
        description: position.description || '',
        displayOrder: position.displayOrder,
        isActive: position.isActive,
      })
    } else {
      setSelectedPosition(null)
      setFormData({
        name: '',
        code: '',
        description: '',
        displayOrder: positions.length,
        isActive: true,
      })
    }
    setIsDialogOpen(true)
  }

  const handleCloseDialog = () => {
    setIsDialogOpen(false)
    setSelectedPosition(null)
    setFormData({
      name: '',
      code: '',
      description: '',
      displayOrder: 0,
      isActive: true,
    })
  }

  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      toast({
        title: 'Lỗi',
        description: 'Tên chức vụ không được để trống',
        variant: 'destructive',
      })
      return
    }

    setIsSubmitting(true)
    try {
      const data = {
        name: formData.name.trim(),
        code: formData.code.trim() || undefined,
        description: formData.description.trim() || undefined,
        displayOrder: formData.displayOrder,
        isActive: formData.isActive,
      }

      if (selectedPosition) {
        await positionApi.update(selectedPosition.id, data)
        toast({
          title: 'Thành công',
          description: 'Cập nhật chức vụ thành công',
        })
      } else {
        await positionApi.create(data)
        toast({
          title: 'Thành công',
          description: 'Tạo chức vụ mới thành công',
        })
      }

      handleCloseDialog()
      fetchPositions()
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
    if (!selectedPosition) return

    setIsSubmitting(true)
    try {
      await positionApi.delete(selectedPosition.id)
      toast({
        title: 'Thành công',
        description: 'Xóa chức vụ thành công',
      })
      setIsDeleteDialogOpen(false)
      setSelectedPosition(null)
      fetchPositions()
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể xóa chức vụ này',
        variant: 'destructive',
      })
    } finally {
      setIsSubmitting(false)
    }
  }

  const filteredPositions = positions.filter((position) => {
    const matchesSearch =
      position.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (position.code && position.code.toLowerCase().includes(searchTerm.toLowerCase()))
    return matchesSearch
  })

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Quản lý Chức vụ</h1>
          <p className="text-gray-500 mt-1">Quản lý các chức vụ trong cơ quan</p>
        </div>
        <Button
          onClick={() => handleOpenDialog()}
          className="bg-[#DA251D] hover:bg-[#b81f18]"
        >
          <Plus className="h-4 w-4 mr-2" />
          Thêm Chức vụ
        </Button>
      </div>

      {/* Positions Table Card */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <CardTitle className="text-lg font-semibold flex items-center gap-2">
                <Award className="h-5 w-5 text-[#DA251D]" />
                Danh sách Chức vụ
              </CardTitle>
              <CardDescription>
                Tổng cộng {positions.length} chức vụ
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
          ) : filteredPositions.length === 0 ? (
            <div className="text-center py-12 text-gray-500">
              {searchTerm ? 'Không tìm thấy chức vụ phù hợp' : 'Chưa có chức vụ nào được tạo'}
            </div>
          ) : (
            <div className="overflow-x-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-16">
                      <div className="flex items-center gap-1">
                        <ArrowUpDown className="h-4 w-4" />
                        STT
                      </div>
                    </TableHead>
                    <TableHead>Tên chức vụ</TableHead>
                    <TableHead>Mã</TableHead>
                    <TableHead>Mô tả</TableHead>
                    <TableHead className="text-center">Trạng thái</TableHead>
                    <TableHead className="text-right">Thao tác</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredPositions.map((position) => (
                    <TableRow key={position.id} className="hover:bg-gray-50">
                      <TableCell className="font-medium text-center">
                        {position.displayOrder + 1}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <div className="p-2 rounded-lg bg-[#DA251D]/10">
                            <Award className="h-4 w-4 text-[#DA251D]" />
                          </div>
                          <span className="font-medium">{position.name}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        {position.code ? (
                          <Badge variant="outline" className="font-mono">
                            {position.code}
                          </Badge>
                        ) : (
                          <span className="text-gray-400">-</span>
                        )}
                      </TableCell>
                      <TableCell className="max-w-xs truncate">
                        {position.description || '-'}
                      </TableCell>
                      <TableCell className="text-center">
                        {position.isActive ? (
                          <Badge className="bg-green-100 text-green-700 hover:bg-green-100">
                            Hoạt động
                          </Badge>
                        ) : (
                          <Badge variant="secondary">
                            Không hoạt động
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleOpenDialog(position)}
                            className="hover:bg-blue-50 hover:text-blue-600"
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => {
                              setSelectedPosition(position)
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
              {selectedPosition ? 'Chỉnh sửa Chức vụ' : 'Thêm Chức vụ mới'}
            </DialogTitle>
            <DialogDescription>
              {selectedPosition
                ? 'Cập nhật thông tin chức vụ'
                : 'Điền thông tin để tạo chức vụ mới'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="name">Tên chức vụ *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder="VD: Chủ tịch, Phó chủ tịch..."
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="code">Mã chức vụ</Label>
                <Input
                  id="code"
                  value={formData.code}
                  onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                  placeholder="VD: CT, PCT..."
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Mô tả</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Nhập mô tả chức vụ..."
                rows={3}
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="displayOrder">Thứ tự hiển thị</Label>
                <Input
                  id="displayOrder"
                  type="number"
                  min={0}
                  value={formData.displayOrder}
                  onChange={(e) => setFormData({ ...formData, displayOrder: parseInt(e.target.value) || 0 })}
                />
              </div>
              <div className="space-y-2">
                <Label>Trạng thái</Label>
                <div className="flex items-center space-x-2 pt-2">
                  <Switch
                    id="isActive"
                    checked={formData.isActive}
                    onCheckedChange={(checked) => setFormData({ ...formData, isActive: checked })}
                  />
                  <Label htmlFor="isActive" className="font-normal">
                    {formData.isActive ? 'Đang hoạt động' : 'Không hoạt động'}
                  </Label>
                </div>
              </div>
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
              {selectedPosition ? 'Cập nhật' : 'Tạo mới'}
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
              Bạn có chắc chắn muốn xóa chức vụ &quot;{selectedPosition?.name}&quot;? Hành động
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

