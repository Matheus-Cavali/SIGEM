package org.example.model;

import java.time.LocalDate;

public class DoacaoMaterial extends Doacao {
    private Integer materialId;
    private int quantidade;

    public DoacaoMaterial() {
        super();
    }

    public DoacaoMaterial(Integer id, LocalDate data, Integer colaboradorId, Integer materialId, int quantidade) {
        super(id, data, colaboradorId);
        this.materialId = materialId;
        this.quantidade = quantidade;
    }

    public DoacaoMaterial(LocalDate data, Integer colaboradorId, Integer materialId, int quantidade) {
        super(data, colaboradorId);
        this.materialId = materialId;
        this.quantidade = quantidade;
    }

    public Integer getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Integer materialId) {
        this.materialId = materialId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}