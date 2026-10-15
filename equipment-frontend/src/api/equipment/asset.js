import request from '@/utils/request'

export function listEquipment(params) {
  return request({ url: '/equipment/asset/list', method: 'get', params })
}

export function listEquipmentOptions(params) {
  return request({ url: '/equipment/asset/options', method: 'get', params })
}

export function getEquipment(id) {
  return request({ url: `/equipment/asset/${id}`, method: 'get' })
}

export function addEquipment(data) {
  return request({ url: '/equipment/asset', method: 'post', data })
}

export function updateEquipment(id, data) {
  return request({ url: `/equipment/asset/${id}`, method: 'put', data })
}

export function changeEquipmentStatus(id, data) {
  return request({ url: `/equipment/asset/${id}/status`, method: 'put', data })
}

export function listEquipmentHistory(id, params) {
  return request({ url: `/equipment/asset/${id}/work-orders`, method: 'get', params })
}
