package com.gudangdamar.main.controller;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.gudangdamar.main.model.Barang;
import com.gudangdamar.main.model.Harga;
import com.gudangdamar.main.model.Kategori;
import com.gudangdamar.main.repository.BarangRepository;

@Controller
public class BarangController {

    @Autowired
    private BarangRepository barangRepository;

    @GetMapping("barang/tambah")
    public String formTambahBarang(Model model) {
        Barang barang = new Barang();
        barang.setHarga(new Harga());
        barang.setKategori(new Kategori());
        model.addAttribute("barang", barang);
        return "pages/tambahBarang";
    }

    @PostMapping("/barang/save")
public String saveBarang(@ModelAttribute Barang barang, RedirectAttributes redirectAttributes) {
    if (barang.getNama() == null || barang.getNama().isBlank()) {
        redirectAttributes.addFlashAttribute("error", "Nama barang tidak boleh kosong");
        return "redirect:/barang/tambah";
    }

    if (barang.getHarga() == null) {
        barang.setHarga(new Harga());
    }

    if (barang.getKategori() == null) {
        barang.setKategori(new Kategori());
    }

    if (barang.getHarga().getHarga() <= 0 || barang.getHarga().getJumlah() <= 0) {
        redirectAttributes.addFlashAttribute("error", "Harga dan jumlah harus lebih dari 0");
        return "redirect:/barang/tambah";
    }

    barang.getHarga().hitungTotalHarga();

    if (barang.getWaktuMasuk() == null) {
        barang.setWaktuMasuk(LocalDateTime.now());
    }

    barang.setWaktuPendataan(LocalDateTime.now());

    Barang saved = barangRepository.save(barang);
    redirectAttributes.addFlashAttribute("success", "Barang berhasil disimpan dengan ID: " + saved.getIdBarang());
    return "redirect:/halamanGudangDetail/" + saved.getIdBarang();
}
 @PostMapping("barang/delete/{id}")
    public String deleteBarang(@PathVariable int id, RedirectAttributes redirectAttributes) {
        if (barangRepository.existsById(id)) {
            barangRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Barang berhasil dihapus.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Barang tidak ditemukan.");
        }
        return "redirect:/halamanGudangBeranda";
    }

    @GetMapping("barang/edit/{id}")
    public String showEditForm(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        Barang barang = barangRepository.findById(id).orElse(null);
        if (barang == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Data barang tidak ditemukan.");
            return "redirect:/halamanGudangBeranda";
        }
        model.addAttribute("barang", barang);
        return "pages/editBarang";
    }

    @PostMapping("barang/update/{id}")
    public String updatedBarang(@PathVariable int id, @ModelAttribute Barang barangForm, RedirectAttributes redirectAttributes) {
        Optional<Barang> optionalBarang = barangRepository.findById(id);
        if (optionalBarang.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Data barang tidak ditemukan.");
            return "redirect:/halamanGudangBeranda";
        }

        Barang barang = optionalBarang.get();

        barang.setNama(barangForm.getNama());

        if (barang.getKategori() == null) {
            barang.setKategori(new Kategori());
        }
        barang.getKategori().setUkuran(barangForm.getKategori().getUkuran());
        barang.getKategori().setKetebalan(barangForm.getKategori().getKetebalan());
        barang.getKategori().setBahan(barangForm.getKategori().getBahan());

        if (barang.getHarga() == null) {
            barang.setHarga(new Harga());
        }
        barang.getHarga().setHarga(barangForm.getHarga().getHarga());
        barang.getHarga().setJumlah(barangForm.getHarga().getJumlah());
        barang.getHarga().hitungTotalHarga();

        barangRepository.save(barang);

        redirectAttributes.addFlashAttribute("success", "Data barang berhasil diperbarui.");
        return "redirect:/halamanGudangDetail/" + barang.getIdBarang();
    }
    @PostMapping("/barang/updateJumlah/{id}")
    public String updateJumlah(@PathVariable int id, @RequestParam int jumlah, RedirectAttributes redirectAttributes) {
        Optional<Barang> optionalBarang = barangRepository.findById(id);
        if (optionalBarang.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Data barang tidak ditemukan.");
            return "redirect:/halamanGudangBeranda";
        }

        Barang barang = optionalBarang.get();
        if (barang.getHarga() == null) {
            barang.setHarga(new Harga());
        }

        barang.getHarga().setJumlah(jumlah);
        barang.getHarga().hitungTotalHarga();

        barangRepository.save(barang);

        redirectAttributes.addFlashAttribute("success", "Jumlah barang berhasil diperbarui.");
        return "redirect:/halamanGudangDetail/" + id;
    }
    @GetMapping("/barang/cari")
    public String cariBarang(@RequestParam("nama") String nama, Model model) {
        if (nama == null || nama.isBlank()) {
            model.addAttribute("error", "Nama barang tidak boleh kosong");
            return "pages/halamanGudangBeranda";
        }

        model.addAttribute("barangList", barangRepository.findByNamaContainingIgnoreCase(nama));
        return "pages/halamanGudangBeranda";
    }
}
