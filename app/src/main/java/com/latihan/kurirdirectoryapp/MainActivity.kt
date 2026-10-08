package com.latihan.kurirdirectoryapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // 1. Deklarasi Komponen Tampilan
    private lateinit var editTextSearchName: EditText
    private lateinit var progressBarLoading: ProgressBar
    private lateinit var textViewErrorMessage: TextView
    private lateinit var recyclerViewUsers: RecyclerView

    // TUGAS MANDIRI: TextView untuk state kosong hasil pencarian
    private lateinit var textViewEmptyState: TextView

    // Variabel Penampung Data
    private lateinit var userAdapter: UserAdapter
    private val daftarPenggunaAsli = mutableListOf<UserResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. Inisialisasi Elemen Antarmuka
        editTextSearchName = findViewById(R.id.editTextSearchName)
        progressBarLoading = findViewById(R.id.progressBarLoading)
        textViewErrorMessage = findViewById(R.id.textViewErrorMessage)
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers)
        textViewEmptyState = findViewById(R.id.textViewEmptyState)

        // Konfigurasi RecyclerView dengan LinearLayoutManager vertikal
        recyclerViewUsers.layoutManager = LinearLayoutManager(this)
        userAdapter = UserAdapter(emptyList())
        recyclerViewUsers.adapter = userAdapter

        // 3. Mengambil Data Langsung Saat Aplikasi Dibuka
        muatDataPengguna()

        // 4. Fitur Pencarian Nama Real-Time (Live Search Filter)
        editTextSearchName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val kataKunci = s.toString().trim()
                filterNamaPengguna(kataKunci)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun muatDataPengguna() {
        progressBarLoading.visibility = View.VISIBLE
        textViewErrorMessage.visibility = View.GONE

        // Eksekusi Panggilan Jaringan Asinkron
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getAllUsers()

                // Kembali ke Main Thread untuk Mengupdate Tampilan UI
                withContext(Dispatchers.Main) {
                    progressBarLoading.visibility = View.GONE
                    if (response.isSuccessful && response.body() != null) {
                        val dataDiterima = response.body()!!
                        daftarPenggunaAsli.clear()
                        daftarPenggunaAsli.addAll(dataDiterima)
                        // Tampilkan Seluruh Data ke RecyclerView
                        userAdapter.perbaruiDaftar(daftarPenggunaAsli)
                        // TUGAS MANDIRI: cek state kosong saat data server memang kosong
                        if (daftarPenggunaAsli.isEmpty()) {
                            tampilkanEmptyState("Belum ada data kurir tersimpan.")
                        } else {
                            sembunyikanEmptyState()
                        }
                    } else {
                        textViewErrorMessage.text = "Gagal memuat data dari server (HTTP ${response.code()})"
                        textViewErrorMessage.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarLoading.visibility = View.GONE
                    textViewErrorMessage.text = "Koneksi internet bermasalah: ${e.localizedMessage ?: "Gagal terhubung"}"
                    textViewErrorMessage.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * TUGAS MANDIRI 1:
     * Pencarian tidak hanya memeriksa nama pengguna, tetapi juga NAMA PERUSAHAAN.
     * Pencarian bersifat live (reactive) dan tidak membedakan huruf besar/kecil.
     */
    private fun filterNamaPengguna(kataKunci: String) {
        if (kataKunci.isEmpty()) {
            // Jika kolom pencarian kosong, tampilkan kembali seluruh data
            userAdapter.perbaruiDaftar(daftarPenggunaAsli)
            if (daftarPenggunaAsli.isEmpty()) {
                tampilkanEmptyState("Belum ada data kurir tersimpan.")
            } else {
                sembunyikanEmptyState()
            }
        } else {
            // Saring pengguna yang NAMANYA atau NAMA PERUSAHAANNYA mengandung kata kunci
            val daftarTersaring = daftarPenggunaAsli.filter { pengguna ->
                val cocokNama = pengguna.name.contains(kataKunci, ignoreCase = true)
                val cocokUsername = pengguna.username.contains(kataKunci, ignoreCase = true)
                val cocokEmail = pengguna.email.contains(kataKunci, ignoreCase = true)
                val cocokPerusahaan = pengguna.company.companyName.contains(kataKunci, ignoreCase = true)

                cocokNama || cocokUsername || cocokEmail || cocokPerusahaan
            }

            userAdapter.perbaruiDaftar(daftarTersaring)

            // TUGAS MANDIRI 2: Validasi state kosong jika tidak ada hasil filter
            if (daftarTersaring.isEmpty()) {
                tampilkanEmptyState(
                    "Tidak ada kurir yang cocok dengan kata kunci \"$kataKunci\"."
                )
            } else {
                sembunyikanEmptyState()
            }
        }
    }

    /**
     * TUGAS MANDIRI 2:
     * Menampilkan pesan "Tidak ada kurir yang cocok..." dan menyembunyikan RecyclerView.
     */
    private fun tampilkanEmptyState(pesan: String) {
        textViewEmptyState.text = pesan
        textViewEmptyState.visibility = View.VISIBLE
        recyclerViewUsers.visibility = View.GONE
    }

    /**
     * Mengembalikan tampilan ke kondisi normal (data ditemukan).
     */
    private fun sembunyikanEmptyState() {
        textViewEmptyState.visibility = View.GONE
        recyclerViewUsers.visibility = View.VISIBLE
    }
}