package com.example.cerime

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cerime.ui.theme.CerimeTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Block screenshots and screen recording (FLAG_SECURE)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()
        setContent {
            CerimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContainer()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: User View, 1: Admin Panel

    var protocolNo by remember { mutableStateOf("YHQ-${Random.nextInt(10000, 99999)}") }
    var plateNo by remember { mutableStateOf("99-XX-999") }
    var article by remember { mutableStateOf("Maddə 125.1 - Dayanma və durma qaydalarının pozulması") }
    var location by remember { mutableStateOf("Nizami küçəsi 45, Bakı şəhəri") }
    var points by remember { mutableStateOf("2") }
    var amount by remember { mutableStateOf("80.00") }
    var discountAmount by remember { mutableStateOf("20.00") }
    var dateTimeText by remember { mutableStateOf(SimpleDateFormat("dd.MM.yyyy / HH:mm", Locale.getDefault()).format(Date())) }
    var carImageUri by remember { mutableStateOf<Uri?>(null) }
    
    var timeLeftSeconds by remember { mutableStateOf(600) }
    var isPaid by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = timeLeftSeconds, key2 = isPaid) {
        if (!isPaid && timeLeftSeconds > 0) {
            delay(1000L)
            timeLeftSeconds -= 1
        }
    }

    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("DYP Elektron Cərimə Sistemi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = if (selectedTab == 0) "İstifadəçi Bildirişi" else "Admin Panel - İdarəetmə",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { selectedTab = if (selectedTab == 0) 1 else 0 }) {
                        Text(
                            text = if (selectedTab == 0) "⚙️ Admin" else "👁️ Görünüş",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (selectedTab == 0) {
                UserFineScreen(
                    protocolNo = protocolNo,
                    plateNo = plateNo,
                    article = article,
                    location = location,
                    points = points,
                    amount = amount,
                    discountAmount = discountAmount,
                    dateTimeText = dateTimeText,
                    carImageUri = carImageUri,
                    timeFormatted = timeFormatted,
                    isPaid = isPaid,
                    onPayClicked = {
                        isPaid = true
                        showSuccessDialog = true
                    }
                )
            } else {
                AdminPanelScreen(
                    plateNo = plateNo,
                    onPlateChanged = { plateNo = it },
                    location = location,
                    onLocationChanged = { location = it },
                    carImageUri = carImageUri,
                    onImageSelected = { carImageUri = it },
                    onSave = {
                        protocolNo = "YHQ-${Random.nextInt(10000, 99999)}"
                        dateTimeText = SimpleDateFormat("dd.MM.yyyy / HH:mm", Locale.getDefault()).format(Date())
                        selectedTab = 0
                    }
                )
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Ödəniş Təsdiq Olundu") },
            text = { Text("Cəriməniz $discountAmount AZN olaraq uğurla ödənildi. $points cərimə balı tətbiq edildi.") },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("Bağla")
                }
            }
        )
    }
}

@Composable
fun UserFineScreen(
    protocolNo: String,
    plateNo: String,
    article: String,
    location: String,
    points: String,
    amount: String,
    discountAmount: String,
    dateTimeText: String,
    carImageUri: Uri?,
    timeFormatted: String,
    isPaid: Boolean,
    onPayClicked: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(text = "⚠️", fontSize = 32.sp)
                Column {
                    Text(
                        text = "DAYANMA VƏ DURMA QAYDASI POZULDU",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 14.sp
                    )
                    Text(
                        text = article,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Photo Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Pozuntu Qeydiyyatı (Foto)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("Dəlil", color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (carImageUri != null) {
                        val bitmap = remember(carImageUri) {
                            try {
                                val source = ImageDecoder.createSource(context.contentResolver, carImageUri)
                                ImageDecoder.decodeBitmap(source)
                            } catch (_: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Maşının Şəkli",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Şəkil yüklənmədi", color = Color.Gray)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = "🚗📸", fontSize = 44.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Maşının Şəkli Əlavə Edilməyib\n(Admin paneldən şəkil seçin)",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Cərimə Detalları", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                HorizontalDivider()
                DetailRow(label = "Protokol Nömrəsi:", value = protocolNo)
                DetailRow(label = "Qeydiyyat Nişanı:", value = plateNo, isHighlight = true)
                DetailRow(label = "Tarix və Saat:", value = dateTimeText)
                DetailRow(label = "Ünvan:", value = location)
                DetailRow(label = "Cərimə Balı:", value = "$points Bal")
                DetailRow(label = "Standart Cərimə Məbləği:", value = "$amount AZN")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isPaid) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isPaid) {
                    Text(text = "✅", fontSize = 40.sp)
                    Text(text = "Cərimə Uğurla Ödənildi!", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF2E7D32))
                    Text(text = "Endirimli məbləğ ödənildi: $discountAmount AZN. Təşəkkür edirik!", textAlign = TextAlign.Center, fontSize = 14.sp)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "⏱️", fontSize = 20.sp)
                        Text(text = "İndi Ödə - 75% Endirim Fürsəti!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(text = "Qalan Vaxt: $timeFormatted", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }

                    Text(
                        text = "İndi ödəniş etsəniz, $amount AZN əvəzinə cəmi $discountAmount AZN ödəyin!",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onPayClicked,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text(text = "İNDİ ÖDƏ - $discountAmount AZN", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPanelScreen(
    plateNo: String, onPlateChanged: (String) -> Unit,
    location: String, onLocationChanged: (String) -> Unit,
    carImageUri: Uri?, onImageSelected: (Uri?) -> Unit,
    onSave: () -> Unit
) {
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onImageSelected(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "🛠️ Sadələşdirilmiş Admin Paneli", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Protokol nömrəsi, tarix, saat, maddə (125.1), 80 AZN cərimə və 2 bal avtomatik olaraq tətbiq olunur. Nömrəni və ünvanı daxil edib şəkil seçə bilərsiniz.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        OutlinedTextField(
            value = plateNo,
            onValueChange = onPlateChanged,
            label = { Text("Avtomobilin Qeydiyyat Nişanı (Nömrə)") },
            placeholder = { Text("Məs: 10-AA-123") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChanged,
            label = { Text("Pozuntu Ünvanı") },
            placeholder = { Text("Məs: Neftçilər prospekti, Bakı") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = { galleryLauncher.launch("image/*") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(
                text = if (carImageUri != null) "🖼️ Maşın Şəkli Seçildi (Dəyişdir)" else "📸 Qalereyadan Maşın Şəkli Seç",
                fontWeight = FontWeight.Bold
            )
        }

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text(text = "Cəriməni Tətbiq Et və Yarat", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isHighlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainAppContainerPreview() {
    CerimeTheme {
        MainAppContainer()
    }
}
