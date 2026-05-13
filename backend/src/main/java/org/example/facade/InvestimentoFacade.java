package org.example.facade;

import org.example.dao.InvestimentoFuturoDao;
import org.example.model.InvestimentoFuturo;
import org.example.util.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InvestimentoFacade {

    public String validarDadosInvestimento(String nome, BigDecimal meta, String dataStr) {
        if (nome == null || nome.trim().isEmpty()) {
            return "Nome do investimento é obrigatório";
        }

        if (meta == null || meta.compareTo(BigDecimal.ZERO) <= 0) {
            return "Valor meta deve ser maior que zero";
        }

        if (dataStr == null || dataStr.trim().isEmpty()) {
            return "Data de abertura é obrigatória";
        }

        if (Data.parseFlexivel(dataStr) == null) {
            return "Data inválida. Use o formato dd/mm/aaaa ou ddmmaaaa";
        }

        InvestimentoFuturoDao dao = new InvestimentoFuturoDao();
        InvestimentoFuturo existente = dao.buscarPorNome(nome.trim());
        if (existente != null) {
            return "Já existe um investimento com este nome";
        }

        return null;
    }

    public String validarDadosAporte(BigDecimal valor, String dataStr) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return "Valor do aporte deve ser maior que zero";
        }

        if (dataStr == null || dataStr.trim().isEmpty()) {
            return "Data da transação é obrigatória";
        }

        if (Data.parseFlexivel(dataStr) == null) {
            return "Data inválida. Use o formato dd/mm/aaaa ou ddmmaaaa";
        }

        return null;
    }
}