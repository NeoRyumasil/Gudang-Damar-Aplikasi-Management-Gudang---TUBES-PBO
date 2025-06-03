package com.gudangdamar.main.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.gudangdamar.main.model.Barang;
import com.gudangdamar.main.model.Pemesanan;
import com.gudangdamar.main.model.Servis;
import com.gudangdamar.main.model.User;
import com.gudangdamar.main.repository.BarangRepository;
import com.gudangdamar.main.repository.PemesananRepository;
import com.gudangdamar.main.repository.ServisRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class PageController {

    @Autowired
    private ServisRepository servisRepository;

    @Autowired
    private BarangRepository barangRepository;

    @Autowired
    private PemesananRepository pemesananRepository;

    @GetMapping("/")
    public String redirectRoot(Model model) {
        model.addAttribute("user", new User());
        return "pages/login"; 
    }

    private boolean isKasirOrAdmin(User user) {
        if (user == null) return false;
        String role = user.getRole();
        return "kasir".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role);
    }

    // --- HALAMAN GUDANG BERANDA ---
    @GetMapping("/halamanGudangBeranda")
    public String showGudangBeranda(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (!isKasirOrAdmin(user)) {
            return "redirect:/login";  // atau halaman error 403 kalau mau lebih jelas
        }

        List<Barang> barangList = barangRepository.findAll();
        
        Map<String, Barang> map = new HashMap<>();

        for (Barang b : barangList) {
            String key = b.getNama() + "|" +
                         b.getKategori().getUkuran() + "|" +
                         b.getKategori().getKetebalan() + "|" +
                         b.getKategori().getBentuk() + "|" +
                         b.getKategori().getBahan() + "|" +
                         b.getKategori().getMerek();

            if (map.containsKey(key)) {
                Barang existing = map.get(key);

                int jumlahLama = existing.getHarga().getJumlah();
                int jumlahBaru = b.getHarga().getJumlah();

                existing.getHarga().setJumlah(jumlahLama + jumlahBaru);
                existing.getHarga().hitungTotalHarga();
            } else {
                b.getHarga().hitungTotalHarga();
                map.put(key, b);
            }
        }

        List<Barang> barangListGrouped = new ArrayList<>(map.values());

        model.addAttribute("barangList", barangListGrouped);
        model.addAttribute("user", user);

        return "pages/halamanGudangBeranda";
    }

    // --- HALAMAN DETAIL BARANG ---
    @GetMapping("/halamanGudangDetail/{id}")
    public String detailBarang(@PathVariable("id") int id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (!isKasirOrAdmin(user)) {
            return "redirect:/login";
        }

        Barang barangDetail = barangRepository.findById(id).orElse(null);
        if (barangDetail == null) {
            return "redirect:/error";
        }

        List<Barang> barangList = barangRepository.findAll();

        Map<String, Barang> map = new HashMap<>();

        for (Barang b : barangList) {
            String key = b.getNama() + "|" +
                         b.getKategori().getUkuran() + "|" +
                         b.getKategori().getKetebalan() + "|" +
                         b.getKategori().getBentuk() + "|" +
                         b.getKategori().getBahan() + "|" +
                         b.getKategori().getMerek();

            if (map.containsKey(key)) {
                Barang existing = map.get(key);

                int jumlahLama = existing.getHarga().getJumlah();
                int jumlahBaru = b.getHarga().getJumlah();

                existing.getHarga().setJumlah(jumlahLama + jumlahBaru);
                existing.getHarga().hitungTotalHarga();
            } else {
                b.getHarga().hitungTotalHarga();
                map.put(key, b);
            }
        }

        List<Barang> barangListGrouped = new ArrayList<>(map.values());

        model.addAttribute("barang", barangDetail);
        model.addAttribute("barangList", barangListGrouped);
        model.addAttribute("user", user);

        return "pages/halamanGudangDetail";
    }

    
    @GetMapping("/halamanGudangServis")
    public String showGudangServis(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (!isKasirOrAdmin(user)) {
            return "redirect:/login";
        }

        model.addAttribute("servisBaru", new Servis());
        model.addAttribute("servisList", servisRepository.findAll());
        model.addAttribute("user", user);

        return "pages/halamanGudangServis";
    }


    @GetMapping("/halamanGudangPesanan")
    public String showGudangPemesanan(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (!isKasirOrAdmin(user)) {
            return "redirect:/login";
        }

        model.addAttribute("pemesananBaru", new Pemesanan());
        model.addAttribute("pemesananList", pemesananRepository.findAll());
        model.addAttribute("user", user);

        return "pages/halamanGudangPesanan";
    }
    @PostMapping("/search/close")
    public String closeSearch() {
        return "redirect:/halamanGudangBeranda";
    }
    @GetMapping("/error")
    public String errorPage(Model model) {
        model.addAttribute("errorMessage", "Barang tidak ditemukan atau terjadi kesalahan.");
        return "pages/error"; // Buat file error.html di templates/pages/
    }
}
