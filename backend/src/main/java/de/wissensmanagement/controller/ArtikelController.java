package de.wissensmanagement.controller;

import de.wissensmanagement.entity.Artikel;
import de.wissensmanagement.repository.ArtikelRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/artikel")
@CrossOrigin(origins = "*")
public class ArtikelController {

    private final ArtikelRepository artikelRepository;

    public ArtikelController(ArtikelRepository artikelRepository) {
        this.artikelRepository = artikelRepository;
    }

    @GetMapping
    public List<Artikel> getAll(@RequestParam(required = false) String kategorie,
                                @RequestParam(required = false) String q) {
        if (q != null && !q.isBlank()) {
            return artikelRepository.search(q);
        }
        if (kategorie != null && !kategorie.isBlank()) {
            return artikelRepository.findByKategorieOrderByErstelltAmDesc(kategorie);
        }
        return artikelRepository.findAllByOrderByErstelltAmDesc();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Artikel> getById(@PathVariable String id) {
        return artikelRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/kategorien")
    public List<String> getKategorien() {
        return artikelRepository.findAllKategorien();
    }

    @PostMapping
    public Artikel create(@Valid @RequestBody Artikel artikel) {
        return artikelRepository.save(artikel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Artikel> update(@PathVariable String id, @Valid @RequestBody Artikel artikel) {
        return artikelRepository.findById(id)
                .map(existing -> {
                    existing.setTitel(artikel.getTitel());
                    existing.setInhalt(artikel.getInhalt());
                    existing.setKategorie(artikel.getKategorie());
                    existing.setAutor(artikel.getAutor());
                    return ResponseEntity.ok(artikelRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        if (!artikelRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        artikelRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statistik")
    public Map<String, Object> getStatistik() {
        return Map.of(
                "gesamt", artikelRepository.count(),
                "kategorien", artikelRepository.findAllKategorien().size()
        );
    }
}
