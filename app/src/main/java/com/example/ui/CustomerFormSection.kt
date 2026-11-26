package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DocumentType
import com.example.ui.theme.RaagBorder
import com.example.ui.theme.RaagCrimson
import com.example.ui.theme.RaagDarkRed
import com.example.ui.theme.RaagGold
import com.example.ui.theme.RaagGoldSoft
import com.example.ui.theme.RaagMuted
import com.example.ui.theme.RaagWhite
import com.example.viewmodel.CustomerFormState

@Composable
fun CustomerFormSection(
    formState: CustomerFormState,
    busy: Boolean,
    focusPhotosOnly: Boolean,
    onUpdateForm: ((CustomerFormState) -> CustomerFormState) -> Unit,
    onPickPhotoGallery: (DocumentType) -> Unit,
    onCapturePhotoCamera: (DocumentType) -> Unit,
    onRemovePhoto: (DocumentType) -> Unit,
    onSubmitApplication: (Boolean) -> Unit,
    onResetForm: () -> Unit
) {
    var showPdfHelp by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (formState.editingApplicationCode != null) {
            Surface(
                color = RaagGoldSoft,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, RaagGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Editing Application: ${formState.editingApplicationCode}",
                        style = MaterialTheme.typography.labelLarge,
                        color = RaagDarkRed
                    )
                    OutlinedButton(onClick = onResetForm) {
                        Text("New Form", color = RaagDarkRed)
                    }
                }
            }
        }

        // Panel 1: Customer Details (ग्राहक की जानकारी)
        if (!focusPhotosOnly) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RaagWhite),
                border = BorderStroke(1.dp, RaagBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = RaagCrimson)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ग्राहक की जानकारी (Customer Details)",
                            style = MaterialTheme.typography.titleMedium,
                            color = RaagDarkRed
                        )
                    }

                    OutlinedTextField(
                        value = formState.fullName,
                        onValueChange = { v -> onUpdateForm { it.copy(fullName = v) } },
                        label = { Text("पूरा नाम * (Full Name)") },
                        placeholder = { Text("ग्राहक का पूरा नाम") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_full_name")
                    )

                    OutlinedTextField(
                        value = formState.mobile,
                        onValueChange = { v -> onUpdateForm { it.copy(mobile = v.filter { ch -> ch.isDigit() || ch == '+' }.take(15)) } },
                        label = { Text("मोबाइल नंबर * (Mobile Number)") },
                        placeholder = { Text("10-digit mobile number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_mobile")
                    )

                    OutlinedTextField(
                        value = formState.address,
                        onValueChange = { v -> onUpdateForm { it.copy(address = v) } },
                        label = { Text("पूरा पता * (Complete Address)") },
                        placeholder = { Text("मकान, गली, शहर, जिला, पिन कोड") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_address")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.city,
                            onValueChange = { v -> onUpdateForm { it.copy(city = v) } },
                            label = { Text("शहर (City)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = formState.pinCode,
                            onValueChange = { v -> onUpdateForm { it.copy(pinCode = v.filter { it.isDigit() }.take(6)) } },
                            label = { Text("पिन कोड (PIN)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = formState.email,
                        onValueChange = { v -> onUpdateForm { it.copy(email = v) } },
                        label = { Text("ईमेल (Email Address)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Panel 2: Vehicle Details (EV / Scooty Details)
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = RaagWhite),
                border = BorderStroke(1.dp, RaagBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ElectricScooter, contentDescription = null, tint = RaagCrimson)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "स्कूटी / EV की जानकारी (Vehicle Details)",
                            style = MaterialTheme.typography.titleMedium,
                            color = RaagDarkRed
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.vehicleBrand,
                            onValueChange = { v -> onUpdateForm { it.copy(vehicleBrand = v) } },
                            label = { Text("ब्रांड (Brand)") },
                            placeholder = { Text("e.g. Ola, TVS, Hero") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = formState.vehicleModel,
                            onValueChange = { v -> onUpdateForm { it.copy(vehicleModel = v) } },
                            label = { Text("मॉडल (Model)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = formState.batteryNumber,
                        onValueChange = { v -> onUpdateForm { it.copy(batteryNumber = v) } },
                        label = { Text("बैटरी नंबर (Battery Number)") },
                        placeholder = { Text("बैटरी नंबर लिखें") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_battery_number")
                    )

                    OutlinedTextField(
                        value = formState.registrationNumber,
                        onValueChange = { v -> onUpdateForm { it.copy(registrationNumber = v) } },
                        label = { Text("स्कूटी का 26 नंबर / Registration No.") },
                        placeholder = { Text("स्कूटी का 26 नंबर लिखें") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_vehicle_number")
                    )

                    OutlinedTextField(
                        value = formState.vehicleNotes,
                        onValueChange = { v -> onUpdateForm { it.copy(vehicleNotes = v) } },
                        label = { Text("अतिरिक्त जानकारी (Additional Notes)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Panel 3: Photo Upload (फोटो अपलोड - exact 7 slots from HTML)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RaagWhite),
            border = BorderStroke(1.dp, RaagBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = RaagCrimson)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "फोटो अपलोड (Documents & EV Photos)",
                        style = MaterialTheme.typography.titleMedium,
                        color = RaagDarkRed
                    )
                }
                Text(
                    text = "फोटो चुनने के बाद सुरक्षित रूप से क्लाउड में सेव और A4 PDF में शामिल होंगी। बेहतर परिणाम के लिए साफ फोटो लें।",
                    style = MaterialTheme.typography.bodySmall,
                    color = RaagMuted
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val twoColumns = maxWidth >= 560.dp
                    val types = DocumentType.applicationPhotoTypes
                    if (twoColumns) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            types.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    pair.forEach { docType ->
                                        PhotoSlotCard(
                                            docType = docType,
                                            uploaded = formState.uploadedDocuments[docType],
                                            hasExistingCloudDoc = formState.existingDocumentIds.containsKey(docType.name),
                                            onGalleryClick = { onPickPhotoGallery(docType) },
                                            onCameraClick = { onCapturePhotoCamera(docType) },
                                            onRemoveClick = { onRemovePhoto(docType) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            types.forEach { docType ->
                                PhotoSlotCard(
                                    docType = docType,
                                    uploaded = formState.uploadedDocuments[docType],
                                    hasExistingCloudDoc = formState.existingDocumentIds.containsKey(docType.name),
                                    onGalleryClick = { onPickPhotoGallery(docType) },
                                    onCameraClick = { onCapturePhotoCamera(docType) },
                                    onRemoveClick = { onRemovePhoto(docType) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Panel 4: Save & Submit Record (रिकॉर्ड सेव करें - exact HTML actions)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = RaagWhite),
            border = BorderStroke(1.dp, RaagBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "रिकॉर्ड सेव करें (Review & Submit)",
                    style = MaterialTheme.typography.titleMedium,
                    color = RaagDarkRed
                )

                Button(
                    onClick = { onSubmitApplication(false) },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = RaagCrimson, contentColor = RaagWhite),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("form_submit_button")
                ) {
                    if (busy) {
                        CircularProgressIndicator(color = RaagGold, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving to Firebase...")
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ग्राहक सेव करें / Submit Application", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSubmitApplication(true) },
                        enabled = !busy,
                        border = BorderStroke(1.dp, RaagDarkRed),
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("form_draft_button")
                    ) {
                        Text("Save Draft", color = RaagDarkRed)
                    }

                    OutlinedButton(
                        onClick = onResetForm,
                        enabled = !busy,
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("form_reset_button")
                    ) {
                        Text("फॉर्म साफ करें", color = RaagMuted)
                    }
                }

                OutlinedButton(
                    onClick = { showPdfHelp = !showPdfHelp },
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = RaagCrimson, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("सेव PDF कहाँ मिलेगी?", color = RaagCrimson)
                }

                AnimatedVisibility(visible = showPdfHelp) {
                    Surface(
                        color = RaagGoldSoft.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "आवेदन सेव होने के बाद Home या PDF Tools टैब में अपने नाम के सामने “PDF खोलें / सेव करें” दबाएँ। आप सीधे A4 पेपर (210 × 297 mm) में “Save as PDF” प्रिंट या WhatsApp/Drive पर शेयर कर सकते हैं।",
                            style = MaterialTheme.typography.bodySmall,
                            color = RaagDarkRed,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
