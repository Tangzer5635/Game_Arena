package net.ent.etnc.game_arena.services.impl;

import lombok.RequiredArgsConstructor;
import net.ent.etnc.game_arena.dtos.UserStatsDto;
import net.ent.etnc.game_arena.models.entities.User;
import net.ent.etnc.game_arena.repositories.UserStatsRepository;
import net.ent.etnc.game_arena.services.UserService;
import net.ent.etnc.game_arena.services.UserStatsService;
import net.ent.etnc.game_arena.services.commons.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserStatsServiceImpl implements UserStatsService {

    private final UserStatsRepository userStatsRepository;
    private final UserService         userService;

    @Override
    @Transactional(readOnly = true)
    public UserStatsDto getStats(Long userId) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new ServiceException("Utilisateur introuvable : " + userId));

        return userStatsRepository.findByUserId(userId)
                .map(stats -> UserStatsDto.builder()
                        .userId(userId)
                        .username(user.getUsername())
                        .partiesJouees(stats.getPartiesJouees())
                        .scoreCumule(stats.getScoreCumule())
                        .meilleurScore(stats.getMeilleurScore())
                        .bonnesReponses(stats.getBonnesReponses())
                        .mauvaisesReponses(stats.getMauvaisesReponses())
                        .tauxReussitePct((int) Math.round(stats.tauxReussite() * 100))
                        .meilleureStreak(stats.getMeilleureStreak())
                        .build())
                .orElse(UserStatsDto.builder()
                        .userId(userId)
                        .username(user.getUsername())
                        .build()); // stats vierges si première partie
    }
}
