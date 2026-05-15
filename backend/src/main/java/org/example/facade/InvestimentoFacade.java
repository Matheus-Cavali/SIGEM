package org.example.facade;

import org.example.dao.AporteInvestimentoDao;
import org.example.dao.InvestimentoFuturoDao;
import org.example.model.AporteInvestimento;
import org.example.model.InvestimentoFuturo;
import org.example.util.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    public int registrarInvestimento(InvestimentoFuturo inv) {
        return new InvestimentoFuturoDao().inserir(inv);
    }

    public List<InvestimentoFuturo> listarInvestimentos(String filtroNome, String filtroStatus) {
        return new InvestimentoFuturoDao().listar(filtroNome, filtroStatus);
    }

    public InvestimentoFuturo buscarInvestimentoPorId(int id) {
        return new InvestimentoFuturoDao().buscarPorId(id);
    }

    public boolean atualizarInvestimento(InvestimentoFuturo inv) {
        return new InvestimentoFuturoDao().atualizar(inv);
    }

    public boolean deletarInvestimento(int id) {
        return new InvestimentoFuturoDao().deletar(id);
    }

    public InvestimentoFuturo buscarInvestimentoPorNome(String nome) {
        return new InvestimentoFuturoDao().buscarPorNome(nome);
    }

    public int lancarAporte(AporteInvestimento aporte) {
        return new AporteInvestimentoDao().inserir(aporte);
    }

    public List<AporteInvestimento> listarAportes(int investimentoId) {
        return new AporteInvestimentoDao().listarPorInvestimento(investimentoId);
    }

    public boolean deletarAporte(int aporteId) {
        return new AporteInvestimentoDao().deletar(aporteId);
    }

    public boolean atualizarAporte(int aporteId, AporteInvestimento aporte) {
        return new AporteInvestimentoDao().atualizar(aporteId, aporte);
    }

    public BigDecimal calcularSaldo(int investimentoId) {
        return new InvestimentoFuturoDao().calcularSaldo(investimentoId);
    }
}
