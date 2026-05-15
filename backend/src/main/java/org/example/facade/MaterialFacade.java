package org.example.facade;

import org.example.dao.MaterialDao;
import org.example.model.Material;

import java.util.List;

public class MaterialFacade {
    private static final MaterialFacade instancia = new MaterialFacade();
    private final MaterialDao dao = MaterialDao.getInstancia();

    public MaterialFacade() {}

    public static MaterialFacade getInstancia(){
        return instancia;
    }

    public void cadastrar(Material m){
        validarMaterial(m);
        validarNomeDuplicado(m.getNome(), null);
        dao.cadastrar(m);
    }

    public void alterar(Material m){
        if(m.getId() == null || m.getId() <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }
        validarMaterial(m);
        validarNomeDuplicado(m.getNome(), m.getId());
        dao.atualizar(m);
    }

    public void excluir(Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }
        dao.excluir(id);
    }

    public Material buscarPorId(Integer id){
        if(id == null || id <= 0){
            throw new IllegalArgumentException("ID inválido.");
        }

        return dao.buscarPorId(id);
    }

    public List<Material> listarTodos(){
        return dao.listarTodos();
    }

    public List<Material> buscarPorNome(String nome){
        return dao.listar(nome, null);
    }

    public List<Material> buscarPorCategoria(Integer categoriaId){
        return dao.listar(null, categoriaId);
    }

    public List<Material> filtrar(String nome, Integer categoriaId){
        return dao.listar(nome, categoriaId);
    }

    private void validarMaterial(Material m){
        if(m.getQuantidadeEstoque() < 0){
            throw new IllegalArgumentException("Estoque não pode ser negativo.");
        }
        if(m.getNome() == null || m.getNome().trim().isEmpty()){
            throw new IllegalArgumentException("Nome obrigatório.");
        }
        if(m.getCategoriaMaterialId() == null || m.getCategoriaMaterialId() <= 0){
            throw new IllegalArgumentException("ID da categoria obrigatória.");
        }
//        if(catMatDao.buscarPorId(m.getCategoriaMaterialId()) == null){
//            throw new IllegalArgumentException("A categoria informada não existe no sistema.");
//        }
    }

    private void validarNomeDuplicado(String nome, Integer idAtual){
        Material existente = dao.buscarPorNomeExato(nome);

        if(existente != null){
            if(idAtual == null || !existente.getId().equals(idAtual)){
                throw new RuntimeException("Material já cadastrado anteriormente.");
            }
        }
    }
}