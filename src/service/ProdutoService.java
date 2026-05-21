package service;

import domain.EntityInterface;
import domain.Produto;
import domain.Link;
import domain.Preco;
import infra.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;
import java.util.UUID;

public class ProdutoService implements ServiceInterface {

    @Override
    public void add(EntityInterface entity) {
        IO.println("Salvando o produto");
        Produto produto = (Produto) entity;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.persist(produto);
            tx.commit();
        }
    }

    @Override
    public void remove(EntityInterface entity) {
        IO.println("Excluindo o produto");
        Produto produto = (Produto) entity;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Produto managed = session.get(Produto.class, produto.getId());
            if (managed != null) {
                session.remove(managed);
            }
            tx.commit();
        }
    }

    @Override
    public void list() {
        List<Produto> produtos = listar();
        for (int i = 0; i < produtos.size(); i++) {
            Produto p = produtos.get(i);
            System.out.printf("\nIndice: %s\n", i);
            System.out.printf("Id: %s\n", p.getId());
            System.out.printf("SKU: %s\n", p.getSku());
            System.out.printf("Nome: %s\n", p.getNome());
            System.out.printf("Descricao: %s\n", p.getDescricao());
            System.out.printf("Marca: %s\n", p.getMarca());
            System.out.printf("Preço Base: R$ %.2f\n", p.getPreco());

            System.out.println("Links monitorados:");
            if (p.getLinks() == null || p.getLinks().isEmpty()) {
                System.out.println("  (Nenhum link cadastrado)");
            } else {
                for (Link link : p.getLinks()) {
                    System.out.printf("  - [%s]: %s\n", link.getLoja(), link.getUrl());
                }
            }

            if (p.getHistoricoDePrecos() == null || p.getHistoricoDePrecos().isEmpty()) {
                System.out.println("Menor preço atual: Sem histórico (Execute o crawler para obter preços)");
            } else {
                Preco menorPreco = null;
                for (Preco precoHist : p.getHistoricoDePrecos()) {
                    if (menorPreco == null || precoHist.getPreco() < menorPreco.getPreco()) {
                        menorPreco = precoHist;
                    }
                }
                if (menorPreco != null) {
                    System.out.printf("Menor preço atual: R$ %.2f\n", menorPreco.getPreco());
                    System.out.printf("Loja: %s\n", menorPreco.getLoja());
                    System.out.printf("Data do menor preço: %s\n", menorPreco.getDataAtual().toString());
                }

                System.out.println("Histórico de preços recente:");
                List<Preco> hist = p.getHistoricoDePrecos();
                int start = Math.max(0, hist.size() - 5);
                for (int h = start; h < hist.size(); h++) {
                    Preco ph = hist.get(h);
                    System.out.printf("  * R$ %.2f em %s na data %s\n", ph.getPreco(), ph.getLoja(), ph.getDataAtual());
                }
            }
            System.out.println("---------------------------------\n");
        }
    }

    @Override
    public EntityInterface findByIndex(int index) {
        List<Produto> produtos = listar();
        return produtos.get(index);
    }

    @Override
    public void edit(EntityInterface entity, UUID id) {
        IO.println("editando o produto");
        Produto atualizado = (Produto) entity;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Produto managed = session.get(Produto.class, id);
            if (managed != null) {
                managed.setSku(atualizado.getSku());
                managed.setNome(atualizado.getNome());
                managed.setMarca(atualizado.getMarca());
                managed.setDescricao(atualizado.getDescricao());
                managed.setPreco(atualizado.getPreco());

                // Atualizar links associados
                managed.getLinks().clear();
                if (atualizado.getLinks() != null) {
                    for (Link link : atualizado.getLinks()) {
                        Link newLink = new Link(link.getLoja(), link.getUrl(), managed);
                        managed.getLinks().add(newLink);
                    }
                }
            }
            tx.commit();
        }
    }

    public void exibirHistoricoCompleto(Produto produto) {
        System.out.println("\n========================================");
        System.out.printf("HISTÓRICO DE PREÇOS COMPLETO: %s\n", produto.getNome());
        System.out.println("========================================");
        System.out.printf("SKU: %s | Marca: %s | Preço Base: R$ %.2f\n\n", 
                produto.getSku(), produto.getMarca(), produto.getPreco());

        List<Preco> historico = produto.getHistoricoDePrecos();
        if (historico == null || historico.isEmpty()) {
            System.out.println("Nenhum preço registrado no histórico ainda.");
            System.out.println("Execute o Crawler para começar a monitorar.");
            System.out.println("========================================\n");
            return;
        }

        System.out.printf("%-20s | %-15s | %-12s\n", "Data/Hora", "Loja", "Preço");
        System.out.println("----------------------------------------------------");

        float menor = Float.MAX_VALUE;
        String lojaMenor = "";
        float maior = Float.MIN_VALUE;
        String lojaMaior = "";
        float soma = 0;

        for (Preco p : historico) {
            String dataFmt = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(p.getDataAtual());
            System.out.printf("%-20s | %-15s | R$ %-10.2f\n", 
                    dataFmt, 
                    p.getLoja(), 
                    p.getPreco());
            
            if (p.getPreco() < menor) {
                menor = p.getPreco();
                lojaMenor = p.getLoja();
            }
            if (p.getPreco() > maior) {
                maior = p.getPreco();
                lojaMaior = p.getLoja();
            }
            soma += p.getPreco();
        }

        System.out.println("----------------------------------------------------");
        System.out.printf("Menor Preço Registrado: R$ %.2f na loja %s\n", menor, lojaMenor);
        System.out.printf("Maior Preço Registrado: R$ %.2f na loja %s\n", maior, lojaMaior);
        System.out.printf("Média de Preços: R$ %.2f\n", soma / historico.size());
        System.out.println("========================================\n");
    }

    private List<Produto> listar() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Produto> list = session.createQuery("from Produto order by nome", Produto.class)
                    .getResultList();
            for (Produto p : list) {
                org.hibernate.Hibernate.initialize(p.getLinks());
                org.hibernate.Hibernate.initialize(p.getHistoricoDePrecos());
            }
            return list;
        }
    }
}
