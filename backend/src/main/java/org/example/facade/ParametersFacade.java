package org.example.facade;

import org.example.dao.ParametrizacaoIgrejaDao;
import org.example.model.ParametrizacaoIgreja;

public class ParametersFacade {
    private final ParametrizacaoIgrejaDao dao = new ParametrizacaoIgrejaDao();

    public ParametrizacaoIgreja buscar() {
        ParametrizacaoIgreja parametros = dao.buscar();

        if (parametros == null) {
            return parametrosPadrao();
        }

        return parametros;
    }

    public ParametrizacaoIgreja salvar(ParametrizacaoIgreja parametros) {
        validar(parametros);
        aplicarDefaults(parametros);
        return dao.salvar(parametros);
    }

    public ParametrizacaoIgreja salvarLogo(String caminhoLogo) {
        ParametrizacaoIgreja parametros = buscar();
        parametros.setCaminhoLogo(caminhoLogo);
        return salvar(parametros);
    }

    private void validar(ParametrizacaoIgreja parametros) {
        if (parametros == null) {
            throw new IllegalArgumentException("Parâmetros da igreja são obrigatórios.");
        }

        if (parametros.getNomeFantasia() == null || parametros.getNomeFantasia().isBlank()) {
            throw new IllegalArgumentException("Nome fantasia é obrigatório.");
        }
    }

    private void aplicarDefaults(ParametrizacaoIgreja parametros) {
        if (parametros.getCorPrimaria() == null || parametros.getCorPrimaria().isBlank()) {
            parametros.setCorPrimaria("#1f4f82");
        }

        if (parametros.getCorSecundaria() == null || parametros.getCorSecundaria().isBlank()) {
            parametros.setCorSecundaria("#7d0a1e");
        }
    }

    private ParametrizacaoIgreja parametrosPadrao() {
        ParametrizacaoIgreja parametros = new ParametrizacaoIgreja();
        parametros.setNomeFantasia("Igreja");
        parametros.setCorPrimaria("#1f4f82");
        parametros.setCorSecundaria("#7d0a1e");
        return parametros;
    }
}
