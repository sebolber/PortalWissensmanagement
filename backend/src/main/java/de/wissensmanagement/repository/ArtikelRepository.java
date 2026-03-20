package de.wissensmanagement.repository;

import de.wissensmanagement.entity.Artikel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArtikelRepository extends JpaRepository<Artikel, String> {

    List<Artikel> findByKategorieOrderByErstelltAmDesc(String kategorie);

    @Query("SELECT a FROM Artikel a WHERE LOWER(a.titel) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(a.inhalt) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY a.erstelltAm DESC")
    List<Artikel> search(String query);

    List<Artikel> findAllByOrderByErstelltAmDesc();

    @Query("SELECT DISTINCT a.kategorie FROM Artikel a WHERE a.kategorie IS NOT NULL ORDER BY a.kategorie")
    List<String> findAllKategorien();
}
