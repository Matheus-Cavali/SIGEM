package org.example.observer;

import org.example.model.Evento;

import java.math.BigDecimal;

public class ResultadoFinanceiroNotificador implements ObservadorEvento
{
    @Override
    public String update(Evento evento, BigDecimal resultadoFinanceiro, String observacoesHistorico)
    {
        if (resultadoFinanceiro.compareTo(BigDecimal.ZERO) > 0)
        {
            return "O evento '" + evento.getNome() +
                    "' teve resultado financeiro positivo de R$ " + resultadoFinanceiro;
        }
        else if (resultadoFinanceiro.compareTo(BigDecimal.ZERO) < 0)
        {
            return "Atenção: o evento '" + evento.getNome() +
                    "' teve resultado financeiro negativo de R$ " + resultadoFinanceiro +
                    ". Recomenda-se revisão orçamentária.";
        }
        else
        {
            return "O evento '" + evento.getNome() +
                    "' encerrou com resultado financeiro neutro (R$ 0,00).";
        }
    }
}