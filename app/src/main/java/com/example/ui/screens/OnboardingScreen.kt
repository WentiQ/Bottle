package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

enum class OnboardingRole {
    FOUNDER,
    TEAM_MEMBER
}

@Composable
fun OnboardingScreen(
    onCreateCompany: suspend (name: String, email: String, companyName: String, role: String) -> Unit,
    onJoinCompany: suspend (companyId: String, name: String, email: String, role: String) -> Result<Unit>,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val authUser = Firebase.auth.currentUser

    var selectedMode by remember { mutableStateOf(OnboardingRole.FOUNDER) }
    var name by remember { mutableStateOf(authUser?.displayName ?: "") }
    var email by remember { mutableStateOf(authUser?.email ?: "") }
    var companyName by remember { mutableStateOf("") }
    var companyIdInput by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("onboarding_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricIndigoSubtle
                ) {
                    Text(
                        text = "ONBOARDING",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        ),
                        color = ElectricIndigo
                    )
                }

                Text(
                    text = "Welcome to Bottle",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Connect with your startup team. Choose whether you are creating a new company or joining an existing team.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Role / Mode Selection Toggle Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Founder Option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedMode = OnboardingRole.FOUNDER
                            errorMessage = null
                        }
                        .testTag("founder_choice_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == OnboardingRole.FOUNDER) ElectricIndigoSubtle else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedMode == OnboardingRole.FOUNDER) ElectricIndigo else Mist),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = if (selectedMode == OnboardingRole.FOUNDER) PureWhite else Steel,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "I'm a Founder",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Create company, generate team ID & QR code",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Team Member Option
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            selectedMode = OnboardingRole.TEAM_MEMBER
                            errorMessage = null
                        }
                        .testTag("teammate_choice_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedMode == OnboardingRole.TEAM_MEMBER) ElectricIndigoSubtle else MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedMode == OnboardingRole.TEAM_MEMBER) ElectricIndigo else Mist),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = if (selectedMode == OnboardingRole.TEAM_MEMBER) PureWhite else Steel,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "I'm a Teammate",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "Join using existing Startup ID from QR",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Error display
            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = UrgencyOverdueRedBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = UrgencyOverdueRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Form Fields
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedMode == OnboardingRole.FOUNDER) {
                        // Founder Form
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Startup / Company Name *") },
                            placeholder = { Text("e.g. Aura Health AI") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_company_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Your Full Name *") },
                            placeholder = { Text("e.g. Alex Rivera") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_email_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Role (Optional)") },
                            placeholder = { Text("e.g. Co-Founder & CEO, CTO") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_role_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        // Team Member Form
                        OutlinedTextField(
                            value = companyIdInput,
                            onValueChange = { companyIdInput = it.uppercase() },
                            label = { Text("Startup / Company ID *") },
                            placeholder = { Text("e.g. COMP-AURA-8942") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_company_id_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Your Full Name *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_name_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email Address *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_email_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = role,
                            onValueChange = { role = it },
                            label = { Text("Role (Optional)") },
                            placeholder = { Text("e.g. Lead Engineer, Product Designer") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_role_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = {
                            errorMessage = null
                            isSubmitting = true
                            coroutineScope.launch {
                                try {
                                    if (selectedMode == OnboardingRole.FOUNDER) {
                                        if (companyName.isBlank() || name.isBlank() || email.isBlank()) {
                                            errorMessage = "Please fill in all required fields."
                                            isSubmitting = false
                                            return@launch
                                        }
                                        onCreateCompany(name.trim(), email.trim(), companyName.trim(), role.trim())
                                    } else {
                                        if (companyIdInput.isBlank() || name.isBlank() || email.isBlank()) {
                                            errorMessage = "Please enter the Company ID, your name, and email."
                                            isSubmitting = false
                                            return@launch
                                        }
                                        val result = onJoinCompany(companyIdInput.trim(), name.trim(), email.trim(), role.trim())
                                        if (result.isFailure) {
                                            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to join team"
                                        }
                                    }
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Setup failed"
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("onboarding_submit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VoidBlack,
                            contentColor = PureWhite
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (selectedMode == OnboardingRole.FOUNDER) "Create Startup & Generate QR" else "Join Team Workspace",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }
            }
        }
    }
}
