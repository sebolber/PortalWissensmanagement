package de.wissensmanagement.repository;

import de.wissensmanagement.entity.ArticleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleVersionRepository extends JpaRepository<ArticleVersion, String> {

    List<ArticleVersion> findByArticleIdOrderByVersionDesc(String articleId);
}
