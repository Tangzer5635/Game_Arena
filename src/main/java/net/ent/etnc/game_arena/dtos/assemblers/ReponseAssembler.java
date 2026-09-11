package net.ent.etnc.game_arena.dtos.assemblers;

import net.ent.etnc.game_arena.dtos.ReponseRequestDto;
import net.ent.etnc.game_arena.dtos.ReponseResponseDto;
import net.ent.etnc.game_arena.models.entities.Reponse;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class ReponseAssembler {

    public ReponseResponseDto toDto(Reponse reponse) {

        return ReponseResponseDto.builder()
                .id(reponse.getId())
                .text(reponse.getText())
                .estBonne(reponse.isEstBonne())
                .build();
    }

    public List<ReponseResponseDto> toDtos(Collection<Reponse> reponses) {

        return reponses.stream()
                .map(this::toDto)
                .toList();
    }

    public Reponse toEntity(ReponseRequestDto dto) {

        Reponse reponse = new Reponse();

        reponse.setId(dto.getId());
        reponse.setText(dto.getText());
        reponse.setEstBonne(dto.isEstBonne());

        return reponse;
    }
}