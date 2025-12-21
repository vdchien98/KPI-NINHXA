"use client"

import { useEffect, useState, useRef } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { 
  ArrowLeft, 
  Loader2, 
  Plus, 
  Trash2, 
  FileText,
  Send,
  XCircle
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { useToast } from '@/components/ui/use-toast'
import { reportRequestApi, reportResponseApi } from '@/lib/api'
import { isPast, formatDateTime } from '@/lib/utils'

interface ReportRequest {
  id: number
  title: string
  description?: string
  deadline: string
  status: string
}

interface ReportItem {
  id?: number
  title?: string // Tiêu đề mục báo cáo
  content?: string // Nội dung mục báo cáo
  progress?: number // Tiến độ hoàn thành (0-100%)
  difficulties?: string // Khó khăn gặp phải
  fileName?: string
  filePath?: string
  fileType?: string
  fileSize?: number
  displayOrder: number
}

interface ExistingResponse {
  id: number
  note?: string
  score?: number
  evaluatedBy?: {
    id: number
    fullName: string
  }
  evaluatedAt?: string
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
    displayOrder: number
  }[]
}

export default function SubmitReportPage() {
  const params = useParams()
  const router = useRouter()
  const { toast } = useToast()
  const [request, setRequest] = useState<ReportRequest | null>(null)
  const [existingResponse, setExistingResponse] = useState<ExistingResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  
  const [note, setNote] = useState('')
  const [items, setItems] = useState<ReportItem[]>([
    { title: '', content: '', progress: 0, difficulties: '', displayOrder: 0 }
  ])
  const fileInputRefs = useRef<{ [key: number]: HTMLInputElement | null }>({})
  const itemFiles = useRef<{ [key: number]: File | null }>({})
  
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

  const fetchData = async () => {
    try {
      // Fetch request details
      const requestRes = await reportRequestApi.getById(Number(params.id))
      setRequest(requestRes.data.data)
      
      // Check if user already submitted
      const responseRes = await reportResponseApi.getMyResponseForRequest(Number(params.id))
      if (responseRes.data.data) {
        // Check if report has been evaluated
        if (responseRes.data.data.score !== null && responseRes.data.data.score !== undefined) {
          toast({
            title: 'Không thể chỉnh sửa',
            description: 'Báo cáo đã được đánh giá, không thể chỉnh sửa',
            variant: 'destructive',
          })
          router.push(`/management/inbox/${params.id}`)
          return
        }
        setExistingResponse(responseRes.data.data)
        setNote(responseRes.data.data.note || '')
        if (responseRes.data.data.items && responseRes.data.data.items.length > 0) {
          setItems(responseRes.data.data.items.map((item: any, index: number) => ({
            id: item.id,
            title: item.title || '',
            content: item.content || '',
            progress: item.progress || 0,
            difficulties: item.difficulties || '',
            displayOrder: item.displayOrder ?? index,
            fileName: item.fileName,
            filePath: item.filePath,
            fileType: item.fileType,
            fileSize: item.fileSize,
          })))
        }
      }
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
      fetchData()
    }
  }, [params.id])

  const addItem = () => {
    setItems([...items, { title: '', content: '', progress: 0, difficulties: '', displayOrder: items.length }])
  }

  const removeItem = (index: number) => {
    if (items.length === 1) {
      toast({
        title: 'Cảnh báo',
        description: 'Phải có ít nhất 1 nội dung báo cáo',
        variant: 'destructive',
      })
      return
    }
    const newItems = items.filter((_, i) => i !== index)
    setItems(newItems.map((item, i) => ({ ...item, displayOrder: i })))
  }

  const updateItemContent = (index: number, field: 'title' | 'content' | 'progress' | 'difficulties', value: string | number) => {
    const newItems = [...items]
    if (field === 'progress') {
      newItems[index].progress = typeof value === 'number' ? value : parseInt(value as string) || 0
    } else {
      (newItems[index] as any)[field] = value
    }
    setItems(newItems)
  }


  const isOverdue = () => {
    if (!request?.deadline) return false
    return isPast(request.deadline)
  }

  const handleSubmit = async () => {
    // Check if deadline has passed
    if (isOverdue()) {
      toast({
        title: 'Không thể nộp báo cáo',
        description: 'Yêu cầu báo cáo đã quá hạn, không thể nộp hoặc chỉnh sửa báo cáo',
        variant: 'destructive',
      })
      return
    }

    // Validate - ít nhất phải có title hoặc content
    const invalidItems = items.filter(item => !item.title?.trim() && !item.content?.trim())
    if (invalidItems.length > 0) {
      toast({
        title: 'Lỗi',
        description: 'Vui lòng nhập tiêu đề hoặc nội dung cho tất cả các mục',
        variant: 'destructive',
      })
      return
    }

    // Validate progress (0-100)
    const invalidProgress = items.filter(item => item.progress !== undefined && (item.progress < 0 || item.progress > 100))
    if (invalidProgress.length > 0) {
      toast({
        title: 'Lỗi',
        description: 'Tiến độ hoàn thành phải từ 0 đến 100%',
        variant: 'destructive',
      })
      return
    }

    setSubmitting(true)
    try {
      // Create or update response
      const responseData = {
        reportRequestId: Number(params.id),
        note: note || undefined,
        items: items.map((item, index) => ({
          title: item.title || undefined,
          content: item.content || undefined,
          progress: item.progress !== undefined ? item.progress : 0,
          difficulties: item.difficulties || undefined,
          displayOrder: index
        }))
      }

      let responseId: number
      
      let savedResponse: any
      if (existingResponse) {
        // Update existing
        const res = await reportResponseApi.update(existingResponse.id, responseData)
        savedResponse = res.data.data
        responseId = existingResponse.id
      } else {
        // Create new
        const res = await reportResponseApi.create(responseData)
        savedResponse = res.data.data
        responseId = res.data.data.id
      }
      
      // Upload files for each item (matching by displayOrder)
      const uploadedItems: { [key: number]: any } = {}
      const sortedItems = (savedResponse?.items || []).slice().sort(
        (a: any, b: any) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0)
      )
      
      const uploadPromises = items.map((item, index) => {
        const file = itemFiles.current[index]
        if (!file) return null
        const targetItem = sortedItems[index]
        if (!targetItem?.id) return null
        return reportResponseApi.uploadItemFile(targetItem.id, file).then((res) => {
          uploadedItems[index] = res.data.data
          // clear selected file after upload
          itemFiles.current[index] = null
          const inputRef = fileInputRefs.current[index]
          if (inputRef) {
            inputRef.value = ''
          }
        })
      }).filter(Boolean)
      
      await Promise.all(uploadPromises)
      
      // Merge uploaded file info into local state for immediate UI update
      if (Object.keys(uploadedItems).length > 0) {
        setItems((prev) => prev.map((it, idx) => {
          const uploaded = uploadedItems[idx]
          if (!uploaded) return it
          return {
            ...it,
            fileName: uploaded.fileName,
            filePath: uploaded.filePath,
            fileType: uploaded.fileType,
            fileSize: uploaded.fileSize,
          }
        }))
      }
      
      // Refresh existingResponse state so future edits preserve ids/files
      if (savedResponse) {
        setExistingResponse(savedResponse)
      }
      
      toast({
        title: 'Thành công',
        description: existingResponse ? 'Cập nhật báo cáo thành công' : 'Nộp báo cáo thành công',
      })
      
      router.push(`/management/inbox/${params.id}`)
    } catch (error: any) {
      toast({
        title: 'Lỗi',
        description: error.response?.data?.message || 'Không thể nộp báo cáo',
        variant: 'destructive',
      })
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-[#DA251D]" />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => router.back()}
        >
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">
            {existingResponse ? 'Chỉnh sửa báo cáo' : 'Thực hiện báo cáo'}
          </h1>
          <p className="text-gray-500 mt-1">
            Yêu cầu: {request?.title}
          </p>
        </div>
      </div>

      {/* Request Info */}
      <Card className={`border-0 shadow-sm ${isOverdue() ? 'bg-red-50 border-red-200' : 'bg-blue-50'}`}>
        <CardContent className="p-4">
          <div className="flex items-start gap-3">
            <FileText className={`h-5 w-5 mt-0.5 ${isOverdue() ? 'text-red-600' : 'text-blue-600'}`} />
            <div className="flex-1">
              <h3 className={`font-medium ${isOverdue() ? 'text-red-900' : 'text-blue-900'}`}>{request?.title}</h3>
              {request?.description && (
                <p className={`text-sm mt-1 ${isOverdue() ? 'text-red-700' : 'text-blue-700'}`}>{request.description}</p>
              )}
              <div className="flex items-center gap-2 mt-2">
                <p className={`text-xs ${isOverdue() ? 'text-red-600' : 'text-blue-600'}`}>
                  Hạn nộp: {request?.deadline && formatDateTime(request.deadline)}
                </p>
                {isOverdue() && (
                  <Badge className="bg-red-100 text-red-700 border-red-300 text-xs">
                    Đã quá hạn
                  </Badge>
                )}
              </div>
              {isOverdue() && (
                <div className="mt-3 p-3 bg-red-100 border border-red-300 rounded-lg">
                  <p className="text-sm font-medium text-red-800">
                    ⚠️ Yêu cầu báo cáo đã quá hạn. Bạn không thể nộp hoặc chỉnh sửa báo cáo.
                  </p>
                </div>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Note */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <CardTitle className="text-lg">Ghi chú chung</CardTitle>
          <CardDescription>Thêm ghi chú tổng quan cho báo cáo (không bắt buộc)</CardDescription>
        </CardHeader>
        <CardContent>
          <Textarea
            placeholder="Nhập ghi chú..."
            value={note}
            onChange={(e) => setNote(e.target.value)}
            rows={3}
            disabled={isOverdue()}
          />
        </CardContent>
      </Card>

      {/* Report Items */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-lg">Nội dung báo cáo</CardTitle>
              <CardDescription>Thêm các mục báo cáo</CardDescription>
            </div>
            <Button 
              onClick={addItem} 
              className="bg-[#DA251D] hover:bg-[#b91c1c]"
              disabled={isOverdue()}
            >
              <Plus className="h-4 w-4 mr-2" />
              Thêm dòng
            </Button>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          {items.map((item, index) => (
            <div key={index} className="p-4 border rounded-lg bg-gray-50 space-y-3">
              <div className="flex items-center justify-between">
                <Badge variant="outline" className="bg-white">
                  Mục {index + 1}
                </Badge>
                <Button
                  variant="ghost"
                  size="icon"
                  onClick={() => removeItem(index)}
                  className="text-red-500 hover:text-red-700 hover:bg-red-50"
                  disabled={isOverdue()}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
              
              <div className="space-y-3">
                  <div className="space-y-2">
                  <Label>Tiêu đề mục *</Label>
                  <Input
                    placeholder="Nhập tiêu đề mục báo cáo..."
                    value={item.title || ''}
                    onChange={(e) => updateItemContent(index, 'title', e.target.value)}
                    disabled={isOverdue()}
                  />
                </div>
                
                <div className="space-y-2">
                  <Label>Nội dung</Label>
                  <Textarea
                    placeholder="Nhập nội dung báo cáo..."
                    value={item.content || ''}
                    onChange={(e) => updateItemContent(index, 'content', e.target.value)}
                    rows={3}
                    disabled={isOverdue()}
                  />
                </div>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label>Tiến độ hoàn thành (%)</Label>
                    <div className="flex items-center gap-2">
                      <Input
                        type="number"
                        min="0"
                        max="100"
                        placeholder="0"
                        value={item.progress || 0}
                        onChange={(e) => updateItemContent(index, 'progress', parseInt(e.target.value) || 0)}
                        className="w-24"
                        disabled={isOverdue()}
                      />
                      <div className="flex-1">
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <div 
                            className="bg-[#DA251D] h-2 rounded-full transition-all"
                            style={{ width: `${item.progress || 0}%` }}
                          />
                        </div>
                      </div>
                      <span className="text-sm text-gray-500 w-12 text-right">{item.progress || 0}%</span>
                    </div>
                  </div>
                </div>
                
                <div className="space-y-2">
                  <Label>Khó khăn gặp phải</Label>
                  <Textarea
                    placeholder="Nhập các khó khăn gặp phải..."
                    value={item.difficulties || ''}
                    onChange={(e) => updateItemContent(index, 'difficulties', e.target.value)}
                    rows={2}
                    disabled={isOverdue()}
                  />
                </div>
                
                <div className="space-y-2">
                  <Label>File đính kèm</Label>
                  {item.filePath && (
                    <div className="flex items-center gap-2 p-2 bg-gray-50 rounded border">
                      <FileText className="h-4 w-4 text-gray-500" />
                      <span className="text-sm text-gray-700 flex-1">{item.fileName}</span>
                      <Button
                        variant="link"
                        className="p-0 text-blue-600"
                        onClick={() => handleDownload(item.filePath, item.fileName)}
                      >
                        Tải xuống
                      </Button>
                    </div>
                  )}
                  <Input
                    type="file"
                    accept="image/*,.pdf"
                    ref={(el) => {
                      fileInputRefs.current[index] = el
                    }}
                    onChange={(e) => {
                      const file = e.target.files?.[0]
                      if (file) {
                        // Lưu file để upload sau
                        itemFiles.current[index] = file
                        const newItems = [...items]
                        newItems[index].fileName = file.name
                        setItems(newItems)
                      } else {
                        itemFiles.current[index] = null
                      }
                    }}
                    className="cursor-pointer"
                    disabled={isOverdue()}
                  />
                  {item.fileName && !item.filePath && (
                    <p className="text-sm text-gray-500">File đã chọn: {item.fileName}</p>
                  )}
                </div>
              </div>
              
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Actions */}
      <div className="flex justify-end gap-3">
        <Button variant="outline" onClick={() => router.back()}>
          Hủy
        </Button>
        <Button 
          onClick={handleSubmit}
          disabled={submitting || isOverdue()}
          className="bg-[#DA251D] hover:bg-[#b91c1c]"
        >
          {submitting ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              Đang xử lý...
            </>
          ) : isOverdue() ? (
            <>
              <XCircle className="h-4 w-4 mr-2" />
              Đã quá hạn
            </>
          ) : (
            <>
              <Send className="h-4 w-4 mr-2" />
              {existingResponse ? 'Cập nhật báo cáo' : 'Nộp báo cáo'}
            </>
          )}
        </Button>
      </div>
    </div>
  )
}

