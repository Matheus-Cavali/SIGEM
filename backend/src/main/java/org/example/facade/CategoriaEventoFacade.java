package org.example.facade;

import org.example.dao.CategoriaEventoDao;
import org.example.model.CategoriaEvento;

import java.util.List;

public class CategoriaEventoFacade {
    private static final CategoriaEventoFacade instancia = new CategoriaEventoFacade();
    private final CategoriaEventoDao dao = CategoriaEventoDao.getInstancia();

    private CategoriaEventoFacade(){}

    public static CategoriaEventoFacade getInstancia(){
        return instancia;
    }

    public void cadastrar(CategoriaEvento ce){
        validarNome(ce);
        validarNomeDuplicado(ce.getNome(), null);
        dao.cadastrar(ce);
    }

    public void alterar(CategoriaEvento ce){
        if(ce.getId() == null || ce.getId() <= 0)
            throw new IllegalArgumentException("ID inválido.");

        validarNome(ce);
        validarNomeDuplicado(ce.getNome(), ce.getId());

        dao.atualizar(ce);
    }

    public void excluir(Integer id){
        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        dao.excluir(id);
    }

    public CategoriaEvento buscarPorId(Integer id){
        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");

        return dao.buscarPorId(id);
    }

    public List<CategoriaEvento> listarTodos(){
        return dao.listarTodos();
    }

    public List<CategoriaEvento> buscarPorNome(String nome){
        if(nome == null || nome.trim().isEmpty())
            throw new IllegalArgumentException("Nome obrigatório.");

        return dao.listar(nome);
    }

    private void validarNome(CategoriaEvento ce){
        if(ce.getNome() == null || ce.getNome().trim().isEmpty())
            throw new IllegalArgumentException("Nome obrigatório.");
    }

    private void validarNomeDuplicado(String nome, Integer idAtual){
        CategoriaEvento existente = dao.buscarPorNomeExato(nome);

        if(existente != null){
            if(idAtual == null || !existente.getId().equals(idAtual))
                throw new RuntimeException("Categoria de evento já cadastrada anteriormente.");
        }
    }
}