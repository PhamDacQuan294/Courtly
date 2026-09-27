/** Gọi API xác thực: 2.1.1 đăng ký, 2.1.2 đăng nhập, 2.1.4 đăng xuất. */

import { clearToken, request, saveToken } from './client'

/**
 * Backend trả `fullName`, giao diện hiện dùng `user.name`.
 * Quy đổi ở đây để component không phải biết hình dạng response.
 */
export function toUiUser(user) {
  if (!user) return null
  return {
    id: user.id,
    name: user.fullName,
    email: user.email ?? '',
    phone: user.phone ?? '',
    avatarUrl: user.avatarUrl ?? null,
    status: user.status,
    roles: user.roles ?? [],
  }
}

/** @returns {Promise<object>} người dùng đã đăng nhập */
export async function register({ fullName, email, phone, password, confirmPassword }) {
  const data = await request('/api/v1/auth/register', {
    method: 'POST',
    auth: false,
    body: { fullName, email, phone, password, confirmPassword },
  })
  saveToken(data.accessToken)
  return toUiUser(data.user)
}

export async function login({ emailOrPhone, password }) {
  const data = await request('/api/v1/auth/login', {
    method: 'POST',
    auth: false,
    body: { emailOrPhone, password },
  })
  saveToken(data.accessToken)
  return toUiUser(data.user)
}

/**
 * Access token không lưu ở server nên việc thu hồi thực tế là xoá token ở client.
 * Vẫn gọi endpoint để sau này thêm thu hồi mà không phải sửa frontend.
 */
export async function logout() {
  try {
    await request('/api/v1/auth/logout', { method: 'POST' })
  } catch {
    // Đăng xuất phải luôn thành công với người dùng, kể cả khi mạng lỗi.
  } finally {
    clearToken()
  }
}

export async function fetchCurrentUser() {
  const data = await request('/api/v1/auth/me')
  return toUiUser(data)
}

// --- 2.1.5 Quên & đặt lại mật khẩu ------------------------------------------

/**
 * Bước 1 — xin gửi mã xác minh.
 *
 * Backend luôn trả về cùng một kết quả dù email có tồn tại hay không, nên giao diện
 * cũng phải hiển thị như nhau; đừng suy ra "email này chưa đăng ký" từ phản hồi.
 *
 * @returns {Promise<{expiresInSeconds: number, resendAfterSeconds: number}>}
 */
export function requestPasswordReset({ channel = 'email', destination }) {
  return request('/api/v1/auth/password-reset', {
    method: 'POST',
    auth: false,
    body: { channel, destination },
  })
}

/**
 * Bước 2 — đổi mã 6 chữ số lấy token dùng một lần.
 *
 * @returns {Promise<{resetToken: string, expiresInSeconds: number}>}
 */
export function verifyPasswordResetCode({ destination, code }) {
  return request('/api/v1/auth/password-reset/verify', {
    method: 'POST',
    auth: false,
    body: { destination, code },
  })
}

/** Bước 3 — đặt mật khẩu mới. Không tự đăng nhập: người dùng đăng nhập lại. */
export function confirmPasswordReset({ resetToken, password, confirmPassword }) {
  return request('/api/v1/auth/password-reset/confirm', {
    method: 'POST',
    auth: false,
    body: { resetToken, password, confirmPassword },
  })
}
