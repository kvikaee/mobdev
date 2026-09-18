package io.github.mobdev

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var contactsList: ListView
    private lateinit var permissionContainer: View
    private lateinit var emptyView: TextView

    private var contacts: List<Contact> = emptyList()

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                loadContacts()
            } else {
                showPermissionState()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        contactsList = findViewById(R.id.contacts_list)
        permissionContainer = findViewById(R.id.permission_container)
        emptyView = findViewById(R.id.empty_view)

        findViewById<Button>(R.id.grant_permission_button)
            .setOnClickListener {
                permissionLauncher.launch(
                    Manifest.permission.READ_CONTACTS
                )
            }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            loadContacts()
        } else {
            showPermissionState()
        }
    }

    private fun loadContacts() {
        contacts = fetchAllContacts()

        showContacts()
    }

    private fun showPermissionState() {
        permissionContainer.visibility = View.VISIBLE
        contactsList.visibility = View.GONE
        emptyView.visibility = View.GONE
    }

    private fun showContacts() {
        permissionContainer.visibility = View.GONE

        if (contacts.isEmpty()) {
            contactsList.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            return
        }

        emptyView.visibility = View.GONE
        contactsList.visibility = View.VISIBLE

        contactsList.adapter = ContactAdapter(
            this,
            contacts
        )

        contactsList.setOnItemClickListener { _, _, position, _ ->
            showContactDialog(contacts[position])
        }
    }

    private fun showContactDialog(contact: Contact) {
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_contact, null)

        val phone = dialogView.findViewById<TextView>(
            R.id.dialog_phone
        )

        val email = dialogView.findViewById<TextView>(
            R.id.dialog_email
        )

        val note = dialogView.findViewById<TextView>(
            R.id.dialog_note
        )

        if (contact.phoneNumber != null) {
            phone.text = contact.phoneNumber
        } else {
            phone.setText(R.string.no_phone)
        }

        if (contact.email != null) {
            email.text = contact.email
        } else {
            email.setText(R.string.no_email)
        }

        if (contact.note != null) {
            note.text = contact.note
        } else {
            note.setText(R.string.no_note)
        }

        val title = contact.name
            ?: getString(R.string.unknown_name)

        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(R.string.close, null)
            .show()
    }
}

data class Contact(
    val id: Long,
    val name: String?,
    val phoneNumber: String?,
    val email: String?,
    val note: String?
)

class ContactAdapter(
    private val context: Context,
    private val contacts: List<Contact>
) : BaseAdapter() {

    override fun getCount(): Int {
        return contacts.size
    }

    override fun getItem(position: Int): Contact {
        return contacts[position]
    }

    override fun getItemId(position: Int): Long {
        return contacts[position].id
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {

        val view = convertView
            ?: LayoutInflater.from(context)
                .inflate(
                    R.layout.item_contact,
                    parent,
                    false
                )

        val name = view.findViewById<TextView>(
            R.id.contact_name
        )

        val phone = view.findViewById<TextView>(
            R.id.contact_phone
        )

        val contact = getItem(position)

        if (contact.name != null) {
            name.text = contact.name
        } else {
            name.setText(R.string.unknown_name)
        }

        if (contact.phoneNumber != null) {
            phone.text = contact.phoneNumber
        } else {
            phone.setText(R.string.no_phone)
        }

        return view
    }
}

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {

    val contacts = mutableListOf<Contact>()

    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        null,
        null,
        null
    )?.use { cursor ->

        while (cursor.moveToNext()) {

            val id = cursor.getLong(
                cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                )
            )

            val name = cursor.getStringOrNull(
                cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                )
            )

            val phoneNumber = cursor.getStringOrNull(
                cursor.getColumnIndex(
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
            )

            val email = fetchEmail(id)
            val note = fetchNote(id)

            contacts.add(
                Contact(
                    id = id,
                    name = name,
                    phoneNumber = phoneNumber,
                    email = email,
                    note = note
                )
            )
        }
    }

    return contacts
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
        arrayOf(contactId.toString()),
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