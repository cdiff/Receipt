package com.pasic.receipt.ui.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings

enum class CameraPermissionStatus {
    GRANTED,
    NEEDS_RATIONALE,
    PERMANENTLY_DENIED,
    NOT_REQUESTED
}

@Composable
fun CameraPermissionHandler(
    onGalleryClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var permissionStatus by remember { mutableStateOf(checkPermission(context)) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            permissionStatus = CameraPermissionStatus.GRANTED
        } else {
            // Check if permanently denied
            permissionStatus = CameraPermissionStatus.PERMANENTLY_DENIED
            showSettingsDialog = true
        }
    }

    LaunchedEffect(Unit) {
        when (permissionStatus) {
            CameraPermissionStatus.GRANTED -> { /* Ready */ }
            CameraPermissionStatus.NOT_REQUESTED, CameraPermissionStatus.NEEDS_RATIONALE -> {
                showRationaleDialog = true
            }
            CameraPermissionStatus.PERMANENTLY_DENIED -> {
                showSettingsDialog = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (permissionStatus == CameraPermissionStatus.GRANTED) {
            content()
        } else {
            // Fallback UI when camera permission is unavailable
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Lucide.Camera,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "카메라 권한이 필요합니다",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "영수증을 직접 촬영하여 바로 인식하려면\n카메라 접근 권한을 허용해 주세요.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onGalleryClick,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Lucide.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "갤러리에서 선택",
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }

                        Button(
                            onClick = {
                                if (permissionStatus == CameraPermissionStatus.PERMANENTLY_DENIED) {
                                    showSettingsDialog = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Lucide.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (permissionStatus == CameraPermissionStatus.PERMANENTLY_DENIED) "설정으로 이동" else "권한 허용하기",
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Rationale Dialog
        if (showRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showRationaleDialog = false },
                title = {
                    Text("카메라 권한 안내", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Text("영수증 텍스트를 실시간으로 촬영하고 인식하기 위해 카메라 권한이 필요합니다.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRationaleDialog = false
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("권한 요청")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRationaleDialog = false }) {
                        Text("취소", color = Color(0xFF64748B))
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        // Permanently Denied Settings Dialog
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = {
                    Text("카메라 권한 설정", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Text("카메라 권한이 거부되어 있습니다. 앱 설정 화면으로 이동하여 권한을 허용해 주시거나, 갤러리 사진을 불러올 수 있습니다.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSettingsDialog = false
                            openAppSettings(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("설정 앱으로 이동")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showSettingsDialog = false
                            onGalleryClick()
                        }
                    ) {
                        Text("갤러리에서 선택", color = Color(0xFF2563EB))
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

private fun checkPermission(context: Context): CameraPermissionStatus {
    return if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
        CameraPermissionStatus.GRANTED
    } else {
        CameraPermissionStatus.NOT_REQUESTED
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
