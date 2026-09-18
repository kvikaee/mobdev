package io.github.mobdev

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ContactsScreen()
        }
    }
}

@Composable
fun ContactsScreen() {

    val context = LocalContext.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var contacts by remember {
        mutableStateOf<List<Contact>>(emptyList())
    }

    var selectedContact by remember {
        mutableStateOf<Contact?>(null)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            contacts = context.fetchAllContacts()
        } else {
            contacts = emptyList()
        }
    }

    Scaffold { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            if (!hasPermission) {

                PermissionContent(
                    onGrantPermission = {
                        permissionLauncher.launch(
                            Manifest.permission.READ_CONTACTS
                        )
                    }
                )

            } else if (contacts.isEmpty()) {

                EmptyContactsContent()

            } else {

                ContactsList(
                    contacts = contacts,
                    onContactClick = { contact ->
                        selectedContact = contact
                    }
                )
            }
        }
    }

    selectedContact?.let { contact ->

        ContactDialog(
            contact = contact,
            onDismiss = {
                selectedContact = null
            }
        )
    }
}

@Composable
fun PermissionContent(
    onGrantPermission: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = stringResource(R.string.no_permission),
                fontSize = 16.sp
            )

            Button(
                onClick = onGrantPermission,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.grant_permission)
                )
            }
        }
    }
}

@Composable
fun EmptyContactsContent() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_contacts),
            fontSize = 16.sp
        )
    }
}

@Composable
fun ContactsList(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 8.dp
        )
    ) {

        items(
            items = contacts,
            key = { contact ->
                contact.id
            }
        ) { contact ->

            ContactItem(
                contact = contact,
                onClick = {
                    onContactClick(contact)
                }
            )
        }
    }
}

@Composable
fun ContactItem(
    contact: Contact,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
            .clickable {
                onClick()
            }
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Text(
                text = contact.name
                    ?: stringResource(R.string.unknown_name),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = contact.phoneNumber
                    ?: stringResource(R.string.no_phone),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ContactDialog(
    contact: Contact,
    onDismiss: () -> Unit
) {
    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text(
                text = contact.name
                    ?: stringResource(R.string.unknown_name)
            )
        },

        text = {
            Column {

                Text(
                    text = stringResource(
                        R.string.phone_format,
                        contact.phoneNumber
                            ?: stringResource(R.string.no_phone)
                    )
                )

                Text(
                    text = stringResource(
                        R.string.email_format,
                        contact.email
                            ?: stringResource(R.string.no_email)
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = stringResource(
                        R.string.note_format,
                        contact.note
                            ?: stringResource(R.string.no_note)
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },

        confirmButton = {

            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(R.string.close)
                )
            }
        }
    )
}

data class Contact(
    val id: Long,
    val name: String?,
    val phoneNumber: String?,
    val email: String?,
    val note: String?
)

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {

    val cursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,

        arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),

        null,
        null,
        null
    )

    cursor?.use {

        return buildList {

            while (it.moveToNext()) {

                val contactId = it.getLong(
                    it.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                    )
                )

                val name = it.getStringOrNull(
                    it.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                    )
                )

                val phoneNumber = it.getStringOrNull(
                    it.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                )

                val email = fetchEmail(contactId)
                val note = fetchNote(contactId)

                add(
                    Contact(
                        id = contactId,
                        name = name,
                        phoneNumber = phoneNumber,
                        email = email,
                        note = note
                    )
                )
            }
        }
    }

    return emptyList()
}

@SuppressLint("Range")
private fun Context.fetchEmail(
    contactId: Long
): String? {

    contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,

        arrayOf(
            ContactsContract.CommonDataKinds.Email.ADDRESS
        ),

        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",

        arrayOf(
            contactId.toString()
        ),

        null

    )?.use { cursor ->

        if (cursor.moveToFirst()) {

            return cursor.getStringOrNull(
                cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Email.ADDRESS
                )
            )
        }
    }

    return null
}

@SuppressLint("Range")
private fun Context.fetchNote(
    contactId: Long
): String? {

    contentResolver.query(
        ContactsContract.Data.CONTENT_URI,

        arrayOf(
            ContactsContract.CommonDataKinds.Note.NOTE
        ),

        "${ContactsContract.Data.MIMETYPE} = ? AND " +
                "${ContactsContract.Data.CONTACT_ID} = ?",

        arrayOf(
            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE,
            contactId.toString()
        ),

        null

    )?.use { cursor ->

        if (cursor.moveToFirst()) {

            return cursor.getStringOrNull(
                cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Note.NOTE
                )
            )
        }
    }

    return null
}

private fun Cursor.getStringOrNull(
    index: Int
): String? {

    return if (index >= 0 && !isNull(index)) {
        getString(index)
    } else {
        null
    }
}