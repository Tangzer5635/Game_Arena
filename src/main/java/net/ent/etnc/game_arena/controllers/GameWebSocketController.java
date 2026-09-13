package net.ent.etnc.game_arena.controllers;

import net.ent.etnc.game_arena.dtos.GameAnswerRequestDto;
import net.ent.etnc.game_arena.services.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class GameWebSocketController {

    private final GameService gameService;

    public GameWebSocketController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/game/{code}/start")
    public void startGame(@DestinationVariable String code) {
        gameService.startGame(code);
    }

    @MessageMapping("/game/{code}/answer")
    public void answer(
            @DestinationVariable String code,
            GameAnswerRequestDto request
    ) {
        gameService.answer(code, request.getUserId(), request.getReponseId());
    }
}
