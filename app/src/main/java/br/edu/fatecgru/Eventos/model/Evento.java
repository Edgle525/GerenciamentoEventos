package br.edu.fatecgru.Eventos.model;

import java.util.List;

/**
 * Representa a estrutura de dados de um evento.
 * Esta classe (POJO - Plain Old Java Object) serve como um modelo para armazenar
 * e transportar as informações de um evento através do aplicativo.
 */
public class Evento {

    private String id;
    private String nome;
    private String data;
    private String horario;
    private String dataTermino;
    private String horarioTermino;
    private String local;
    private String descricao;
    private int tempoMinimo;
    private List<String> cursosPermitidos;

    /**
     * Construtor vazio necessário para o Firebase Firestore.
     */
    public Evento() {
    }

    // --- Getters e Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getDataTermino() {
        return dataTermino;
    }

    public void setDataTermino(String dataTermino) {
        this.dataTermino = dataTermino;
    }

    public String getHorarioTermino() {
        return horarioTermino;
    }

    public void setHorarioTermino(String horarioTermino) {
        this.horarioTermino = horarioTermino;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public int getTempoMinimo() {
        return tempoMinimo;
    }

    public void setTempoMinimo(int tempoMinimo) {
        this.tempoMinimo = tempoMinimo;
    }

    public List<String> getCursosPermitidos() {
        return cursosPermitidos;
    }

    public void setCursosPermitidos(List<String> cursosPermitidos) {
        this.cursosPermitidos = cursosPermitidos;
    }
}
