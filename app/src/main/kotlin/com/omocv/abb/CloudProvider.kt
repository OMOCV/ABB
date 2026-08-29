package com.omocv.abb

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Enum representing all supported cloud storage providers
 * 支持的云存储服务提供商枚举
 */
enum class CloudProvider(
    @StringRes val displayNameRes: Int,
    @DrawableRes val iconRes: Int,
    val requiresAuth: Boolean,
    val authType: AuthType
) {
    WEBDAV(
        displayNameRes = R.string.cloud_provider_webdav,
        iconRes = R.drawable.ic_cloud_webdav,
        requiresAuth = true,
        authType = AuthType.USERNAME_PASSWORD
    ),

    GOOGLE_DRIVE(
        displayNameRes = R.string.cloud_provider_google_drive,
        iconRes = R.drawable.ic_cloud_google_drive,
        requiresAuth = true,
        authType = AuthType.OAUTH
    ),

    ONEDRIVE(
        displayNameRes = R.string.cloud_provider_onedrive,
        iconRes = R.drawable.ic_cloud_onedrive,
        requiresAuth = true,
        authType = AuthType.OAUTH
    ),

    ALIYUN_DRIVE(
        displayNameRes = R.string.cloud_provider_aliyun,
        iconRes = R.drawable.ic_cloud_aliyun,
        requiresAuth = true,
        authType = AuthType.TOKEN
    ),

    BAIDU_CLOUD(
        displayNameRes = R.string.cloud_provider_baidu,
        iconRes = R.drawable.ic_cloud_baidu,
        requiresAuth = true,
        authType = AuthType.OAUTH
    ),

    E_CLOUD(
        displayNameRes = R.string.cloud_provider_ecloud,
        iconRes = R.drawable.ic_cloud_ecloud,
        requiresAuth = true,
        authType = AuthType.USERNAME_PASSWORD
    ),

    LANZOU_CLOUD(
        displayNameRes = R.string.cloud_provider_lanzou,
        iconRes = R.drawable.ic_cloud_lanzou,
        requiresAuth = true,
        authType = AuthType.USERNAME_PASSWORD
    ),

    CLOUD_123(
        displayNameRes = R.string.cloud_provider_123,
        iconRes = R.drawable.ic_cloud_123,
        requiresAuth = true,
        authType = AuthType.USERNAME_PASSWORD
    ),

    QUARK_CLOUD(
        displayNameRes = R.string.cloud_provider_quark,
        iconRes = R.drawable.ic_cloud_quark,
        requiresAuth = true,
        authType = AuthType.TOKEN
    ),

    CLOUD_115(
        displayNameRes = R.string.cloud_provider_115,
        iconRes = R.drawable.ic_cloud_115,
        requiresAuth = true,
        authType = AuthType.USERNAME_PASSWORD
    ),

    UC_CLOUD(
        displayNameRes = R.string.cloud_provider_uc,
        iconRes = R.drawable.ic_cloud_uc,
        requiresAuth = true,
        authType = AuthType.USERNAME_PASSWORD
    );

    /**
     * Authentication type required by the provider
     */
    enum class AuthType {
        USERNAME_PASSWORD,  // 用户名密码认证
        OAUTH,             // OAuth 2.0 认证
        TOKEN,             // Token 认证
        API_KEY            // API Key 认证
    }
}
