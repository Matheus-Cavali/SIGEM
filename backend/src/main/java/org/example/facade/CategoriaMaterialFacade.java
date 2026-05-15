package org.example.facade;

import org.example.dao.CategoriaMaterialDao;
import org.example.model.CategoriaMaterial;
import java.util.List;

public class CategoriaMaterialFacade {
    private static final CategoriaMaterialFacade instancia = new CategoriaMaterialFacade();
    private final CategoriaMaterialDao dao = CategoriaMaterialDao.getInstancia();

    private CategoriaMaterialFacade(){}

    public static CategoriaMaterialFacade getInstancia(){
        return instancia;
    }

    public void cadastrar(CategoriaMaterial cm){
        validarNome(cm);
        validarNomeDuplicado(cm.getNome(), null);
        dao.cadastrar(cm);
    }

    public void alterar(CategoriaMaterial cm){
        if(cm.getId() == null || cm.getId() <= 0)
            throw new IllegalArgumentException("ID inválido.");
        validarNome(cm);
        validarNomeDuplicado(cm.getNome(), cm.getId());
        dao.atualizar(cm);
    }

    public void excluir(Integer id) {
        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");
        dao.excluir(id);
    }

    public CategoriaMaterial buscarPorId(Integer id) {
        if(id == null || id <= 0)
            throw new IllegalArgumentException("ID inválido.");
        return dao.buscarPorId(id);
    }

    public List<CategoriaMaterial> listarTodos(){
        return dao.listarTodos();
    }

    public List<CategoriaMaterial> buscarPorNome(String nome){
        if(nome == null || nome.trim().isEmpty())
            throw new IllegalArgumentException("Nome obrigatório.");
        return dao.listar(nome);
    }

    private void validarNome(CategoriaMaterial cm){
        if(cm.getNome() == null || cm.getNome().trim().isEmpty())
            throw new IllegalArgumentException("Nome obrigatório.");
    }

    private void validarNomeDuplicado(String nome, Integer idAtual){
        CategoriaMaterial existente = dao.buscarPorNomeExato(nome);

        if(existente != null){
            if(idAtual == null || !existente.getId().equals(idAtual))
                throw new RuntimeException("Categoria de material já cadastrada anteriormente.");
        }
    }
}