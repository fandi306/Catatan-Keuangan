package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import coil.compose.AsyncImage
import com.example.ui.auth.AuthManager
import com.example.ui.components.AccountManagementDialog
import com.example.ui.components.EditTargetDialog
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.GoldTarget
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.NavyHero
import com.example.ui.theme.NavyHeroDark
import com.example.ui.theme.NavySurfaceDark
import com.example.ui.viewmodel.FinanceViewModel
import com.example.util.AppPreferences
import com.example.util.AppThemeMode
import com.example.util.Formatters
import com.example.util.ProfilePhotoHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ProfileScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val user = try { Firebase.auth.currentUser } catch (_: Exception) { null }
    val userProfile by viewModel.userProfile.collectAsState()
    val savingsTarget by viewModel.savingsTarget.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val accountBalances by viewModel.accountBalances.collectAsState()

    var showEditTargetDialog by remember { mutableStateOf(false) }
    var showManageAccountsDialog by remember { mutableStateOf(false) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }
    var showRestoreSeedConfirmationDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmationDialog by remember { mutableStateOf(false) }
    var showSwitchAccountConfirmationDialog by remember { mutableStateOf(false) }
    var showCreatorDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showCustomEmojiDialog by remember { mutableStateOf(false) }

    val appPreferences = remember { AppPreferences.getInstance(context) }
    val themeMode by appPreferences.themeMode.collectAsState()
    val homeSettings by appPreferences.homeSettings.collectAsState()

    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadProfilePhoto(context, uri)
        }
    }

    val displayName = userProfile?.displayName?.ifBlank { null }
        ?: user?.displayName?.ifBlank { null }
        ?: "Fandi Ahmad"
    val email = userProfile?.email?.ifBlank { null }
        ?: user?.email?.ifBlank { null }
        ?: "fandiahmad306@gmail.com"
    val rawPhotoUri = userProfile?.photoUri?.ifBlank { null } ?: user?.photoUrl?.toString()
    val displayPhotoModel = ProfilePhotoHelper.resolveImageModel(rawPhotoUri)
    val presetEmoji = ProfilePhotoHelper.getPresetEmoji(rawPhotoUri)

    val topBarSection: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali ke Beranda",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Profil & Pengaturan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }

    // Profile Card with Photo Sync, Preset Avatars, & Gallery Picker
    val profileCardSection: @Composable () -> Unit = {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(76.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) NavySurfaceDark else NavyHero,
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, GoldTarget, CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (presetEmoji != null) {
                                    Text(
                                        text = presetEmoji,
                                        fontSize = 38.sp
                                    )
                                } else if (displayPhotoModel != null) {
                                    AsyncImage(
                                        model = displayPhotoModel,
                                        contentDescription = "Foto Profil",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = displayName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 30.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("btn_edit_profile_name")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Nama Profil",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Photo Actions: Galeri & Hapus (hapus duplikasi tombol Emot HP)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(99.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_change_photo")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Galeri", style = MaterialTheme.typography.labelSmall)
                            }

                            if (rawPhotoUri != null && rawPhotoUri.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { viewModel.removeProfilePhoto() },
                                    shape = RoundedCornerShape(99.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("btn_remove_photo")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hapus", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar Options: Quick Preset & Single Emoticon HP Button (tanpa tombol duplikat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Avatar & Emotikon Profil:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showCustomEmojiDialog = true },
                        shape = RoundedCornerShape(99.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("btn_custom_emoji_avatar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SentimentSatisfiedAlt,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pilih Emot HP", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (preset in ProfilePhotoHelper.PRESET_AVATARS) {
                        val isSelected = rawPhotoUri == preset.id
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) GoldTarget.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) GoldTarget else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.selectPresetAvatar(preset.id)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = preset.emoji,
                                    fontSize = 20.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Ruang Khusus Bio & Catatan Keuangan Pribadi
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📝", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Bio & Catatan Keuangan",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            TextButton(
                                onClick = { showEditProfileDialog = true },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (userProfile?.bio.isNullOrBlank()) "+ Tambah Bio" else "Ubah Bio",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (!userProfile?.bio.isNullOrBlank()) {
                                userProfile?.bio ?: ""
                            } else {
                                "Target tabungan: bebas finansial | Tuliskan impian finansial atau moto hemat Anda di sini."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!userProfile?.bio.isNullOrBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = IncomeGreen.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cloud Sync Aktif",
                                color = IncomeGreen,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = GoldTarget.copy(alpha = 0.15f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isDark) GoldTarget else NavyHero,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Akun Google Aman",
                                color = if (isDark) MaterialTheme.colorScheme.onSurface else NavyHero,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Developer / Creator Bio Card (Click to Open Full Details)
    val creatorCardSection: @Composable () -> Unit = {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCreatorDialog = true }
                .testTag("card_about_creator")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = (if (isDark) GoldTarget else NavyHero).copy(alpha = 0.12f),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                tint = if (isDark) GoldTarget else NavyHero,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tentang Pembuat Aplikasi",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "Official",
                                tint = IncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Versi 1.1.0.0 Beta tester version · Info pengembang",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { showCreatorDialog = true },
                    shape = RoundedCornerShape(99.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) GoldTarget else NavyHero,
                        contentColor = if (isDark) NavyHeroDark else Color.White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Buka", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Device Sync Explanation
    val syncExplanationSection: @Composable () -> Unit = {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sinkronisasi Antar Perangkat",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Data transaksi, aset, dan akun Anda tersimpan aman di database cloud (Firebase Firestore). Jika Anda membuka aplikasi ini di perangkat Android lain dengan login menggunakan akun Google Anda yang sama ($email), data akan otomatis tersinkron secara real-time tanpa perlu ekspor/impor manual.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }

    // Theme & Preferences Section
    val themeSection: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Tema Tampilan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = (if (isDark) GoldTarget else NavyHero).copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = null,
                                    tint = if (isDark) GoldTarget else NavyHero,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pilihan Tema",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pilih tema gelap, standar terang, atau ikuti HP",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.SYSTEM,
                        onClick = { appPreferences.setThemeMode(AppThemeMode.SYSTEM) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("Ikuti HP", fontSize = 11.sp)
                    }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.LIGHT,
                        onClick = { appPreferences.setThemeMode(AppThemeMode.LIGHT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("Standar", fontSize = 11.sp)
                    }
                    SegmentedButton(
                        selected = themeMode == AppThemeMode.DARK,
                        onClick = { appPreferences.setThemeMode(AppThemeMode.DARK) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("Dark Mode", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

    // Kustomisasi Tampilan Beranda (Pengaturan Clean Mode & On/Off Komponen)
    val homeCustomizationSection: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Kustomisasi Tampilan Beranda",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth().testTag("home_customization_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = GoldTarget.copy(alpha = 0.15f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = GoldTarget,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Atur Elemen Beranda",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sembunyikan komponen untuk tampilan super bersih (clean mode)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Cepat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { appPreferences.resetHomeSettingsToDefault() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Semua Aktif", fontSize = 11.sp, maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { appPreferences.setSuperCleanMode() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Super Bersih", fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Kutipan & Tips Finansial
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Kutipan & Tips Finansial",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kutipan bijak keuangan & tips otomatis dari server online",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = homeSettings.showQuotes,
                            onCheckedChange = { appPreferences.setShowQuotes(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isDark) GoldTarget else NavyHero
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Dompet & Saldo Akun
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Dompet & Akun Saya",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Pilihan kartu dompet, rekening bank & e-wallet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = homeSettings.showAccountChips,
                            onCheckedChange = { appPreferences.setShowAccountChips(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isDark) GoldTarget else NavyHero
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Peringatan & Evaluasi Keuangan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Peringatan Finansial",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Evaluasi defisit & formula anggaran 40-30-20-10 OJK",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = homeSettings.showFinancialWarnings,
                            onCheckedChange = { appPreferences.setShowFinancialWarnings(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isDark) GoldTarget else NavyHero
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Daftar Transaksi Terkini
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Daftar Transaksi Terkini",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Riwayat 5 transaksi terakhir langsung di beranda",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = homeSettings.showRecentTransactions,
                            onCheckedChange = { appPreferences.setShowRecentTransactions(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isDark) GoldTarget else NavyHero
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Grafik Lengkung Target Aset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Grafik Lengkung Target Aset",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Kurva sparkline pencapaian di kartu Total Aset",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = homeSettings.showSparklineCurve,
                            onCheckedChange = { appPreferences.setShowSparklineCurve(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (isDark) GoldTarget else NavyHero
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Info: Komponen Inti (Total Aset & Ringkasan Bulanan) Selalu Tampil
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Total Aset & Ringkasan Bulanan",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Komponen inti catatan keuangan wajib selalu aktif dan tidak dapat disembunyikan.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Financial Settings & Management
    val financeSettingsSection: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Pengaturan Keuangan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Target Tabungan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Target Tabungan",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = Formatters.formatRupiah(savingsTarget),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = { showEditTargetDialog = true },
                        modifier = Modifier.testTag("profile_edit_target")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ubah")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Kelola Akun
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kelola Akun & Dompet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${accounts.size} akun terdaftar (Liquid Aset & Non-Liquid Aset)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = { showManageAccountsDialog = true },
                        modifier = Modifier.testTag("profile_manage_accounts")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buka")
                    }
                }
            }
        }
    }
}

    // Data Reset Section
    val resetRestoreSection: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Menu Reset & Pemulihan Data",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Reset Semua Transaksi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reset Semua Transaksi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                        Text(
                            text = "Hapus seluruh catatan transaksi untuk memulai dari nol.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { showResetConfirmationDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_reset_all_data")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulihkan Data Contoh Konsep
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pulihkan Data Konsep Awal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Muat ulang data contoh (Gaji, Kos, Aset, dll) sesuai konsep UI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedButton(
                        onClick = { showRestoreSeedConfirmationDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_restore_seed_data")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pulihkan")
                    }
                }
            }
        }
    }
}

    // Account Actions: Ganti Akun & Keluar
    val accountActionsSection: @Composable () -> Unit = {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { showSwitchAccountConfirmationDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("switch_account_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SwitchAccount,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Ganti Akun",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = { showLogoutConfirmationDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1.2f)
                    .height(52.dp)
                    .testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Keluar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Responsive layout: 2-column on tablet / wide screen, single column on phone
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val isTablet = maxWidth >= 720.dp

        if (isTablet) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1040.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .testTag("profile_screen")
            ) {
                topBarSection()
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        profileCardSection()
                        creatorCardSection()
                        syncExplanationSection()
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        homeCustomizationSection()
                        themeSection()
                        financeSettingsSection()
                        resetRestoreSection()
                        accountActionsSection()
                    }
                }
                Spacer(modifier = Modifier.height(70.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .testTag("profile_screen")
            ) {
                topBarSection()
                Spacer(modifier = Modifier.height(16.dp))
                profileCardSection()
                Spacer(modifier = Modifier.height(16.dp))
                creatorCardSection()
                Spacer(modifier = Modifier.height(16.dp))
                syncExplanationSection()
                Spacer(modifier = Modifier.height(16.dp))
                homeCustomizationSection()
                Spacer(modifier = Modifier.height(16.dp))
                themeSection()
                Spacer(modifier = Modifier.height(16.dp))
                financeSettingsSection()
                Spacer(modifier = Modifier.height(16.dp))
                resetRestoreSection()
                Spacer(modifier = Modifier.height(24.dp))
                accountActionsSection()
                Spacer(modifier = Modifier.height(70.dp))
            }
        }
    }

    // Confirmation Dialog for Logout
    if (showLogoutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmationDialog = false },
            title = {
                Text("Konfirmasi Keluar Akun", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Apakah Anda yakin ingin keluar dari akun ini? Seluruh data catatan keuangan Anda tetap tersimpan aman di cloud dan akan otomatis sinkron saat Anda masuk kembali.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmationDialog = false
                        AuthManager.signOut(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = onSignedOut,
                            scope = coroutineScope
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Ya, Keluar", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirmationDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation Dialog for Switch Account
    if (showSwitchAccountConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchAccountConfirmationDialog = false },
            title = {
                Text("Konfirmasi Ganti Akun", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Apakah Anda ingin keluar untuk berganti ke akun Google yang lain?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSwitchAccountConfirmationDialog = false
                        AuthManager.signOut(
                            context = context,
                            credentialManager = credentialManager,
                            onSignOutComplete = onSignedOut,
                            scope = coroutineScope
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSystemInDarkTheme()) GoldTarget else NavyHero,
                        contentColor = if (isSystemInDarkTheme()) NavyHeroDark else Color.White
                    )
                ) {
                    Text("Ganti Akun", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSwitchAccountConfirmationDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation Dialog for Reset All Data
    if (showResetConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            title = {
                Text("Konfirmasi Reset Transaksi", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Apakah Anda yakin ingin menghapus seluruh data transaksi? Tindakan ini akan mengosongkan seluruh riwayat keuangan Anda.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Ya, Hapus Semua", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetConfirmationDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Confirmation Dialog for Restore Seed Data
    if (showRestoreSeedConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreSeedConfirmationDialog = false },
            title = {
                Text("Pulihkan Data Konsep Awal", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Apakah Anda ingin mengganti data saat ini dengan data sampel konsep awal (Akun Tunai, BRI, Line Bank, aset & transaksi Oktober)?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreSeedData()
                        showRestoreSeedConfirmationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyHero)
                ) {
                    Text("Ya, Pulihkan", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreSeedConfirmationDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showCreatorDialog) {
        AlertDialog(
            onDismissRequest = { showCreatorDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = if (isDark) GoldTarget else NavyHero,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tentang Pembuat Aplikasi",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Nama Pengembang: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Fandi Ahmad",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) GoldTarget else NavyHero
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Versi Aplikasi: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = (if (isDark) GoldTarget else NavyHero).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "1.1.0.0 Beta tester version",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) GoldTarget else NavyHero,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Aplikasi Catatan Keuangan dibuat khusus oleh Fandi Ahmad untuk memberikan solusi pencatatan finansial pribadi yang bersih, cepat, aman, dan tersinkronisasi antar perangkat secara real-time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Kontak & Portofolio Pengembang",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ContactRow(
                        icon = Icons.Default.Email,
                        label = "Email Resmi",
                        value = "fandiahmad306@gmail.com",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:fandiahmad306@gmail.com")
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ContactRow(
                        icon = Icons.Default.Phone,
                        label = "WhatsApp / Kontak",
                        value = "+62 898-3691-505",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/628983691505"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ContactRow(
                        icon = Icons.Default.Link,
                        label = "Instagram",
                        value = "@fandi_.ahmd",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/fandi_.ahmd"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    ContactRow(
                        icon = Icons.Default.Link,
                        label = "GitHub & Repository",
                        value = "github.com/fandi306",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/fandi306"))
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Versi 1.1.0.0 Beta tester version · Kotlin Compose · Firebase Firestore",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCreatorDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) GoldTarget else NavyHero,
                        contentColor = if (isDark) NavyHeroDark else Color.White
                    )
                ) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showEditTargetDialog) {
        EditTargetDialog(
            currentTarget = savingsTarget,
            onDismiss = { showEditTargetDialog = false },
            onSave = { newTarget ->
                viewModel.updateSavingsTarget(newTarget)
                showEditTargetDialog = false
            }
        )
    }

    if (showManageAccountsDialog) {
        AccountManagementDialog(
            accounts = accounts,
            balances = accountBalances,
            onDismiss = { showManageAccountsDialog = false },
            onAddAccount = { name, type ->
                viewModel.addAccount(name, type)
            },
            onDeleteAccount = { accId ->
                viewModel.deleteAccount(accId)
            }
        )
    }

    // Dialog Edit Profil (Ganti Nama & Bio)
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(displayName) }
        var tempBio by remember { mutableStateOf(userProfile?.bio ?: "") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditProfileDialog = false },
            title = {
                Text("Edit Profil Pengguna", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ubah nama tampilan akun Anda. Nama ini akan tersimpan dan tersinkron ke cloud.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Nama Lengkap") },
                        placeholder = { Text("Masukkan nama Anda") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_display_name")
                    )

                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Catatan / Bio Singkat (Opsional)") },
                        placeholder = { Text("Contoh: Menuju merdeka finansial") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_edit_profile_bio")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            isSaving = true
                            viewModel.updateProfileBio(tempName.trim(), tempBio.trim())
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) GoldTarget else NavyHero,
                        contentColor = if (isDark) NavyHeroDark else Color.White
                    ),
                    modifier = Modifier.testTag("btn_save_profile_name")
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditProfileDialog = false },
                    enabled = !isSaving
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Input Custom Emoji Avatar dari Keyboard HP
    if (showCustomEmojiDialog) {
        var customEmojiInput by remember { mutableStateOf("") }
        val quickEmojiPicks = listOf("😀", "😎", "🥳", "🤠", "🤑", "🦊", "🦁", "🐼", "🚀", "👑", "💎", "⭐", "🥑", "☕", "⚽")

        AlertDialog(
            onDismissRequest = { showCustomEmojiDialog = false },
            title = {
                Text("Pilih atau Ketik Emoticon HP", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Anda bisa mengetik emotikon apa saja langsung dari keyboard HP Anda, atau pilih salah satu emot populer di bawah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customEmojiInput,
                        onValueChange = { customEmojiInput = it },
                        label = { Text("Ketik Emoticon dari Keyboard HP") },
                        placeholder = { Text("Contoh: 🦊 atau 🦄 atau 💰") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_emoji_avatar")
                    )

                    Text(
                        text = "Rekomendasi Cepat:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (emoji in quickEmojiPicks.take(7)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { customEmojiInput = emoji }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (emoji in quickEmojiPicks.drop(7).take(7)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { customEmojiInput = emoji }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = customEmojiInput.trim()
                        if (cleaned.isNotEmpty()) {
                            viewModel.updateProfilePhoto("emoji:$cleaned")
                            showCustomEmojiDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) GoldTarget else NavyHero,
                        contentColor = if (isDark) NavyHeroDark else Color.White
                    ),
                    modifier = Modifier.testTag("btn_save_custom_emoji")
                ) {
                    Text("Gunakan Emot", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCustomEmojiDialog = false }
                ) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDark) GoldTarget else NavyHero,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
