import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { http } from './client'
import type { UserProfile, UserProfileRequest, UserSetting, UserSettingRequest } from './types'

const PROFILE = '/api/user/profile'
const SETTINGS = '/api/user/settings'

export function useProfile() {
  return useQuery({ queryKey: [PROFILE], queryFn: () => http.get<UserProfile>(PROFILE) })
}

export function useUpdateProfile() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: UserProfileRequest) => http.put<UserProfile>(PROFILE, body),
    onSuccess: (data) => qc.setQueryData([PROFILE], data),
  })
}

export function useSettings() {
  return useQuery({ queryKey: [SETTINGS], queryFn: () => http.get<UserSetting>(SETTINGS) })
}

export function useUpdateSettings() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: UserSettingRequest) => http.put<UserSetting>(SETTINGS, body),
    onSuccess: (data) => qc.setQueryData([SETTINGS], data),
  })
}
