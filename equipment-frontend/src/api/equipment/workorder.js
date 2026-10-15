import request from '@/utils/request'

export function listWorkOrders(params) {
  return request({ url: '/equipment/workorder/list', method: 'get', params })
}

export function getWorkOrder(id) {
  return request({ url: `/equipment/workorder/${id}`, method: 'get' })
}

export function getWorkOrderRecords(id) {
  return request({ url: `/equipment/workorder/${id}/records`, method: 'get' })
}

export function listMaintainers(params) {
  return request({ url: '/equipment/workorder/maintainers', method: 'get', params })
}

export function createWorkOrder(data) {
  return request({ url: '/equipment/workorder', method: 'post', data })
}

export function assignWorkOrder(id, data) {
  return request({ url: `/equipment/workorder/${id}/assign`, method: 'post', data })
}

export function cancelWorkOrder(id, data) {
  return request({ url: `/equipment/workorder/${id}/cancel`, method: 'post', data })
}

export function addProcessRecord(id, data) {
  return request({ url: `/equipment/workorder/${id}/records`, method: 'post', data })
}

export function submitWorkOrderReview(id, data) {
  return request({ url: `/equipment/workorder/${id}/submit-review`, method: 'post', data })
}

export function reviewWorkOrder(id, data) {
  return request({ url: `/equipment/workorder/${id}/review`, method: 'post', data })
}
