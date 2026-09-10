package net.ent.etnc.game_arena.services.impl;

import net.ent.etnc.game_arena.models.entities.RefreshToken;
import net.ent.etnc.game_arena.repositories.RefreshTokenRepository;
import net.ent.etnc.game_arena.services.RefreshTokenService;
import net.ent.etnc.game_arena.services.commons.AbstractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenServiceImpl extends AbstractService<RefreshToken, RefreshTokenRepository> implements RefreshTokenService {

    @Autowired
    public RefreshTokenServiceImpl(RefreshTokenRepository refreshtokenRepository) {
        super(refreshtokenRepository);
    }
}