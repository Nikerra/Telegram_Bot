package io.project.telegram_bot.model.repository;

import io.project.telegram_bot.model.entity.Ads;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdsRepository extends JpaRepository<Ads, Long> {
}
