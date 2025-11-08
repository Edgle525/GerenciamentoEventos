package br.edu.fatecgru.Eventos.model;

/**
 * Representa a estrutura de dados de um evento.
 * Esta classe (POJO - Plain Old Java Object) serve como um modelo para armazenar
 * e transportar as informações de um evento através do aplicativo.
 */
public class Evento {

    private String id;
    private String nome;
    private String data;
    private String dia;
    private String horario;
    private String descricao;

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

    public String getDia() {
        return dia;
    }

    public void setDia(String dia) {
        this.dia = dia;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
}
