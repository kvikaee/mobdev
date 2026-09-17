package io.github.mobdev

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private val adapter = ContactAdapter { contact -> showContactDialog(contact) }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            showContacts(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<RecyclerView>(R.id.recycler_contacts).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<Button>(R.id.button_grant).setOnClickListener {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }

        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        showContacts(granted)
    }

    private fun showContacts(hasPermission: Boolean) {
        findViewById<View>(R.id.permission_container).visibility =
            if (hasPermission) View.GONE else View.VISIBLE
        findViewById<View>(R.id.recycler_contacts).visibility =
            if (hasPermission) View.VISIBLE else View.GONE
        findViewById<View>(R.id.empty_view).visibility = View.GONE

        if (hasPermission) {
            val contacts = fetchAllContacts()
            if (contacts.isEmpty()) {
                findViewById<View>(R.id.empty_view).visibility = View.VISIBLE
            } else {
                adapter.submitList(contacts)
            }
        }
    }

    private fun showContactDialog(contact: Contact) {
        val message = "${getString(R.string.phone)}: ${contact.phoneNumber ?: getString(R.string.no_phone)}\n" +
            "${getString(R.string.email)}: ${contact.email ?: getString(R.string.no_email)}\n" +
            "${getString(R.string.note)}: ${contact.note ?: getString(R.string.no_note)}"
        AlertDialog.Builder(this)
            .setTitle(contact.name ?: getString(R.string.unknown_name))
            .setMessage(message)
            .setPositiveButton(R.string.close, null)
            .show()
    }
}

data class Contact(
    val name: String?,
    val phoneNumber: String?,
    val email: String?,
    val note: String?
)

@SuppressLint("Range")
fun Context.fetchAllContacts(): List<Contact> {
    val cursor = contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        null, null, null, null
    )
    cursor?.use {
        return buildList {
            while (it.moveToNext()) {
                val contactId = it.getLong(
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                )
                val name = it.getStringOrNull(
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                )
                val phoneNumber = it.getStringOrNull(
                    it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                )
                val email = fetchEmail(contactId)
                val note = fetchNote(contactId)
                add(Contact(name, phoneNumber, email, note))
            }
        }
    }
    return emptyList()
}

@SuppressLint("Range")
private fun Context.fetchEmail(contactId: Long): String? {
    contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getStringOrNull(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            )
        }
    }
    return null
}

@SuppressLint("Range")
private fun Context.fetchNote(contactId: Long): String? {
    contentResolver.query(
        ContactsContract.Data.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Note.NOTE),
        "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.Data.CONTACT_ID} = ?",
        arrayOf(
            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE,
            contactId.toString()
        ),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getStringOrNull(
                cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)
            )
        }
    }
    return null
}

private fun Cursor.getStringOrNull(index: Int): String? =
    if (index >= 0 && !isNull(index)) getString(index) else null

class ContactAdapter(
    private val onClick: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    private val items = mutableListOf<Contact>()

    fun submitList(contacts: List<Contact>) {
        items.clear()
        items.addAll(contacts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val nameText = view.findViewById<TextView>(R.id.text_name)
        private val phoneText = view.findViewById<TextView>(R.id.text_phone)

        fun bind(contact: Contact) {
            nameText.text = contact.name ?: itemView.context.getString(R.string.unknown_name)
            phoneText.text = contact.phoneNumber ?: itemView.context.getString(R.string.no_phone)
            itemView.setOnClickListener { onClick(contact) }
        }
    }
}