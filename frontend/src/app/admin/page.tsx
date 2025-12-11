"use client"

import { useEffect, useState } from 'react'
import { Users, Building2, Shield, Building, Star } from 'lucide-react'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuthStore } from '@/lib/store'
import { userApi, roleApi, organizationApi, departmentApi } from '@/lib/api'

interface DashboardStats {
  users: number
  roles: number
  organizations: number
  departments: number
}

export default function AdminDashboard() {
  const { user } = useAuthStore()
  const [stats, setStats] = useState<DashboardStats>({
    users: 0,
    roles: 0,
    organizations: 0,
    departments: 0,
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const [usersRes, rolesRes, orgsRes, deptsRes] = await Promise.all([
          userApi.getAll(),
          roleApi.getAll(),
          organizationApi.getAll(),
          departmentApi.getAll(),
        ])

        setStats({
          users: usersRes.data.data?.length || 0,
          roles: rolesRes.data.data?.length || 0,
          organizations: orgsRes.data.data?.length || 0,
          departments: deptsRes.data.data?.length || 0,
        })
      } catch (error) {
        console.error('Error fetching stats:', error)
      } finally {
        setLoading(false)
      }
    }

    fetchStats()
  }, [])

  const statCards = [
    {
      title: 'Người dùng',
      value: stats.users,
      icon: Users,
      color: 'bg-blue-500',
      description: 'Tổng số người dùng trong hệ thống',
    },
    {
      title: 'Vai trò (Role)',
      value: stats.roles,
      icon: Shield,
      color: 'bg-[#DA251D]',
      description: 'Tổng số vai trò được thiết lập',
    },
    {
      title: 'Cơ quan',
      value: stats.organizations,
      icon: Building2,
      color: 'bg-green-500',
      description: 'Tổng số cơ quan trong hệ thống',
    },
    {
      title: 'Phòng ban',
      value: stats.departments,
      icon: Building,
      color: 'bg-[#FFCD00]',
      description: 'Tổng số phòng ban trong các cơ quan',
    },
  ]

  return (
    <div className="space-y-6">
      {/* Welcome Section */}
      <div className="bg-gradient-to-r from-[#DA251D] to-[#b81f18] rounded-xl p-6 text-white shadow-lg">
        <div className="flex items-center gap-4">
          <div className="p-3 bg-white/20 rounded-full">
            <Star className="h-8 w-8 text-[#FFCD00] fill-current" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">Xin chào, {user?.fullName}!</h1>
            <p className="text-white/80 mt-1">
              Chào mừng đến với hệ thống quản trị Phường Ninh Xá
            </p>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((stat, index) => (
          <Card key={index} className="border-0 shadow-md hover:shadow-lg transition-shadow">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-gray-600">
                {stat.title}
              </CardTitle>
              <div className={`p-2 rounded-lg ${stat.color}`}>
                <stat.icon className="h-5 w-5 text-white" />
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="h-8 w-16 bg-gray-200 animate-pulse rounded"></div>
              ) : (
                <div className="text-3xl font-bold text-gray-900">{stat.value}</div>
              )}
              <p className="text-xs text-gray-500 mt-1">{stat.description}</p>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Quick Actions */}
      <Card className="border-0 shadow-md">
        <CardHeader>
          <CardTitle className="text-lg font-semibold text-gray-900">
            Thao tác nhanh
          </CardTitle>
          <CardDescription>
            Các chức năng quản trị thường dùng
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <a
              href="/admin/users"
              className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-[#DA251D] hover:bg-[#DA251D]/5 transition-all cursor-pointer"
            >
              <Users className="h-6 w-6 text-[#DA251D]" />
              <div>
                <p className="font-medium text-gray-900">Quản lý người dùng</p>
                <p className="text-xs text-gray-500">Thêm, sửa, xóa người dùng</p>
              </div>
            </a>
            <a
              href="/admin/roles"
              className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-[#DA251D] hover:bg-[#DA251D]/5 transition-all cursor-pointer"
            >
              <Shield className="h-6 w-6 text-[#DA251D]" />
              <div>
                <p className="font-medium text-gray-900">Quản lý vai trò</p>
                <p className="text-xs text-gray-500">Phân quyền hệ thống</p>
              </div>
            </a>
            <a
              href="/admin/organizations"
              className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-[#DA251D] hover:bg-[#DA251D]/5 transition-all cursor-pointer"
            >
              <Building2 className="h-6 w-6 text-[#DA251D]" />
              <div>
                <p className="font-medium text-gray-900">Quản lý cơ quan</p>
                <p className="text-xs text-gray-500">Thêm, sửa cơ quan</p>
              </div>
            </a>
            <a
              href="/admin/departments"
              className="flex items-center gap-3 p-4 rounded-lg border border-gray-200 hover:border-[#DA251D] hover:bg-[#DA251D]/5 transition-all cursor-pointer"
            >
              <Building className="h-6 w-6 text-[#DA251D]" />
              <div>
                <p className="font-medium text-gray-900">Quản lý phòng ban</p>
                <p className="text-xs text-gray-500">Thêm, sửa phòng ban</p>
              </div>
            </a>
          </div>
        </CardContent>
      </Card>

      {/* Footer info */}
      <div className="text-center text-sm text-gray-500 mt-8">
        <p className="flex items-center justify-center gap-2">
          <span className="text-[#DA251D]">★</span>
          <span>UBND Phường Ninh Xá - TP Bắc Ninh - Tỉnh Bắc Ninh</span>
          <span className="text-[#FFCD00]">★</span>
        </p>
      </div>
    </div>
  )
}

