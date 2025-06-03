package com.gudangdamar.main.controller;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import com.gudangdamar.main.model.User;
import com.gudangdamar.main.repository.BarangRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class BarangController {

    @Autowired
    private BarangRepository barangRepository;

    // Form tambah barang
    @GetMapping("/barang/tambah")
    public String formTambahBarang(Model model) {
        Barang barang = new Barang();
        barang.setHarga(new Harga());
        barang.setKategori(new Kategori());
        model.addAttribute("barang", barang);
        return "pages/tambahBarang";
    }

    // Menyimpan barang baru
    @PostMapping("/barang/save")
    public String saveBarang(@ModelAttribute Barang barang, RedirectAttributes redirectAttributes) {
        if (barang.getNama() == null || barang.getNama().isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Nama barang tidak boleh kosong");
            return "redirect:/barang/tambah";
        }

        barang.setHarga(Optional.ofNullable(barang.getHarga()).orElse(new Harga()));
        barang.setKategori(Optional.ofNullable(barang.getKategori()).orElse(new Kategori()));

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

    // Menghapus barang
    @PostMapping("/barang/delete/{id}")
    public String deleteBarang(@PathVariable int id, RedirectAttributes redirectAttributes) {
        if (barangRepository.existsById(id)) {
            barangRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Barang berhasil dihapus.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Barang tidak ditemukan.");
        }
        return "redirect:/halamanGudangBeranda";
    }

    // Form edit barang
    @GetMapping("/barang/edit/{id}")
    public String showEditForm(@PathVariable int id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Barang> optionalBarang = barangRepository.findById(id);
        if (optionalBarang.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Data barang tidak ditemukan.");
            return "redirect:/halamanGudangBeranda";
        }

        model.addAttribute("barang", optionalBarang.get());
        return "pages/editBarang";
    }

    // Memperbarui barang
    @PostMapping("/barang/update/{id}")
    public String updatedBarang(@PathVariable int id, @ModelAttribute Barang barangForm, RedirectAttributes redirectAttributes) {
        Optional<Barang> optionalBarang = barangRepository.findById(id);
        if (optionalBarang.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Data barang tidak ditemukan.");
            return "redirect:/halamanGudangBeranda";
        }

        Barang barang = optionalBarang.get();
        barang.setNama(barangForm.getNama());

        if (barang.getKategori() == null) barang.setKategori(new Kategori());
        Kategori kategori = barang.getKategori();
        kategori.setUkuran(barangForm.getKategori().getUkuran());
        kategori.setKetebalan(barangForm.getKategori().getKetebalan());
        kategori.setBahan(barangForm.getKategori().getBahan());

        if (barang.getHarga() == null) barang.setHarga(new Harga());
        Harga harga = barang.getHarga();
        harga.setHarga(barangForm.getHarga().getHarga());
        harga.setJumlah(barangForm.getHarga().getJumlah());
        harga.hitungTotalHarga();

        barangRepository.save(barang);

        redirectAttributes.addFlashAttribute("success", "Data barang berhasil diperbarui.");
        return "redirect:/halamanGudangDetail/" + barang.getIdBarang();
    }

    // Memperbarui jumlah barang
    @PostMapping("/barang/updateJumlah/{id}")
    public String updateJumlah(@PathVariable int id, @RequestParam int jumlah, RedirectAttributes redirectAttributes) {
        Optional<Barang> optionalBarang = barangRepository.findById(id);
        if (optionalBarang.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Data barang tidak ditemukan.");
            return "redirect:/halamanGudangBeranda";
        }

        Barang barang = optionalBarang.get();
        barang.setHarga(Optional.ofNullable(barang.getHarga()).orElse(new Harga()));
        barang.getHarga().setJumlah(jumlah);
        barang.getHarga().hitungTotalHarga();

        barangRepository.save(barang);

        redirectAttributes.addFlashAttribute("success", "Jumlah barang berhasil diperbarui.");
        return "redirect:/halamanGudangDetail/" + id;
    }

    // Pencarian barang
    @GetMapping("/search/result")
    public String searchBarang(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String filter,
            Model model, HttpSession session) {

        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || (!"kasir".equalsIgnoreCase(user.getRole()) && !"admin".equalsIgnoreCase(user.getRole()))) {
            return "redirect:/login";
        }

        List<Barang> barangList = (query != null && !query.trim().isEmpty())
                ? barangRepository.findByNamaContainingIgnoreCase(query)
                : barangRepository.findAll();

        if (filter != null) {
            barangList = barangList.stream().filter(b -> {
                Kategori k = b.getKategori();
                return switch (filter) {
                    case "ukuran" -> k != null && k.getUkuran() > 0;
                    case "bentuk" -> k != null && k.getBentuk() != null && !k.getBentuk().isBlank();
                    case "material" -> k != null && k.getBahan() != null && !k.getBahan().isBlank();
                    case "brand" -> b.getMerek() != null && !b.getMerek().isBlank();
                    case "ketebalan" -> k != null && k.getKetebalan() != null && !k.getKetebalan().isBlank();
                    default -> true;
                };
            }).collect(Collectors.toList());
        }

        if (sort != null) {
            switch (sort) {
                case "az" -> barangList.sort(Comparator.comparing(Barang::getNama, String.CASE_INSENSITIVE_ORDER));
                case "za" -> barangList.sort(Comparator.comparing(Barang::getNama, String.CASE_INSENSITIVE_ORDER).reversed());
                case "expensive" -> barangList.sort(Comparator.comparing(
                        b -> Optional.ofNullable(b.getHarga()).map(Harga::getHarga).orElse(0),
                        Comparator.reverseOrder()));
                case "cheap" -> barangList.sort(Comparator.comparing(
                        b -> Optional.ofNullable(b.getHarga()).map(Harga::getHarga).orElse(0)));
            }
        }

        model.addAttribute("barangList", barangList);
        model.addAttribute("query", query);
        model.addAttribute("sort", sort);
        model.addAttribute("filter", filter);

        return "pages/halamanGudangBeranda";
    }

}
