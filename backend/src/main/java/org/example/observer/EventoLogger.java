package org.example.observer;

import org.example.model.Evento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventoLogger implements ObservadorEvento {

    @Override
    public String update(Evento evento, BigDecimal resultadoFinanceiro, String observacoesHistorico) {
        System.out.println("[LOG " + LocalDateTime.now() + "] Evento '" + evento.getNome() +
                "' (id=" + evento.getId() + ") foi encerrado. Resultado financeiro: R$ " +
                resultadoFinanceiro + ". Observações: " + observacoesHistorico);
        return null;
    }
}