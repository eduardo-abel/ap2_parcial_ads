package service;

import domain.Link;
import domain.Preco;
import domain.Produto;
import infra.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Date;
import java.util.List;

public class CrawlerService {

    private final ProdutoService produtoService;

    public CrawlerService() {
        this.produtoService = new ProdutoService();
    }

    public CrawlerService(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    public void executar() {
        System.out.println("\n========================================");
        System.out.println("INICIANDO A EXECUÇÃO DO CRAWLER DE PREÇOS");
        System.out.println("========================================");

        List<Produto> produtos = carregarProdutos();
        if (produtos.isEmpty()) {
            System.out.println("Nenhum produto cadastrado no sistema.");
            return;
        }

        int produtosProcessados = 0;
        int precosRegistrados = 0;

        for (Produto produto : produtos) {
            System.out.printf("\nProcessando produto: %s (SKU: %s)\n", produto.getNome(), produto.getSku());

            List<Link> links = produto.getLinks();
            if (links == null || links.isEmpty()) {
                System.out.println("  [Aviso] Este produto não possui links de lojas cadastrados. Ignorando.");
                continue;
            }

            if (links.size() < 2) {
                System.out.printf("  [Aviso] O produto possui apenas %d link(s). Recomendado: pelo menos 2.\n", links.size());
            }

            float menorPreco = Float.MAX_VALUE;
            String lojaMenorPreco = "";
            Link linkMenorPreco = null;

            for (Link link : links) {
                System.out.printf("  - Acessando loja '%s' via URL: %s...\n", link.getLoja(), link.getUrl());
                float precoEncontrado = buscarPreco(link, produto.getPreco());
                System.out.printf("    -> Preço encontrado: R$ %.2f\n", precoEncontrado);

                if (precoEncontrado < menorPreco) {
                    menorPreco = precoEncontrado;
                    lojaMenorPreco = link.getLoja();
                    linkMenorPreco = link;
                }
            }

            if (linkMenorPreco != null) {
                System.out.printf("  [Resultado] Menor preço encontrado: R$ %.2f na loja '%s'\n", menorPreco, lojaMenorPreco);
                salvarHistoricoPreco(produto, menorPreco, lojaMenorPreco);
                precosRegistrados++;
            }
            produtosProcessados++;
        }

        System.out.println("\n========================================");
        System.out.printf("CRAWLER FINALIZADO: %d produto(s) processado(s), %d histórico(s) salvo(s).\n", 
                produtosProcessados, precosRegistrados);
        System.out.println("========================================\n");
    }

    private List<Produto> carregarProdutos() {
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

    private float buscarPreco(Link link, float precoBase) {
        String url = link.getUrl();
        
        // Simulação de preço determinístico na URL (ex: ?price=3699.00 ou &price=3699)
        if (url.contains("price=")) {
            try {
                int index = url.indexOf("price=");
                String sub = url.substring(index + 6);
                int ampersandIndex = sub.indexOf("&");
                if (ampersandIndex != -1) {
                    sub = sub.substring(0, ampersandIndex);
                }
                return Float.parseFloat(sub.trim());
            } catch (Exception e) {
                // Em caso de erro na conversão, prossegue para a simulação dinâmica
            }
        }

        // Simulação dinâmica: gera um preço variando entre -15% e +15% do preço base do produto
        double percentualVariacao = 0.85 + (Math.random() * 0.30); // 0.85 a 1.15
        float precoSimulado = (float) (precoBase * percentualVariacao);
        
        // Arredonda para 2 casas decimais
        return Math.round(precoSimulado * 100.0f) / 100.0f;
    }

    private void salvarHistoricoPreco(Produto produto, float valor, String loja) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Produto managed = session.get(Produto.class, produto.getId());
            if (managed != null) {
                Preco historico = new Preco(new Date(), valor, managed, loja);
                managed.getHistoricoDePrecos().add(historico);
                session.persist(historico);
            }
            tx.commit();
        } catch (Exception e) {
            System.err.println("Erro ao salvar histórico de preço para " + produto.getNome() + ": " + e.getMessage());
        }
    }
}
