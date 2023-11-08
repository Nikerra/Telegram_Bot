package io.project.telegram_bot.model.repository;

import io.project.telegram_bot.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByChatId(long chatId);
}
