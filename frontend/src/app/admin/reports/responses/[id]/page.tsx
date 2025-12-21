"use client"

import { useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { ArrowLeft, Save, Loader2, Plus, Trash2 } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { useToast } from '@/components/ui/use-toast'
import { adminReportApi } from '@/lib/api'

interface ReportResponseItem {
  id?: number
  title?: string
  content?: string
  progress?: number
  difficulties?: string
}

interface ReportResponse {
  id: number
  note?: string
  score?: number
  comment?: string
  selfScore?: number
  submittedBy: {
    id: number
    fullName: string
    email: string
  }
  reportRequest: {
    id: number
    title: string
  }
  items: ReportResponseItem[]
}

export default function AdminEditResponsePage() {
  const params = useParams()
  const router = useRouter()
  const { toast } = useToast()
  const [response, setResponse] = useState<ReportResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  
  const [formData, setFormData] = useState({
    note: '',
    score: '',
    comment: '',
    selfScore: '',
    items: [] as ReportResponseItem[],
  })

  useEffect(() => {
    fetchResponse()
  }, [params.id])

  const fetchResponse = async () => {
    try {
      setLoading(true)
      const res = await adminReportApi.getResponseById(Number(params.id))
      const data = res.data.data
      setResponse(data)
      
      setFormData({
        note: data.note || '',
        score: data.score?.toString() || '',
        comment: data.comment || '',
        selfScore: data.selfScore?.toString() || '',
        items: data.items || [],
      })
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể tải dữ liệu',
        variant: 'destructive',
      })
      router.push('/admin/reports')
    } finally {
      setLoading(false)
    }
  }

  const handleAddItem = () => {
    setFormData({
      ...formData,
      items: [
        ...formData.items,
        { title: '', content: '', progress: 0, difficulties: '' },
      ],
    })
  }

  const handleRemoveItem = (index: number) => {
    setFormData({
      ...formData,
      items: formData.items.filter((_, i) => i !== index),
    })
  }

  const handleItemChange = (index: number, field: keyof ReportResponseItem, value: any) => {
    const newItems = [...formData.items]
    newItems[index] = { ...newItems[index], [field]: value }
    setFormData({ ...formData, items: newItems })
  }

  const handleSave = async () => {
    try {
      setSaving(true)
      
      await adminReportApi.updateResponse(Number(params.id), {
        note: formData.note || undefined,
        score: formData.score ? parseFloat(formData.score) : undefined,
        comment: formData.comment || undefined,
        selfScore: formData.selfScore ? parseFloat(formData.selfScore) : undefined,
        items: formData.items.map(item => ({
          title: item.title || '',
          content: item.content || '',
          progress: item.progress || 0,
          difficulties: item.difficulties || '',
        })),
      })
      
      toast({
        title: 'Thành công',
        description: 'Cập nhật nội dung báo cáo thành công',
      })
      
      router.push('/admin/reports')
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể cập nhật báo cáo',
        variant: 'destructive',
      })
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <div className="text-center">
          <Loader2 className="h-12 w-12 animate-spin text-[#DA251D] mx-auto" />
          <p className="mt-4 text-gray-600">Đang tải dữ liệu...</p>
        </div>
      </div>
    )
  }

  if (!response) {
    return null
  }

  return (
    <div className="container mx-auto p-6 max-w-5xl">
      {/* Header */}
      <div className="mb-6">
        <Button
          variant="ghost"
          onClick={() => router.push('/admin/reports')}
          className="mb-4"
        >
          <ArrowLeft className="h-4 w-4 mr-2" />
          Quay lại
        </Button>
        
        <h1 className="text-3xl font-bold text-gray-900">Chỉnh sửa nội dung báo cáo</h1>
        <p className="text-gray-500 mt-1">
          Báo cáo: {response.reportRequest.title}
        </p>
        <p className="text-gray-500 text-sm">
          Người báo cáo: {response.submittedBy.fullName} ({response.submittedBy.email})
        </p>
      </div>

      <div className="space-y-6">
        {/* Ghi chú */}
        <Card>
          <CardHeader>
            <CardTitle>Ghi chú</CardTitle>
            <CardDescription>Ghi chú chung của báo cáo</CardDescription>
          </CardHeader>
          <CardContent>
            <Textarea
              value={formData.note}
              onChange={(e) => setFormData({ ...formData, note: e.target.value })}
              placeholder="Nhập ghi chú..."
              className="min-h-[100px]"
            />
          </CardContent>
        </Card>

        {/* Điểm đánh giá */}
        <Card>
          <CardHeader>
            <CardTitle>Đánh giá</CardTitle>
            <CardDescription>Điểm đánh giá và nhận xét</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="score">Điểm đánh giá (người yêu cầu)</Label>
                <Input
                  id="score"
                  type="number"
                  min="0"
                  max="10"
                  step="0.1"
                  value={formData.score}
                  onChange={(e) => setFormData({ ...formData, score: e.target.value })}
                  placeholder="0-10"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="selfScore">Điểm tự đánh giá</Label>
                <Input
                  id="selfScore"
                  type="number"
                  min="0"
                  max="10"
                  step="0.1"
                  value={formData.selfScore}
                  onChange={(e) => setFormData({ ...formData, selfScore: e.target.value })}
                  placeholder="0-10"
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="comment">Nhận xét</Label>
              <Textarea
                id="comment"
                value={formData.comment}
                onChange={(e) => setFormData({ ...formData, comment: e.target.value })}
                placeholder="Nhập nhận xét..."
                className="min-h-[100px]"
              />
            </div>
          </CardContent>
        </Card>

        {/* Nội dung báo cáo */}
        <Card>
          <CardHeader>
            <div className="flex justify-between items-center">
              <div>
                <CardTitle>Nội dung báo cáo</CardTitle>
                <CardDescription>Các mục báo cáo chi tiết</CardDescription>
              </div>
              <Button onClick={handleAddItem} size="sm">
                <Plus className="h-4 w-4 mr-2" />
                Thêm mục
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-6">
            {formData.items.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                Chưa có mục báo cáo nào. Nhấn "Thêm mục" để thêm.
              </div>
            ) : (
              formData.items.map((item, index) => (
                <div key={index} className="border rounded-lg p-4 space-y-4">
                  <div className="flex justify-between items-start">
                    <h3 className="font-semibold text-lg">Mục {index + 1}</h3>
                    <Button
                      variant="ghost"
                      size="icon"
                      onClick={() => handleRemoveItem(index)}
                      className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`item-title-${index}`}>Tiêu đề</Label>
                    <Input
                      id={`item-title-${index}`}
                      value={item.title || ''}
                      onChange={(e) => handleItemChange(index, 'title', e.target.value)}
                      placeholder="Nhập tiêu đề mục báo cáo"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`item-content-${index}`}>Mô tả / Nội dung</Label>
                    <Textarea
                      id={`item-content-${index}`}
                      value={item.content || ''}
                      onChange={(e) => handleItemChange(index, 'content', e.target.value)}
                      placeholder="Nhập mô tả chi tiết"
                      className="min-h-[100px]"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`item-progress-${index}`}>
                      Tiến độ hoàn thành (%)
                    </Label>
                    <Input
                      id={`item-progress-${index}`}
                      type="number"
                      min="0"
                      max="100"
                      value={item.progress || 0}
                      onChange={(e) => handleItemChange(index, 'progress', parseInt(e.target.value) || 0)}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`item-difficulties-${index}`}>
                      Khó khăn gặp phải
                    </Label>
                    <Textarea
                      id={`item-difficulties-${index}`}
                      value={item.difficulties || ''}
                      onChange={(e) => handleItemChange(index, 'difficulties', e.target.value)}
                      placeholder="Nhập khó khăn gặp phải (nếu có)"
                      className="min-h-[80px]"
                    />
                  </div>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        {/* Action Buttons */}
        <div className="flex justify-end gap-4">
          <Button
            variant="outline"
            onClick={() => router.push('/admin/reports')}
            disabled={saving}
          >
            Hủy
          </Button>
          <Button
            onClick={handleSave}
            disabled={saving}
            className="bg-[#DA251D] hover:bg-[#b91d17]"
          >
            {saving ? (
              <>
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                Đang lưu...
              </>
            ) : (
              <>
                <Save className="h-4 w-4 mr-2" />
                Lưu thay đổi
              </>
            )}
          </Button>
        </div>
      </div>
    </div>
  )
}

