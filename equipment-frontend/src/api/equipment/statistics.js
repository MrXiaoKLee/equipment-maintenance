import request from '@/utils/request'

export function getStatisticsOverview(params) {
  return request({ url: '/equipment/statistics/overview', method: 'get', params })
}
