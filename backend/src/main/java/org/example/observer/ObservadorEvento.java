package org.example.observer;

import org.example.model.Evento;

import java.math.BigDecimal;

public interface ObservadorEvento
{
    String update(Evento evento, BigDecimal resultadoFinanceiro, String observacoesHistorico);
}